/item { Spacer(Modifier.height(32.dp)) }/i \
        item {\
            SettingsAccordionPanel(\
                    title = "Focus Mode",\
                    subtitle = "Manage timer and whitelisted apps",\
                    icon = Icons.Default.Timer,\
                    expanded = focusModeExpanded,\
                    onExpandedChange = { focusModeExpanded = it }\
            ) {\
                androidx.compose.foundation.layout.Column(\
                    modifier = androidx.compose.ui.Modifier\
                        .fillMaxWidth()\
                        .padding(horizontal = 24.dp, vertical = 12.dp)\
                ) {\
                    Text(text = "Timer duration (${uiState.focusModeDurationMins} min)", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)\
                    androidx.compose.material3.Slider(\
                        value = uiState.focusModeDurationMins.toFloat(),\
                        onValueChange = { viewModel.setFocusModeDurationMins(it.toInt()) },\
                        valueRange = 5f..120f,\
                        steps = 22\
                    )\
                }\
                SettingsRow(\
                    label = "Whitelisted Apps",\
                    subtitle = "Select apps allowed during Focus Mode (${uiState.focusModeWhitelist.size} selected)",\
                    verticalPadding = 12.dp,\
                    onClick = { onShowAppPicker("focus_mode_whitelist") }\
                )\
            }\
        }
