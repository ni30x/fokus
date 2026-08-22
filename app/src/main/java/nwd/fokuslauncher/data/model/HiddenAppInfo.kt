package nwd.fokuslauncher.data.model

data class HiddenAppInfo(
        val packageName: String,
        val profileKey: String,
        val launcherShortcutId: String,
        val label: String,
        val profileLabel: String? = null,
) {
    val stableKey: String
        get() = metadataSettingsStableKey(packageName, profileKey, launcherShortcutId)
}
