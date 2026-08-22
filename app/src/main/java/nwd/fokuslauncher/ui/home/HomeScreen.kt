package nwd.fokuslauncher.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.IntOffset
import nwd.fokuslauncher.data.model.HiddenAppInfo
import nwd.fokuslauncher.ui.util.clickableWithSystemSound
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nwd.fokuslauncher.R
import nwd.fokuslauncher.data.model.LauncherFontScale
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import nwd.fokuslauncher.data.model.AppInfo
import nwd.fokuslauncher.data.model.appProfileKey
import nwd.fokuslauncher.ui.settings.ShortcutActionPickerDialog
import nwd.fokuslauncher.ui.drawer.profileOriginLabelForFavorite
import nwd.fokuslauncher.data.model.FavoriteApp
import nwd.fokuslauncher.data.model.HomeAlignment
import nwd.fokuslauncher.data.model.HomeExtraWidgetEntry
import nwd.fokuslauncher.data.model.HomeShortcut
import nwd.fokuslauncher.data.model.NotificationIndicatorStyle
import nwd.fokuslauncher.data.model.drawerOpenCountKey
import nwd.fokuslauncher.ui.components.HomeExtraChipsRow
import nwd.fokuslauncher.ui.components.ClockWidget
import nwd.fokuslauncher.ui.components.DateBatteryRow
import nwd.fokuslauncher.ui.components.FokusBottomSheet
import nwd.fokuslauncher.ui.components.MediaWidget
import nwd.fokuslauncher.ui.components.PomodoroWidget
import nwd.fokuslauncher.ui.components.ScreenTimeWidget
import nwd.fokuslauncher.ui.components.FokusOutlinedButton
import nwd.fokuslauncher.ui.components.LauncherIcon
import nwd.fokuslauncher.ui.components.MinimalIcons
import nwd.fokuslauncher.ui.components.OutlinedText
import nwd.fokuslauncher.ui.components.SheetActionRow
import nwd.fokuslauncher.ui.components.WeatherWidget
import nwd.fokuslauncher.ui.theme.LocalLauncherFontScale
import nwd.fokuslauncher.ui.theme.LocalPhotoWallpaperOutlineWidthDp
import nwd.fokuslauncher.ui.util.OnResumeEffect
import nwd.fokuslauncher.ui.util.clickableNoRippleWithSystemSound
import nwd.fokuslauncher.ui.util.combinedClickableWithSystemSound
import nwd.fokuslauncher.ui.util.LocalSystemClickSound
import nwd.fokuslauncher.utils.LockScreenHelper

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    onOpenSettings: () -> Unit = {},
    onOpenEditHomeApps: () -> Unit = {},
    onOpenEditShortcuts: () -> Unit = {},
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clockUiState by viewModel.clockUiState.collectAsStateWithLifecycle()
    val weatherUiState by viewModel.weatherUiState.collectAsStateWithLifecycle()
    val mediaUiState by viewModel.mediaUiState.collectAsStateWithLifecycle()
    val screenTimeUiState by viewModel.screenTimeUiState.collectAsStateWithLifecycle()
    val worldClockUiState by viewModel.worldClockUiState.collectAsStateWithLifecycle()
    val countdownUiState by viewModel.countdownUiState.collectAsStateWithLifecycle()
    val pomodoroUiState by viewModel.pomodoroUiState.collectAsStateWithLifecycle()
    val homeExtraWidgets by viewModel.homeExtraWidgets.collectAsStateWithLifecycle()
    val notificationIndicatorUiState by
            viewModel.notificationIndicatorUiState.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val rightSideShortcuts by viewModel.rightSideShortcuts.collectAsStateWithLifecycle()
    val allInstalledApps by viewModel.allInstalledApps.collectAsStateWithLifecycle()
    val allShortcutActions by viewModel.allShortcutActions.collectAsStateWithLifecycle()
    val profileDisplayNameOverrides by viewModel.profileDisplayNameOverrides.collectAsStateWithLifecycle()
    val categoryOptions by viewModel.categoryOptions.collectAsStateWithLifecycle()
    val showWeatherAppPicker by viewModel.showWeatherAppPicker.collectAsStateWithLifecycle()
    val appMenuTarget by viewModel.appMenuTarget.collectAsStateWithLifecycle()
    val showHomeScreenMenu by viewModel.showHomeScreenMenu.collectAsStateWithLifecycle()
    val hiddenApps by viewModel.hiddenApps.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val onFavoriteClick = viewModel::launchFavorite
    val onFavoriteLongPress = viewModel::onFavoriteLongPress
    val onHomeLongPress = viewModel::onHomeScreenLongPress
    val onShortcutClick = viewModel::launchShortcut
    val onSetDefaultLauncher = viewModel::openDefaultLauncherSettings
    val onClockClick = viewModel::openClockApp
    val onDateClick = viewModel::openCalendarApp
    val onWeatherClick = viewModel::openWeatherAppPicker
    val onScreenTimeClick = viewModel::openDigitalWellbeing
    val onDoubleTapEmptyLock = viewModel::onDoubleTapEmptyLock

    LaunchedEffect(viewModel) {
        viewModel.requestLockAccessibilitySettings.collect {
            LockScreenHelper.openAccessibilitySettings(context)
        }
    }

    OnResumeEffect(lifecycleOwner, viewModel, alsoRunIfAlreadyResumed = true) {
        viewModel.recheckDefaultLauncher()
        viewModel.refreshDoubleTapLockEffective()
        viewModel.refreshWeather()
        viewModel.refreshMedia()
        viewModel.refreshNotificationIndicators()
        viewModel.refreshScreenTime()
    }

    Box(modifier = modifier.fillMaxSize()) {
        HomeScreenContent(
            uiState = uiState,
            clockUiState = clockUiState,
            weatherUiState = weatherUiState,
            mediaUiState = mediaUiState,
            screenTimeUiState = screenTimeUiState,
            worldClockUiState = worldClockUiState,
            countdownUiState = countdownUiState,
            pomodoroUiState = pomodoroUiState,

            homeExtraWidgets = homeExtraWidgets,
            notificationIndicatorUiState = notificationIndicatorUiState,
            favorites = favorites,
            installedApps = allInstalledApps,
            rightSideShortcuts = rightSideShortcuts,
            profileDisplayNameOverrides = profileDisplayNameOverrides,
            onLabelClick = onFavoriteClick,
            onLabelLongPress = onFavoriteLongPress,
            onHomeScreenLongPress = onHomeLongPress,
            onIconClick = onShortcutClick,
            onSetDefaultLauncher = onSetDefaultLauncher,
            onClockClick = onClockClick,
            onDateClick = onDateClick,
            onWeatherClick = onWeatherClick,
            onScreenTimeClick = onScreenTimeClick,
            onPomodoroClick = viewModel::togglePomodoro,
            onMediaOpenApp = viewModel::mediaOpenApp,
            onMediaPrevious = viewModel::mediaSkipToPrevious,
            onMediaPlayPause = viewModel::mediaPlayPause,
            onMediaNext = viewModel::mediaSkipToNext,
            onMediaLike = viewModel::mediaLike,
            onMediaSave = viewModel::mediaSave,
            doubleTapEmptyLockEnabled = uiState.doubleTapEmptyLockEnabled,
            onDoubleTapEmptyLock = onDoubleTapEmptyLock,
        )
    }

    // ── Dialogs & sheets (render as overlay windows) ────────────────

    // App menu bottom sheet (opened directly on long-press)
    appMenuTarget?.let { fav ->
        val currentCategory =
                allInstalledApps
                        .firstOrNull {
                            it.packageName == fav.packageName &&
                                    appProfileKey(it.userHandle) == fav.profileKey
                        }
                        ?.category
                        .orEmpty()
        HomeAppMenuSheet(
            fav = fav,
            currentCategory = currentCategory,
            categoryOptions = categoryOptions,
            onDismiss = { viewModel.dismissAppMenu() },
            onRename = { newName -> viewModel.renameApp(fav, newName) },
            onSetCategory = { category -> viewModel.setFavoriteCategory(fav, category) },
            onRemoveFromHome = { viewModel.removeFavorite(fav) },
            onEditHomeScreen = {
                viewModel.dismissAppMenu()
                onOpenEditHomeApps()
            },
            onAppInfo = { viewModel.openAppInfo(fav) },
            onHide = { viewModel.hideApp(fav) },
            onUninstall = { viewModel.uninstallApp(fav) }
        )
    }

    if (showHomeScreenMenu) {
        HomeScreenLongPressSheet(
            onDismiss = { viewModel.dismissHomeScreenMenu() },
            onEditHomeScreen = {
                viewModel.dismissHomeScreenMenu()
                onOpenEditHomeApps()
            },
            onEditShortcuts = {
                viewModel.dismissHomeScreenMenu()
                onOpenEditShortcuts()
            },
            onOpenSettings = {
                viewModel.dismissHomeScreenMenu()
                onOpenSettings()
            },
            onTogglePomodoro = {
                viewModel.dismissHomeScreenMenu()
                viewModel.togglePomodoroVisibility()
            }
        )
    }

    if (showWeatherAppPicker) {
        ShortcutActionPickerDialog(
            allActions = allShortcutActions,
            allApps = allInstalledApps,
            title = stringResource(R.string.home_weather_app_picker_title),
            onSelect = viewModel::setPreferredWeatherTap,
            onDismiss = { viewModel.closeWeatherAppPicker() },
            profileDisplayNameOverrides = profileDisplayNameOverrides,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    clockUiState: HomeClockUiState,
    weatherUiState: HomeWeatherUiState,
    favorites: List<FavoriteApp>,
    rightSideShortcuts: List<HomeShortcut>,
    profileDisplayNameOverrides: Map<String, String> = emptyMap(),
    onLabelClick: (FavoriteApp) -> Unit,
    onIconClick: (HomeShortcut) -> Unit,
    modifier: Modifier = Modifier,
    mediaUiState: HomeMediaUiState = HomeMediaUiState(),
    screenTimeUiState: HomeScreenTimeUiState = HomeScreenTimeUiState(),
    worldClockUiState: HomeWorldClockUiState = HomeWorldClockUiState(),
    countdownUiState: HomeCountdownUiState = HomeCountdownUiState(),
    pomodoroUiState: HomePomodoroUiState = HomePomodoroUiState(),

    homeExtraWidgets: List<HomeExtraWidgetEntry> = emptyList(),
    notificationIndicatorUiState: HomeNotificationIndicatorUiState =
            HomeNotificationIndicatorUiState(),
    installedApps: List<AppInfo> = emptyList(),
    onLabelLongPress: (FavoriteApp) -> Unit = {},
    onHomeScreenLongPress: () -> Unit = {},
    onSetDefaultLauncher: () -> Unit = {},
    onClockClick: () -> Unit = {},
    onDateClick: () -> Unit = {},
    onWeatherClick: () -> Unit = {},
    onScreenTimeClick: () -> Unit = {},
    onPomodoroClick: () -> Unit = {},

    onMediaOpenApp: () -> Unit = {},
    onMediaPrevious: () -> Unit = {},
    onMediaPlayPause: () -> Unit = {},
    onMediaNext: () -> Unit = {},
    onMediaLike: () -> Unit = {},
    onMediaSave: () -> Unit = {},
    doubleTapEmptyLockEnabled: Boolean = false,
    onDoubleTapEmptyLock: () -> Unit = {},
) {
    val play = LocalSystemClickSound.current
    val noIndication = remember { MutableInteractionSource() }
    val outlineWidthDp =
            if (uiState.usesPhotoWallpaper) uiState.photoWallpaperOutlineWidthDp else 0f
    Box(
        modifier = modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .combinedClickable(
                indication = null,
                interactionSource = noIndication,
                onClick = { },
                onLongClick = onHomeScreenLongPress,
                onDoubleClick = if (doubleTapEmptyLockEnabled) {
                    {
                        play()
                        onDoubleTapEmptyLock()
                    }
                } else null
            )
            .testTag("home_screen")
    ) {
        CompositionLocalProvider(LocalPhotoWallpaperOutlineWidthDp provides outlineWidthDp) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility)
                .padding(top = 48.dp)
                .navigationBarsPadding()
                .padding(bottom = 48.dp)
        ) {
            HomeWidgetsSection(
                uiState = uiState,
                clockUiState = clockUiState,
            weatherUiState = weatherUiState,
            mediaUiState = mediaUiState,
            screenTimeUiState = screenTimeUiState,
            worldClockUiState = worldClockUiState,
            countdownUiState = countdownUiState,
            pomodoroUiState = pomodoroUiState,

            homeExtraWidgets = homeExtraWidgets,
            onClockClick = onClockClick,
            onDateClick = onDateClick,
            onWeatherClick = onWeatherClick,
            onScreenTimeClick = onScreenTimeClick,
            onPomodoroClick = onPomodoroClick,

            onMediaOpenApp = onMediaOpenApp,
                onMediaPrevious = onMediaPrevious,
                onMediaPlayPause = onMediaPlayPause,
                onMediaNext = onMediaNext,
                onMediaLike = onMediaLike,
                onMediaSave = onMediaSave,
                outlined = uiState.usesPhotoWallpaper,
            )

            Spacer(modifier = Modifier.weight(1f))

            HomeFavoritesSection(
                homeAlignment = uiState.homeAlignment,
                favorites = favorites,
                installedApps = installedApps,
                rightSideShortcuts = rightSideShortcuts,
                profileDisplayNameOverrides = profileDisplayNameOverrides,
                launcherFontScale = uiState.launcherFontScale,
                outlined = uiState.usesPhotoWallpaper,
                notificationIndicatorUiState = notificationIndicatorUiState,
                onLabelClick = onLabelClick,
                onLabelLongPress = onLabelLongPress,
                onIconClick = onIconClick
            )

            if (uiState.homeAlignment == HomeAlignment.MIDDLE) {
                Spacer(modifier = Modifier.weight(1f))
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        HomeDefaultLauncherBanner(
            isDefaultLauncher = uiState.isDefaultLauncher,
            onSetDefaultLauncher = onSetDefaultLauncher
        )
        }
    }
}

@Composable
private fun rememberTitleMediumRowHeight(extra: Dp = 8.dp): Dp {
    val density = LocalDensity.current
    val style = MaterialTheme.typography.titleMedium
    return remember(density, style, extra) { with(density) { style.fontSize.toDp() } + extra }
}

/**
 * Clock [TopStart], weather [TopEnd] on the full content width. Baseline-based placement was wrong:
 * the clock’s first baseline sits far below the top, which shoved weather down into the AM/PM
 * cluster. A small top inset on weather matches roughly where [displayLarge] glyphs start.
 * Screen time sits under weather via [Modifier.offset] so it does not expand the header height.
 */
@Composable
private fun HomeClockWeatherHeader(
        clockUiState: HomeClockUiState,
        weatherUiState: HomeWeatherUiState,
        screenTimeUiState: HomeScreenTimeUiState,
        showWeather: Boolean,
        onClockClick: () -> Unit,
        onWeatherClick: () -> Unit,
        onScreenTimeClick: () -> Unit,
        outlined: Boolean,
) {
    val density = LocalDensity.current
    val clockStyle = MaterialTheme.typography.displayLarge
    val weatherTopPad =
            remember(clockStyle, density.density, density.fontScale) {
                val lead =
                        ((clockStyle.lineHeight.value - clockStyle.fontSize.value) / 2f)
                                .coerceAtLeast(0f)
                with(density) { lead.sp.toDp() }
            }
    val launcherScale =
            LocalLauncherFontScale.current.coerceIn(LauncherFontScale.MIN, LauncherFontScale.MAX)
    val weatherLowerInset =
            remember(density.density, density.fontScale, launcherScale) {
                with(density) { (10f * launcherScale).sp.toDp() } + 8.dp
            }
    val weatherRowHeight = rememberTitleMediumRowHeight()
    val weatherTop = weatherTopPad + weatherLowerInset
    val screenTimeTop =
            weatherTop + if (showWeather) weatherRowHeight + 4.dp else 0.dp
    Box(modifier = Modifier.fillMaxWidth()) {
        if (showWeather) {
            WeatherWidget(
                    weather = weatherUiState.weather,
                    useFahrenheit = weatherUiState.weatherUseFahrenheit,
                    prominent = false,
                    outlined = outlined,
                    onClick = onWeatherClick,
                    modifier =
                            Modifier.align(Alignment.TopEnd)
                                    .padding(top = weatherTop),
            )
        }
        if (screenTimeUiState.showWidget) {
            ScreenTimeWidget(
                    durationText = screenTimeUiState.durationText.orEmpty(),
                    outlined = outlined,
                    onClick = onScreenTimeClick,
                    modifier =
                            Modifier.align(Alignment.TopEnd)
                                    .offset(y = screenTimeTop),
            )
        }
        ClockWidget(
                time = clockUiState.currentTime,
                is24HourFormat = clockUiState.is24HourFormat,
                outlined = outlined,
                onClick = onClockClick,
                modifier = Modifier
                        .align(Alignment.TopStart)
                        .testTag("clock_widget"),
        )
    }
}

@Composable
private fun HomeWidgetsSection(
    uiState: HomeUiState,
    clockUiState: HomeClockUiState,
    weatherUiState: HomeWeatherUiState,
    mediaUiState: HomeMediaUiState,
    screenTimeUiState: HomeScreenTimeUiState,
    worldClockUiState: HomeWorldClockUiState,
    countdownUiState: HomeCountdownUiState,
    pomodoroUiState: HomePomodoroUiState,

    homeExtraWidgets: List<HomeExtraWidgetEntry>,
    onClockClick: () -> Unit,
    onDateClick: () -> Unit,
    onWeatherClick: () -> Unit,
    onScreenTimeClick: () -> Unit,
    onPomodoroClick: () -> Unit,

    onMediaOpenApp: () -> Unit,
    onMediaPrevious: () -> Unit,
    onMediaPlayPause: () -> Unit,
    onMediaNext: () -> Unit,
    onMediaLike: () -> Unit,
    onMediaSave: () -> Unit,
    outlined: Boolean,
) {
    val showClock = uiState.showHomeClock
    val showWeather = uiState.showHomeWeather && weatherUiState.showWeatherWidget
    val showDateOrBattery = uiState.showHomeDate || uiState.showHomeBattery
    val weatherRowHeight = rememberTitleMediumRowHeight()

    when {
        showClock -> {
            HomeClockWeatherHeader(
                    clockUiState = clockUiState,
                    weatherUiState = weatherUiState,
                    screenTimeUiState = screenTimeUiState,
                    showWeather = showWeather,
                    onClockClick = onClockClick,
                    onWeatherClick = onWeatherClick,
                    onScreenTimeClick = onScreenTimeClick,
                    outlined = outlined,
            )
        }
        showWeather || screenTimeUiState.showWidget -> {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (showWeather) {
                    WeatherWidget(
                            weather = weatherUiState.weather,
                            useFahrenheit = weatherUiState.weatherUseFahrenheit,
                            prominent = false,
                            outlined = outlined,
                            onClick = onWeatherClick,
                            modifier = Modifier.align(Alignment.TopEnd),
                    )
                }
                if (screenTimeUiState.showWidget) {
                    ScreenTimeWidget(
                            durationText = screenTimeUiState.durationText.orEmpty(),
                            outlined = outlined,
                            onClick = onScreenTimeClick,
                            modifier =
                                    Modifier.align(Alignment.TopEnd)
                                            .offset(
                                                    y =
                                                            if (showWeather) {
                                                                weatherRowHeight + 4.dp
                                                            } else {
                                                                0.dp
                                                            },
                                            ),
                    )
                }
            }
        }
    }

    if (showDateOrBattery) {
        DateBatteryRow(
                date = clockUiState.currentDate,
                batteryPercent = clockUiState.batteryPercent,
                showDate = uiState.showHomeDate,
                showBattery = uiState.showHomeBattery,
                outlined = outlined,
                onDateClick = onDateClick,
                modifier =
                        Modifier.fillMaxWidth()
                                .padding(top = if (showClock) 8.dp else 0.dp)
                                .testTag("date_battery_row"),
        )
    }

    val belowHeaderTopPad =
            if (showDateOrBattery || showClock || showWeather || screenTimeUiState.showWidget) {
                8.dp
            } else {
                0.dp
            }
    // A bit of air under the date line so extras don't sit flush against it.
    val extrasTopPad = if (showDateOrBattery) 8.dp else belowHeaderTopPad
    var nextTopPad = belowHeaderTopPad

    val extraChips =
            remember(homeExtraWidgets, worldClockUiState, countdownUiState) {
                homeExtraWidgets.mapNotNull { entry ->
                    when (entry) {
                        is HomeExtraWidgetEntry.WorldClock ->
                                worldClockUiState.citiesById[entry.cityId]?.let {
                                    HomeExtraChipUi.WorldClock(it)
                                }
                        is HomeExtraWidgetEntry.Countdown ->
                                countdownUiState.eventsById[entry.eventId]?.let { event ->
                                    if (event.title.isNotBlank() &&
                                                    event.remainingText.isNotBlank()
                                    ) {
                                        HomeExtraChipUi.Countdown(
                                                event.title,
                                                event.remainingText,
                                        )
                                    } else {
                                        null
                                    }
                                }
                    }
                }
            }
    if (extraChips.isNotEmpty()) {
        HomeExtraChipsRow(
                chips = extraChips,
                outlined = outlined,
                modifier = Modifier.fillMaxWidth().padding(top = extrasTopPad),
        )
        nextTopPad = 8.dp
    } else {
        nextTopPad = belowHeaderTopPad
    }

    if (pomodoroUiState.showWidget) {
        PomodoroWidget(
            timeRemainingText = pomodoroUiState.formattedTime,
            isRunning = pomodoroUiState.isRunning,
            isBreak = pomodoroUiState.isBreak,
            outlined = outlined,
            onClick = onPomodoroClick,
            modifier = Modifier.fillMaxWidth().padding(top = nextTopPad)
        )
        nextTopPad = 8.dp
    }

    val playback = mediaUiState.playback
    if (mediaUiState.showWidget && playback != null) {
        MediaWidget(
                title = playback.title,
                artist = playback.artist,
                isPlaying = playback.isPlaying,
                isBuffering = playback.isBuffering,
                canSkipToPrevious = playback.canSkipToPrevious,
                canSkipToNext = playback.canSkipToNext,
                like = playback.like,
                save = playback.save,
                outlined = outlined,
                onOpenApp = onMediaOpenApp,
                onLike = onMediaLike,
                onPrevious = onMediaPrevious,
                onPlayPause = onMediaPlayPause,
                onNext = onMediaNext,
                onSave = onMediaSave,
                modifier = Modifier.fillMaxWidth().padding(top = nextTopPad),
        )
    }
}

@Composable
private fun FavoritesList(
    favorites: List<FavoriteApp>,
    installedApps: List<AppInfo>,
    profileDisplayNameOverrides: Map<String, String>,
    horizontalAlignment: Alignment.Horizontal,
    onLabelClick: (FavoriteApp) -> Unit,
    onLabelLongPress: (FavoriteApp) -> Unit,
    modifier: Modifier = Modifier,
    outlined: Boolean = false,
    notificationIndicatorUiState: HomeNotificationIndicatorUiState =
            HomeNotificationIndicatorUiState(),
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = horizontalAlignment,
    ) {
        favorites.forEach { fav ->
            FavoriteAppItem(
                fav = fav,
                installedApps = installedApps,
                profileDisplayNameOverrides = profileDisplayNameOverrides,
                onClick = { onLabelClick(fav) },
                onLongPress = { onLabelLongPress(fav) },
                horizontalAlignment = horizontalAlignment,
                outlined = outlined,
                notificationIndicatorUiState = notificationIndicatorUiState,
            )
        }
    }
}

@Composable
private fun ShortcutIconsColumn(
    shortcuts: List<HomeShortcut>,
    onIconClick: (HomeShortcut) -> Unit,
    iconSize: Dp,
    touchTargetSize: Dp,
    iconAlignment: Alignment,
    verticalSpacing: Dp,
    modifier: Modifier = Modifier,
    outlined: Boolean = false,
) {
    Column(
        modifier = modifier.wrapContentHeight(align = Alignment.Bottom),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RightShortcutIcons(
                shortcuts = shortcuts,
                onIconClick = onIconClick,
                iconSize = iconSize,
                touchTargetSize = touchTargetSize,
                iconAlignment = iconAlignment,
                outlined = outlined,
        )
    }
}

@Composable
private fun HomeFavoritesSection(
    homeAlignment: HomeAlignment,
    favorites: List<FavoriteApp>,
    installedApps: List<AppInfo>,
    rightSideShortcuts: List<HomeShortcut>,
    profileDisplayNameOverrides: Map<String, String>,
    launcherFontScale: Float,
    outlined: Boolean,
    notificationIndicatorUiState: HomeNotificationIndicatorUiState =
            HomeNotificationIndicatorUiState(),
    onLabelClick: (FavoriteApp) -> Unit,
    onLabelLongPress: (FavoriteApp) -> Unit,
    onIconClick: (HomeShortcut) -> Unit,
) {
    val sc =
            launcherFontScale.coerceIn(LauncherFontScale.MIN, LauncherFontScale.MAX)
    // Base dp only: [LauncherIcon] applies [launcherIconDp] so shortcut size tracks font scale once.
    val shortcutIconSize = 24.dp
    val shortcutTouchTargetSize = (48f * sc).dp
    val backdropStrength =
            if (outlined) {
                (LocalPhotoWallpaperOutlineWidthDp.current / 100f).coerceIn(0f, 1f)
            } else {
                0f
            }
    val shortcutIconSpacing = ((8f + 16f * backdropStrength) * sc).dp
    val shortcutGutter = (24f * sc).dp
    val shortcutRowTopSpacer = (20f * sc).dp

    val listModifier =
        Modifier.fillMaxWidth().testTag("favorites_list")
    when (homeAlignment) {
        HomeAlignment.CENTER, HomeAlignment.MIDDLE ->
            Column(
                modifier = listModifier,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FavoritesList(
                    favorites = favorites,
                    installedApps = installedApps,
                    profileDisplayNameOverrides = profileDisplayNameOverrides,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    onLabelClick = onLabelClick,
                    onLabelLongPress = onLabelLongPress,
                    outlined = outlined,
                    notificationIndicatorUiState = notificationIndicatorUiState,
                )
                if (rightSideShortcuts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(shortcutRowTopSpacer))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(shortcutIconSpacing),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RightShortcutIcons(
                                shortcuts = rightSideShortcuts,
                                onIconClick = onIconClick,
                                iconSize = shortcutIconSize,
                                touchTargetSize = shortcutTouchTargetSize,
                                iconAlignment = Alignment.Center,
                                outlined = outlined,
                        )
                    }
                }
            }

        HomeAlignment.LEFT, HomeAlignment.RIGHT -> {
            val favAlign =
                    if (homeAlignment == HomeAlignment.LEFT) Alignment.Start else Alignment.End
            Row(
                    modifier = listModifier,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
            ) {
                val favs: @Composable () -> Unit = {
                    FavoritesList(
                            favorites = favorites,
                            installedApps = installedApps,
                            profileDisplayNameOverrides = profileDisplayNameOverrides,
                            horizontalAlignment = favAlign,
                            onLabelClick = onLabelClick,
                            onLabelLongPress = onLabelLongPress,
                            modifier = Modifier.weight(1f),
                            outlined = outlined,
                            notificationIndicatorUiState = notificationIndicatorUiState,
                    )
                }
                val icons: @Composable () -> Unit = {
                    val iconAlignment =
                            if (homeAlignment == HomeAlignment.LEFT) {
                                Alignment.CenterEnd
                            } else {
                                Alignment.CenterStart
                            }
                    ShortcutIconsColumn(
                            shortcuts = rightSideShortcuts,
                            onIconClick = onIconClick,
                            iconSize = shortcutIconSize,
                            touchTargetSize = shortcutTouchTargetSize,
                            iconAlignment = iconAlignment,
                            verticalSpacing = shortcutIconSpacing,
                            modifier = Modifier.offset(y = (-8).dp),
                            outlined = outlined,
                    )
                }
                if (homeAlignment == HomeAlignment.LEFT) {
                    favs()
                    Spacer(modifier = Modifier.width(shortcutGutter))
                    icons()
                } else {
                    icons()
                    Spacer(modifier = Modifier.width(shortcutGutter))
                    favs()
                }
            }
        }
    }
}

@Composable
private fun RightShortcutIcons(
    shortcuts: List<HomeShortcut>,
    onIconClick: (HomeShortcut) -> Unit,
    iconSize: Dp,
    touchTargetSize: Dp,
    iconAlignment: Alignment,
    outlined: Boolean = false,
) {
    shortcuts.reversed().forEachIndexed { index, shortcut ->
        Box(
                modifier =
                        Modifier.size(touchTargetSize)
                                .clickableNoRippleWithSystemSound { onIconClick(shortcut) }
                                .testTag("right_shortcut_icon_$index"),
                contentAlignment = iconAlignment,
        ) {
            LauncherIcon(
                    imageVector = MinimalIcons.iconFor(shortcut.iconName),
                    contentDescription = stringResource(R.string.cd_shortcut_icon),
                    tint = MaterialTheme.colorScheme.onBackground,
                    iconSize = iconSize,
                    outlined = outlined,
            )
        }
    }
}

@Composable
private fun BoxScope.HomeDefaultLauncherBanner(
    isDefaultLauncher: Boolean,
    onSetDefaultLauncher: () -> Unit,
) {
    if (isDefaultLauncher) return

    FokusOutlinedButton(
        onClick = onSetDefaultLauncher,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onBackground
        ),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 12.dp)
            .testTag("set_default_launcher_button")
    ) {
        Text(
            text = stringResource(R.string.home_set_default_launcher),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteAppItem(
        fav: FavoriteApp,
        installedApps: List<AppInfo>,
        profileDisplayNameOverrides: Map<String, String>,
        onClick: () -> Unit,
        onLongPress: () -> Unit,
        horizontalAlignment: Alignment.Horizontal,
        outlined: Boolean,
        notificationIndicatorUiState: HomeNotificationIndicatorUiState =
                HomeNotificationIndicatorUiState(),
) {
    val context = LocalContext.current
    val badge =
            remember(fav, installedApps, profileDisplayNameOverrides, context) {
                val match =
                        installedApps.find {
                            it.packageName == fav.packageName &&
                                    appProfileKey(it.userHandle) == fav.profileKey
                        }
                profileOriginLabelForFavorite(context, fav, match, profileDisplayNameOverrides)
            }
    val appKey = drawerOpenCountKey(fav.packageName, fav.profileKey)
    val hasNotification =
            notificationIndicatorUiState.enabled &&
                    appKey in notificationIndicatorUiState.appsWithNotifications
    val textColor = MaterialTheme.colorScheme.onBackground
    val indicatorColor = Color(notificationIndicatorUiState.colorArgb)
    val labelColor =
            if (hasNotification &&
                            notificationIndicatorUiState.style ==
                                    NotificationIndicatorStyle.COLORED_LABEL
            ) {
                indicatorColor
            } else {
                textColor
            }
    val showDot =
            hasNotification &&
                    notificationIndicatorUiState.style == NotificationIndicatorStyle.DOT
    val dotLeading = horizontalAlignment == Alignment.Start
    Column(
            horizontalAlignment = horizontalAlignment,
            modifier =
                    Modifier.combinedClickableWithSystemSound(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = onClick,
                                    onLongClick = onLongPress,
                            )
                            .testTag("favorite_${fav.label}"),
    ) {
        Box {
            if (outlined) {
                OutlinedText(
                        text = fav.label,
                        style = MaterialTheme.typography.headlineMedium,
                        color = labelColor,
                )
            } else {
                Text(
                        text = fav.label,
                        style = MaterialTheme.typography.headlineMedium,
                        color = labelColor,
                )
            }
            if (showDot) {
                NotificationIndicatorDot(
                        color = textColor,
                        modifier =
                                Modifier.align(
                                                if (dotLeading) Alignment.CenterStart
                                                else Alignment.CenterEnd
                                        )
                                        .offset(x = if (dotLeading) (-16).dp else 16.dp),
                )
            }
        }
        if (badge != null) {
            if (outlined) {
                OutlinedText(
                        text = badge,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                        outlineWidth = 1.5f,
                )
            } else {
                Text(
                        text = badge,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun NotificationIndicatorDot(color: Color, modifier: Modifier = Modifier) {
    Box(
            modifier =
                    modifier.size(8.dp)
                            .background(color = color, shape = CircleShape),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenLongPressSheet(
    onDismiss: () -> Unit,
    onEditHomeScreen: () -> Unit,
    onEditShortcuts: () -> Unit,
    onOpenSettings: () -> Unit,
    onTogglePomodoro: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    FokusBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
            SheetActionRow(
                label = "Toggle Pomodoro Timer",
                onClick = onTogglePomodoro,
                icon = Icons.Default.Timer,
                iconContentDescription = "Toggle Pomodoro Timer",
            )
            SheetActionRow(
                label = stringResource(R.string.settings_edit_home_screen),
                onClick = onEditHomeScreen,
                icon = Icons.Default.Home,
                iconContentDescription = stringResource(R.string.cd_edit_home_screen),
            )
            SheetActionRow(
                label = stringResource(R.string.settings_edit_shortcuts),
                onClick = onEditShortcuts,
                icon = Icons.Filled.TouchApp,
                iconContentDescription = stringResource(R.string.settings_edit_shortcuts),
            )
            SheetActionRow(
                label = stringResource(R.string.settings_title),
                onClick = onOpenSettings,
                icon = Icons.Default.Settings,
                iconContentDescription = stringResource(R.string.cd_settings),
            )
    }
}
