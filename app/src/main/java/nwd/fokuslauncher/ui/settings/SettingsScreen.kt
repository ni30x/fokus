package nwd.fokuslauncher.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import nwd.fokuslauncher.ui.components.AccessibilityProminentDisclosureOverlay
import nwd.fokuslauncher.ui.components.FokusIconButton
import nwd.fokuslauncher.ui.components.FokusTextButton
import nwd.fokuslauncher.ui.components.LauncherIcon
import nwd.fokuslauncher.ui.util.rememberBooleanChangeWithSystemSound
import nwd.fokuslauncher.ui.util.rememberClickWithSystemSound
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.runtime.saveable.rememberSaveable
import nwd.fokuslauncher.ui.settings.components.SettingsAccordionPanel
import nwd.fokuslauncher.ui.settings.components.SettingsSelectionChipsRow
import nwd.fokuslauncher.media.MediaNotificationHelper
import nwd.fokuslauncher.usage.UsageStatsHelper
import nwd.fokuslauncher.ui.components.FokusAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.rememberUpdatedState
import nwd.fokuslauncher.data.model.HomeExtraWidgetAddType
import nwd.fokuslauncher.data.model.HomeExtraWidgetEntry
import nwd.fokuslauncher.data.model.displayNameForTimeZoneId
import nwd.fokuslauncher.data.model.formatUtcOffsetLabel
import nwd.fokuslauncher.ui.home.formatCountdownDateTimeLabel
import nwd.fokuslauncher.ui.util.rememberLocallyReorderedList
import nwd.fokuslauncher.ui.util.rememberVerticalSlotReorderState
import nwd.fokuslauncher.ui.settings.components.EditorDragHandleReorderIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nwd.fokuslauncher.R
import nwd.fokuslauncher.data.font.CustomFontImportFailure
import nwd.fokuslauncher.data.model.LauncherFontPreferences
import nwd.fokuslauncher.data.model.LauncherFontScale
import nwd.fokuslauncher.data.model.PhotoWallpaperDrawerOverlayIntensity
import nwd.fokuslauncher.data.model.PhotoWallpaperOutlineWidthDp
import java.text.Collator
import java.util.Locale
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nwd.fokuslauncher.data.model.AppInfo
import nwd.fokuslauncher.data.model.AppShortcutAction
import nwd.fokuslauncher.data.model.DrawerAppSortMode
import nwd.fokuslauncher.data.model.HomeDateFormatStyle
import nwd.fokuslauncher.data.model.HomeAlignment
import nwd.fokuslauncher.data.model.NotificationIndicatorColorPreset
import nwd.fokuslauncher.data.model.NotificationIndicatorStyle
import nwd.fokuslauncher.data.model.TemperatureUnit
import nwd.fokuslauncher.data.model.LauncherVisualStyle
import nwd.fokuslauncher.data.model.ShortcutTarget
import nwd.fokuslauncher.data.model.WidgetTapTarget
import nwd.fokuslauncher.utils.LockScreenHelper
import nwd.fokuslauncher.ui.theme.FokusBackdrop
import nwd.fokuslauncher.ui.theme.LocalLauncherFontScale
import nwd.fokuslauncher.ui.theme.LocalLauncherIconGlow
import nwd.fokuslauncher.ui.theme.settingsPreviewColor
import nwd.fokuslauncher.ui.theme.withLauncherTextGlowRecolored
import nwd.fokuslauncher.ui.theme.withoutLauncherTextGlow
import nwd.fokuslauncher.ui.settings.components.SettingsDropdown
import nwd.fokuslauncher.ui.settings.components.SettingsRow
import nwd.fokuslauncher.ui.settings.components.SettingsToggleRow
import nwd.fokuslauncher.ui.theme.composeFontFamilyFromStoredName
import nwd.fokuslauncher.ui.theme.launcherIconDp
import nwd.fokuslauncher.ui.util.OnResumeEffect
import nwd.fokuslauncher.ui.util.clickableWithSystemSound
import nwd.fokuslauncher.ui.util.formatShortcutTargetDisplay
import android.app.Activity
import androidx.annotation.StringRes

private data class SubpageNavRow(
        @param:StringRes val labelRes: Int,
        val subtitle: String? = null,
        val onClick: () -> Unit,
)

private data class SwipeTargetPick(
        val pickerKey: String,
        @param:StringRes val labelRes: Int,
        val target: ShortcutTarget?,
        val onClear: () -> Unit,
)

private data class WidgetTapPickerRow(
        @param:StringRes val labelRes: Int,
        val tapTarget: WidgetTapTarget?,
        val pickerKey: String,
        val onClear: () -> Unit,
        val emptyLabel: (Context, Resources) -> String = ::formatWidgetAppEmptyLabel,
)

private data class DeviceControlToggleRow(
        @param:StringRes val labelRes: Int,
        val subtitle: String,
        val checked: Boolean,
        val onCheckedChange: (Boolean) -> Unit,
)

private data class CommunityLink(
        val icon: ImageVector,
        val titleRes: Int,
        val subtitleRes: Int,
        val url: String,
)

private val communityLinks =
        listOf(
                CommunityLink(
                        Icons.Filled.Star,
                        R.string.settings_profile_title,
                        R.string.settings_profile_subtitle,
                        "https://github.com/ni30x",
                ),
        )

private fun Context.hasCoarseLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

private fun <T> LazyListScope.manageableAppsSection(
        headerRes: Int,
        emptyTextRes: Int,
        apps: List<T>,
        key: (T) -> Any,
        label: (T) -> String,
        subtitle: (T) -> String,
        onRowClick: (T) -> Unit,
        trailingContent: @Composable RowScope.(T) -> Unit,
) {
    item { SectionHeader(stringResource(headerRes)) }
    if (apps.isEmpty()) {
        item { EmptySettingsStateText(text = stringResource(emptyTextRes)) }
    } else {
        items(apps, key = key) { app ->
            SettingsRow(
                    label = label(app),
                    subtitle = subtitle(app),
                    subtitleStyle = MaterialTheme.typography.labelMedium,
                    onClick = { onRowClick(app) },
                    trailing = { trailingContent(app) },
            )
        }
    }
}

@Composable
private fun rememberCoarseLocationPermission(context: Context, activity: Activity?): Pair<Boolean, () -> Unit> {
    var granted by remember { mutableStateOf(context.hasCoarseLocationPermission()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    OnResumeEffect(lifecycleOwner) { granted = context.hasCoarseLocationPermission() }
    val launcher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
            ) {
                granted = context.hasCoarseLocationPermission()
                if (!granted &&
                                activity != null &&
                                !ActivityCompat.shouldShowRequestPermissionRationale(
                                        activity,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                )
                ) {
                    context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                    )
                }
            }
    val request = remember(launcher) { { launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) } }
    return granted to request
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
        viewModel: SettingsViewModel = hiltViewModel(),
        onNavigateBack: () -> Unit = {},
        onNavigateToHome: () -> Unit = {},
        onEditHomeScreen: () -> Unit = {},
        onEditRightShortcuts: () -> Unit = {},
        onOpenDeviceControlSettings: () -> Unit = {},
        onEditCategories: () -> Unit = {},
        onDrawerDotSearchSettings: () -> Unit = {},
        onOpenHomeWidgetsSettings: () -> Unit = {},
        backgroundScrim: Color = FokusBackdrop.ScrimColorWithoutBlur
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val installedFontFamilies by viewModel.installedFontFamilies.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current

    // Dialog states
    val showAppPickerFor = remember { mutableStateOf<String?>(null) } // swipeLeft/swipeRight
    val showResetConfirm = remember { mutableStateOf(false) }

    val wallpaperPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setSystemWallpaper(it)
            onNavigateToHome()
        }
    }

    val fontImportFailedUnreadable =
            stringResource(R.string.settings_font_import_failed_unreadable)
    val fontImportFailedExtension =
            stringResource(R.string.settings_font_import_failed_extension)
    val fontImportFailedInvalid = stringResource(R.string.settings_font_import_failed_invalid)
    val fontImportFailedIo = stringResource(R.string.settings_font_import_failed_io)
    val fontPickerLauncher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) {
                    uri: Uri? ->
                uri?.let { picked ->
                    viewModel.importCustomFont(picked) { failure ->
                        val message =
                                when (failure) {
                                    CustomFontImportFailure.UNREADABLE_URI ->
                                            fontImportFailedUnreadable
                                    CustomFontImportFailure.INVALID_EXTENSION ->
                                            fontImportFailedExtension
                                    CustomFontImportFailure.INVALID_FONT ->
                                            fontImportFailedInvalid
                                    CustomFontImportFailure.IO_ERROR -> fontImportFailedIo
                                    null -> null
                                }
                        if (message != null) {
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(backgroundScrim)
        .navigationBarsPadding()
        .testTag("settings_screen")
    ) {
        FokusSettingsTopBar(
                titleText = stringResource(R.string.settings_title),
                onNavigateBack = onNavigateBack,
                containerColor = MaterialTheme.colorScheme.background,
        )

        SettingsScreenContent(
                viewModel = viewModel,
                uiState = uiState,
                installedFontFamilies = installedFontFamilies,
                context = context,
                resources = resources,
                onPickWallpaper = { wallpaperPickerLauncher.launch("image/*") },
                onImportCustomFont = {
                    fontPickerLauncher.launch(
                            arrayOf(
                                    "font/ttf",
                                    "application/x-font-ttf",
                                    "application/font-sfnt",
                                    "application/octet-stream",
                            )
                    )
                },
                resolveCustomFontFile = viewModel::resolveCustomFontFile,
                onSetBlackWallpaper = {
                    viewModel.setBlackWallpaper()
                    onNavigateToHome()
                },
                onOpenHomeWidgetsSettings = onOpenHomeWidgetsSettings,
                onOpenDeviceControlSettings = onOpenDeviceControlSettings,
                onEditHomeScreen = onEditHomeScreen,
                onEditRightShortcuts = onEditRightShortcuts,
                onEditCategories = onEditCategories,
                onDrawerDotSearchSettings = onDrawerDotSearchSettings,
                onShowAppPicker = { showAppPickerFor.value = it },
                onShowResetConfirm = { showResetConfirm.value = true },
        )
    }

    SettingsScreenDialogs(
            uiState = uiState,
            showResetConfirm = showResetConfirm.value,
            pickerTarget = showAppPickerFor.value,
            onDismissResetConfirm = { showResetConfirm.value = false },
            onResetConfirmed = {
                viewModel.resetAllState()
                onNavigateBack()
            },
            onDismissPicker = { showAppPickerFor.value = null },
            onShortcutTargetSelected = { target, action ->
                when (target) {
                    "swipeLeft" -> viewModel.setSwipeLeftTarget(action.target)
                    "swipeRight" -> viewModel.setSwipeRightTarget(action.target)
                }
            },
    )
}

@Composable
private fun SettingsScreenContent(
        viewModel: SettingsViewModel,
        uiState: SettingsUiState,
        installedFontFamilies: List<String>,
        context: Context,
        resources: Resources,
        onPickWallpaper: () -> Unit,
        onImportCustomFont: () -> Unit,
        resolveCustomFontFile: (String) -> java.io.File?,
        onSetBlackWallpaper: () -> Unit,
        onOpenHomeWidgetsSettings: () -> Unit,
        onOpenDeviceControlSettings: () -> Unit,
        onEditHomeScreen: () -> Unit,
        onEditRightShortcuts: () -> Unit,
        onEditCategories: () -> Unit,
        onDrawerDotSearchSettings: () -> Unit,
        onShowAppPicker: (String) -> Unit,
        onShowResetConfirm: () -> Unit,
) {
    var appearanceExpanded by rememberSaveable { mutableStateOf(false) }
    var homeScreenExpanded by rememberSaveable { mutableStateOf(false) }
    var appDrawerExpanded by rememberSaveable { mutableStateOf(false) }
    var appOrganizationExpanded by rememberSaveable { mutableStateOf(false) }
    var communityExpanded by rememberSaveable { mutableStateOf(false) }
    var systemDataExpanded by rememberSaveable { mutableStateOf(false) }

    val homeScreenSubpageRows = listOf(
            SubpageNavRow(
                    R.string.settings_home_widgets,
                    stringResource(R.string.settings_home_widgets_subtitle),
                    onOpenHomeWidgetsSettings,
            ),
            SubpageNavRow(
                    R.string.settings_accessibility,
                    stringResource(R.string.settings_accessibility_subtitle),
                    onOpenDeviceControlSettings,
            ),
            SubpageNavRow(
                    R.string.settings_edit_home_screen,
                    pluralStringResource(
                            R.plurals.settings_home_screen_apps_count,
                            uiState.favorites.size,
                            uiState.favorites.size,
                    ),
                    onEditHomeScreen,
            ),
            SubpageNavRow(
                    R.string.settings_edit_shortcuts,
                    pluralStringResource(
                            R.plurals.settings_shortcuts_configured,
                            uiState.rightSideShortcuts.size,
                            uiState.rightSideShortcuts.size
                    ),
                    onEditRightShortcuts,
            ),
    )
    val editableCategoryCount = remember(uiState.allApps, uiState.categoryDefinitions) {
        editableCategoriesForSettings(uiState).size
    }
    val drawerSubpageRows = listOf(
            SubpageNavRow(
                    R.string.settings_edit_app_categories,
                    pluralStringResource(
                            R.plurals.settings_categories_count,
                            editableCategoryCount,
                            editableCategoryCount
                    ),
                    onEditCategories,
            ),
            SubpageNavRow(
                    R.string.settings_dot_search_title,
                    stringResource(R.string.settings_dot_search_subtitle),
                    onDrawerDotSearchSettings,
            ),
    )

    LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // 1. APPEARANCE ACCORDION PANEL
        item {
            SettingsAccordionPanel(
                    title = stringResource(R.string.settings_section_appearance),
                    subtitle = "Visual style, font, status bar & background",
                    icon = Icons.Outlined.Palette,
                    expanded = appearanceExpanded,
                    onExpandedChange = { appearanceExpanded = it }
            ) {
                SettingsToggleRow(
                        label = stringResource(R.string.settings_show_status_bar),
                        checked = uiState.showStatusBar,
                        onCheckedChange = viewModel::setShowStatusBar
                )
                SettingsToggleRow(
                        label = stringResource(R.string.settings_allow_landscape_rotation),
                        checked = uiState.allowLandscapeRotation,
                        onCheckedChange = viewModel::setAllowLandscapeRotation
                )
                AppLanguageDropdown(
                        currentTag = uiState.appLocaleTag,
                        onTagSelected = viewModel::setAppLocaleTag
                )
                LauncherFontFamilyDropdown(
                        currentFamilyName = uiState.launcherFontFamilyName,
                        installedFamilies = installedFontFamilies,
                        hasCustomFontFile = uiState.hasCustomFontFile,
                        customFontDisplayName = uiState.customFontDisplayName,
                        resolveCustomFontFile = resolveCustomFontFile,
                        onFamilySelected = viewModel::setLauncherFontFamilyName
                )
                SettingsRow(
                        label = stringResource(R.string.settings_font_import_ttf),
                        subtitle = stringResource(R.string.settings_font_import_ttf_subtitle),
                        verticalPadding = 12.dp,
                        onClick = onImportCustomFont
                )
                LauncherFontSizeSlider(
                        currentScale = uiState.launcherFontScale,
                        onScaleChange = viewModel::setLauncherFontScale
                )
                LauncherVisualStyleChipsRow(
                        currentStyle = uiState.launcherVisualStyle,
                        onStyleSelected = viewModel::setLauncherVisualStyle,
                        homeUsesPhotoWallpaper = uiState.homeUsesPhotoWallpaper
                )
                SettingsToggleRow(
                        label = stringResource(R.string.settings_glow_label),
                        checked = uiState.launcherGlowEnabled && !uiState.homeUsesPhotoWallpaper,
                        onCheckedChange = viewModel::setLauncherGlowEnabled,
                        subtitle = stringResource(
                                if (uiState.homeUsesPhotoWallpaper) {
                                    R.string.settings_look_locked_image_wallpaper
                                } else {
                                    R.string.settings_glow_subtitle
                                }
                        ),
                        enabled = !uiState.homeUsesPhotoWallpaper
                )
                SettingsRow(
                        label = stringResource(R.string.settings_set_background_image),
                        verticalPadding = 12.dp,
                        onClick = onPickWallpaper
                )
                SettingsRow(
                        label = stringResource(R.string.settings_set_black_wallpaper),
                        verticalPadding = 12.dp,
                        onClick = onSetBlackWallpaper
                )
                if (uiState.homeUsesPhotoWallpaper) {
                    SectionHeader(
                            stringResource(R.string.settings_section_image_wallpaper_accessibility)
                    )
                    PhotoWallpaperOutlineWidthSlider(
                            currentWidthDp = uiState.photoWallpaperOutlineWidthDp,
                            onWidthDpChange = viewModel::setPhotoWallpaperOutlineWidthDp
                    )
                    PhotoWallpaperDrawerOverlaySlider(
                            currentIntensity = uiState.photoWallpaperDrawerOverlayIntensity,
                            onIntensityChange = viewModel::setPhotoWallpaperDrawerOverlayIntensity
                    )
                }
            }
        }

        // 2. HOME SCREEN ACCORDION PANEL
        item {
            SettingsAccordionPanel(
                    title = stringResource(R.string.settings_section_home_screen),
                    subtitle = "Widgets, accessibility, apps & gesture shortcuts",
                    icon = Icons.Outlined.Home,
                    expanded = homeScreenExpanded,
                    onExpandedChange = { homeScreenExpanded = it }
            ) {
                homeScreenSubpageRows.forEach { row ->
                    SettingsRow(
                            label = stringResource(row.labelRes),
                            subtitle = row.subtitle,
                            verticalPadding = 12.dp,
                            onClick = row.onClick,
                            trailing = { SubpageChevron() }
                    )
                }
                HomeAlignmentRow(
                        currentAlignment = uiState.homeAlignment,
                        onAlignmentChanged = viewModel::setHomeAlignment
                )
                ShortcutTargetRow(
                        label = stringResource(R.string.settings_swipe_left),
                        currentTarget = formatShortcutTarget(
                                context,
                                resources,
                                uiState.swipeLeftTarget,
                                uiState.allApps
                        ),
                        onPickApp = { onShowAppPicker("swipeLeft") },
                        onClear = { viewModel.setSwipeLeftTarget(null) }
                )
                ShortcutTargetRow(
                        label = stringResource(R.string.settings_swipe_right),
                        currentTarget = formatShortcutTarget(
                                context,
                                resources,
                                uiState.swipeRightTarget,
                                uiState.allApps
                        ),
                        onPickApp = { onShowAppPicker("swipeRight") },
                        onClear = { viewModel.setSwipeRightTarget(null) }
                )
            }
        }

        // 3. APP DRAWER ACCORDION PANEL
        item {
            SettingsAccordionPanel(
                    title = stringResource(R.string.settings_section_app_drawer),
                    subtitle = "Categories, dot search, auto-keyboard & sorting",
                    icon = Icons.Outlined.Apps,
                    expanded = appDrawerExpanded,
                    onExpandedChange = { appDrawerExpanded = it }
            ) {
                drawerSubpageRows.forEach { row ->
                    SettingsRow(
                            label = stringResource(row.labelRes),
                            subtitle = row.subtitle,
                            verticalPadding = 12.dp,
                            onClick = row.onClick,
                            trailing = { SubpageChevron() }
                    )
                }
                SettingsToggleRow(
                        label = stringResource(R.string.settings_drawer_search_auto_launch),
                        subtitle = stringResource(R.string.settings_drawer_search_auto_launch_subtitle),
                        checked = uiState.drawerSearchAutoLaunch,
                        onCheckedChange = viewModel::setDrawerSearchAutoLaunch
                )
                SettingsToggleRow(
                        label = stringResource(R.string.settings_drawer_scroll_to_top_auto_keyboard),
                        subtitle = stringResource(R.string.settings_drawer_scroll_to_top_auto_keyboard_subtitle),
                        checked = uiState.drawerScrollToTopAutoKeyboard,
                        onCheckedChange = viewModel::setDrawerScrollToTopAutoKeyboard
                )
                SettingsToggleRow(
                        label = stringResource(R.string.settings_drawer_show_scrollbar),
                        subtitle = stringResource(R.string.settings_drawer_show_scrollbar_subtitle),
                        checked = uiState.drawerShowScrollbar,
                        onCheckedChange = viewModel::setDrawerShowScrollbar
                )
                SettingsToggleRow(
                        label = stringResource(R.string.settings_drawer_sidebar_categories),
                        subtitle = stringResource(R.string.settings_drawer_sidebar_categories_subtitle),
                        checked = uiState.drawerSidebarCategories,
                        onCheckedChange = viewModel::setDrawerSidebarCategories
                )
                if (uiState.drawerSidebarCategories) {
                    DrawerCategoryRailSideRow(
                            railOnLeft = uiState.drawerCategorySidebarOnLeft,
                            onRailOnLeftChanged = viewModel::setDrawerCategorySidebarOnLeft
                    )
                }
                DrawerAppSortRow(
                        currentMode = uiState.drawerAppSortMode,
                        showCustomSortOption = uiState.drawerSidebarCategories,
                        onModeChanged = viewModel::setDrawerAppSortMode
                )
            }
        }

        // 4. APP MANAGEMENT ACCORDION PANEL
        item {
            val totalManagedAppsCount = uiState.hiddenApps.size + uiState.renamedApps.size + uiState.archivedApps.size
            val badge = if (totalManagedAppsCount > 0) "$totalManagedAppsCount" else null
            SettingsAccordionPanel(
                    title = "Apps & Organization",
                    subtitle = "Manage hidden, archived and renamed apps",
                    icon = Icons.Outlined.FolderSpecial,
                    expanded = appOrganizationExpanded,
                    onExpandedChange = { appOrganizationExpanded = it },
                    badgeText = badge
            ) {
                if (Build.VERSION.SDK_INT >= 35) {
                    SectionHeader(stringResource(R.string.settings_section_archived_apps))
                    if (uiState.archivedApps.isEmpty()) {
                        EmptySettingsStateText(stringResource(R.string.settings_no_archived_apps))
                    } else {
                        uiState.archivedApps.forEach { archived ->
                            SettingsRow(
                                    label = archived.app.label,
                                    subtitle = archived.profileLabel?.let { pl -> "$pl • ${archived.app.packageName}" }
                                            ?: archived.app.packageName,
                                    onClick = { viewModel.restoreArchivedApp(archived) },
                                    trailing = {
                                        Spacer(Modifier.width(8.dp))
                                        LauncherIcon(
                                                Icons.Default.Restore,
                                                stringResource(R.string.cd_restore_archived_app),
                                                tint = MaterialTheme.colorScheme.secondary,
                                                iconSize = 22.dp
                                        )
                                    }
                            )
                        }
                    }
                }

                SectionHeader(stringResource(R.string.settings_section_hidden_apps))
                if (uiState.hiddenApps.isEmpty()) {
                    EmptySettingsStateText(stringResource(R.string.settings_no_hidden_apps))
                } else {
                    uiState.hiddenApps.forEach { app ->
                        SettingsRow(
                                label = app.label,
                                subtitle = app.profileLabel?.let { pl -> "$pl • ${app.packageName}" } ?: app.packageName,
                                onClick = {
                                    viewModel.unhideApp(app.packageName, app.profileKey, app.launcherShortcutId)
                                },
                                trailing = {
                                    Spacer(Modifier.width(8.dp))
                                    LauncherIcon(
                                            Icons.Default.Visibility,
                                            stringResource(R.string.cd_unhide_app),
                                            tint = MaterialTheme.colorScheme.secondary,
                                            iconSize = 22.dp
                                    )
                                }
                        )
                    }
                }

                SectionHeader(stringResource(R.string.settings_section_renamed_apps))
                if (uiState.renamedApps.isEmpty()) {
                    EmptySettingsStateText(stringResource(R.string.settings_no_renamed_apps))
                } else {
                    uiState.renamedApps.forEach { app ->
                        SettingsRow(
                                label = app.customName,
                                subtitle = app.profileLabel?.let { pl -> "$pl • ${app.packageName}" } ?: app.packageName,
                                onClick = {
                                    viewModel.removeRename(app.packageName, app.profileKey, app.launcherShortcutId)
                                },
                                trailing = {
                                    Spacer(Modifier.width(8.dp))
                                    LauncherIcon(
                                            Icons.Default.Close,
                                            stringResource(R.string.cd_remove_rename),
                                            tint = MaterialTheme.colorScheme.secondary,
                                            iconSize = 22.dp
                                    )
                                }
                        )
                    }
                }
            }
        }

        // 5. PROFILE ACCORDION PANEL
        item {
            SettingsAccordionPanel(
                    title = stringResource(R.string.settings_connect_section),
                    subtitle = "Developer profile & social links",
                    icon = Icons.Outlined.Share,
                    expanded = communityExpanded,
                    onExpandedChange = { communityExpanded = it }
            ) {
                communityLinks.forEach { link ->
                    SettingsRow(
                            label = stringResource(link.titleRes),
                            subtitle = stringResource(link.subtitleRes),
                            verticalPadding = 12.dp,
                            leading = {
                                LauncherIcon(
                                        imageVector = link.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        iconSize = 22.dp
                                )
                            },
                            trailing = {
                                LauncherIcon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = stringResource(R.string.cd_open_link),
                                        tint = MaterialTheme.colorScheme.secondary,
                                        iconSize = 18.dp
                                )
                            },
                            onClick = {
                                context.startActivity(
                                        Intent(Intent.ACTION_VIEW, link.url.toUri())
                                                .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                )
                            }
                    )
                }
            }
        }

        // 6. DATA & SYSTEM ACCORDION PANEL
        item {
            SettingsAccordionPanel(
                    title = stringResource(R.string.settings_section_data),
                    subtitle = "Diagnostics logs & reset configuration",
                    icon = Icons.Outlined.Storage,
                    expanded = systemDataExpanded,
                    onExpandedChange = { systemDataExpanded = it }
            ) {
                ExportLogsRow(
                        context = context,
                        createLogShareIntent = viewModel::createLogShareIntent
                )
                SettingsRow(
                        label = stringResource(R.string.settings_reset_all_data),
                        labelStyle = MaterialTheme.typography.bodyLarge.withLauncherTextGlowRecolored(
                                MaterialTheme.colorScheme.error
                        ),
                        labelColor = MaterialTheme.colorScheme.error,
                        verticalPadding = 12.dp,
                        leading = {
                            LauncherIcon(
                                    Icons.Default.Restore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    iconSize = 22.dp
                            )
                        },
                        onClick = onShowResetConfirm
                )
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun SettingsScreenDialogs(
        uiState: SettingsUiState,
        showResetConfirm: Boolean,
        pickerTarget: String?,
        onDismissResetConfirm: () -> Unit,
        onResetConfirmed: suspend () -> Unit,
        onDismissPicker: () -> Unit,
        onShortcutTargetSelected: (String, AppShortcutAction) -> Unit,
) {
    if (showResetConfirm) {
        FokusAlertDialog(
                onDismissRequest = onDismissResetConfirm,
                title = {
                    Text(stringResource(R.string.settings_reset_confirm_title), color = MaterialTheme.colorScheme.onBackground)
                },
                text = {
                    Text(
                            stringResource(R.string.settings_reset_confirm_message),
                            color = MaterialTheme.colorScheme.onBackground
                    )
                },
                confirmButton = {
                    val scope = rememberCoroutineScope()
                    FokusTextButton(
                            onClick = {
                                scope.launch {
                                    onResetConfirmed()
                                    onDismissResetConfirm()
                                }
                            }
                    ) {
                        Text(
                                stringResource(R.string.action_reset),
                                style =
                                        MaterialTheme.typography.labelLarge
                                                .withLauncherTextGlowRecolored(
                                                        MaterialTheme.colorScheme.error
                                                ),
                                color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                dismissButton = {
                    FokusTextButton(onClick = onDismissResetConfirm) {
                        Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.primary)
                    }
                },
        )
    }

    pickerTarget?.let { target ->
        when (target) {
            "swipeLeft", "swipeRight" -> {
                ShortcutActionPickerDialog(
                        allActions = uiState.allShortcutActions,
                        allApps = uiState.allApps,
                        title = stringResource(R.string.edit_shortcuts_section_all_actions),
                        onSelect = { action ->
                            onShortcutTargetSelected(target, action)
                            onDismissPicker()
                        },
                        onDismiss = onDismissPicker,
                        includeWidgetPageTarget = true,
                        profileDisplayNameOverrides = uiState.profileDisplayNameOverrides,
                )
            }
            else -> onDismissPicker()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeWidgetsSettingsScreen(
        viewModel: SettingsViewModel = hiltViewModel(),
        onNavigateBack: () -> Unit = {},
        backgroundScrim: Color = FokusBackdrop.ScrimColorWithoutBlur
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val activity = LocalActivity.current
    val showAppPickerFor = remember { mutableStateOf<String?>(null) }

    val (hasCoarseLocationPermission, requestCoarseLocation) =
            rememberCoarseLocationPermission(context, activity)

    var mediaNotificationAccessTick by remember { mutableIntStateOf(0) }
    var pendingMediaEnable by remember { mutableStateOf(false) }
    var pendingNotificationIndicatorsEnable by remember { mutableStateOf(false) }
    var usageAccessTick by remember { mutableIntStateOf(0) }
    var pendingScreenTimeEnable by remember { mutableStateOf(false) }
    var showAddOtherWidget by remember { mutableStateOf(false) }
    var editingCityId by remember { mutableStateOf<String?>(null) }
    var pendingEditNewestCity by remember { mutableStateOf(false) }
    var editingCountdownId by remember { mutableStateOf<String?>(null) }
    var pendingEditNewestCountdown by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val extraWidgetsSource = uiState.homeExtraWidgets
    val citiesById = remember(uiState.worldClockCities) { uiState.worldClockCities.associateBy { it.id } }
    val eventsById = remember(uiState.countdownEvents) { uiState.countdownEvents.associateBy { it.id } }
    val localExtras = rememberLocallyReorderedList(extraWidgetsSource)
    val extraWidgets = localExtras.items
    val reorderState = rememberVerticalSlotReorderState()
    val onExtraCommit by rememberUpdatedState(viewModel::setHomeExtraWidgets)
    OnResumeEffect(lifecycleOwner) {
        mediaNotificationAccessTick++
        usageAccessTick++
    }
    val mediaNotificationAccessEnabled =
            remember(mediaNotificationAccessTick) {
                MediaNotificationHelper.isListenerEnabled(context)
            }
    val usageAccessEnabled =
            remember(usageAccessTick) { UsageStatsHelper.hasUsageAccess(context) }
    LaunchedEffect(mediaNotificationAccessTick, uiState.showHomeMedia, pendingMediaEnable) {
        if (pendingMediaEnable && mediaNotificationAccessEnabled) {
            pendingMediaEnable = false
            viewModel.setShowHomeMedia(true)
        } else if (uiState.showHomeMedia && !mediaNotificationAccessEnabled) {
            viewModel.setShowHomeMedia(false)
        }
    }
    LaunchedEffect(
            mediaNotificationAccessTick,
            uiState.showNotificationIndicators,
            pendingNotificationIndicatorsEnable,
    ) {
        if (pendingNotificationIndicatorsEnable && mediaNotificationAccessEnabled) {
            pendingNotificationIndicatorsEnable = false
            viewModel.setShowNotificationIndicators(true)
        } else if (uiState.showNotificationIndicators && !mediaNotificationAccessEnabled) {
            viewModel.setShowNotificationIndicators(false)
        }
    }
    LaunchedEffect(usageAccessTick, uiState.showHomeScreenTime, pendingScreenTimeEnable) {
        if (pendingScreenTimeEnable && usageAccessEnabled) {
            pendingScreenTimeEnable = false
            viewModel.setShowHomeScreenTime(true)
        } else if (uiState.showHomeScreenTime && !usageAccessEnabled) {
            viewModel.setShowHomeScreenTime(false)
        }
    }

    Column(
            modifier =
                    Modifier.fillMaxSize()
                            .background(backgroundScrim)
                            .navigationBarsPadding()
                            .testTag("home_widgets_settings_screen")
    ) {
        FokusSettingsTopBar(
                titleText = stringResource(R.string.settings_home_widgets),
                onNavigateBack = onNavigateBack,
                containerColor = MaterialTheme.colorScheme.background,
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(
                    listOf(
                            Triple(R.string.settings_show_home_clock, uiState.showHomeClock, viewModel::setShowHomeClock),
                            Triple(R.string.settings_show_home_date, uiState.showHomeDate, viewModel::setShowHomeDate),
                    ),
                    key = { it.first },
            ) { (labelRes, checked, onChange) ->
                SettingsToggleRow(
                        label = stringResource(labelRes),
                        checked = checked,
                        onCheckedChange = onChange,
                )
            }
            item {
                HomeDateFormatChipsRow(
                        currentStyle = uiState.homeDateFormatStyle,
                        enabled = uiState.showHomeDate,
                        onStyleSelected = viewModel::setHomeDateFormatStyle,
                )
            }
            item {
                TemperatureUnitChipsRow(
                        currentUnit = uiState.temperatureUnit,
                        enabled = uiState.showHomeWeather || uiState.showWorldClockWeather,
                        onUnitSelected = viewModel::setTemperatureUnit,
                )
            }
            items(
                    listOf(
                            Triple(R.string.settings_show_home_weather, uiState.showHomeWeather, viewModel::setShowHomeWeather),
                    ),
                    key = { it.first },
            ) { (labelRes, checked, onChange) ->
                SettingsToggleRow(
                        label = stringResource(labelRes),
                        checked = checked,
                        onCheckedChange = onChange,
                )
            }
            item {
                SettingsToggleRow(
                        label = stringResource(R.string.settings_show_world_clock_weather),
                        subtitle =
                                stringResource(R.string.settings_show_world_clock_weather_subtitle),
                        checked = uiState.showWorldClockWeather,
                        onCheckedChange = viewModel::setShowWorldClockWeather,
                )
            }
            item {
                SettingsToggleRow(
                        label = stringResource(R.string.settings_show_home_screen_time),
                        subtitle =
                                if (usageAccessEnabled) {
                                    stringResource(R.string.settings_show_home_screen_time_subtitle)
                                } else {
                                    stringResource(
                                            R.string.settings_show_home_screen_time_subtitle_grant_access
                                    )
                                },
                        checked = uiState.showHomeScreenTime,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (usageAccessEnabled) {
                                    viewModel.setShowHomeScreenTime(true)
                                } else {
                                    pendingScreenTimeEnable = true
                                    UsageStatsHelper.openUsageAccessSettings(context)
                                }
                            } else {
                                pendingScreenTimeEnable = false
                                viewModel.setShowHomeScreenTime(false)
                            }
                        },
                )
            }
            items(
                    listOf(
                            Triple(R.string.settings_show_home_battery, uiState.showHomeBattery, viewModel::setShowHomeBattery),
                    ),
                    key = { it.first },
            ) { (labelRes, checked, onChange) ->
                SettingsToggleRow(
                        label = stringResource(labelRes),
                        checked = checked,
                        onCheckedChange = onChange,
                )
            }
            item {
                SettingsToggleRow(
                        label = stringResource(R.string.settings_show_home_media),
                        subtitle =
                                if (mediaNotificationAccessEnabled) {
                                    stringResource(R.string.settings_show_home_media_subtitle)
                                } else {
                                    stringResource(R.string.settings_show_home_media_subtitle_grant_access)
                                },
                        checked = uiState.showHomeMedia,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (mediaNotificationAccessEnabled) {
                                    viewModel.setShowHomeMedia(true)
                                } else {
                                    pendingMediaEnable = true
                                    MediaNotificationHelper.openListenerSettings(context)
                                }
                            } else {
                                pendingMediaEnable = false
                                viewModel.setShowHomeMedia(false)
                            }
                        },
                )
            }
            item { SettingsDivider() }
            item {
                SettingsRow(
                        label = stringResource(R.string.settings_home_extra_widgets),
                        subtitle =
                                if (extraWidgets.isEmpty()) {
                                    stringResource(R.string.settings_home_extra_widgets_empty)
                                } else {
                                    null
                                },
                )
            }
            items(
                    count = extraWidgets.size,
                    key = { extraWidgets[it].stableKey },
            ) { index ->
                val entry = extraWidgets[index]
                val title: String
                val subtitle: String
                when (entry) {
                    is HomeExtraWidgetEntry.WorldClock -> {
                        val city = citiesById[entry.cityId]
                        title = city?.label ?: stringResource(R.string.settings_world_clock_cities)
                        subtitle =
                                if (city == null) {
                                    stringResource(R.string.settings_world_clock_cities_empty)
                                } else {
                                    val zone = displayNameForTimeZoneId(city.timeZoneId)
                                    val offset = formatUtcOffsetLabel(city.timeZoneId)
                                    "$zone · $offset"
                                }
                    }
                    is HomeExtraWidgetEntry.Countdown -> {
                        val event = eventsById[entry.eventId]
                        title = event?.title ?: stringResource(R.string.settings_countdown_event)
                        subtitle =
                                if (event == null) {
                                    stringResource(R.string.settings_countdown_event_empty)
                                } else {
                                    formatCountdownDateTimeLabel(context, event)
                                }
                    }
                }
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                                Modifier.fillMaxWidth()
                                        .heightIn(min = 56.dp)
                                        .graphicsLayer {
                                            translationY = reorderState.translationYForIndex(index)
                                        }
                                        .clickableWithSystemSound {
                                            when (entry) {
                                                is HomeExtraWidgetEntry.WorldClock ->
                                                        editingCityId = entry.cityId
                                                is HomeExtraWidgetEntry.Countdown ->
                                                        editingCountdownId = entry.eventId
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    EditorDragHandleReorderIcon(
                            reorderState = reorderState,
                            index = index,
                            lastIndex = extraWidgets.lastIndex,
                            onReorder = localExtras::reorder,
                            onReset = {
                                reorderState.reset {
                                    localExtras.onDragEnd(onExtraCommit)
                                }
                            },
                            entry.stableKey,
                            extraWidgets.size,
                            onDragGestureStart = localExtras::onDragStart,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    FokusTextButton(onClick = { viewModel.removeHomeExtraWidget(entry) }) {
                        Text(stringResource(R.string.settings_remove_other_widget))
                    }
                }
            }
            item {
                SettingsRow(
                        label = stringResource(R.string.settings_add_other_widget),
                        onClick = { showAddOtherWidget = true },
                )
            }
            item { SettingsDivider() }
            item {
                SettingsToggleRow(
                        label = stringResource(R.string.settings_notification_indicators),
                        subtitle =
                                if (mediaNotificationAccessEnabled) {
                                    stringResource(R.string.settings_notification_indicators_subtitle)
                                } else {
                                    stringResource(
                                            R.string.settings_notification_indicators_subtitle_grant_access
                                    )
                                },
                        checked = uiState.showNotificationIndicators,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (mediaNotificationAccessEnabled) {
                                    viewModel.setShowNotificationIndicators(true)
                                } else {
                                    pendingNotificationIndicatorsEnable = true
                                    MediaNotificationHelper.openListenerSettings(context)
                                }
                            } else {
                                pendingNotificationIndicatorsEnable = false
                                viewModel.setShowNotificationIndicators(false)
                            }
                        },
                )
            }
            if (uiState.showNotificationIndicators) {
                item {
                    NotificationIndicatorStyleChipsRow(
                            currentStyle = uiState.notificationIndicatorStyle,
                            onStyleSelected = viewModel::setNotificationIndicatorStyle,
                    )
                }
                item {
                    NotificationIndicatorColorChipsRow(
                            currentColor = uiState.notificationIndicatorColor,
                            onColorSelected = viewModel::setNotificationIndicatorColorPreset,
                    )
                }
            }
            item { SettingsDivider() }
            item {
                WeatherAppSettingRow(
                        hasCoarseLocationPermission = hasCoarseLocationPermission,
                        onRequestLocationPermission = requestCoarseLocation,
                        context = context,
                        resources = resources,
                        preferredWeatherTap = uiState.preferredWeatherTap,
                        allApps = uiState.allApps,
                        allShortcutActions = uiState.allShortcutActions,
                        onPickApp = { showAppPickerFor.value = "weather" },
                        onClear = { viewModel.setPreferredWeatherTap(null) },
                )
            }
            items(
                    listOf(
                            WidgetTapPickerRow(
                                    labelRes = R.string.settings_widget_clock_app,
                                    tapTarget = uiState.preferredClockTap,
                                    pickerKey = "clock",
                                    onClear = { viewModel.setPreferredClockTap(null) },
                            ),
                            WidgetTapPickerRow(
                                    labelRes = R.string.settings_widget_calendar_app,
                                    tapTarget = uiState.preferredCalendarTap,
                                    pickerKey = "calendar",
                                    onClear = { viewModel.setPreferredCalendarTap(null) },
                            ),
                    ),
                    key = { it.labelRes },
            ) { row ->
                ShortcutTargetRow(
                        label = stringResource(row.labelRes),
                        currentTarget =
                                formatWidgetTapTarget(
                                        context = context,
                                        resources = resources,
                                        binding = row.tapTarget,
                                        allApps = uiState.allApps,
                                        allActions = uiState.allShortcutActions,
                                        emptyLabel = row.emptyLabel,
                                ),
                        onPickApp = { showAppPickerFor.value = row.pickerKey },
                        onClear = row.onClear,
                )
            }
        }
    }

    showAppPickerFor.value?.let { pickerTarget ->
        ShortcutActionPickerDialog(
                allActions = uiState.allShortcutActions,
                allApps = uiState.allApps,
                title = stringResource(R.string.edit_shortcuts_section_all_actions),
                onSelect = { action ->
                    when (pickerTarget) {
                        "weather" -> viewModel.setPreferredWeatherTap(action)
                        "clock" -> viewModel.setPreferredClockTap(action)
                        "calendar" -> viewModel.setPreferredCalendarTap(action)
                    }
                    showAppPickerFor.value = null
                },
                onDismiss = { showAppPickerFor.value = null },
                profileDisplayNameOverrides = uiState.profileDisplayNameOverrides,
        )
    }

    if (showAddOtherWidget) {
        FokusAlertDialog(
                onDismissRequest = { showAddOtherWidget = false },
                title = { Text(stringResource(R.string.settings_add_other_widget_title)) },
                text = {
                    Column {
                        HomeExtraWidgetAddType.entries.forEach { type ->
                            SettingsRow(
                                    label = stringResource(type.labelRes),
                                    subtitle =
                                            stringResource(
                                                    when (type) {
                                                        HomeExtraWidgetAddType.WORLD_CLOCK ->
                                                                R.string.settings_show_home_world_clock_subtitle
                                                        HomeExtraWidgetAddType.COUNTDOWN ->
                                                                R.string.settings_show_home_countdown_subtitle
                                                    }
                                            ),
                                    horizontalPadding = 0.dp,
                                    onClick = {
                                        viewModel.addHomeExtraWidget(type)
                                        showAddOtherWidget = false
                                        when (type) {
                                            HomeExtraWidgetAddType.WORLD_CLOCK ->
                                                    pendingEditNewestCity = true
                                            HomeExtraWidgetAddType.COUNTDOWN ->
                                                    pendingEditNewestCountdown = true
                                        }
                                    },
                            )
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    FokusTextButton(onClick = { showAddOtherWidget = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
        )
    }

    // After adding a world clock, open the newest city for editing.
    LaunchedEffect(uiState.worldClockCities, pendingEditNewestCity) {
        if (pendingEditNewestCity) {
            val newest = uiState.worldClockCities.maxByOrNull { it.position }
            if (newest != null) {
                editingCityId = newest.id
                pendingEditNewestCity = false
            }
        }
    }

    LaunchedEffect(uiState.countdownEvents, pendingEditNewestCountdown) {
        if (pendingEditNewestCountdown) {
            val newest = uiState.countdownEvents.lastOrNull()
            if (newest != null) {
                editingCountdownId = newest.id
                pendingEditNewestCountdown = false
            }
        }
    }

    editingCityId?.let { cityId ->
        val city = citiesById[cityId]
        if (city != null) {
            WorldClockCityEditDialog(
                    title = stringResource(R.string.settings_world_clock_edit_city),
                    initialLabel = city.label,
                    initialTimeZoneId = city.timeZoneId,
                    onDismiss = { editingCityId = null },
                    onSave = { label, zone ->
                        if (viewModel.updateWorldClockCity(city.id, label, zone)) {
                            editingCityId = null
                        }
                    },
            )
        }
    }

    editingCountdownId?.let { eventId ->
        val event = eventsById[eventId]
        if (event != null) {
            CountdownEditDialog(
                    initialTitle = event.title,
                    initialEpochMillis = event.targetEpochMillis,
                    onDismiss = { editingCountdownId = null },
                    onSave = { title, epochMillis ->
                        if (viewModel.saveCountdownEvent(event.id, title, epochMillis)) {
                            editingCountdownId = null
                        }
                    },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceControlSettingsScreen(
        viewModel: SettingsViewModel = hiltViewModel(),
        onNavigateBack: () -> Unit = {},
        backgroundScrim: Color = FokusBackdrop.ScrimColorWithoutBlur
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val accessibilityDisclosureAccepted by viewModel.accessibilityProminentDisclosureAccepted.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAccessibilityProminentDisclosure by remember { mutableStateOf(false) }
    var accessibilityResumeTick by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    OnResumeEffect(lifecycleOwner) { accessibilityResumeTick++ }

    val lockAccessibilityOn =
            remember(accessibilityResumeTick) {
                LockScreenHelper.isLockAccessibilityServiceEnabled(context)
            }

    LaunchedEffect(lockAccessibilityOn, uiState.longLockReturnHome) {
        if (uiState.longLockReturnHome && !lockAccessibilityOn) {
            viewModel.setLongLockReturnHome(false)
        }
    }

    val deviceControlToggleRows =
            listOf(
                    DeviceControlToggleRow(
                            R.string.settings_double_tap_to_lock,
                            stringResource(R.string.settings_double_tap_to_lock_subtitle),
                            uiState.doubleTapEmptyLock,
                            viewModel::setDoubleTapEmptyLock,
                    ),
                    DeviceControlToggleRow(
                            R.string.settings_return_home_after_long_lock,
                            stringResource(R.string.settings_return_home_after_long_lock_subtitle),
                            uiState.longLockReturnHome,
                            viewModel::setLongLockReturnHome,
                    ),
            )

    Box(
            modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundScrim)
                    .navigationBarsPadding()
                    .testTag("device_control_settings_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            FokusSettingsTopBar(
                    titleText = stringResource(R.string.settings_accessibility_page_title),
                    onNavigateBack = onNavigateBack,
                    containerColor = MaterialTheme.colorScheme.background,
            )

            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                item {
                    SettingsToggleRow(
                            label = stringResource(R.string.settings_accessibility_permission),
                            subtitle =
                                    stringResource(
                                            if (lockAccessibilityOn) {
                                                R.string.settings_accessibility_permission_enabled
                                            } else {
                                                R.string.settings_accessibility_permission_disabled
                                            }
                                    ),
                            checked = lockAccessibilityOn,
                            onCheckedChange = {
                                when {
                                    !lockAccessibilityOn && !accessibilityDisclosureAccepted ->
                                            showAccessibilityProminentDisclosure = true
                                    else ->
                                            LockScreenHelper.openAccessibilitySettings(context)
                                }
                            }
                    )
                }

                items(
                        deviceControlToggleRows,
                        key = { it.labelRes },
                ) { row ->
                    SettingsToggleRow(
                            label = stringResource(row.labelRes),
                            subtitle = row.subtitle,
                            checked = row.checked,
                            onCheckedChange = row.onCheckedChange,
                            enabled = lockAccessibilityOn,
                    )
                }

                if (lockAccessibilityOn && uiState.longLockReturnHome) {
                    item {
                        LongLockThresholdRow(
                                currentMinutes = uiState.longLockReturnHomeThresholdMinutes,
                                onMinutesSelected = viewModel::setLongLockReturnHomeThresholdMinutes
                        )
                    }
                }
            }
        }

        if (showAccessibilityProminentDisclosure) {
            AccessibilityProminentDisclosureOverlay(
                    onAccept = {
                        showAccessibilityProminentDisclosure = false
                        viewModel.acceptAccessibilityProminentDisclosureAndOpenSettings()
                    },
                    onNotNow = { showAccessibilityProminentDisclosure = false },
            )
        }
    }
}

/**
 * Endonym: name of the language written in that language (e.g. English, Polski), independent of
 * app UI locale.
 */
private fun languageAutonym(localeTag: String, allTags: List<String>): String {
    val locale = Locale.forLanguageTag(localeTag)
    val sameLanguageTags =
            allTags.filter { Locale.forLanguageTag(it).language == locale.language }
    val displayLocale =
            when {
                sameLanguageTags.size <= 1 -> locale
                localeTag == "pt" -> Locale("pt", "PT")
                else -> locale
            }
    val raw =
            if (sameLanguageTags.size > 1) {
                displayLocale.getDisplayName(displayLocale).trim()
            } else {
                locale.getDisplayLanguage(locale).trim()
            }
    if (raw.isBlank()) return localeTag
    return raw.replaceFirstChar { ch ->
        if (ch.isLowerCase()) ch.titlecase(displayLocale) else ch.toString()
    }
}

@Composable
private fun HomeDateFormatChipsRow(
        currentStyle: HomeDateFormatStyle,
        enabled: Boolean,
        onStyleSelected: (HomeDateFormatStyle) -> Unit
) {
    val options = remember { HomeDateFormatStyle.entries }
    SettingsSelectionChipsRow(
            title = stringResource(R.string.settings_home_date_format),
            options = options,
            selectedOption = currentStyle,
            onOptionSelected = onStyleSelected,
            enabled = enabled,
            labelProvider = { stringResource(it.labelRes) }
    )
}

@Composable
private fun TemperatureUnitChipsRow(
        currentUnit: TemperatureUnit,
        enabled: Boolean,
        onUnitSelected: (TemperatureUnit) -> Unit
) {
    val options = remember { TemperatureUnit.entries }
    SettingsSelectionChipsRow(
            title = stringResource(R.string.settings_temperature_unit),
            options = options,
            selectedOption = currentUnit,
            onOptionSelected = onUnitSelected,
            enabled = enabled,
            labelProvider = { stringResource(it.labelRes) }
    )
}

@Composable
private fun NotificationIndicatorStyleChipsRow(
        currentStyle: NotificationIndicatorStyle,
        onStyleSelected: (NotificationIndicatorStyle) -> Unit,
) {
    val options = remember { NotificationIndicatorStyle.entries }
    SettingsSelectionChipsRow(
            title = stringResource(R.string.settings_notification_indicator_style),
            options = options,
            selectedOption = currentStyle,
            onOptionSelected = onStyleSelected,
            labelProvider = { stringResource(it.labelRes) }
    )
}

@Composable
private fun NotificationIndicatorColorChipsRow(
        currentColor: Int,
        onColorSelected: (NotificationIndicatorColorPreset) -> Unit,
) {
    val options = remember { NotificationIndicatorColorPreset.entries }
    val currentPreset = remember(currentColor) {
        NotificationIndicatorColorPreset.fromArgb(currentColor)
    }
    SettingsSelectionChipsRow(
            title = stringResource(R.string.settings_notification_indicator_color),
            options = options,
            selectedOption = currentPreset,
            onOptionSelected = onColorSelected,
            chipTextColor = { Color(it.argb) },
            labelProvider = { stringResource(it.labelRes) },
            leadingIconProvider = { preset ->
                Box(
                        modifier = Modifier
                                .size(10.dp)
                                .background(Color(preset.argb), CircleShape)
                )
            }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppLanguageDropdown(
        currentTag: String,
        onTagSelected: (String) -> Unit
) {
    val systemDefaultLabel = stringResource(R.string.settings_language_system_default)
    val supportedLocaleTags =
            remember {
                listOf(
                        "ca",
                        "da",
                        "de",
                        "en",
                        "es",
                        "eu",
                        "fi",
                        "fr",
                        "in",
                        "it",
                        "pl",
                        "pt",
                        "pt-BR",
                        "ro",
                        "ru",
                        "ta",
                        "tr",
                        "uk",
                        "zh-CN",
                )
            }
    val options =
            remember(systemDefaultLabel) {
                val collator = Collator.getInstance(Locale.ROOT).apply { strength = Collator.PRIMARY }
                buildList {
                    add("" to systemDefaultLabel)
                    supportedLocaleTags
                            .map { tag -> tag to languageAutonym(tag, supportedLocaleTags) }
                            .sortedWith { a, b -> collator.compare(a.second, b.second) }
                            .forEach { add(it) }
                }
            }
    var expanded by remember { mutableStateOf(false) }
    val onLanguageExpandedChange = rememberBooleanChangeWithSystemSound { expanded = it }
    val selectedDisplayText =
            options.find { (tag, _) -> tag == currentTag }?.second
                    ?: if (currentTag.isBlank()) {
                        systemDefaultLabel
                    } else {
                        languageAutonym(currentTag, supportedLocaleTags)
                    }
    SettingsDropdown(
            title = stringResource(R.string.settings_language_label),
            options = options,
            expanded = expanded,
            onExpandedChange = onLanguageExpandedChange,
            selectedDisplayText = selectedDisplayText,
            itemContent = { (_, label) ->
                Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                )
            },
            onItemSelected = { (storageTag, _) -> onTagSelected(storageTag) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LauncherFontFamilyDropdown(
        currentFamilyName: String,
        installedFamilies: List<String>,
        hasCustomFontFile: Boolean,
        customFontDisplayName: String,
        resolveCustomFontFile: (String) -> java.io.File?,
        onFamilySelected: (String) -> Unit
) {
    val systemDefault = stringResource(R.string.settings_weather_app_system_default)
    val customImportedFallback = stringResource(R.string.settings_font_custom_imported)
    val customFontLabel =
            customFontDisplayName.trim().ifBlank { customImportedFallback }
    val options =
            remember(
                    currentFamilyName,
                    installedFamilies,
                    systemDefault,
                    hasCustomFontFile,
                    customFontLabel,
            ) {
                buildList {
                    add("" to systemDefault)
                    if (hasCustomFontFile) {
                        add(LauncherFontPreferences.CUSTOM_FONT_STORAGE to customFontLabel)
                    }
                    val sorted = installedFamilies.sortedWith(String.CASE_INSENSITIVE_ORDER)
                    sorted.forEach { add(it to it) }
                    val cur = currentFamilyName.trim()
                    if (cur.isNotEmpty() &&
                                    sorted.none { it.equals(cur, ignoreCase = true) } &&
                                    !(hasCustomFontFile &&
                                            cur == LauncherFontPreferences.CUSTOM_FONT_STORAGE)
                    ) {
                        val label =
                                if (LauncherFontPreferences.isCustomFont(cur)) {
                                    customFontLabel
                                } else {
                                    cur
                                }
                        add(cur to label)
                    }
                }
            }
    var expanded by remember { mutableStateOf(false) }
    val onFontExpandedChange = rememberBooleanChangeWithSystemSound { expanded = it }
    val selectedLabel =
            options.find { (value, _) -> value == currentFamilyName }?.second
                    ?: currentFamilyName.ifBlank { systemDefault }
    SettingsDropdown(
            title = stringResource(R.string.settings_font_label),
            options = options,
            expanded = expanded,
            onExpandedChange = onFontExpandedChange,
            selectedDisplayText = selectedLabel,
            textStyle =
                    MaterialTheme.typography.bodyLarge.copy(
                            fontFamily =
                                    composeFontFamilyFromStoredName(currentFamilyName) {
                                        resolveCustomFontFile(it)
                                    }
                    ),
            itemContent = { (storageValue, label) ->
                Text(
                        text = label,
                        style =
                                MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily =
                                                composeFontFamilyFromStoredName(storageValue) {
                                                    resolveCustomFontFile(it)
                                                }
                                ),
                        color = MaterialTheme.colorScheme.onBackground,
                )
            },
            onItemSelected = { (storageValue, _) -> onFamilySelected(storageValue) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LauncherFontSizeSlider(
        currentScale: Float,
        onScaleChange: (Float) -> Unit,
) {
    val synced = LauncherFontScale.snapToStep(currentScale)
    var pending by remember { mutableFloatStateOf(synced) }
    LaunchedEffect(synced) { pending = synced }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                    text = stringResource(R.string.settings_font_size_label),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
            )
            Text(
                    text = String.format(Locale.US, "%.1fx", pending),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
                text = stringResource(R.string.settings_font_size_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(12.dp))
        Slider(
                value = pending,
                onValueChange = { raw ->
                    pending = LauncherFontScale.snapToStep(raw)
                },
                onValueChangeFinished = {
                    val v = LauncherFontScale.snapToStep(pending)
                    if (v != synced) {
                        onScaleChange(v)
                    }
                },
                valueRange = LauncherFontScale.MIN..LauncherFontScale.MAX,
                steps = LauncherFontScale.SLIDER_STEPS,
                modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoWallpaperOutlineWidthSlider(
        currentWidthDp: Float,
        onWidthDpChange: (Float) -> Unit,
) {
    val synced = PhotoWallpaperOutlineWidthDp.snapToStep(currentWidthDp)
    var pending by remember { mutableFloatStateOf(synced) }
    LaunchedEffect(synced) { pending = synced }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Text(
                text = stringResource(R.string.settings_photo_outline_strength_label),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
                text = stringResource(R.string.settings_photo_outline_strength_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(12.dp))
        Slider(
                value = pending,
                onValueChange = { raw ->
                    pending = raw.coerceIn(PhotoWallpaperOutlineWidthDp.MIN, PhotoWallpaperOutlineWidthDp.MAX)
                },
                onValueChangeFinished = {
                    val v = PhotoWallpaperOutlineWidthDp.snapToStep(pending)
                    if (v != synced) {
                        onWidthDpChange(v)
                    }
                },
                valueRange = PhotoWallpaperOutlineWidthDp.MIN..PhotoWallpaperOutlineWidthDp.MAX,
                steps = PhotoWallpaperOutlineWidthDp.SLIDER_STEPS,
                modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoWallpaperDrawerOverlaySlider(
        currentIntensity: Float,
        onIntensityChange: (Float) -> Unit,
) {
    val synced = PhotoWallpaperDrawerOverlayIntensity.snapToStep(currentIntensity)
    var pending by remember { mutableFloatStateOf(synced) }
    LaunchedEffect(synced) { pending = synced }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                    text = stringResource(R.string.settings_photo_drawer_overlay_label),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
            )
            Text(
                    text = String.format(Locale.US, "%.1fx", pending),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
                text = stringResource(R.string.settings_photo_drawer_overlay_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(12.dp))
        Slider(
                value = pending,
                onValueChange = { raw ->
                    pending = PhotoWallpaperDrawerOverlayIntensity.snapToStep(raw)
                },
                onValueChangeFinished = {
                    val v = PhotoWallpaperDrawerOverlayIntensity.snapToStep(pending)
                    if (v != synced) {
                        onIntensityChange(v)
                    }
                },
                valueRange =
                        PhotoWallpaperDrawerOverlayIntensity.MIN..PhotoWallpaperDrawerOverlayIntensity.MAX,
                steps = PhotoWallpaperDrawerOverlayIntensity.SLIDER_STEPS,
                modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LauncherVisualStyleChipsRow(
        currentStyle: LauncherVisualStyle,
        onStyleSelected: (LauncherVisualStyle) -> Unit,
        homeUsesPhotoWallpaper: Boolean,
) {
    val options = remember { LauncherVisualStyle.entries.toList() }
    val displayStyle = if (homeUsesPhotoWallpaper) LauncherVisualStyle.CLASSIC else currentStyle
    val lockedSubtitle = stringResource(R.string.settings_look_locked_image_wallpaper)
    val normalSubtitle = stringResource(R.string.settings_visual_style_subtitle)

    SettingsSelectionChipsRow(
            title = stringResource(R.string.settings_visual_style_label),
            subtitle = if (homeUsesPhotoWallpaper) lockedSubtitle else normalSubtitle,
            options = options,
            selectedOption = displayStyle,
            onOptionSelected = onStyleSelected,
            enabled = !homeUsesPhotoWallpaper,
            chipTextColor = { it.settingsPreviewColor() },
            labelProvider = { stringResource(it.labelRes) }
    )
}

@Composable
private fun DrawerCategoryRailSideRow(
        railOnLeft: Boolean,
        onRailOnLeftChanged: (Boolean) -> Unit
) {
    val options = remember { listOf(true, false) }
    val leftLabel = stringResource(R.string.settings_drawer_rail_position_left)
    val rightLabel = stringResource(R.string.settings_drawer_rail_position_right)
    SettingsSelectionChipsRow(
            title = stringResource(R.string.settings_drawer_category_rail_side),
            subtitle = stringResource(R.string.settings_drawer_category_rail_side_subtitle),
            options = options,
            selectedOption = railOnLeft,
            onOptionSelected = onRailOnLeftChanged,
            labelProvider = { if (it) leftLabel else rightLabel }
    )
}

@Composable
private fun DrawerAppSortRow(
        currentMode: DrawerAppSortMode,
        showCustomSortOption: Boolean,
        onModeChanged: (DrawerAppSortMode) -> Unit
) {
    val modes = remember(showCustomSortOption) {
        if (showCustomSortOption) DrawerAppSortMode.entries.toList()
        else DrawerAppSortMode.entries.filterNot { it == DrawerAppSortMode.CUSTOM }
    }
    val coercedMode = remember(currentMode, showCustomSortOption, modes) {
        if (!showCustomSortOption && currentMode == DrawerAppSortMode.CUSTOM) {
            DrawerAppSortMode.ALPHABETICAL
        } else {
            currentMode
        }
    }
    SettingsSelectionChipsRow(
            title = stringResource(R.string.settings_drawer_app_sort),
            subtitle = stringResource(R.string.settings_drawer_app_sort_subtitle),
            options = modes,
            selectedOption = coercedMode,
            onOptionSelected = onModeChanged,
            labelProvider = { stringResource(it.labelRes) }
    )
}

@Composable
private fun LongLockThresholdRow(
        currentMinutes: Int,
        onMinutesSelected: (Int) -> Unit
) {
    val options = remember { listOf(1, 5, 15, 30) }
    SettingsSelectionChipsRow(
            title = stringResource(R.string.settings_long_lock_duration),
            subtitle = stringResource(R.string.settings_long_lock_duration_subtitle),
            options = options,
            selectedOption = currentMinutes,
            onOptionSelected = onMinutesSelected,
            labelProvider = { minutes ->
                pluralStringResource(
                        R.plurals.settings_long_lock_duration_minutes,
                        minutes,
                        minutes
                )
            }
    )
}

@Composable
private fun HomeAlignmentRow(
        currentAlignment: HomeAlignment,
        onAlignmentChanged: (HomeAlignment) -> Unit
) {
    SettingsSelectionChipsRow(
            title = stringResource(R.string.home_alignment_title),
            subtitle = stringResource(R.string.home_alignment_subtitle),
            options = remember { HomeAlignment.entries.toList() },
            selectedOption = currentAlignment,
            onOptionSelected = onAlignmentChanged,
            labelProvider = { stringResource(it.labelRes) }
    )
}

@Composable
private fun SubpageChevron() {
    LauncherIcon(
            imageVector = Icons.AutoMirrored.Filled.NavigateNext,
            contentDescription = stringResource(R.string.cd_open_subpage),
            tint = MaterialTheme.colorScheme.secondary,
            iconSize = 22.dp,
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
            text = title,
            style =
                    MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp,
                    ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsDivider() {
    Spacer(Modifier.height(10.dp))
    HorizontalDivider(
            modifier = Modifier.padding(horizontal = 24.dp),
            thickness = 0.75.dp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.16f)
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun EmptySettingsStateText(text: String) {
    Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
private fun ExportLogsRow(
        context: Context,
        createLogShareIntent: suspend () -> Intent?
) {
    val scope = rememberCoroutineScope()
    val activity = LocalActivity.current
    val shareChooserTitle = stringResource(R.string.settings_export_logs_share_chooser)
    val exportLogsFailedToast = stringResource(R.string.toast_export_logs_failed)
    SettingsRow(
            label = stringResource(R.string.settings_export_logs_title),
            subtitle = stringResource(R.string.settings_export_logs_subtitle),
            verticalPadding = 14.dp,
            onClick = {
                scope.launch {
                    val shareIntent = createLogShareIntent()
                    if (shareIntent != null && activity != null) {
                        activity.startActivity(
                                Intent.createChooser(shareIntent, shareChooserTitle)
                        )
                    } else {
                        Toast.makeText(context, exportLogsFailedToast, Toast.LENGTH_SHORT).show()
                    }
                }
            },
    )
}

// --- Weather app (location gate + shortcut row) ---

@Composable
private fun WeatherAppSettingRow(
        hasCoarseLocationPermission: Boolean,
        onRequestLocationPermission: () -> Unit,
        context: Context,
        resources: Resources,
        preferredWeatherTap: WidgetTapTarget?,
        allApps: List<AppInfo>,
        allShortcutActions: List<AppShortcutAction>,
        onPickApp: () -> Unit,
        onClear: () -> Unit,
) {
    Column {
        if (!hasCoarseLocationPermission) {
            SettingsRow(
                    label = stringResource(R.string.settings_weather_location_disabled),
                    subtitle = stringResource(R.string.settings_weather_location_disabled_subtitle),
                    onClick = onRequestLocationPermission,
                    leading = {
                        LauncherIcon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                iconSize = 24.dp,
                        )
                    },
                    trailing = {
                        FokusTextButton(onClick = onRequestLocationPermission) {
                            Text(stringResource(R.string.settings_weather_location_enable_button))
                        }
                    },
            )
        } else {
            val weatherAppLabel =
                    formatWidgetTapTarget(
                            context = context,
                            resources = resources,
                            binding = preferredWeatherTap,
                            allApps = allApps,
                            allActions = allShortcutActions,
                            emptyLabel = ::formatWeatherAppEmptyLabel,
                    )
            ShortcutTargetRow(
                    label = stringResource(R.string.settings_weather_app),
                    currentTarget = weatherAppLabel,
                    onPickApp = onPickApp,
                    onClear = onClear,
            )
        }
    }
}

// --- Swipe shortcut row ---

@Composable
private fun ShortcutTargetRow(
        label: String,
        currentTarget: String,
        onPickApp: () -> Unit,
        onClear: () -> Unit
) {
    Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                    currentTarget,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
            )
        }
        Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp),
        ) {
            FokusIconButton(
                    onClick = onPickApp,
                    modifier = Modifier.size(36.dp.launcherIconDp()),
            ) {
                LauncherIcon(
                        Icons.Outlined.Edit,
                        stringResource(R.string.action_change),
                        tint = MaterialTheme.colorScheme.primary,
                        iconSize = 20.dp,
                )
            }
            FokusIconButton(
                    onClick = onClear,
                    modifier = Modifier.size(36.dp.launcherIconDp()),
            ) {
                LauncherIcon(
                        Icons.Default.Close,
                        stringResource(R.string.action_clear),
                        tint = MaterialTheme.colorScheme.error,
                        iconSize = 18.dp,
                )
            }
        }
    }
}

// =====================  DIALOGS  =====================

private fun formatWidgetAppEmptyLabel(context: Context, resources: Resources): String =
        resources.getString(R.string.settings_weather_app_system_default)

private fun formatWeatherAppEmptyLabel(context: Context, resources: Resources): String {
    val hasSystemWeatherApp =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Intent(Intent.ACTION_MAIN)
                        .apply { addCategory(Intent.CATEGORY_APP_WEATHER) }
                        .resolveActivity(context.packageManager) != null
            } else {
                false
            }
    return if (hasSystemWeatherApp) {
        resources.getString(R.string.settings_weather_app_system_default)
    } else {
        resources.getString(R.string.settings_weather_app_not_configured)
    }
}

private fun formatWidgetTapTarget(
        context: Context,
        resources: Resources,
        binding: WidgetTapTarget?,
        allApps: List<AppInfo>,
        allActions: List<AppShortcutAction>,
        emptyLabel: (Context, Resources) -> String,
): String {
    if (binding == null) return emptyLabel(context, resources)
    val resolvedLabel =
            allActions.find {
                it.target == binding.target && it.profileKey == binding.profileKey
            }?.actionLabel
    return formatShortcutTargetDisplay(
            context = context,
            target = binding.target,
            allApps = allApps,
            notSetLabel = emptyLabel(context, resources),
            resolvedLauncherActionLabel = resolvedLabel,
            profileKey = binding.profileKey,
    )
}

private fun formatShortcutTarget(
        context: Context,
        resources: Resources,
        target: ShortcutTarget?,
        allApps: List<AppInfo>
): String {
    return formatShortcutTargetDisplay(
            context = context,
            target = target,
            allApps = allApps,
            notSetLabel = resources.getString(R.string.shortcut_target_not_set),
            resolvedLauncherActionLabel = null
    )
}
