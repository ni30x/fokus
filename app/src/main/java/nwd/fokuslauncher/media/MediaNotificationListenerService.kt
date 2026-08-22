package nwd.fokuslauncher.media

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import nwd.fokuslauncher.notification.NotificationIndicatorRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Lets [MediaRepository] read active media sessions (including Spotify) via
 * [android.media.session.MediaSessionManager]. Also feeds [NotificationIndicatorRepository] for
 * home/drawer notification status indicators. The user must grant notification access in system
 * settings.
 */
@AndroidEntryPoint
class MediaNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var mediaRepository: MediaRepository

    @Inject lateinit var notificationIndicatorRepository: NotificationIndicatorRepository

    override fun onListenerConnected() {
        super.onListenerConnected()
        mediaRepository.onNotificationListenerConnected(
                ComponentName(this, MediaNotificationListenerService::class.java)
        )
        notificationIndicatorRepository.onListenerConnected(this)
    }

    override fun onListenerDisconnected() {
        notificationIndicatorRepository.onListenerDisconnected()
        mediaRepository.onNotificationListenerDisconnected()
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        notificationIndicatorRepository.onNotificationPosted(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        notificationIndicatorRepository.onNotificationRemoved(sbn)
    }
}
