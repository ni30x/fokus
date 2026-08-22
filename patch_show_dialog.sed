/SettingsScreenDialogs(/i \
    if (showAppPickerFor.value == "focus_mode_whitelist") {\
        FocusModeWhitelistDialog(\
            allApps = uiState.allApps,\
            whitelistedApps = uiState.focusModeWhitelist,\
            onToggleApp = { pkg, isChecked ->\
                val newSet = uiState.focusModeWhitelist.toMutableSet()\
                if (isChecked) newSet.add(pkg) else newSet.remove(pkg)\
                viewModel.setFocusModeWhitelist(newSet)\
            },\
            onDismiss = { showAppPickerFor.value = null }\
        )\
    }\

