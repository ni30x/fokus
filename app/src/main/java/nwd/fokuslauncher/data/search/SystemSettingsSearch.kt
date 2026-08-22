package nwd.fokuslauncher.data.search

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class SettingsSearchResult(
    val id: String,
    val title: String,
    val subtitle: String,
    val intentAction: String,
    val icon: ImageVector,
    val keywords: List<String>
)

object SystemSettingsSearch {

    val allSettingsList = listOf(
        SettingsSearchResult(
            id = "wifi",
            title = "Wi-Fi & Internet",
            subtitle = "Wireless networks, IP address, hotspot",
            intentAction = Settings.ACTION_WIFI_SETTINGS,
            icon = Icons.Default.Wifi,
            keywords = listOf("wifi", "wi-fi", "internet", "network", "wireless", "broadband", "connection")
        ),
        SettingsSearchResult(
            id = "bluetooth",
            title = "Bluetooth & Paired Devices",
            subtitle = "Pair headphones, smartwatch, accessories",
            intentAction = Settings.ACTION_BLUETOOTH_SETTINGS,
            icon = Icons.Default.Bluetooth,
            keywords = listOf("bluetooth", "bt", "pair", "headphones", "earbuds", "wireless", "connect")
        ),
        SettingsSearchResult(
            id = "display",
            title = "Display & Brightness",
            subtitle = "Screen brightness, dark mode, font size, timeout",
            intentAction = Settings.ACTION_DISPLAY_SETTINGS,
            icon = Icons.Default.Brightness6,
            keywords = listOf("display", "brightness", "screen", "dark mode", "font", "fontSize", "wallpaper", "night")
        ),
        SettingsSearchResult(
            id = "sound",
            title = "Sound & Vibration",
            subtitle = "Ringtone, volume, do not disturb, haptics",
            intentAction = Settings.ACTION_SOUND_SETTINGS,
            icon = Icons.Default.VolumeUp,
            keywords = listOf("sound", "volume", "ringtone", "vibrate", "vibration", "silent", "dnd", "audio")
        ),
        SettingsSearchResult(
            id = "battery",
            title = "Battery & Power",
            subtitle = "Battery saver, usage percentage, charging",
            intentAction = Settings.ACTION_BATTERY_SAVER_SETTINGS,
            icon = Icons.Default.BatteryFull,
            keywords = listOf("battery", "power", "charger", "battery saver", "charging", "energy")
        ),
        SettingsSearchResult(
            id = "storage",
            title = "Storage",
            subtitle = "Internal storage, clean space, downloads",
            intentAction = Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
            icon = Icons.Default.Storage,
            keywords = listOf("storage", "space", "memory", "sd card", "clean", "disk", "mb", "gb")
        ),
        SettingsSearchResult(
            id = "apps",
            title = "Apps & Permissions",
            subtitle = "Manage installed applications and permissions",
            intentAction = Settings.ACTION_APPLICATION_SETTINGS,
            icon = Icons.Default.Apps,
            keywords = listOf("apps", "applications", "permissions", "app info", "manage apps", "uninstall")
        ),
        SettingsSearchResult(
            id = "location",
            title = "Location",
            subtitle = "GPS, location access for apps",
            intentAction = Settings.ACTION_LOCATION_SOURCE_SETTINGS,
            icon = Icons.Default.LocationOn,
            keywords = listOf("location", "gps", "maps", "place", "tracking", "position")
        ),
        SettingsSearchResult(
            id = "security",
            title = "Security & Screen Lock",
            subtitle = "PIN, pattern, password, fingerprint, face unlock",
            intentAction = Settings.ACTION_SECURITY_SETTINGS,
            icon = Icons.Default.Lock,
            keywords = listOf("security", "lock", "screen lock", "pin", "password", "pattern", "fingerprint", "face")
        ),
        SettingsSearchResult(
            id = "accessibility",
            title = "Accessibility",
            subtitle = "TalkBack, font size, screen reader, captions",
            intentAction = Settings.ACTION_ACCESSIBILITY_SETTINGS,
            icon = Icons.Default.Accessibility,
            keywords = listOf("accessibility", "a11y", "talkback", "caption", "magnification", "text size")
        ),
        SettingsSearchResult(
            id = "privacy",
            title = "Privacy & Passwords",
            subtitle = "Camera/mic access, permission manager, autofill",
            intentAction = Settings.ACTION_PRIVACY_SETTINGS,
            icon = Icons.Default.Security,
            keywords = listOf("privacy", "passwords", "autofill", "camera access", "microphone", "permission")
        ),
        SettingsSearchResult(
            id = "date_time",
            title = "Date & Time",
            subtitle = "Clock format, time zone, automatic time",
            intentAction = Settings.ACTION_DATE_SETTINGS,
            icon = Icons.Default.Schedule,
            keywords = listOf("date", "time", "clock", "timezone", "time zone", "24 hour", "format")
        ),
        SettingsSearchResult(
            id = "language",
            title = "Language & Input",
            subtitle = "Keyboard, languages, voice typing",
            intentAction = Settings.ACTION_LOCALE_SETTINGS,
            icon = Icons.Default.Language,
            keywords = listOf("language", "input", "keyboard", "locale", "speech", "gboard", "voice")
        ),
        SettingsSearchResult(
            id = "device_info",
            title = "About Phone",
            subtitle = "Device model, Android version, build number",
            intentAction = Settings.ACTION_DEVICE_INFO_SETTINGS,
            icon = Icons.Default.Info,
            keywords = listOf("about", "phone", "device", "model", "android version", "build number", "system info")
        )
    )

    fun search(query: String): List<SettingsSearchResult> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return emptyList()

        return allSettingsList.filter { item ->
            item.title.lowercase().contains(trimmed) ||
            item.subtitle.lowercase().contains(trimmed) ||
            item.keywords.any { it.contains(trimmed) || trimmed.contains(it) }
        }
    }
}
