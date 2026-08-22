package nwd.fokuslauncher.ui.components

import nwd.fokuslauncher.ui.util.clickableNoRippleWithSystemSound
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nwd.fokuslauncher.ui.theme.LocalPhotoWallpaperOutlineWidthDp

@Composable
fun PomodoroWidget(
    timeRemainingText: String,
    isRunning: Boolean,
    isBreak: Boolean,
    modifier: Modifier = Modifier,
    outlined: Boolean = false,
    onClick: () -> Unit = {},
) {
    val textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    val textColor = if (isBreak) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onBackground
    val backdropStrength = LocalPhotoWallpaperOutlineWidthDp.current
    val useSharedBackdrop = outlined && backdropStrength > 0f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickableNoRippleWithSystemSound(onClick = onClick)
            .then(
                if (useSharedBackdrop) {
                    Modifier.photoBackdropPill(
                        strength = backdropStrength,
                        horizontalPaddingMin = 5.dp,
                        horizontalPaddingMax = 18.dp,
                        verticalPaddingMin = 2.dp,
                        verticalPaddingMax = 8.dp,
                    )
                } else {
                    Modifier
                }
            )
            .testTag("pomodoro_widget"),
    ) {
        Icon(
            imageVector = Icons.Default.Timer,
            contentDescription = "Pomodoro Timer",
            tint = textColor,
            modifier = Modifier.padding(end = 4.dp).size(18.dp)
        )
        if (outlined && !useSharedBackdrop) {
            OutlinedText(
                text = timeRemainingText,
                style = textStyle,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        } else {
            Text(
                text = timeRemainingText,
                style = textStyle,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }
}
