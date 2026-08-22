package nwd.fokuslauncher.data.search

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Telephony
import androidx.core.content.ContextCompat

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

    private fun queryMediaUri(
        context: Context,
        uri: Uri,
        query: String,
        limit: Int
    ): List<FileSearchResult> {
        val trimmed = query.trim()
        if (trimmed.length < 2) return emptyList()

        val results = mutableListOf<FileSearchResult>()
        val resolver: ContentResolver = context.contentResolver

        try {
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.TITLE
            )
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ? OR ${MediaStore.MediaColumns.TITLE} LIKE ?"
            val selectionArgs = arrayOf("%$trimmed%", "%$trimmed%")
            val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC LIMIT $limit"

            resolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val pathCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val mimeCol = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                val titleCol = cursor.getColumnIndex(MediaStore.MediaColumns.TITLE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: (if (titleCol >= 0) cursor.getString(titleCol) else "File") ?: "File"
                    val path = if (pathCol >= 0) cursor.getString(pathCol) ?: "" else ""
                    val size = cursor.getLong(sizeCol)
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) else null
                    val fileUri = Uri.withAppendedPath(uri, id.toString())

                    results.add(FileSearchResult(id, name, path, size, fileUri, mime))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("LocalSearchManager", "Error searching uri $uri", e)
        }

        return results
    }

    fun searchFiles(context: Context, query: String, limit: Int = 10): List<FileSearchResult> {
        val results = mutableListOf<FileSearchResult>()
        val uris = listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Files.getContentUri("external")
        )
        
        for (uri in uris) {
            if (results.size >= limit) break
            val r = queryMediaUri(context, uri, query, limit)
            for (item in r) {
                if (results.none { it.path == item.path }) {
                    results.add(item)
                }
            }
        }
        
        return results.take(limit)
    }

    fun searchContacts(context: Context, query: String, limit: Int = 8): List<ContactSearchResult> {
        val trimmed = query.trim()
        if (trimmed.length < 2) return emptyList()

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }

        val results = mutableListOf<ContactSearchResult>()
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

            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
                val nameCol = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                val hasPhoneCol = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                val photoCol = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)

                while (cursor.moveToNext()) {
                    val id = cursor.getString(idCol)
                    val name = cursor.getString(nameCol) ?: "Contact"
                    val hasPhone = cursor.getInt(hasPhoneCol) > 0
                    val photoStr = if (photoCol >= 0) cursor.getString(photoCol) else null
                    val photoUri = photoStr?.let { Uri.parse(it) }

                    var phone: String? = null
                    if (hasPhone) {
                        phone = getPrimaryPhone(context, id)
                    }

                    results.add(ContactSearchResult(id, name, phone, photoUri))
                }
            }
        } catch (e: Exception) {
            // Permission or Contacts error handled gracefully
        }

        return results
    }

    private fun getPrimaryPhone(context: Context, contactId: String): String? {
        try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(contactId),
                null
            )?.use { pCursor ->
                if (pCursor.moveToFirst()) {
                    return pCursor.getString(0)
                }
            }
        } catch (e: Exception) { }
        return null
    }

    fun searchCallLogs(context: Context, query: String, limit: Int = 5): List<CallLogSearchResult> {
        val trimmed = query.trim()
        if (trimmed.length < 2) return emptyList()

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
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

            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
                val numberCol = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val nameCol = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val dateCol = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                val typeCol = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getString(idCol)
                    val number = cursor.getString(numberCol) ?: ""
                    val name = if (nameCol >= 0) cursor.getString(nameCol) else null
                    val date = cursor.getLong(dateCol)
                    val type = cursor.getInt(typeCol)

                    results.add(CallLogSearchResult(id, number, name, date, type))
                }
            }
        } catch (e: Exception) { }

        return results
    }

    fun searchMessages(context: Context, query: String, limit: Int = 5): List<MessageSearchResult> {
        val trimmed = query.trim()
        if (trimmed.length < 2) return emptyList()

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
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

            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
                val addressCol = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyCol = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateCol = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

                while (cursor.moveToNext()) {
                    val id = cursor.getString(idCol)
                    val address = cursor.getString(addressCol) ?: ""
                    val body = cursor.getString(bodyCol) ?: ""
                    val date = cursor.getLong(dateCol)

                    results.add(MessageSearchResult(id, address, body, date))
                }
            }
        } catch (e: Exception) { }

        return results
    }

    fun searchCalendar(context: Context, query: String, limit: Int = 5): List<CalendarSearchResult> {
        val trimmed = query.trim()
        if (trimmed.length < 2) return emptyList()

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
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

            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)
                val titleCol = cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
                val descCol = cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION)
                val startCol = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
                val locCol = cursor.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Event"
                    val desc = if (descCol >= 0) cursor.getString(descCol) else null
                    val start = cursor.getLong(startCol)
                    val loc = if (locCol >= 0) cursor.getString(locCol) else null

                    results.add(CalendarSearchResult(id, title, desc, start, loc))
                }
            }
        } catch (e: Exception) { }

        return results
    }
}
