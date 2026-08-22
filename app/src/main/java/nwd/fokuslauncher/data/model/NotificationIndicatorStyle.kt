package nwd.fokuslauncher.data.model

import androidx.annotation.StringRes
import nwd.fokuslauncher.R

/**
 * How notification status is shown on home favorites and drawer list items.
 *
 * - [DOT]: Small circular accent beside the app label.
 * - [COLORED_LABEL]: Tint the app label with the indicator color.
 */
enum class NotificationIndicatorStyle(@param:StringRes val labelRes: Int) {
    DOT(R.string.settings_notification_indicator_style_dot),
    COLORED_LABEL(R.string.settings_notification_indicator_style_colored_label);

    companion object {
        fun fromString(value: String?): NotificationIndicatorStyle =
                entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DOT
    }
}
