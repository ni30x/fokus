package nwd.fokuslauncher.notification

import android.app.Notification
import android.os.UserHandle
import android.service.notification.StatusBarNotification
import nwd.fokuslauncher.data.model.appMetadataKey
import nwd.fokuslauncher.data.model.appProfileKey

/** Builds the stable package+profile key used to match home/drawer rows. */
fun notificationAppKey(packageName: String, userHandle: UserHandle?): String =
        appMetadataKey(packageName, appProfileKey(userHandle))

/**
 * Returns the app key when a notification should contribute to indicators, or null when it should
 * be ignored (group summaries, own package, blank package).
 */
fun notificationAppKeyOrNull(
        packageName: String?,
        flags: Int,
        userHandle: UserHandle?,
        ownPackageName: String,
): String? {
    if (flags and Notification.FLAG_GROUP_SUMMARY != 0) return null
    if (packageName.isNullOrBlank() || packageName == ownPackageName) return null
    return notificationAppKey(packageName, userHandle)
}

/**
 * Returns the app key for [sbn] when it should contribute to notification indicators, or null when
 * the notification should be ignored.
 */
fun notificationAppKeyOrNull(
        sbn: StatusBarNotification,
        ownPackageName: String,
): String? =
        notificationAppKeyOrNull(
                packageName = sbn.packageName,
                flags = sbn.notification.flags,
                userHandle = sbn.user,
                ownPackageName = ownPackageName,
        )

/** Builds the set of apps with active (non-ignored) notifications from a listener snapshot. */
fun appsWithNotificationsFrom(
        notifications: Array<StatusBarNotification>?,
        ownPackageName: String,
): Set<String> {
    if (notifications.isNullOrEmpty()) return emptySet()
    return buildSet {
        for (sbn in notifications) {
            notificationAppKeyOrNull(sbn, ownPackageName)?.let(::add)
        }
    }
}

/** Builds the set of apps with notifications from package/flag/user triples (for unit tests). */
fun appsWithNotificationsFromEntries(
        entries: List<Triple<String?, Int, UserHandle?>>,
        ownPackageName: String,
): Set<String> =
        buildSet {
            for ((packageName, flags, userHandle) in entries) {
                notificationAppKeyOrNull(packageName, flags, userHandle, ownPackageName)?.let(::add)
            }
        }
