package nwd.fokuslauncher.ui.drawer

import nwd.fokuslauncher.data.model.AppInfo
import nwd.fokuslauncher.data.model.AppShortcutAction

/** One app-drawer block for a single Android user profile (owner, work, clone, …). */
data class DrawerProfileSectionUi(
        val id: String,
        val title: String,
        val apps: List<AppInfo>
)

/** Same profile bucketing as [DrawerProfileSectionUi], for edit-shortcuts action rows. */
data class DrawerProfileShortcutSectionUi(
        val id: String,
        val title: String,
        val actions: List<AppShortcutAction>
)
