package nwd.fokuslauncher.notification

import android.app.Notification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationIndicatorKeysTest {

    @Test
    fun `notificationAppKey uses profile pipe package format`() {
        assertEquals("0|com.example.app", notificationAppKey("com.example.app", null))
    }

    @Test
    fun `notificationAppKeyOrNull ignores group summaries`() {
        assertNull(
                notificationAppKeyOrNull(
                        packageName = "com.example.app",
                        flags = Notification.FLAG_GROUP_SUMMARY,
                        userHandle = null,
                        ownPackageName = "nwd.fokuslauncher",
                )
        )
    }

    @Test
    fun `notificationAppKeyOrNull ignores own package`() {
        assertNull(
                notificationAppKeyOrNull(
                        packageName = "nwd.fokuslauncher",
                        flags = 0,
                        userHandle = null,
                        ownPackageName = "nwd.fokuslauncher",
                )
        )
    }

    @Test
    fun `notificationAppKeyOrNull returns key for normal notification`() {
        assertEquals(
                "0|com.example.mail",
                notificationAppKeyOrNull(
                        packageName = "com.example.mail",
                        flags = 0,
                        userHandle = null,
                        ownPackageName = "nwd.fokuslauncher",
                ),
        )
    }

    @Test
    fun `appsWithNotificationsFromEntries builds unique keys and skips ignored`() {
        val own = "nwd.fokuslauncher"
        val keys =
                appsWithNotificationsFromEntries(
                        listOf(
                                Triple("com.example.mail", 0, null),
                                Triple("com.example.mail", Notification.FLAG_GROUP_SUMMARY, null),
                                Triple(own, 0, null),
                                Triple("com.example.chat", 0, null),
                        ),
                        own,
                )

        assertEquals(setOf("0|com.example.mail", "0|com.example.chat"), keys)
    }

    @Test
    fun `appsWithNotificationsFrom returns empty for null or empty`() {
        assertTrue(appsWithNotificationsFrom(null, "nwd.fokuslauncher").isEmpty())
        assertTrue(appsWithNotificationsFrom(emptyArray(), "nwd.fokuslauncher").isEmpty())
    }
}
