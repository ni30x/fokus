/fun SettingsScreenDialogs/i \
@Composable\
private fun FocusModeWhitelistDialog(\
    allApps: List<nwd.fokuslauncher.data.model.AppInfo>,\
    whitelistedApps: Set<String>,\
    onToggleApp: (String, Boolean) -> Unit,\
    onDismiss: () -> Unit\
) {\
    androidx.compose.material3.AlertDialog(\
        onDismissRequest = onDismiss,\
        title = { androidx.compose.material3.Text("Focus Mode Apps") },\
        text = {\
            androidx.compose.foundation.lazy.LazyColumn {\
                items(allApps.size) { i ->\
                    val app = allApps[i]\
                    val isChecked = whitelistedApps.contains(app.packageName)\
                    androidx.compose.foundation.layout.Row(\
                        modifier = androidx.compose.ui.Modifier\
                            .fillMaxWidth()\
                            .clickable { onToggleApp(app.packageName, !isChecked) }\
                            .padding(vertical = 12.dp),\
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically\
                    ) {\
                        androidx.compose.material3.Checkbox(checked = isChecked, onCheckedChange = { onToggleApp(app.packageName, it) })\
                        androidx.compose.foundation.layout.Spacer(Modifier.width(12.dp))\
                        androidx.compose.material3.Text(app.label)\
                    }\
                }\
            }\
        },\
        confirmButton = {\
            androidx.compose.material3.TextButton(onClick = onDismiss) { androidx.compose.material3.Text("Done") }\
        }\
    )\
}\

