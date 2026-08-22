package nwd.fokuslauncher.data.database.entity

import androidx.room.Entity
import nwd.fokuslauncher.data.model.LEGACY_PACKAGE_WIDE_METADATA

/**
 * Represents a user-assigned category for an app.
 *
 * [launcherShortcutId] uses the same semantics as [HiddenAppEntity].
 */
@Entity(
        tableName = "app_categories",
        primaryKeys = ["packageName", "profileKey", "launcherShortcutId"],
)
data class AppCategoryEntity(
        val packageName: String,
        val profileKey: String,
        val category: String,
        val launcherShortcutId: String = LEGACY_PACKAGE_WIDE_METADATA,
)
