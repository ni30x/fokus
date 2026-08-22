package nwd.fokuslauncher.data.database.entity

import androidx.room.Entity
import nwd.fokuslauncher.data.model.LEGACY_PACKAGE_WIDE_METADATA

/**
 * Represents an app that has been hidden by the user.
 *
 * [launcherShortcutId] semantics:
 * - Specific shortcut id → hide only that PWA row
 * - [nwd.fokuslauncher.data.model.HOST_APP_METADATA_SENTINEL] → hide only the host app row
 * - [LEGACY_PACKAGE_WIDE_METADATA] → hide every row for package + profile (pre-v5 behavior)
 */
@Entity(
        tableName = "hidden_apps",
        primaryKeys = ["packageName", "profileKey", "launcherShortcutId"],
)
data class HiddenAppEntity(
        val packageName: String,
        val profileKey: String,
        val launcherShortcutId: String = LEGACY_PACKAGE_WIDE_METADATA,
)
