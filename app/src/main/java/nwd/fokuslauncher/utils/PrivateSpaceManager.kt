package nwd.fokuslauncher.utils

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import nwd.fokuslauncher.data.model.AppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Android 15+ Private Space functionality.
 *
 * Private Space uses profile type "android.os.usertype.profile.PRIVATE" and
 * is controlled via quiet mode:
 *   - Locked = quiet mode enabled
 *   - Unlocked = quiet mode disabled
 *
 * Unlocking triggers the system authentication prompt (PIN / biometric).
 *
 * On Android < 15, all methods are safe to call and return empty/false.
 */
@Singleton
class PrivateSpaceManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val userManager: UserManager by lazy {
        context.getSystemService(Context.USER_SERVICE) as UserManager
    }

    private val launcherApps: LauncherApps by lazy {
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    }

    private val prefs by lazy {
        context.getSharedPreferences("private_space_prefs", Context.MODE_PRIVATE)
    }

    @Volatile
    private var cachedPrivateProfile: UserHandle? = null

    /**
     * Emits whenever the Private Space profile becomes available (unlocked) or
     * unavailable (locked). Collectors should call [isPrivateSpaceUnlocked] and
     * [getPrivateSpaceApps] to read the new state.
     */
    private val _profileStateChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val profileStateChanged: SharedFlow<Unit> = _profileStateChanged.asSharedFlow()

    /** Whether the device supports Private Space (Android 15 / API 35+). */
    val isSupported: Boolean
        get() = Build.VERSION.SDK_INT >= 35

    /** Whether the user has set up a Private Space profile on this device. */
    fun hasPrivateSpaceProfile(): Boolean = getPrivateSpaceProfile() != null

    init {
        if (isSupported) {
            registerProfileReceiver()
        }
    }

    /**
     * Listens for profile availability broadcasts so the launcher can
     * immediately refresh after the user unlocks or locks Private Space.
     */
    private fun registerProfileReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                _profileStateChanged.tryEmit(Unit)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PROFILE_AVAILABLE)
            addAction(Intent.ACTION_PROFILE_UNAVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED)
            addAction(Intent.ACTION_USER_UNLOCKED)
            addAction(Intent.ACTION_USER_FOREGROUND)
            addAction(Intent.ACTION_USER_BACKGROUND)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
    }

    /**
     * Returns the UserHandle of the Private Space profile, or null if none
     * exists or the device is pre-Android 15.
     */
    fun getPrivateSpaceProfile(): UserHandle? {
        if (!isSupported) return null

        val myUser = android.os.Process.myUserHandle()

        // 1. Check cached profile in memory if it is still valid
        cachedPrivateProfile?.let { cached ->
            if (isProfileAlive(cached)) {
                return cached
            } else {
                cachedPrivateProfile = null
            }
        }

        // 2. Query LauncherApps.profiles first (returns hidden/quiet profiles on Android 15+)
        try {
            val launcherProfiles = launcherApps.profiles
            val found = launcherProfiles.firstOrNull { profile ->
                profile != myUser && isPrivateSpaceProfile(profile)
            }
            if (found != null) {
                cachedPrivateProfile = found
                savePersistedSerialNumber(found)
                return found
            }
        } catch (_: Exception) {}

        // 3. Fallback to UserManager.userProfiles
        try {
            val userProfiles = userManager.userProfiles
            val found = userProfiles.firstOrNull { profile ->
                profile != myUser && isPrivateSpaceProfile(profile)
            }
            if (found != null) {
                cachedPrivateProfile = found
                savePersistedSerialNumber(found)
                return found
            }
        } catch (_: Exception) {}

        // 4. Fallback to persisted serial number if profile is hidden in quiet mode
        val persistedSerial = prefs.getLong(KEY_PERSISTED_SERIAL, -1L)
        if (persistedSerial != -1L) {
            try {
                val restoredProfile = userManager.getUserForSerialNumber(persistedSerial)
                if (restoredProfile != null && restoredProfile != myUser && isProfileAlive(restoredProfile)) {
                    cachedPrivateProfile = restoredProfile
                    return restoredProfile
                }
            } catch (_: Exception) {}
        }

        return null
    }

    private fun isProfileAlive(profile: UserHandle): Boolean {
        return try {
            val serial = userManager.getSerialNumberForUser(profile)
            serial != -1L
        } catch (_: Exception) {
            false
        }
    }

    private fun savePersistedSerialNumber(profile: UserHandle) {
        try {
            val serial = userManager.getSerialNumberForUser(profile)
            if (serial != -1L) {
                prefs.edit().putLong(KEY_PERSISTED_SERIAL, serial).apply()
            }
        } catch (_: Exception) {}
    }

    /**
     * True if [profile] is the Android 15+ Private Space user (not clones / work / etc.).
     * Used to keep Private Space apps in the dedicated drawer section only.
     */
    fun isPrivateSpaceProfile(profile: UserHandle): Boolean {
        if (Build.VERSION.SDK_INT < 35) return false
        if (profile == android.os.Process.myUserHandle()) return false
        return try {
            val info = launcherApps.getLauncherUserInfo(profile)
            val userType = info?.userType
            if (userType == "android.os.usertype.profile.PRIVATE" ||
                userType?.contains("PRIVATE", ignoreCase = true) == true
            ) {
                true
            } else {
                // If userType is not explicitly private, verify if it's not managed work
                userType == "android.os.usertype.profile.PRIVATE"
            }
        } catch (_: Exception) {
            false
        }
    }

    /** Returns non-archived apps installed in the Private Space profile. */
    fun getPrivateSpaceApps(): List<AppInfo> =
            loadPrivateSpaceApps(includeArchived = false)

    /** Returns archived apps installed in the Private Space profile. */
    fun getArchivedPrivateSpaceApps(): List<AppInfo> =
            loadPrivateSpaceApps(includeArchived = true)

    private fun loadPrivateSpaceApps(includeArchived: Boolean): List<AppInfo> {
        val profile = getPrivateSpaceProfile() ?: return emptyList()
        return try {
            launcherApps.getActivityList(null, profile).mapNotNull { activityInfo ->
                val isArchived =
                        Build.VERSION.SDK_INT >= 35 &&
                                runCatching {
                                    activityInfo.applicationInfo.isArchived
                                }.getOrDefault(false)
                if (isArchived != includeArchived) return@mapNotNull null
                AppInfo(
                    packageName = activityInfo.applicationInfo.packageName,
                    label = activityInfo.label.toString(),
                    icon = null,
                    userHandle = profile,
                    componentName = activityInfo.componentName,
                    isArchived = isArchived,
                )
            }.sortedBy { it.label.lowercase() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Launches a Private Space app via [LauncherApps.startMainActivity].
     * Regular [PackageManager.getLaunchIntentForPackage] does not work across
     * user profiles, so this is the only correct way to start an activity in
     * Private Space.
     *
     * @return true if the activity was started, false otherwise.
     */
    fun launchApp(componentName: ComponentName, userHandle: UserHandle): Boolean {
        return try {
            launcherApps.startMainActivity(componentName, userHandle, null, null)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks if the Private Space is currently unlocked.
     * "Quiet mode" = locked; not quiet = unlocked.
     */
    fun isPrivateSpaceUnlocked(): Boolean {
        if (!isSupported) return false
        val profile = getPrivateSpaceProfile() ?: return false
        return try {
            !userManager.isQuietModeEnabled(profile)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Requests unlocking of the Private Space.
     * This disables quiet mode, which triggers the system authentication
     * prompt (PIN, pattern, or biometric).
     * @return true if the request was initiated, false otherwise.
     */
    fun requestUnlock(): Boolean {
        if (!isSupported) return false
        val profile = getPrivateSpaceProfile() ?: return false
        return try {
            // false = disable quiet mode = unlock
            userManager.requestQuietModeEnabled(false, profile)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Locks the Private Space by enabling quiet mode.
     * @return true if the request was initiated, false otherwise.
     */
    fun lock(): Boolean {
        if (!isSupported) return false
        val profile = getPrivateSpaceProfile() ?: return false
        return try {
            userManager.requestQuietModeEnabled(true, profile)
            true
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private const val KEY_PERSISTED_SERIAL = "persisted_private_space_serial"
    }
}
