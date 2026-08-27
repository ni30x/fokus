package nwd.fokuslauncher.ui.drawer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.provider.CallLog
import android.provider.ContactsContract
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import coil.size.Scale
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
    contactResults: List<ContactSearchResult>,
    callLogResults: List<CallLogSearchResult>,
    messageResults: List<MessageSearchResult>,
    calendarResults: List<CalendarSearchResult>,
    hasAnyMatches: Boolean = true,
    searchQuery: String = "",
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

    // 2. Contacts Results
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
                                data = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contact.id)
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

    // 3. Call Logs Results
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

    // 4. Messages Results
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

    // 5. Calendar Results
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
