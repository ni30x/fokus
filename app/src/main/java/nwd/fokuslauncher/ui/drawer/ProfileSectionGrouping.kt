package nwd.fokuslauncher.ui.drawer

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import nwd.fokuslauncher.R
import nwd.fokuslauncher.data.model.AppInfo
import nwd.fokuslauncher.data.model.AppShortcutAction
import nwd.fokuslauncher.data.model.FavoriteApp
import nwd.fokuslauncher.data.model.HomeShortcut
import nwd.fokuslauncher.data.model.ShortcutTarget
import nwd.fokuslauncher.data.model.appProfileKey
import nwd.fokuslauncher.utils.PrivateSpaceManager
import nwd.fokuslauncher.utils.ProfileHeuristics

/** Case-insensitive label order (matches drawer alphabetical mode). */
val alphabeticalAppComparatorForProfiles =
        compareBy<AppInfo, String>(String.CASE_INSENSITIVE_ORDER) { it.label }

fun sortAppsAlphabeticallyByProfileSection(apps: List<AppInfo>): List<AppInfo> =
        apps.sortedWith(alphabeticalAppComparatorForProfiles)

/**
 * Applies a user-defined profile title from [profileDisplayNameOverrides] when set; otherwise
 * returns [defaultTitle].
 */
fun resolvedProfileDisplayTitle(
        profileDisplayNameOverrides: Map<String, String>,
        profileKey: String,
        defaultTitle: String,
): String {
    val custom = profileDisplayNameOverrides[profileKey]?.trim()
    return if (!custom.isNullOrEmpty()) custom else defaultTitle
}

/**
 * Groups [apps] into the same profile sections as the app drawer (personal, work, clone, …),
 * applying [sortWithinSection] inside each section (drawer uses this for alphabetical vs
 * most-opened).
 */
fun groupAppsIntoProfileSections(
        context: Context,
        apps: List<AppInfo>,
        sortWithinSection: (List<AppInfo>) -> List<AppInfo>,
        profileDisplayNameOverrides: Map<String, String> = emptyMap(),
): List<DrawerProfileSectionUi> {
    val userManager =
            try {
                context.getSystemService(Context.USER_SERVICE) as? UserManager
            } catch (_: Exception) {
                null
            }
    if (userManager == null) {
        return buildProfileSectionsWithoutUserManager(
                context,
                apps,
                sortWithinSection,
                profileDisplayNameOverrides,
        )
    }

    val ownerApps = sortWithinSection(apps.filter { it.userHandle == null })
    val byUser = apps.filter { it.userHandle != null }.groupBy { it.userHandle!! }
    val orderedUsers =
            byUser.keys.sortedBy { uh ->
                try {
                    userManager.getSerialNumberForUser(uh)
                } catch (_: Exception) {
                    Long.MAX_VALUE
                }
            }

    return buildList {
        if (ownerApps.isNotEmpty()) {
            add(
                    DrawerProfileSectionUi(
                            id = "owner",
                            title =
                                    resolvedProfileDisplayTitle(
                                            profileDisplayNameOverrides,
                                            "0",
                                            context.getString(R.string.drawer_section_personal),
                                    ),
                            apps = ownerApps,
                    )
            )
        }
        for (user in orderedUsers) {
            val list = sortWithinSection(byUser.getValue(user))
            add(
                    DrawerProfileSectionUi(
                            id = "u_${user.hashCode()}",
                            title =
                                    profileSectionTitleForUser(
                                            context = context,
                                            user = user,
                                            userManager = userManager,
                                            totalSecondaryProfiles = orderedUsers.size,
                                            profileDisplayNameOverrides = profileDisplayNameOverrides,
                                    ),
                            apps = list,
                    )
            )
        }
    }
}

private fun buildProfileSectionsWithoutUserManager(
        context: Context,
        apps: List<AppInfo>,
        sortWithinSection: (List<AppInfo>) -> List<AppInfo>,
        profileDisplayNameOverrides: Map<String, String>,
): List<DrawerProfileSectionUi> {
    val ownerApps = sortWithinSection(apps.filter { it.userHandle == null })
    val byUser = apps.filter { it.userHandle != null }.groupBy { it.userHandle!! }
    return buildList {
        if (ownerApps.isNotEmpty()) {
            add(
                    DrawerProfileSectionUi(
                            id = "owner",
                            title =
                                    resolvedProfileDisplayTitle(
                                            profileDisplayNameOverrides,
                                            "0",
                                            context.getString(R.string.drawer_section_personal),
                                    ),
                            apps = ownerApps,
                    )
            )
        }
        for (user in byUser.keys) {
            val list = sortWithinSection(byUser.getValue(user))
            val defaultTitle =
                    when {
                        byUser.keys.size == 1 &&
                                !ProfileHeuristics.isLikelyCloneOrParallelProfile(context, user) ->
                                context.getString(R.string.drawer_section_work_profile)
                        ProfileHeuristics.isLikelyCloneOrParallelProfile(context, user) ->
                                context.getString(R.string.drawer_section_clone_profile)
                        else -> context.getString(R.string.drawer_section_other_profile)
                    }
            val pk = appProfileKey(user)
            add(
                    DrawerProfileSectionUi(
                            id = "u_${user.hashCode()}",
                            title = resolvedProfileDisplayTitle(profileDisplayNameOverrides, pk, defaultTitle),
                            apps = list,
                    )
            )
        }
    }
}

/** Profile subtitle for an [AppInfo] row (null = personal / owner). Drawer-style naming. */
fun profileOriginLabelForApp(
        context: Context,
        app: AppInfo,
        profileDisplayNameOverrides: Map<String, String> = emptyMap(),
): String? {
    val user = app.userHandle ?: return null
    val pk = appProfileKey(user)
    if (PrivateSpaceManager(context).isPrivateSpaceProfile(user)) {
        val def = context.getString(R.string.drawer_section_private_space)
        return resolvedProfileDisplayTitle(profileDisplayNameOverrides, pk, def)
    }
    val userManager =
            try {
                context.getSystemService(Context.USER_SERVICE) as? UserManager
            } catch (_: Exception) {
                null
            }
    if (userManager != null) {
        val myUser = Process.myUserHandle()
        val totalSecondary = userManager.userProfiles.count { it != myUser }
        return profileSectionTitleForUser(
                context,
                user,
                userManager,
                totalSecondary.coerceAtLeast(1),
                profileDisplayNameOverrides,
        )
    }
    val defaultTitle =
            when {
                ProfileHeuristics.isLikelyCloneOrParallelProfile(context, user) ->
                        context.getString(R.string.drawer_section_clone_profile)
                else -> context.getString(R.string.drawer_section_work_profile)
            }
    return resolvedProfileDisplayTitle(profileDisplayNameOverrides, pk, defaultTitle)
}

/**
 * Short label for the profile a home-screen favorite came from (null = personal / owner).
 * Uses the same naming rules as the app drawer when [matchingApp] supplies a [UserHandle].
 */
/** Profile subtitle for a saved right-side shortcut (null = personal / owner). */
fun profileOriginLabelForHomeShortcut(
        context: Context,
        shortcut: HomeShortcut,
        allApps: List<AppInfo>,
        profileDisplayNameOverrides: Map<String, String> = emptyMap(),
): String? {
    if (shortcut.profileKey == "0") return null
    matchingAppInfoForHomeShortcut(shortcut, allApps)?.let {
        return profileOriginLabelForApp(context, it, profileDisplayNameOverrides)
    }
    val fallback = context.getString(R.string.drawer_section_work_profile)
    return resolvedProfileDisplayTitle(profileDisplayNameOverrides, shortcut.profileKey, fallback)
}

private fun matchingAppInfoForHomeShortcut(shortcut: HomeShortcut, allApps: List<AppInfo>): AppInfo? {
    val packageName =
            when (val t = shortcut.target) {
                is ShortcutTarget.App -> t.packageName
                is ShortcutTarget.LauncherShortcut -> t.packageName
                else -> return null
            }
    return allApps.find {
        it.packageName == packageName && appProfileKey(it.userHandle) == shortcut.profileKey
    }
}

fun profileOriginLabelForFavorite(
        context: Context,
        fav: FavoriteApp,
        matchingApp: AppInfo?,
        profileDisplayNameOverrides: Map<String, String> = emptyMap(),
): String? {
    if (fav.profileKey == "0") return null
    val app =
            matchingApp?.takeIf {
                it.packageName == fav.packageName &&
                        appProfileKey(it.userHandle) == fav.profileKey
            }
    return app?.let { profileOriginLabelForApp(context, it, profileDisplayNameOverrides) }
            ?: resolvedProfileDisplayTitle(
                    profileDisplayNameOverrides,
                    fav.profileKey,
                    context.getString(R.string.drawer_section_work_profile),
            )
}

/**
 * True when [user] is treated as the primary “work-style” profile in the drawer (managed profile,
 * MANAGED user type, or the single non-clone secondary). Used for the Work category chip.
 */
internal fun isWorkProfileSectionUser(
        context: Context,
        user: UserHandle,
        userManager: UserManager,
        totalSecondaryProfiles: Int,
): Boolean {
    if (ProfileHeuristics.isManagedProfileForUser(userManager, user)) return true
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val launcherApps =
                try {
                    context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
                } catch (_: Exception) {
                    null
                }
        if (launcherApps != null &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
        ) {
            try {
                if (launcherApps.getLauncherUserInfo(user)?.userType ==
                                "android.os.usertype.profile.MANAGED"
                ) {
                    return true
                }
            } catch (_: Exception) {}
        }
    }
    if (ProfileHeuristics.isLikelyCloneOrParallelProfile(context, user)) return false
    return totalSecondaryProfiles == 1
}

/** True if [app] belongs to a profile shown under the Work drawer tab (non-owner only). */
fun isDrawerWorkProfileApp(context: Context, app: AppInfo): Boolean {
    val user = app.userHandle ?: return false
    val userManager =
            try {
                context.getSystemService(Context.USER_SERVICE) as? UserManager
            } catch (_: Exception) {
                null
            }
    if (userManager == null) {
        return !ProfileHeuristics.isLikelyCloneOrParallelProfile(context, user)
    }
    val myUser = Process.myUserHandle()
    val totalSecondary = userManager.userProfiles.count { it != myUser }
    return isWorkProfileSectionUser(context, user, userManager, totalSecondary)
}

internal fun profileSectionTitleForUser(
        context: Context,
        user: UserHandle,
        userManager: UserManager,
        totalSecondaryProfiles: Int,
        profileDisplayNameOverrides: Map<String, String> = emptyMap(),
): String {
    val defaultTitle =
            when {
                isWorkProfileSectionUser(context, user, userManager, totalSecondaryProfiles) ->
                        context.getString(R.string.drawer_section_work_profile)
                ProfileHeuristics.isLikelyCloneOrParallelProfile(context, user) ->
                        context.getString(R.string.drawer_section_clone_profile)
                else -> {
                    val serial =
                            try {
                                userManager.getSerialNumberForUser(user)
                            } catch (_: Exception) {
                                -1L
                            }
                    if (serial >= 0L) {
                        context.getString(R.string.drawer_section_profile_numbered, serial)
                    } else {
                        context.getString(R.string.drawer_section_other_profile)
                    }
                }
            }
    return resolvedProfileDisplayTitle(
            profileDisplayNameOverrides,
            appProfileKey(user),
            defaultTitle,
    )
}

private val alphabeticalShortcutComparator =
        compareBy<AppShortcutAction, String>(String.CASE_INSENSITIVE_ORDER) { it.appLabel }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.actionLabel }

fun sortShortcutActionsAlphabetically(actions: List<AppShortcutAction>): List<AppShortcutAction> =
        actions.sortedWith(alphabeticalShortcutComparator)

/**
 * Buckets shortcut actions by [AppShortcutAction.profileKey] using the same ordering as
 * [groupAppsIntoProfileSections] (owner block, then secondary profiles by user serial).
 */
fun groupShortcutActionsIntoProfileSections(
        context: Context,
        actions: List<AppShortcutAction>,
        allApps: List<AppInfo>,
        profileDisplayNameOverrides: Map<String, String> = emptyMap(),
): List<DrawerProfileShortcutSectionUi> {
    if (actions.isEmpty()) return emptyList()
    val byProfile = actions.groupBy { it.profileKey }
    val userManager =
            try {
                context.getSystemService(Context.USER_SERVICE) as? UserManager
            } catch (_: Exception) {
                null
            }
    if (userManager == null) {
        return buildShortcutSectionsWithoutUserManager(
                context,
                byProfile,
                allApps,
                profileDisplayNameOverrides,
        )
    }
    val byUser = allApps.filter { it.userHandle != null }.groupBy { it.userHandle!! }
    val orderedUsers =
            byUser.keys.sortedBy { uh ->
                try {
                    userManager.getSerialNumberForUser(uh)
                } catch (_: Exception) {
                    Long.MAX_VALUE
                }
            }
    return buildList {
        val ownerActions = sortShortcutActionsAlphabetically(byProfile["0"].orEmpty())
        if (ownerActions.isNotEmpty()) {
            add(
                    DrawerProfileShortcutSectionUi(
                            id = "owner",
                            title =
                                    resolvedProfileDisplayTitle(
                                            profileDisplayNameOverrides,
                                            "0",
                                            context.getString(R.string.drawer_section_personal),
                                    ),
                            actions = ownerActions,
                    )
            )
        }
        for (user in orderedUsers) {
            val pk = appProfileKey(user)
            val sectionActions = sortShortcutActionsAlphabetically(byProfile[pk].orEmpty())
            if (sectionActions.isEmpty()) continue
            add(
                    DrawerProfileShortcutSectionUi(
                            id = "u_${user.hashCode()}",
                            title =
                                    profileSectionTitleForUser(
                                            context = context,
                                            user = user,
                                            userManager = userManager,
                                            totalSecondaryProfiles = orderedUsers.size,
                                            profileDisplayNameOverrides = profileDisplayNameOverrides,
                                    ),
                            actions = sectionActions,
                    )
            )
        }
    }
}

private fun buildShortcutSectionsWithoutUserManager(
        context: Context,
        byProfile: Map<String, List<AppShortcutAction>>,
        allApps: List<AppInfo>,
        profileDisplayNameOverrides: Map<String, String>,
): List<DrawerProfileShortcutSectionUi> {
    return buildList {
        val ownerActions = sortShortcutActionsAlphabetically(byProfile["0"].orEmpty())
        if (ownerActions.isNotEmpty()) {
            add(
                    DrawerProfileShortcutSectionUi(
                            id = "owner",
                            title =
                                    resolvedProfileDisplayTitle(
                                            profileDisplayNameOverrides,
                                            "0",
                                            context.getString(R.string.drawer_section_personal),
                                    ),
                            actions = ownerActions,
                    )
            )
        }
        val byUser = allApps.filter { it.userHandle != null }.groupBy { it.userHandle!! }
        for (user in byUser.keys) {
            val pk = appProfileKey(user)
            val sectionActions = sortShortcutActionsAlphabetically(byProfile[pk].orEmpty())
            if (sectionActions.isEmpty()) continue
            val defaultTitle =
                    when {
                        byUser.keys.size == 1 &&
                                !ProfileHeuristics.isLikelyCloneOrParallelProfile(context, user) ->
                                context.getString(R.string.drawer_section_work_profile)
                        ProfileHeuristics.isLikelyCloneOrParallelProfile(context, user) ->
                                context.getString(R.string.drawer_section_clone_profile)
                        else -> context.getString(R.string.drawer_section_other_profile)
                    }
            add(
                    DrawerProfileShortcutSectionUi(
                            id = "u_${user.hashCode()}",
                            title = resolvedProfileDisplayTitle(profileDisplayNameOverrides, pk, defaultTitle),
                            actions = sectionActions,
                    )
            )
        }
    }
}
