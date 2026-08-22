package nwd.fokuslauncher.data.database.entity

import androidx.room.Entity
import nwd.fokuslauncher.data.model.LEGACY_PACKAGE_WIDE_METADATA

/**
 * Represents an app that has been renamed by the user.
 *
 * [launcherShortcutId] uses the same semantics as [HiddenAppEntity].
 */
@Entity(
        tableName = "renamed_apps",
        primaryKeys = ["packageName", "profileKey", "launcherShortcutId"],
)
data class RenamedAppEntity(
        val packageName: String,
        val profileKey: String,
        val customName: String,
        val launcherShortcutId: String = LEGACY_PACKAGE_WIDE_METADATA,
)
