package nwd.fokuslauncher.ui.drawer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.provider.CallLog
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import coil.size.Scale
import coil.size.Size
import android.graphics.Bitmap
import android.os.Build
import android.os.CancellationSignal
import android.util.Log
import android.util.Size as AndroidSize
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nwd.fokuslauncher.BuildConfig
import nwd.fokuslauncher.data.database.entity.IndexedFolderEntity
import nwd.fokuslauncher.data.search.*
import java.text.DateFormat
import java.util.Date

@Composable
fun AppTargetChipsList(
    query: String,
    chips: List<AppSearchChip>,
    onChipClick: (AppSearchChip) -> Unit,
    modifier: Modifier = Modifier
) {
    if (query.isBlank() || chips.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 16.dp)) {
        chips.forEach { chip ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onChipClick(chip) },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 14.dp, horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = chip.icon,
                        contentDescription = chip.label,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = if (chip.id == "browser") "Search on Web" else "Search on ${chip.label}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun InLineQuickActionCard(
    result: QuickActionResult,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (result) {
                    is QuickActionResult.MathResult -> Icons.Default.Calculate
                    is QuickActionResult.ConversionResult -> Icons.Default.SwapHoriz
                },
                contentDescription = "Calculation Result",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.input,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = result.displayResult,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Search Quick Action", result.displayResult)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Result",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

typealias MediaPermissionStatus = MediaPermissionState

fun LazyListScope.universalSearchResults(
    context: Context,
    settingsResults: List<SettingsSearchResult>,
    mediaResults: List<MediaSearchResult>,
    documentResults: List<DocumentSearchResult>,
    fileResults: List<FileSearchResult> = emptyList(),
    contactResults: List<ContactSearchResult>,
    callLogResults: List<CallLogSearchResult>,
    messageResults: List<MessageSearchResult>,
    calendarResults: List<CalendarSearchResult>,
    indexedFolders: List<IndexedFolderEntity> = emptyList(),
    totalIndexedDocuments: Int = 0,
    isIndexingDocuments: Boolean = false,
    mediaPermissionStatus: MediaPermissionState = MediaPermissionState.FULL,
    hasBroadFileAccess: Boolean = false,
    hasAnyMatches: Boolean = true,
    searchQuery: String = "",
    onRequestMediaPermission: () -> Unit = {},
    onRequestBroadFileAccess: () -> Unit = {},
    onOpenAppSettings: () -> Unit = {},
    onChooseFolder: () -> Unit = {},
    onOpenManageFolders: () -> Unit = {},
    onCloseDrawer: () -> Unit
) {
    // 0. Empty State / No Results
    if (!hasAnyMatches && searchQuery.isNotBlank()) {
        item(key = "no_results_card", contentType = "no_results") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = "No results",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No results",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "No matches found for \"$searchQuery\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }

    // 1. Settings Results
    if (settingsResults.isNotEmpty()) {
        item(key = "hdr_settings_results", contentType = "header") {
            DrawerListSectionHeader(text = "System Settings")
        }
        items(
            count = settingsResults.size,
            key = { index -> "setting_${settingsResults[index].id}" },
            contentType = { "setting_result" }
        ) { index ->
            val item = settingsResults[index]
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        try {
                            val intent = Intent(item.intentAction).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                            onCloseDrawer()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open setting", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // 2. Media Results (Scoped MediaStore: Images, Videos, Audio)
    when (mediaPermissionStatus) {
        MediaPermissionStatus.REQUIRED -> {
            item(key = "hdr_media_permission_required", contentType = "header") {
                DrawerListSectionHeader(text = "Media")
            }
            item(key = "media_permission_required_card", contentType = "permission_card") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRequestMediaPermission() }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PermMedia,
                            contentDescription = "Media access required",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Media access required",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Allow access to search photos, videos, and music",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        MediaPermissionStatus.DENIED -> {
            item(key = "hdr_media_permission_denied", contentType = "header") {
                DrawerListSectionHeader(text = "Media")
            }
            item(key = "media_permission_denied_card", contentType = "permission_card") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenAppSettings() }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PermMedia,
                            contentDescription = "Permission denied",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Permission denied",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Media permission denied. Tap to open app settings and enable access.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        MediaPermissionStatus.PARTIAL -> {
            item(key = "hdr_media_partial", contentType = "header") {
                DrawerListSectionHeader(text = "Media")
            }
            item(key = "media_partial_banner", contentType = "partial_media_banner") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Partial access",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Some media selected / partial access",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Only selected photos and videos are searchable",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = onRequestMediaPermission,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Select more", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            if (mediaResults.isNotEmpty()) {
                items(
                    count = mediaResults.size,
                    key = { index -> "media_${mediaResults[index].id}_${mediaResults[index].mediaType}" },
                    contentType = { "media_result" }
                ) { index ->
                    val media = mediaResults[index]
                    MediaResultRow(media = media, context = context, onCloseDrawer = onCloseDrawer)
                }
            }
        }
        MediaPermissionState.FULL -> {
            if (mediaResults.isNotEmpty()) {
                item(key = "hdr_media_results", contentType = "header") {
                    DrawerListSectionHeader(text = "Media")
                }
                items(
                    count = mediaResults.size,
                    key = { index -> "media_${mediaResults[index].id}_${mediaResults[index].mediaType}" },
                    contentType = { "media_result" }
                ) { index ->
                    val media = mediaResults[index]
                    MediaResultRow(media = media, context = context, onCloseDrawer = onCloseDrawer)
                }
            }
        }
    }

    // 3. Documents & Files (Storage Access Framework Index & Broad File Access)
    item(key = "hdr_documents_results", contentType = "header") {
        DrawerListSectionHeader(text = "Documents & Files")
    }

    if (!hasBroadFileAccess && indexedFolders.isEmpty()) {
        item(key = "choose_folders_cta", contentType = "choose_folders_cta") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenManageFolders() }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderSpecial,
                        contentDescription = "Choose folders or enable access",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SAF folders & broad file search",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Index SAF folders or enable broad file search access to search device documents",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = onOpenManageFolders) {
                        Text("Setup", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    } else {
        item(key = "folders_status_bar", contentType = "folders_status") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = when {
                        isIndexingDocuments -> "Indexing documents..."
                        hasBroadFileAccess && indexedFolders.isNotEmpty() -> "Broad file search & ${indexedFolders.size} SAF folders"
                        hasBroadFileAccess -> "Broad file search access enabled"
                        else -> "$totalIndexedDocuments documents indexed"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onChooseFolder,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Folder",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Folder", style = MaterialTheme.typography.labelMedium)
                    }
                    TextButton(
                        onClick = onOpenManageFolders,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Manage", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        if (documentResults.isNotEmpty()) {
            items(
                count = documentResults.size,
                key = { index -> "doc_${documentResults[index].id}" },
                contentType = { "doc_result" }
            ) { index ->
                val doc = documentResults[index]
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(doc.uri, doc.mimeType)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                                onCloseDrawer()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open document", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = getDocumentIcon(doc.mimeType, doc.displayName),
                            contentDescription = doc.displayName,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = doc.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = buildString {
                                    append(formatFileSize(doc.sizeBytes))
                                    if (doc.lastModified > 0) {
                                        append(" • ")
                                        append(DateFormat.getDateInstance(DateFormat.SHORT).format(Date(doc.lastModified)))
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // 4. Contacts Results
    if (contactResults.isNotEmpty()) {
        item(key = "hdr_contacts_results", contentType = "header") {
            DrawerListSectionHeader(text = "Contacts")
        }
        items(
            count = contactResults.size,
            key = { index -> "contact_${contactResults[index].id}" },
            contentType = { "contact_result" }
        ) { index ->
            val contact = contactResults[index]
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.withAppendedPath(android.provider.ContactsContract.Contacts.CONTENT_URI, contact.id)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                            onCloseDrawer()
                        } catch (e: Exception) {
                            if (!contact.phoneNumber.isNullOrEmpty()) {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phoneNumber}")).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                                onCloseDrawer()
                            }
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (contact.photoUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(contact.photoUri)
                                .size(120, 120)
                                .scale(Scale.FILL)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .crossfade(true)
                                .build(),
                            contentDescription = contact.displayName,
                            placeholder = rememberVectorPainter(Icons.Default.Person),
                            error = rememberVectorPainter(Icons.Default.Person),
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = contact.displayName,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = contact.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        if (!contact.phoneNumber.isNullOrEmpty()) {
                            Text(
                                text = contact.phoneNumber,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (!contact.phoneNumber.isNullOrEmpty()) {
                        IconButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phoneNumber}")).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                    onCloseDrawer()
                                } catch (e: Exception) {}
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Call Contact",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    // 5. Call Logs Results
    if (callLogResults.isNotEmpty()) {
        item(key = "hdr_call_logs_results", contentType = "header") {
            DrawerListSectionHeader(text = "Call Logs")
        }
        items(
            count = callLogResults.size,
            key = { index -> "calllog_${callLogResults[index].id}" },
            contentType = { "calllog_result" }
        ) { index ->
            val log = callLogResults[index]
            val dispName = log.cachedName ?: log.number
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        try {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${log.number}")).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                            onCloseDrawer()
                        } catch (e: Exception) {}
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (log.type) {
                            CallLog.Calls.INCOMING_TYPE -> Icons.Default.CallReceived
                            CallLog.Calls.OUTGOING_TYPE -> Icons.Default.CallMade
                            CallLog.Calls.MISSED_TYPE -> Icons.Default.CallMissed
                            else -> Icons.Default.Phone
                        },
                        contentDescription = dispName,
                        tint = if (log.type == CallLog.Calls.MISSED_TYPE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = dispName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(log.dateMs)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // 6. Messages Results
    if (messageResults.isNotEmpty()) {
        item(key = "hdr_message_results", contentType = "header") {
            DrawerListSectionHeader(text = "Messages")
        }
        items(
            count = messageResults.size,
            key = { index -> "msg_${messageResults[index].id}" },
            contentType = { "message_result" }
        ) { index ->
            val msg = messageResults[index]
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:${msg.address}")).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                            onCloseDrawer()
                        } catch (e: Exception) {}
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sms,
                        contentDescription = "Message",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = msg.address,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = msg.body,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    // 7. Calendar Results
    if (calendarResults.isNotEmpty()) {
        item(key = "hdr_calendar_results", contentType = "header") {
            DrawerListSectionHeader(text = "Calendar Events")
        }
        items(
            count = calendarResults.size,
            key = { index -> "cal_${calendarResults[index].id}" },
            contentType = { "calendar_result" }
        ) { index ->
            val event = calendarResults[index]
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        try {
                            val uri = Uri.withAppendedPath(CalendarContract.Events.CONTENT_URI, event.id.toString())
                            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                            onCloseDrawer()
                        } catch (e: Exception) {}
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = event.title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(event.startTimeMs)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageIndexedFoldersBottomSheet(
    folders: List<IndexedFolderEntity>,
    isIndexing: Boolean,
    hasBroadFileAccess: Boolean = false,
    onRequestBroadFileAccess: () -> Unit = {},
    onDismiss: () -> Unit,
    onAddFolder: () -> Unit,
    onRemoveFolder: (Long, String) -> Unit,
    onReindexAll: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Search Folders & Access",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onReindexAll, enabled = !isIndexing && folders.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reindex all folders",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Configure SAF folders and broad file search access to locate files and documents across device storage.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Broad File Search Access Section
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRequestBroadFileAccess() },
                shape = RoundedCornerShape(12.dp),
                color = if (hasBroadFileAccess) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (hasBroadFileAccess) Icons.Default.CheckCircle else Icons.Default.FolderSpecial,
                        contentDescription = null,
                        tint = if (hasBroadFileAccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (hasBroadFileAccess) "Broad file search access enabled" else "Enable broad file search access",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (hasBroadFileAccess) {
                                "All non-restricted files across storage are searchable (excludes Android/data, Android/obb & app private sandboxes)"
                            } else {
                                "Allow all-files access to search non-restricted documents across device storage"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!hasBroadFileAccess) {
                        TextButton(onClick = onRequestBroadFileAccess) {
                            Text("Enable", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "SAF folders",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Choose specific Storage Access Framework folders like Documents or Downloads to index for local offline search.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isIndexing) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Indexing files...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (folders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No SAF folders added yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    items(folders, key = { it.id }) { folder ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "Folder",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = folder.displayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${folder.documentCount} documents indexed",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { onRemoveFolder(folder.id, folder.treeUri) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Remove folder",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onAddFolder,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Folder")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Folder")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun MediaResultRow(
    media: MediaSearchResult,
    context: Context,
    onCloseDrawer: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(media.uri, media.mimeType ?: "*/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    onCloseDrawer()
                } catch (e: Exception) {
                    Toast.makeText(context, "Cannot open media", Toast.LENGTH_SHORT).show()
                }
            }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            when (media.mediaType) {
                MediaType.IMAGE, MediaType.VIDEO -> {
                    val isVideo = media.mediaType == MediaType.VIDEO
                    var loadedBitmap by remember(media.uri) { mutableStateOf<Bitmap?>(null) }
                    var loadFailed by remember(media.uri) { mutableStateOf(false) }

                    LaunchedEffect(media.uri) {
                        loadedBitmap = null
                        loadFailed = false
                        withContext(Dispatchers.IO) {
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    if (BuildConfig.DEBUG) {
                                        Log.d("MediaThumbnail", "Loading thumbnail: type=${media.mediaType}, auth=${media.uri.authority}, path=${media.uri.path}, id=${media.uri.lastPathSegment}, mime=${media.mimeType}, strategy=ContentResolver.loadThumbnail")
                                    }
                                    val signal = CancellationSignal()
                                    val bitmap = context.contentResolver.loadThumbnail(
                                        media.uri,
                                        AndroidSize(120, 120),
                                        signal
                                    )
                                    withContext(Dispatchers.Main) {
                                        loadedBitmap = bitmap
                                    }
                                } else {
                                    if (BuildConfig.DEBUG) {
                                        Log.d("MediaThumbnail", "Skipping loadThumbnail on API < 29: type=${media.mediaType}, auth=${media.uri.authority}, path=${media.uri.path}, id=${media.uri.lastPathSegment}, mime=${media.mimeType}, strategy=Coil fallback")
                                    }
                                    withContext(Dispatchers.Main) {
                                        loadFailed = true
                                    }
                                }
                            } catch (e: Exception) {
                                if (BuildConfig.DEBUG) {
                                    Log.d("MediaThumbnail", "loadThumbnail failed, switching to Coil fallback: type=${media.mediaType}, auth=${media.uri.authority}, path=${media.uri.path}, id=${media.uri.lastPathSegment}, mime=${media.mimeType}, strategy=ContentResolver.loadThumbnail, exception=${e.javaClass.simpleName}: ${e.message}")
                                }
                                withContext(Dispatchers.Main) {
                                    loadFailed = true
                                }
                            }
                        }
                    }

                    val fallbackPainter = rememberVectorPainter(
                        if (isVideo) Icons.Default.VideoFile else Icons.Default.Image
                    )
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (loadedBitmap != null) {
                            Image(
                                bitmap = loadedBitmap!!.asImageBitmap(),
                                contentDescription = media.displayName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (loadFailed) {
                            // Secondary fallback path using Coil with content:// URI
                            val imageRequest = ImageRequest.Builder(LocalContext.current)
                                .data(media.uri)
                                .size(120, 120)
                                .scale(Scale.FILL)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .crossfade(true)
                                .listener(
                                    onError = { _, result ->
                                        if (BuildConfig.DEBUG) {
                                            Log.d("MediaThumbnail", "Coil fallback failed: type=${media.mediaType}, auth=${media.uri.authority}, path=${media.uri.path}, id=${media.uri.lastPathSegment}, mime=${media.mimeType}, strategy=Coil fallback, exception=${result.throwable.javaClass.simpleName}: ${result.throwable.message}")
                                        }
                                    }
                                )
                                .apply {
                                    if (isVideo) {
                                        videoFrameMillis(1000)
                                    }
                                }
                                .build()

                            AsyncImage(
                                model = imageRequest,
                                contentDescription = media.displayName,
                                placeholder = fallbackPainter,
                                error = fallbackPainter,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = fallbackPainter,
                                contentDescription = media.displayName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        if (isVideo) {
                            Icon(
                                imageVector = Icons.Default.PlayCircleFilled,
                                contentDescription = "Video",
                                tint = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                MediaType.AUDIO -> {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AudioFile,
                                contentDescription = "Audio",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = media.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatFileSize(media.sizeBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun getDocumentIcon(mimeType: String, fileName: String): androidx.compose.ui.graphics.vector.ImageVector {
    val lowerName = fileName.lowercase()
    return when {
        mimeType.contains("pdf") || lowerName.endsWith(".pdf") -> Icons.Default.PictureAsPdf
        mimeType.contains("word") || lowerName.endsWith(".doc") || lowerName.endsWith(".docx") -> Icons.Default.Description
        mimeType.contains("sheet") || mimeType.contains("excel") || lowerName.endsWith(".xls") || lowerName.endsWith(".xlsx") || lowerName.endsWith(".csv") -> Icons.Default.TableChart
        mimeType.contains("presentation") || mimeType.contains("powerpoint") || lowerName.endsWith(".ppt") || lowerName.endsWith(".pptx") -> Icons.Default.Slideshow
        mimeType.contains("zip") || mimeType.contains("compressed") || mimeType.contains("tar") || lowerName.endsWith(".zip") || lowerName.endsWith(".rar") || lowerName.endsWith(".7z") -> Icons.Default.FolderZip
        mimeType.startsWith("text") || lowerName.endsWith(".txt") || lowerName.endsWith(".md") || lowerName.endsWith(".json") || lowerName.endsWith(".xml") -> Icons.Default.Article
        else -> Icons.Default.InsertDriveFile
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
