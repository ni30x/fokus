package nwd.fokuslauncher.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.annotation.VisibleForTesting
import nwd.fokuslauncher.accessibility.LockScreenAccessibilityService

object LockScreenHelper {

    @VisibleForTesting
    @Volatile
    var lockScreenOverride: (() -> Boolean)? = null

    @VisibleForTesting
    @Volatile
    var accessibilityEnabledOverride: ((Context) -> Boolean)? = null

    private fun lockServiceComponent(context: Context): ComponentName =
            ComponentName(context, LockScreenAccessibilityService::class.java)

    /** Whether our lock service appears in the system enabled-accessibility list. */
    fun isLockAccessibilityServiceEnabled(context: Context): Boolean {
        accessibilityEnabledOverride?.let { return it(context) }
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        if (!am.isEnabled) return false
        val enabled =
                Settings.Secure.getString(
                        context.contentResolver,
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                )
                        ?: return false
        val expected = lockServiceComponent(context).flattenToString()
        return enabled.split(':').any { it.trim().equals(expected, ignoreCase = true) }
    }

    fun lockScreenIfPossible(): Boolean {
        lockScreenOverride?.let { return it() }
        return LockScreenAccessibilityService.lockScreenNow()
    }

    fun openAccessibilitySettings(context: Context) {
        try {
            context.startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
            )
        } catch (_: Exception) {}
    }
}
