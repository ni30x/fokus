package nwd.fokuslauncher.ui.theme

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontFamily
import nwd.fokuslauncher.data.model.LauncherFontScale
import nwd.fokuslauncher.data.model.LauncherVisualStyle
import nwd.fokuslauncher.ui.util.ProvideSystemClickSound

private val FokusColorSchemeClassic = darkColorScheme(
        primary = White,
        onPrimary = Black,
        secondary = LightGray,
        onSecondary = Black,
        secondaryContainer = ChipBackground,
        onSecondaryContainer = White,
        background = Transparent,
        onBackground = White,
        surface = Transparent,
        onSurface = White,
        surfaceVariant = DarkGray,
        onSurfaceVariant = LightGray,
        surfaceContainerLowest = Transparent,
        surfaceContainerLow = Transparent,
        surfaceContainer = Transparent,
        surfaceContainerHigh = Transparent,
        surfaceContainerHighest = Transparent,
        surfaceBright = Transparent,
        surfaceDim = Transparent,
        inverseSurface = White,
        inverseOnSurface = Black,
        error = DestructiveRed,
        onError = Black,
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD4),
)

fun fokusColorSchemeFor(style: LauncherVisualStyle): ColorScheme {
    val palette = style.neonPalette() ?: return FokusColorSchemeClassic
    val sheetSurface = palette.primary.copy(alpha = 0.09f).compositeOver(Black)
    val segmentSelectedSurface =
            palette.primary.copy(alpha = 0.22f).compositeOver(Black)
    return FokusColorSchemeClassic.copy(
            primary = palette.primary,
            secondary = palette.muted,
            onBackground = palette.primary,
            onSurface = palette.primary,
            secondaryContainer = segmentSelectedSurface,
            onSecondaryContainer = palette.primary,
            surfaceVariant = sheetSurface,
            onSurfaceVariant = palette.muted,
            error = NeonDestructiveRed,
            onError = Black,
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD4),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FokusLauncherTheme(
        fontFamily: FontFamily = FontFamily.Default,
        fontScale: Float = 1f,
        visualStyle: LauncherVisualStyle = LauncherVisualStyle.CLASSIC,
        glowEnabled: Boolean = false,
        content: @Composable () -> Unit
) {
    val colorScheme = remember(visualStyle) { fokusColorSchemeFor(visualStyle) }
    val typography =
            remember(fontFamily, fontScale, visualStyle, glowEnabled) {
                fokusTypographyForLauncher(
                        fontFamily,
                        fontScale,
                        visualStyle,
                        glowEnabled,
                )
            }
    val resolvedIconScale =
            remember(fontScale) {
                fontScale.coerceIn(LauncherFontScale.MIN, LauncherFontScale.MAX)
            }
    val iconGlow =
            remember(visualStyle, glowEnabled) {
                if (!glowEnabled) {
                    LauncherIconGlowSpec.None
                } else {
                    val palette = visualStyle.neonPalette()
                    val halo = palette?.primary ?: White
                    LauncherIconGlowSpec(enabled = true, haloColor = halo)
                }
            }
    CompositionLocalProvider(
            LocalOverscrollFactory provides null,
            LocalLauncherFontScale provides resolvedIconScale,
            LocalLauncherIconGlow provides iconGlow,
    ) {
        ProvideSystemClickSound {
            MaterialTheme(colorScheme = colorScheme, typography = typography, content = content)
        }
    }
}

@Composable
fun HomeDrawerBlackTextTheme(
        enabled: Boolean,
        content: @Composable () -> Unit,
) {
    if (!enabled) {
        content()
        return
    }
    val currentColorScheme = MaterialTheme.colorScheme
    val blackColorScheme = remember(currentColorScheme) {
        currentColorScheme.copy(
                primary = Color.Black,
                onPrimary = Color.White,
                secondary = Color(0xFF333333),
                onSecondary = Color.White,
                secondaryContainer = Color(0x1F000000),
                onSecondaryContainer = Color.Black,
                background = Color.Transparent,
                onBackground = Color.Black,
                surface = Color.Transparent,
                onSurface = Color.Black,
                surfaceVariant = Color(0xFFE0E0E0),
                onSurfaceVariant = Color(0xFF444444),
                outline = Color(0xFF555555),
                outlineVariant = Color(0xFF888888),
        )
    }
    val currentTypography = MaterialTheme.typography
    val blackTypography = remember(currentTypography) {
        currentTypography.copy(
                displayLarge = currentTypography.displayLarge.copy(color = Color.Black).withoutLauncherTextGlow(),
                displayMedium = currentTypography.displayMedium.copy(color = Color.Black).withoutLauncherTextGlow(),
                displaySmall = currentTypography.displaySmall.copy(color = Color.Black).withoutLauncherTextGlow(),
                headlineLarge = currentTypography.headlineLarge.copy(color = Color.Black).withoutLauncherTextGlow(),
                headlineMedium = currentTypography.headlineMedium.copy(color = Color.Black).withoutLauncherTextGlow(),
                headlineSmall = currentTypography.headlineSmall.copy(color = Color.Black).withoutLauncherTextGlow(),
                titleLarge = currentTypography.titleLarge.copy(color = Color.Black).withoutLauncherTextGlow(),
                titleMedium = currentTypography.titleMedium.copy(color = Color(0xFF333333)).withoutLauncherTextGlow(),
                titleSmall = currentTypography.titleSmall.copy(color = Color(0xFF333333)).withoutLauncherTextGlow(),
                bodyLarge = currentTypography.bodyLarge.copy(color = Color.Black).withoutLauncherTextGlow(),
                bodyMedium = currentTypography.bodyMedium.copy(color = Color.Black).withoutLauncherTextGlow(),
                bodySmall = currentTypography.bodySmall.copy(color = Color(0xFF333333)).withoutLauncherTextGlow(),
                labelLarge = currentTypography.labelLarge.copy(color = Color.Black).withoutLauncherTextGlow(),
                labelMedium = currentTypography.labelMedium.copy(color = Color.Black).withoutLauncherTextGlow(),
                labelSmall = currentTypography.labelSmall.copy(color = Color(0xFF333333)).withoutLauncherTextGlow(),
        )
    }
    CompositionLocalProvider(
            LocalContentColor provides Color.Black,
            LocalLauncherIconGlow provides LauncherIconGlowSpec.None,
    ) {
        MaterialTheme(
                colorScheme = blackColorScheme,
                typography = blackTypography,
                content = content,
        )
    }
}
