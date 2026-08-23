package nwd.fokuslauncher.ui.drawer

import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import coil.request.ImageRequest
import coil.request.videoFrameMillis

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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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

fun LazyListScope.universalSearchResults(
    context: Context,
    settingsResults: List<SettingsSearchResult>,
    fileResults: List<FileSearchResult>,
    contactResults: List<ContactSearchResult>,
    callLogResults: List<CallLogSearchResult>,
    messageResults: List<MessageSearchResult>,
    calendarResults: List<CalendarSearchResult>,
    hasStoragePermission: Boolean = true,
    hasVisualMediaPermission: Boolean = true,
    hasAudioMediaPermission: Boolean = true,
    onRequestStoragePermission: () -> Unit = {},
    onCloseDrawer: () -> Unit
) {
    if (settingsResults.isNotEmpty()) {
        item(key = "hdr_settings_results") {
            DrawerListSectionHeader(text = "System Settings")
        }
        items(
            count = settingsResults.size,
            key = { index -> "setting_${settingsResults[index].id}" }
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
                            Toast.makeText(context, "Cannot open ${item.title}", Toast.LENGTH_SHORT).show()
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    if (contactResults.isNotEmpty()) {
        item(key = "hdr_contacts_results") {
            DrawerListSectionHeader(text = "Contacts")
        }
        items(
            count = contactResults.size,
            key = { index -> "contact_${contactResults[index].id}" }
        ) { index ->
            val contact = contactResults[index]
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        try {
                            val intent = if (!contact.phoneNumber.isNull_or_blank()) {
                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phoneNumber}"))
                            } else {
                                Intent(Intent.ACTION_VIEW, Uri.withAppendedPath(android.provider.ContactsContract.Contacts.CONTENT_URI, contact.id))
                            }
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                            onCloseDrawer()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open contact", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = contact.displayName,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
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

    val hasAnyStoragePermission = hasStoragePermission && (hasVisualMediaPermission || hasAudioMediaPermission)
    if (!hasAnyStoragePermission && fileResults.isEmpty()) {
        item(key = "hdr_files_permission") {
            DrawerListSectionHeader(text = "Local Files")
        }
        item(key = "files_permission_btn") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRequestStoragePermission() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Grant Storage Permission",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Grant storage permission to search files",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    } else if (fileResults.isNotEmpty()) {
        item(key = "hdr_files_results") {
            DrawerListSectionHeader(text = "Local Files")
        }
        items(
            count = fileResults.size,
            key = { index -> "file_${fileResults[index].id}" }
        ) { index ->
            val file = fileResults[index]
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(file.uri, file.mimeType ?: "*/*")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                            onCloseDrawer()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open file", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (file.mimeType?.startsWith("image") == true ||
                        file.mimeType?.startsWith("video") == true) {
                        val isVideo = file.mimeType?.startsWith("video") == true
                        val imageRequest = ImageRequest.Builder(LocalContext.current)
                            .data(file.uri)
                            .crossfade(true)
                            .apply {
                                if (isVideo) {
                                    videoFrameMillis(1000)
                                }
                            }
                            .build()
                        val fallbackPainter = rememberVectorPainter(
                            if (isVideo) Icons.Default.VideoFile else Icons.Default.InsertDriveFile
                        )
                        coil.compose.AsyncImage(
                            model = imageRequest,
                            contentDescription = file.displayName,
                            placeholder = fallbackPainter,
                            error = fallbackPainter,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(
                            imageVector = when {
                                file.mimeType?.startsWith("audio") == true -> Icons.Default.AudioFile
                                file.mimeType?.contains("pdf") == true -> Icons.Default.PictureAsPdf
                                else -> Icons.Default.InsertDriveFile
                            },
                            contentDescription = file.displayName,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatFileSize(file.sizeBytes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (callLogResults.isNotEmpty()) {
        item(key = "hdr_call_logs_results") {
            DrawerListSectionHeader(text = "Call Logs")
        }
        items(
            count = callLogResults.size,
            key = { index -> "calllog_${callLogResults[index].id}" }
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

    if (messageResults.isNotEmpty()) {
        item(key = "hdr_message_results") {
            DrawerListSectionHeader(text = "Messages")
        }
        items(
            count = messageResults.size,
            key = { index -> "msg_${messageResults[index].id}" }
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

    if (calendarResults.isNotEmpty()) {
        item(key = "hdr_calendar_results") {
            DrawerListSectionHeader(text = "Calendar Events")
        }
        items(
            count = calendarResults.size,
            key = { index -> "cal_${calendarResults[index].id}" }
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

private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
