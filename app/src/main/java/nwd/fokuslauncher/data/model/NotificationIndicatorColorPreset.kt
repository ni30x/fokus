package nwd.fokuslauncher.data.model

import androidx.annotation.StringRes
import nwd.fokuslauncher.R

/**
 * Preset ARGB colors for notification status indicators.
 *
 * Default is [RED] (`#FF5252`) — readable on both black and white themes.
 */
enum class NotificationIndicatorColorPreset(
        val argb: Int,
        @param:StringRes val labelRes: Int,
) {
    RED(0xFFFF5252.toInt(), R.string.settings_notification_indicator_color_red),
    ORANGE(0xFFFF9800.toInt(), R.string.settings_notification_indicator_color_orange),
    YELLOW(0xFFFFEB3B.toInt(), R.string.settings_notification_indicator_color_yellow),
    GREEN(0xFF4CAF50.toInt(), R.string.settings_notification_indicator_color_green),
    CYAN(0xFF00BCD4.toInt(), R.string.settings_notification_indicator_color_cyan),
    BLUE(0xFF2196F3.toInt(), R.string.settings_notification_indicator_color_blue),
    PURPLE(0xFF9C27B0.toInt(), R.string.settings_notification_indicator_color_purple),
    PINK(0xFFE91E63.toInt(), R.string.settings_notification_indicator_color_pink),
    WHITE(0xFFFFFFFF.toInt(), R.string.settings_notification_indicator_color_white),
    GRAY(0xFF9E9E9E.toInt(), R.string.settings_notification_indicator_color_gray);

    companion object {
        val DEFAULT = RED

        fun fromArgb(value: Int?): NotificationIndicatorColorPreset =
                entries.firstOrNull { it.argb == value } ?: DEFAULT
    }
}
