package nwd.fokuslauncher.data.search

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

enum class ProviderType {
    MEDIA_IMAGES,
    MEDIA_VIDEO,
    MEDIA_AUDIO,
    DOCUMENTS,
    CONTACTS,
    CALL_LOGS,
    SMS_MESSAGES,
    CALENDAR
}

enum class QueryStage {
    PERMISSION_CHECK,
    RESOLVER_QUERY,
    CURSOR_ITERATION,
    BATCH_FETCH,
    MAP_RESULTS
}

sealed class ProviderQueryResult<out T> {
    data class Success<T>(val data: List<T>) : ProviderQueryResult<T>()
    object PermissionRequired : ProviderQueryResult<Nothing>()
    object NoResults : ProviderQueryResult<Nothing>()
    data class ProviderFailure(val exceptionType: String, val message: String?) : ProviderQueryResult<Nothing>()

    fun getOrEmpty(): List<T> = when (this) {
        is Success -> data
        else -> emptyList()
    }
}

private fun logProviderDebug(
    providerType: ProviderType,
    uri: Uri?,
    stage: QueryStage,
    throwable: Throwable?
) {
    if (android.util.Log.isLoggable("LocalSearch", android.util.Log.DEBUG)) {
        val uriStr = uri?.let { "${it.scheme}://${it.authority}${it.path ?: ""}" } ?: "none"
        val exType = throwable?.javaClass?.simpleName ?: "None"
        val exMsg = throwable?.message ?: "None"
        android.util.Log.d(
            "LocalSearch",
            "Provider: $providerType | Stage: $stage | URI: $uriStr | Exception: $exType($exMsg)"
        )
    }
}

enum class MediaType {
    IMAGE,
    VIDEO,
    AUDIO
}

data class MediaSearchResult(
    val id: Long,
    val displayName: String,
    val uri: Uri,
    val mimeType: String?,
    val sizeBytes: Long,
    val mediaType: MediaType
)

data class FileSearchResult(
    val id: Long,
    val displayName: String,
    val path: String,
    val sizeBytes: Long,
    val uri: Uri,
    val mimeType: String?
)

data class ContactSearchResult(
    val id: String,
    val displayName: String,
    val phoneNumber: String?,
    val photoUri: Uri?
)

data class CallLogSearchResult(
    val id: String,
    val number: String,
    val cachedName: String?,
    val dateMs: Long,
    val type: Int
)

data class MessageSearchResult(
    val id: String,
    val address: String,
    val body: String,
    val dateMs: Long
)

data class CalendarSearchResult(
    val id: Long,
    val title: String,
    val description: String?,
    val startTimeMs: Long,
    val location: String?
)

object LocalSearchManager {

    fun hasVisualMediaPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasImagesPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasVideoPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasAudioPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getRequiredMediaPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    internal fun queryMediaCollection(
        context: Context,
        collectionUri: Uri,
        mediaType: MediaType,
        query: String,
        limit: Int
    ): ProviderQueryResult<MediaSearchResult> {
        val trimmed = query.trim()
        if (trimmed.length < 2) return ProviderQueryResult.NoResults

        val providerType = when (mediaType) {
            MediaType.IMAGE -> ProviderType.MEDIA_IMAGES
            MediaType.VIDEO -> ProviderType.MEDIA_VIDEO
            MediaType.AUDIO -> ProviderType.MEDIA_AUDIO
        }

        val results = mutableListOf<MediaSearchResult>()
        val resolver: ContentResolver = context.contentResolver

        try {
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.TITLE
            )
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ? OR ${MediaStore.MediaColumns.TITLE} LIKE ?"
            val selectionArgs = arrayOf("%$trimmed%", "%$trimmed%")
            val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC LIMIT $limit"

            val cursor = try {
                resolver.query(
                    collectionUri,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )
            } catch (e: SecurityException) {
                logProviderDebug(providerType, collectionUri, QueryStage.PERMISSION_CHECK, e)
                return ProviderQueryResult.PermissionRequired
            } catch (e: Exception) {
                logProviderDebug(providerType, collectionUri, QueryStage.RESOLVER_QUERY, e)
                return ProviderQueryResult.ProviderFailure(e.javaClass.simpleName, e.message)
            }

            cursor?.use { c ->
                try {
                    val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val sizeCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                    val mimeCol = c.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                    val titleCol = c.getColumnIndex(MediaStore.MediaColumns.TITLE)

                    while (c.moveToNext()) {
                        val id = c.getLong(idCol)
                        val name = c.getString(nameCol) ?: (if (titleCol >= 0) c.getString(titleCol) else "Media") ?: "Media"
                        val size = c.getLong(sizeCol)
                        val mime = if (mimeCol >= 0) c.getString(mimeCol) else null
                        val fileUri = Uri.withAppendedPath(collectionUri, id.toString())

                        results.add(
                            MediaSearchResult(
                                id = id,
                                displayName = name,
                                uri = fileUri,
                                mimeType = mime,
                                sizeBytes = size,
                                mediaType = mediaType
                            )
                        )
                    }
                } catch (e: Exception) {
                    logProviderDebug(providerType, collectionUri, QueryStage.CURSOR_ITERATION, e)
                    return ProviderQueryResult.ProviderFailure(e.javaClass.simpleName, e.message)
                }
            }
        } catch (e: SecurityException) {
            logProviderDebug(providerType, collectionUri, QueryStage.PERMISSION_CHECK, e)
            return ProviderQueryResult.PermissionRequired
        } catch (e: Exception) {
            logProviderDebug(providerType, collectionUri, QueryStage.MAP_RESULTS, e)
            return ProviderQueryResult.ProviderFailure(e.javaClass.simpleName, e.message)
        }

        return if (results.isEmpty()) ProviderQueryResult.NoResults else ProviderQueryResult.Success(results)
    }

    suspend fun searchMediaResult(context: Context, query: String, limit: Int = 10): ProviderQueryResult<MediaSearchResult> = coroutineScope {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@coroutineScope ProviderQueryResult.NoResults

        val canReadImages = hasImagesPermission(context)
        val canReadVideo = hasVideoPermission(context)
        val canReadAudio = hasAudioPermission(context)

        if (!canReadImages && !canReadVideo && !canReadAudio) {
            logProviderDebug(ProviderType.MEDIA_IMAGES, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, QueryStage.PERMISSION_CHECK, null)
            return@coroutineScope ProviderQueryResult.PermissionRequired
        }

        val queries = mutableListOf<Pair<Uri, MediaType>>()
        if (canReadImages) {
            queries.add(MediaStore.Images.Media.EXTERNAL_CONTENT_URI to MediaType.IMAGE)
        }
        if (canReadVideo) {
            queries.add(MediaStore.Video.Media.EXTERNAL_CONTENT_URI to MediaType.VIDEO)
        }
        if (canReadAudio) {
            queries.add(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI to MediaType.AUDIO)
        }

        val queryJobs = queries.map { (uri, type) ->
            async {
                queryMediaCollection(context, uri, type, query, limit)
            }
        }

        val allResults = queryJobs.awaitAll()
        val combined = mutableListOf<MediaSearchResult>()
        var hadFailure: ProviderQueryResult.ProviderFailure? = null

        for (res in allResults) {
            when (res) {
                is ProviderQueryResult.Success -> {
                    for (item in res.data) {
                        if (combined.size >= limit) break
                        if (combined.none { it.id == item.id && it.mediaType == item.mediaType }) {
                            combined.add(item)
                        }
                    }
                }
                is ProviderQueryResult.ProviderFailure -> hadFailure = res
                else -> Unit
            }
            if (combined.size >= limit) break
        }

        when {
            combined.isNotEmpty() -> ProviderQueryResult.Success(combined.take(limit))
            hadFailure != null -> hadFailure
            else -> ProviderQueryResult.NoResults
        }
    }

    suspend fun searchMedia(context: Context, query: String, limit: Int = 10): List<MediaSearchResult> {
        return searchMediaResult(context, query, limit).getOrEmpty()
    }

    suspend fun searchFiles(context: Context, query: String, limit: Int = 10): List<FileSearchResult> {
        val media = searchMedia(context, query, limit)
        return media.map {
            FileSearchResult(
                id = it.id,
                displayName = it.displayName,
                path = it.uri.toString(),
                sizeBytes = it.sizeBytes,
                uri = it.uri,
                mimeType = it.mimeType
            )
        }
    }

    fun searchContactsResult(context: Context, query: String, limit: Int = 8): ProviderQueryResult<ContactSearchResult> {
        val trimmed = query.trim()
        if (trimmed.length < 2) return ProviderQueryResult.NoResults

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            logProviderDebug(ProviderType.CONTACTS, ContactsContract.Contacts.CONTENT_URI, QueryStage.PERMISSION_CHECK, null)
            return ProviderQueryResult.PermissionRequired
        }

        data class TempContact(
            val id: String,
            val name: String,
            val hasPhone: Boolean,
            val photoUri: Uri?
        )

        val tempContacts = mutableListOf<TempContact>()
        val seenContactIds = mutableSetOf<String>()

        try {
            val projection = arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                ContactsContract.Contacts.HAS_PHONE_NUMBER,
                ContactsContract.Contacts.PHOTO_THUMBNAIL_URI
            )
            val selection = "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} LIKE ?"
            val selectionArgs = arrayOf("%$trimmed%")
            val sortOrder = "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC LIMIT $limit"

            val cursor = try {
                context.contentResolver.query(
                    ContactsContract.Contacts.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )
            } catch (e: SecurityException) {
                logProviderDebug(ProviderType.CONTACTS, ContactsContract.Contacts.CONTENT_URI, QueryStage.PERMISSION_CHECK, e)
                return ProviderQueryResult.PermissionRequired
            } catch (e: Exception) {
                logProviderDebug(ProviderType.CONTACTS, ContactsContract.Contacts.CONTENT_URI, QueryStage.RESOLVER_QUERY, e)
                return ProviderQueryResult.ProviderFailure(e.javaClass.simpleName, e.message)
            }

            cursor?.use { c ->
                try {
                    val idCol = c.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
                    val nameCol = c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                    val hasPhoneCol = c.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                    val photoCol = c.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)

                    while (c.moveToNext()) {
                        val id = c.getString(idCol)
                        if (id != null && seenContactIds.add(id)) {
                            val name = c.getString(nameCol) ?: "Contact"
                            val hasPhone = c.getInt(hasPhoneCol) > 0
                            val photoStr = if (photoCol >= 0) c.getString(photoCol) else null
                            val photoUri = photoStr?.let { Uri.parse(it) }
                            tempContacts.add(TempContact(id, name, hasPhone, photoUri))
                        }
                    }
                } catch (e: Exception) {
                    logProviderDebug(ProviderType.CONTACTS, ContactsContract.Contacts.CONTENT_URI, QueryStage.CURSOR_ITERATION, e)
                    return ProviderQueryResult.ProviderFailure(e.javaClass.simpleName, e.message)
                }
            }

            // Also search by phone number if query contains digits and limit not yet reached
            if (trimmed.any { it.isDigit() } && tempContacts.size < limit) {
                val phoneRemainingLimit = limit - tempContacts.size
                val phoneProjection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
                )
                val phoneSelection = "${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
                val phoneArgs = arrayOf("%$trimmed%")
                val phoneOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} ASC LIMIT $phoneRemainingLimit"

                try {
                    context.contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        phoneProjection,
                        phoneSelection,
                        phoneArgs,
                        phoneOrder
                    )?.use { phoneCursor ->
                        val idCol = phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                        val nameCol = phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
                        val photoCol = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

                        while (phoneCursor.moveToNext()) {
                            val id = phoneCursor.getString(idCol)
                            if (id != null && seenContactIds.add(id)) {
                                val name = phoneCursor.getString(nameCol) ?: "Contact"
                                val photoStr = if (photoCol >= 0) phoneCursor.getString(photoCol) else null
                                val photoUri = photoStr?.let { Uri.parse(it) }
                                tempContacts.add(TempContact(id, name, true, photoUri))
                            }
                        }
                    }
                } catch (e: Exception) {
                    logProviderDebug(ProviderType.CONTACTS, ContactsContract.CommonDataKinds.Phone.CONTENT_URI, QueryStage.RESOLVER_QUERY, e)
                }
            }

            // Batch fetch phone numbers in a single query across all matched contacts
            val contactIdsWithPhone = tempContacts.filter { it.hasPhone }.map { it.id }
            val phoneMap = mutableMapOf<String, String>()

            if (contactIdsWithPhone.isNotEmpty()) {
                val placeholders = contactIdsWithPhone.joinToString(",") { "?" }
                val phoneBatchProjection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.IS_PRIMARY
                )
                val phoneBatchSelection = "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} IN ($placeholders)"
                val phoneBatchArgs = contactIdsWithPhone.toTypedArray()
                val phoneBatchOrder = "${ContactsContract.CommonDataKinds.Phone.IS_PRIMARY} DESC"

                try {
                    context.contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        phoneBatchProjection,
                        phoneBatchSelection,
                        phoneBatchArgs,
                        phoneBatchOrder
                    )?.use { pCursor ->
                        val cIdCol = pCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                        val numCol = pCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        while (pCursor.moveToNext()) {
                            val cId = pCursor.getString(cIdCol)
                            val num = pCursor.getString(numCol)
                            if (cId != null && num != null && !phoneMap.containsKey(cId)) {
                                phoneMap[cId] = num
                            }
                        }
                    }
                } catch (e: Exception) {
                    logProviderDebug(ProviderType.CONTACTS, ContactsContract.CommonDataKinds.Phone.CONTENT_URI, QueryStage.BATCH_FETCH, e)
                }
            }

            val mapped = tempContacts.map { contact ->
                ContactSearchResult(
                    id = contact.id,
                    displayName = contact.name,
                    phoneNumber = phoneMap[contact.id],
                    photoUri = contact.photoUri
                )
            }
            return if (mapped.isEmpty()) ProviderQueryResult.NoResults else ProviderQueryResult.Success(mapped)
        } catch (e: SecurityException) {
            logProviderDebug(ProviderType.CONTACTS, ContactsContract.Contacts.CONTENT_URI, QueryStage.PERMISSION_CHECK, e)
            return ProviderQueryResult.PermissionRequired
        } catch (e: Exception) {
            logProviderDebug(ProviderType.CONTACTS, ContactsContract.Contacts.CONTENT_URI, QueryStage.MAP_RESULTS, e)
            return ProviderQueryResult.ProviderFailure(e.javaClass.simpleName, e.message)
        }
    }

    fun searchContacts(context: Context, query: String, limit: Int = 8): List<ContactSearchResult> {
        return searchContactsResult(context, query, limit).getOrEmpty()
    }

    fun searchCallLogsResult(context: Context, query: String, limit: Int = 5): ProviderQueryResult<CallLogSearchResult> {
        val trimmed = query.trim()
        if (trimmed.length < 2) return ProviderQueryResult.NoResults

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            logProviderDebug(ProviderType.CALL_LOGS, CallLog.Calls.CONTENT_URI, QueryStage.PERMISSION_CHECK, null)
            return ProviderQueryResult.PermissionRequired
        }

        val results = mutableListOf<CallLogSearchResult>()
        try {
            val projection = arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.DATE,
                CallLog.Calls.TYPE
            )
            val selection = "${CallLog.Calls.NUMBER} LIKE ? OR ${CallLog.Calls.CACHED_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$trimmed%", "%$trimmed%")
            val sortOrder = "${CallLog.Calls.DATE} DESC LIMIT $limit"

            val cursor = try {
                context.contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )
            } catch (e: SecurityException) {
                logProviderDebug(ProviderType.CALL_LOGS, CallLog.Calls.CONTENT_URI, QueryStage.PERMISSION_CHECK, e)
                return ProviderQueryResult.PermissionRequired
            } catch (e: Exception) {
                logProviderDebug(ProviderType.CALL_LOGS, CallLog.Calls.CONTENT_URI, QueryStage.RESOLVER_QUERY, e)
                return ProviderQueryResult.ProviderFailure(e.javaClass.simpleName, e.message)
            }

            cursor?.use { c ->
                try {
                    val idCol = c.getColumnIndexOrThrow(CallLog.Calls._ID)
                    val numberCol = c.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                    val nameCol = c.getColumnIndex(CallLog.Calls.CACHED_NAME)
                    val dateCol = c.getColumnIndexOrThrow(CallLog.Calls.DATE)
                    val typeCol = c.getColumnIndexOrThrow(CallLog.Calls.TYPE)

                    while (c.moveToNext()) {
                        val id = c.getString(idCol)
                        val number = c.getString(numberCol) ?: ""
                        val name = if (nameCol >= 0) c.getString(nameCol) else null
                        val date = c.getLong(dateCol)
                        val type = c.getInt(typeCol)

                        results.add(CallLogSearchResult(id, number, name, date, type))
                    }
                } catch (e: Exception) {
                    logProviderDebug(ProviderType.CALL_LOGS, CallLog.Calls.CONTENT_URI, QueryStage.CURSOR_ITERATION, e)
                    return ProviderQueryResult.ProviderFailure(e.javaClass.simpleName, e.message)
                }
            }
        } catch (e: SecurityException) {
            logProviderDebug(ProviderType.CALL_LOGS, CallLog.Calls.CONTENT_URI, QueryStage.PERMISSION_CHECK, e)
            return ProviderQueryResult.PermissionRequired
        } catch (e: Exception) {
            logProviderDebug(ProviderType.CALL_LOGS, CallLog.Calls.CONTENT_URI, QueryStage.MAP_RESULTS, e)
            return ProviderQueryResult.ProviderFailure(e.javaClass.simpleName, e.message)
        }

        return if (results.isEmpty()) ProviderQueryResult.NoResults else ProviderQueryResult.Success(results)
    }

    fun searchCallLogs(context: Context, query: String, limit: Int = 5): List<CallLogSearchResult> {
        return searchCallLogsResult(context, query, limit).getOrEmpty()
    }

    fun searchMessagesResult(context: Context, query: String, limit: Int = 5): ProviderQueryResult<MessageSearchResult> {
        val trimmed = query.trim()
        if (trimmed.length < 2) return ProviderQueryResult.NoResults

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            logProviderDebug(ProviderType.SMS_MESSAGES, Telephony.Sms.CONTENT_URI, QueryStage.PERMISSION_CHECK, null)
            return ProviderQueryResult.PermissionRequired
        }

        val results = mutableListOf<MessageSearchResult>()
        try {
            val projection = arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE
            )
            val selection = "${Telephony.Sms.BODY} LIKE ? OR ${Telephony.Sms.ADDRESS} LIKE ?"
            val selectionArgs = arrayOf("%$trimmed%", "%$trimmed%")
            val sortOrder = "${Telephony.Sms.DATE} DESC LIMIT $limit"

            val cursor = try {
                context.contentResolver.query(
                    Telephony.Sms.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )
            } catch (e: SecurityException) {
                logProviderDebug(ProviderType.SMS_MESSAGES, Telephony.Sms.CONTENT_URI, QueryStage.PERMISSION_CHECK, e)
                return ProviderQueryResult.PermissionRequired
            } catch (e: Exception) {
                logProviderDebug(ProviderType.SMS_MESSAGES, Telephony.Sms.CONTENT_URI, QueryStage.RESOLVER_QUERY, e)
                return ProviderQueryResult.ProviderFailure(e.javaClass.simpleName, e.message)
            }

            cursor?.use { c ->
                try {
                    val idCol = c.getColumnIndexOrThrow(Telephony.Sms._ID)
                    val addressCol = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                    val bodyCol = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
                    val dateCol = c.getColumnIndexOrThrow(Telephony.Sms.DATE)

                    while (c.moveToNext()) {
                        val id = c.getString(idCol)
                        val address = c.getString(addressCol) ?: ""
                        val body = c.getString(bodyCol) ?: ""
                        val date = c.getLong(dateCol)

                        results.add(MessageSearchResult(id, address, body, date))
                    }
                } catch (e: Exception) {
                    logProviderDebug(ProviderType.SMS_MESSAGES, Telephony.Sms.CONTENT_URI, QueryStage.CURSOR_ITERATION, e)
                    return ProviderQueryResult.ProviderFailure(e.javaClass.simpleName, e.message)
                }
            }
        } catch (e: SecurityException) {
            logProviderDebug(ProviderType.SMS_MESSAGES, Telephony.Sms.CONTENT_URI, QueryStage.PERMISSION_CHECK, e)
            return ProviderQueryResult.PermissionRequired
        } catch (e: Exception) {
            logProviderDebug(ProviderType.SMS_MESSAGES, Telephony.Sms.CONTENT_URI, QueryStage.MAP_RESULTS, e)
            return ProviderQueryResult.ProviderFailure(e.javaClass.simpleName, e.message)
        }

        return if (results.isEmpty()) ProviderQueryResult.NoResults else ProviderQueryResult.Success(results)
    }

    fun searchMessages(context: Context, query: String, limit: Int = 5): List<MessageSearchResult> {
        return searchMessagesResult(context, query, limit).getOrEmpty()
    }

    fun searchCalendarResult(context: Context, query: String, limit: Int = 5): ProviderQueryResult<CalendarSearchResult> {
        val trimmed = query.trim()
        if (trimmed.length < 2) return ProviderQueryResult.NoResults

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            logProviderDebug(ProviderType.CALENDAR, CalendarContract.Events.CONTENT_URI, QueryStage.PERMISSION_CHECK, null)
            return ProviderQueryResult.PermissionRequired
        }

        val results = mutableListOf<CalendarSearchResult>()
        try {
            val projection = arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.EVENT_LOCATION
            )
            val selection = "${CalendarContract.Events.TITLE} LIKE ? OR ${CalendarContract.Events.DESCRIPTION} LIKE ?"
            val selectionArgs = arrayOf("%$trimmed%", "%$trimmed%")
            val sortOrder = "${CalendarContract.Events.DTSTART} DESC LIMIT $limit"

            val cursor = try {
                context.contentResolver.query(
                    CalendarContract.Events.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )
            } catch (e: SecurityException) {
                logProviderDebug(ProviderType.CALENDAR, CalendarContract.Events.CONTENT_URI, QueryStage.PERMISSION_CHECK, e)
                return ProviderQueryResult.PermissionRequired
            } catch (e: Exception) {
                logProviderDebug(ProviderType.CALENDAR, CalendarContract.Events.CONTENT_URI, QueryStage.RESOLVER_QUERY, e)
                return ProviderQueryResult.ProviderFailure(e.javaClass.simpleName, e.message)
            }

            cursor?.use { c ->
                try {
                    val idCol = c.getColumnIndexOrThrow(CalendarContract.Events._ID)
                    val titleCol = c.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
                    val descCol = c.getColumnIndex(CalendarContract.Events.DESCRIPTION)
                    val startCol = c.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
                    val locCol = c.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)

                    while (c.moveToNext()) {
                        val id = c.getLong(idCol)
                        val title = c.getString(titleCol) ?: "Event"
                        val desc = if (descCol >= 0) c.getString(descCol) else null
                        val start = c.getLong(startCol)
                        val loc = if (locCol >= 0) c.getString(locCol) else null

                        results.add(CalendarSearchResult(id, title, desc, start, loc))
                    }
                } catch (e: Exception) {
                    logProviderDebug(ProviderType.CALENDAR, CalendarContract.Events.CONTENT_URI, QueryStage.CURSOR_ITERATION, e)
                    return ProviderQueryResult.ProviderFailure(e.javaClass.simpleName, e.message)
                }
            }
        } catch (e: SecurityException) {
            logProviderDebug(ProviderType.CALENDAR, CalendarContract.Events.CONTENT_URI, QueryStage.PERMISSION_CHECK, e)
            return ProviderQueryResult.PermissionRequired
        } catch (e: Exception) {
            logProviderDebug(ProviderType.CALENDAR, CalendarContract.Events.CONTENT_URI, QueryStage.MAP_RESULTS, e)
            return ProviderQueryResult.ProviderFailure(e.javaClass.simpleName, e.message)
        }

        return if (results.isEmpty()) ProviderQueryResult.NoResults else ProviderQueryResult.Success(results)
    }

    fun searchCalendar(context: Context, query: String, limit: Int = 5): List<CalendarSearchResult> {
        return searchCalendarResult(context, query, limit).getOrEmpty()
    }
}
