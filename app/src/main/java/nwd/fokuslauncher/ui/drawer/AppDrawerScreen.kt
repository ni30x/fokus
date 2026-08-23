package nwd.fokuslauncher.ui.drawer
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import nwd.fokuslauncher.ui.util.applyVerticalSlotReorder
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nwd.fokuslauncher.R
import nwd.fokuslauncher.data.model.AppInfo
import nwd.fokuslauncher.data.model.DrawerAppSortMode
import nwd.fokuslauncher.data.model.NotificationIndicatorColorPreset
import nwd.fokuslauncher.data.model.NotificationIndicatorStyle
import nwd.fokuslauncher.data.model.ReservedCategoryNames
import nwd.fokuslauncher.data.model.appListStableKey
import nwd.fokuslauncher.data.model.appMetadataKey
import nwd.fokuslauncher.data.model.drawerOpenCountKey
import nwd.fokuslauncher.utils.DotSearchSyntax
import nwd.fokuslauncher.ui.components.CategoryChips
import nwd.fokuslauncher.ui.components.CategoryIconPickerDialog
import nwd.fokuslauncher.ui.components.DrawerCategorySidebar
import nwd.fokuslauncher.ui.components.FokusBottomSheet
import nwd.fokuslauncher.ui.components.FokusIconButton
import nwd.fokuslauncher.ui.components.LauncherIcon
import nwd.fokuslauncher.ui.components.FokusTextButton
import nwd.fokuslauncher.ui.components.MinimalIcons
import nwd.fokuslauncher.ui.components.SearchBar
import nwd.fokuslauncher.ui.components.SheetActionRow
import nwd.fokuslauncher.ui.components.SheetInlineRenameTitleRow
import nwd.fokuslauncher.ui.util.categoryChipDisplayLabel
import nwd.fokuslauncher.ui.util.clickableWithSystemSound
import nwd.fokuslauncher.ui.util.combinedClickableWithSystemSound
import nwd.fokuslauncher.ui.util.rememberClickWithSystemSound
import nwd.fokuslauncher.ui.util.resolvedCategoryDrawerIconName

private fun deepCopyProfileSections(
        sections: List<DrawerProfileSectionUi>
): List<DrawerProfileSectionUi> =
        sections.map { DrawerProfileSectionUi(it.id, it.title, it.apps.toList()) }

/** Applies one adjacent move in [sectionId]'s app list (same semantics as VM reorder). */
private fun swapAdjacentInProfileSection(
        sections: List<DrawerProfileSectionUi>,
        sectionId: String,
        from: Int,
        to: Int
): List<DrawerProfileSectionUi> {
    return sections.map { sec ->
        if (sec.id != sectionId) sec
        else {
            val apps = sec.apps.toMutableList()
            val item = apps.removeAt(from)
            apps.add(to, item)
            sec.copy(apps = apps)
        }
    }
}

private fun applyOptimisticProfileSwap(
        optimistic: List<DrawerProfileSectionUi>?,
        fallback: List<DrawerProfileSectionUi>,
        sectionId: String,
        from: Int,
        to: Int
): List<DrawerProfileSectionUi> {
    val base = optimistic ?: deepCopyProfileSections(fallback)
    return swapAdjacentInProfileSection(base, sectionId, from, to)
}

private fun applyOptimisticPrivateSwap(
        optimistic: List<AppInfo>?,
        fallback: List<AppInfo>,
        from: Int,
        to: Int
): List<AppInfo> {
    val base = optimistic ?: fallback.toList()
    val apps = base.toMutableList()
    val item = apps.removeAt(from)
    apps.add(to, item)
    return apps
}

private fun profileSectionsOrderKey(sections: List<DrawerProfileSectionUi>): String =
        sections.joinToString(separator = "|") { section ->
            buildString {
                append(section.id)
                append(':')
                append(section.apps.joinToString(separator = ",") { appListStableKey(it) })
            }
        }

private fun appOrderKey(apps: List<AppInfo>): String =
        apps.joinToString(separator = ",") { appListStableKey(it) }

/** Horizontal swipe distance (px) to move to the next/previous category in the app list. */
private const val DRAWER_CATEGORY_SWIPE_THRESHOLD_PX = 60f
private val DRAWER_MIN_TOP_PADDING = 12.dp
private val DRAWER_TOP_INSET_BUFFER = 16.dp
private val DRAWER_CATEGORY_CHIPS_TOP_OFFSET = 2.dp

private val ReservedDrawerActionCategories =
        setOf(
                ReservedCategoryNames.ALL_APPS,
                ReservedCategoryNames.PRIVATE,
                ReservedCategoryNames.WORK,
                ReservedCategoryNames.UNCATEGORIZED,
        )

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LazyItemScope.ReorderableDrawerAppRow(
        allowCustomDragReorder: Boolean,
        placementAnimationEnabled: Boolean,
        offsetY: Float,
        dragHandleModifier: Modifier,
        rowAlpha: Float = 1f,
        content: @Composable RowScope.() -> Unit,
) {
    val animatedAlpha by animateFloatAsState(targetValue = rowAlpha, label = "rowAlpha")
    Row(
            modifier =
                    Modifier.then(
                                    if (allowCustomDragReorder && placementAnimationEnabled) {
                                        Modifier.animateItem(
                                                fadeInSpec = null,
                                                fadeOutSpec = null,
                                                placementSpec =
                                                        tween(
                                                                180,
                                                                easing = FastOutSlowInEasing,
                                                        ),
                                        )
                                    } else {
                                        Modifier
                                    }
                            )
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .graphicsLayer { 
                                translationY = offsetY
                                alpha = animatedAlpha
                            },
            verticalAlignment = Alignment.CenterVertically,
    ) {
        if (allowCustomDragReorder) {
            LauncherIcon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = stringResource(R.string.cd_drag_to_reorder),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconSize = 24.dp,
                    modifier =
                            Modifier.padding(start = 8.dp).then(dragHandleModifier),
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LazyItemScope.ReorderableDrawerAppListItem(
        app: AppInfo,
        allowCustomDragReorder: Boolean,
        isDraggedRow: Boolean,
        offsetY: Float,
        dragHandleModifier: Modifier,
        onLaunchWhenNotReordering: () -> Unit,
        onLongPressWhenNotReordering: () -> Unit,
        showNotificationIndicators: Boolean = false,
        notificationIndicatorStyle: NotificationIndicatorStyle = NotificationIndicatorStyle.DOT,
        notificationIndicatorColor: Int = NotificationIndicatorColorPreset.DEFAULT.argb,
        appsWithNotifications: Set<String> = emptySet(),
        rowAlpha: Float = 1f,
) {
    ReorderableDrawerAppRow(
            allowCustomDragReorder = allowCustomDragReorder,
            placementAnimationEnabled = !isDraggedRow,
            offsetY = offsetY,
            dragHandleModifier = dragHandleModifier,
            rowAlpha = rowAlpha,
    ) {
        val hasNotification =
                showNotificationIndicators &&
                        drawerOpenCountKey(app.packageName, app.userHandle) in appsWithNotifications
        AppListItem(
                app = app,
                onClick = {
                    if (allowCustomDragReorder) return@AppListItem
                    onLaunchWhenNotReordering()
                },
                onLongClick = {
                    if (!allowCustomDragReorder) onLongPressWhenNotReordering()
                },
                modifier = Modifier.weight(1f),
                hasNotification = hasNotification,
                notificationIndicatorStyle = notificationIndicatorStyle,
                notificationIndicatorColor = notificationIndicatorColor,
                reserveNotificationDotSlot = showNotificationIndicators &&
                        notificationIndicatorStyle == NotificationIndicatorStyle.DOT,
        )
    }
}

@Composable
private fun DrawerDropdownMenuItem(
        text: @Composable () -> Unit,
        onClick: () -> Unit,
        leadingIcon: @Composable () -> Unit,
        testTag: String,
) {
    DropdownMenuItem(
            text = text,
            onClick = rememberClickWithSystemSound(onClick),
            leadingIcon = leadingIcon,
            modifier = Modifier.testTag(testTag),
            colors =
                    MenuDefaults.itemColors(
                            textColor = MaterialTheme.colorScheme.onBackground,
                            leadingIconColor = MaterialTheme.colorScheme.onBackground,
                            trailingIconColor = MaterialTheme.colorScheme.onBackground,
                    ),
    )
}

@Composable
private fun DrawerOverflowMenu(
        uiState: AppDrawerUiState,
        onMenuToggle: () -> Unit,
        onMenuDismiss: () -> Unit,
        onPrivateSpaceToggle: () -> Unit,
        onSettingsClick: () -> Unit,
        showReorderMenuItem: Boolean,
        onToggleReorderApps: () -> Unit,
        modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        FokusIconButton(onClick = onMenuToggle, modifier = Modifier.testTag("settings_button")) {
            LauncherIcon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.cd_menu),
                    tint = MaterialTheme.colorScheme.onBackground,
                    iconSize = 24.dp,
            )
        }
        DropdownMenu(
                expanded = uiState.showMenu,
                onDismissRequest = onMenuDismiss,
                modifier =
                        Modifier.background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(16.dp)
                        )
        ) {
            if (uiState.isPrivateSpaceSupported) {
                DrawerDropdownMenuItem(
                        text = {
                            Text(
                                    text =
                                            if (uiState.isPrivateSpaceUnlocked)
                                                    stringResource(R.string.drawer_private_space_lock)
                                            else stringResource(R.string.drawer_private_space_unlock)
                            )
                        },
                        onClick = onPrivateSpaceToggle,
                        leadingIcon = {
                            LauncherIcon(
                                    imageVector =
                                            if (uiState.isPrivateSpaceUnlocked)
                                                    Icons.Default.LockOpen
                                            else Icons.Default.Lock,
                                    contentDescription = null,
                                    iconSize = 24.dp,
                            )
                        },
                        testTag = "menu_private_space",
                )
            }
            if (showReorderMenuItem) {
                DrawerDropdownMenuItem(
                        text = {
                            Text(
                                    stringResource(
                                            if (uiState.drawerReorderSessionActive) {
                                                R.string.drawer_reorder_done
                                            } else {
                                                R.string.drawer_reorder
                                            }
                                    )
                            )
                        },
                        onClick = onToggleReorderApps,
                        leadingIcon = {
                            LauncherIcon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = null,
                                    iconSize = 24.dp,
                            )
                        },
                        testTag = "menu_reorder_apps",
                )
            }
            DrawerDropdownMenuItem(
                    text = { Text(stringResource(R.string.drawer_menu_launcher_settings)) },
                    onClick = onSettingsClick,
                    leadingIcon = {
                        LauncherIcon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                iconSize = 24.dp,
                        )
                    },
                    testTag = "menu_settings",
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerAppListColumn(
        listState: LazyListState,
        modifier: Modifier,
        uiState: AppDrawerUiState,
        showProfileSections: Boolean,
        anyProfileAppsVisible: Boolean,
        focusManager: FocusManager,
        onAppClick: (LaunchTarget) -> Unit,
        onAppLongPress: (AppInfo) -> Unit,
        allowCustomDragReorder: Boolean,
        activeLetter: String?,
        onReorderProfileSection: (sectionId: String, fromIndex: Int, toIndex: Int) -> Unit,
        onReorderPrivateApps: (fromIndex: Int, toIndex: Int) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val closeWithFocusReset: () -> Unit = {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
    }
    // Match CategorySettingsScreen.ReorderableCategoryList: 56dp steps, ±slot-coerced
    // offset, adjacent swaps in while-loops during drag (well-tested pattern).
    val itemHeightPx = with(LocalDensity.current) { 56.dp.toPx() }
    var optimisticProfileSections by remember { mutableStateOf<List<DrawerProfileSectionUi>?>(null) }
    var optimisticPrivateApps by remember { mutableStateOf<List<AppInfo>?>(null) }
    val profileSectionsOrderSignature = remember(uiState.filteredProfileSections) {
        profileSectionsOrderKey(uiState.filteredProfileSections)
    }
    val privateAppsOrderSignature = remember(uiState.filteredPrivateSpaceApps) {
        appOrderKey(uiState.filteredPrivateSpaceApps)
    }

    LaunchedEffect(allowCustomDragReorder, profileSectionsOrderSignature, privateAppsOrderSignature) {
        if (!allowCustomDragReorder) {
            optimisticProfileSections = null
            optimisticPrivateApps = null
        } else {
            optimisticProfileSections = deepCopyProfileSections(uiState.filteredProfileSections)
            optimisticPrivateApps = uiState.filteredPrivateSpaceApps.toList()
        }
    }

    val displayProfileSections = optimisticProfileSections ?: uiState.filteredProfileSections
    val displayPrivateApps = optimisticPrivateApps ?: uiState.filteredPrivateSpaceApps
    val latestDisplayProfileSections by rememberUpdatedState(displayProfileSections)
    val latestDisplayPrivateApps by rememberUpdatedState(displayPrivateApps)
    val currentOnReorderProfile by rememberUpdatedState(onReorderProfileSection)
    val currentOnReorderPrivate by rememberUpdatedState(onReorderPrivateApps)
    var draggedProfileSectionId by remember(allowCustomDragReorder) {
        mutableStateOf<String?>(null)
    }
    var draggedProfileIndex by remember(allowCustomDragReorder) { mutableIntStateOf(-1) }
    var profileDragOffset by remember(allowCustomDragReorder) { mutableFloatStateOf(0f) }
    var draggedPrivateIndex by remember(allowCustomDragReorder) { mutableIntStateOf(-1) }
    var privateDragOffset by remember(allowCustomDragReorder) { mutableFloatStateOf(0f) }
    val resetProfileDrag = {
        draggedProfileSectionId = null
        draggedProfileIndex = -1
        profileDragOffset = 0f
    }
    val resetPrivateDrag = {
        draggedPrivateIndex = -1
        privateDragOffset = 0f
    }

    val drawerContext = LocalContext.current
    var permissionCheckTrigger by remember { mutableIntStateOf(0) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionCheckTrigger++
    }

    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    val hasVisualMediaPermission = remember(permissionCheckTrigger, uiState.searchQuery) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            (ContextCompat.checkSelfPermission(drawerContext, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
             ContextCompat.checkSelfPermission(drawerContext, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED) ||
             ContextCompat.checkSelfPermission(drawerContext, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(drawerContext, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(drawerContext, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(drawerContext, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    val hasAudioMediaPermission = remember(permissionCheckTrigger, uiState.searchQuery) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(drawerContext, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(drawerContext, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    val hasStoragePermission = hasVisualMediaPermission || hasAudioMediaPermission

    LazyColumn(state = listState, modifier = modifier) {
        if (uiState.searchQuery.isNotBlank()) {
            item(key = "app_target_chips") {
                AppTargetChipsList(
                    query = uiState.searchQuery,
                    chips = uiState.targetAppChips,
                    onChipClick = { chip ->
                        chip.launchAction(drawerContext, uiState.searchQuery)
                        closeWithFocusReset()
                    }
                )
            }
            uiState.quickActionResult?.let { result ->
                item(key = "quick_action_card") {
                    InLineQuickActionCard(result = result)
                }
            }
        }
        if (showProfileSections) {
            var hasEmittedProfileListContent = false
            for (section in displayProfileSections) {
                if (section.apps.isEmpty()) continue
                val showSectionLabel = section.id != "owner"
                if (showSectionLabel) {
                    if (hasEmittedProfileListContent) {
                        item(key = "div_profile_${section.id}") { DrawerListSectionDivider() }
                    }
                    item(key = "hdr_profile_${section.id}") {
                        DrawerListSectionHeader(text = section.title)
                    }
                }
                hasEmittedProfileListContent = true
                items(
                        count = section.apps.size,
                        key = { index ->
                            "${section.id}_${appListStableKey(section.apps[index])}"
                        }
                ) { index ->
                    val app = section.apps[index]
                    val currentIndex by rememberUpdatedState(index)
                    val latestSectionApps = rememberUpdatedState(section.apps)
                    val isDraggedRow =
                            allowCustomDragReorder &&
                                    section.id == draggedProfileSectionId &&
                                    index == draggedProfileIndex
                    val offsetY =
                            if (isDraggedRow) {
                                profileDragOffset.coerceIn(-itemHeightPx, itemHeightPx)
                            } else {
                                0f
                            }
                    val isFadedOut = activeLetter != null && if (activeLetter == "#") {
                        app.label.firstOrNull()?.isLetter() == true
                    } else {
                        !app.label.startsWith(activeLetter, ignoreCase = true)
                    }
                    val rowAlpha = if (isFadedOut) 0.3f else 1f

                    ReorderableDrawerAppListItem(
                            app = app,
                            allowCustomDragReorder = allowCustomDragReorder,
                            isDraggedRow = isDraggedRow,
                            offsetY = offsetY,
                            rowAlpha = rowAlpha,
                            dragHandleModifier =
                                    Modifier.pointerInput(
                                            section.id,
                                            appListStableKey(app),
                                    ) {
                                        detectVerticalDragGestures(
                                                onDragStart = {
                                                    draggedProfileSectionId = section.id
                                                    draggedProfileIndex = currentIndex
                                                    profileDragOffset = 0f
                                                },
                                                onVerticalDrag = { change, amount ->
                                                    change.consume()
                                                    val sectionApps = latestSectionApps.value
                                                    if (draggedProfileSectionId == section.id &&
                                                                    draggedProfileIndex in sectionApps.indices
                                                    ) {
                                                        profileDragOffset += amount
                                                        val (newOff, newIdx) =
                                                                applyVerticalSlotReorder(
                                                                        itemHeightPx,
                                                                        profileDragOffset,
                                                                        draggedProfileIndex,
                                                                        sectionApps.lastIndex,
                                                                ) { from, to ->
                                                                    optimisticProfileSections =
                                                                            applyOptimisticProfileSwap(
                                                                                    optimisticProfileSections,
                                                                                    latestDisplayProfileSections,
                                                                                    section.id,
                                                                                    from,
                                                                                    to
                                                                            )
                                                                    currentOnReorderProfile(
                                                                            section.id,
                                                                            from,
                                                                            to
                                                                    )
                                                                }
                                                        profileDragOffset = newOff
                                                        draggedProfileIndex = newIdx
                                                    }
                                                },
                                                onDragEnd = { resetProfileDrag() },
                                                onDragCancel = { resetProfileDrag() }
                                        )
                                    },
                            onLaunchWhenNotReordering = {
                                onAppClick(launchTargetFromAppInfo(app))
                            },
                            onLongPressWhenNotReordering = { onAppLongPress(app) },
                            showNotificationIndicators = uiState.showNotificationIndicators,
                            notificationIndicatorStyle = uiState.notificationIndicatorStyle,
                            notificationIndicatorColor = uiState.notificationIndicatorColor,
                            appsWithNotifications = uiState.appsWithNotifications,
                    )
                }
            }
        }
        if (uiState.isPrivateSpaceUnlocked && displayPrivateApps.isNotEmpty()) {
            if (showProfileSections && anyProfileAppsVisible) {
                item { DrawerListSectionDivider() }
            }
            item {
                DrawerListSectionHeader(
                        text = stringResource(R.string.drawer_section_private_space)
                )
            }
            items(
                    count = displayPrivateApps.size,
                    key = { index ->
                        "private_${appListStableKey(displayPrivateApps[index])}"
                    }
            ) { index ->
                val app = displayPrivateApps[index]
                val currentIndex by rememberUpdatedState(index)
                val isDraggedRow =
                        allowCustomDragReorder && index == draggedPrivateIndex
                val offsetY =
                        if (isDraggedRow) {
                            privateDragOffset.coerceIn(-itemHeightPx, itemHeightPx)
                        } else {
                            0f
                        }
                val isFadedOut = activeLetter != null && if (activeLetter == "#") {
                    app.label.firstOrNull()?.isLetter() == true
                } else {
                    !app.label.startsWith(activeLetter, ignoreCase = true)
                }
                val rowAlpha = if (isFadedOut) 0.3f else 1f

                ReorderableDrawerAppListItem(
                        app = app,
                        allowCustomDragReorder = allowCustomDragReorder,
                        isDraggedRow = isDraggedRow,
                        offsetY = offsetY,
                        rowAlpha = rowAlpha,
                        dragHandleModifier =
                                Modifier.pointerInput(
                                        appListStableKey(app),
                                        latestDisplayPrivateApps.size,
                                ) {
                                    detectVerticalDragGestures(
                                            onDragStart = {
                                                draggedPrivateIndex = currentIndex
                                                privateDragOffset = 0f
                                            },
                                            onVerticalDrag = { change, amount ->
                                                change.consume()
                                                if (draggedPrivateIndex in latestDisplayPrivateApps.indices) {
                                                    privateDragOffset += amount
                                                    val (newOff, newIdx) =
                                                            applyVerticalSlotReorder(
                                                                    itemHeightPx,
                                                                    privateDragOffset,
                                                                    draggedPrivateIndex,
                                                                    latestDisplayPrivateApps.lastIndex,
                                                            ) { from, to ->
                                                                optimisticPrivateApps =
                                                                        applyOptimisticPrivateSwap(
                                                                                optimisticPrivateApps,
                                                                                latestDisplayPrivateApps,
                                                                                from,
                                                                                to
                                                                        )
                                                                currentOnReorderPrivate(from, to)
                                                            }
                                                    privateDragOffset = newOff
                                                    draggedPrivateIndex = newIdx
                                                }
                                            },
                                            onDragEnd = { resetPrivateDrag() },
                                            onDragCancel = { resetPrivateDrag() }
                                    )
                                },
                        onLaunchWhenNotReordering = {
                            val componentName = app.componentName
                            val userHandle = app.userHandle
                            if (componentName != null && userHandle != null) {
                                onAppClick(
                                        LaunchTarget.PrivateApp(
                                                packageName = app.packageName,
                                                componentName = componentName,
                                                userHandle = userHandle
                                        )
                                )
                            }
                        },
                        onLongPressWhenNotReordering = { onAppLongPress(app) },
                        showNotificationIndicators = uiState.showNotificationIndicators,
                        notificationIndicatorStyle = uiState.notificationIndicatorStyle,
                        notificationIndicatorColor = uiState.notificationIndicatorColor,
                        appsWithNotifications = uiState.appsWithNotifications,
                )
            }
        }
        if (uiState.searchQuery.isNotBlank()) {
            universalSearchResults(
                context = drawerContext,
                settingsResults = uiState.settingsResults,
                fileResults = uiState.fileResults,
                contactResults = uiState.contactResults,
                callLogResults = uiState.callLogResults,
                messageResults = uiState.messageResults,
                calendarResults = uiState.calendarResults,
                hasStoragePermission = hasStoragePermission,
                hasVisualMediaPermission = hasVisualMediaPermission,
                hasAudioMediaPermission = hasAudioMediaPermission,
                onRequestStoragePermission = {
                    permissionLauncher.launch(permissionsToRequest)
                },
                onCloseDrawer = closeWithFocusReset
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ColumnScope.DrawerAppListBody(
        listState: LazyListState,
        categorySwipeModifier: Modifier,
        uiState: AppDrawerUiState,
        showProfileSections: Boolean,
        anyProfileAppsVisible: Boolean,
        focusManager: FocusManager,
        onAppClick: (LaunchTarget) -> Unit,
        onAppLongPress: (AppInfo) -> Unit,
        allowCustomDragReorder: Boolean,
        activeLetter: String?,
        onReorderDrawerProfileSection: (sectionId: String, fromIndex: Int, toIndex: Int) -> Unit,
        onReorderPrivateDrawerApps: (fromIndex: Int, toIndex: Int) -> Unit,
) {
    DrawerAppListColumn(
            listState = listState,
            modifier =
                    Modifier.weight(1f)
                            .fillMaxWidth()
                            .then(categorySwipeModifier)
                            .testTag("app_list"),
            uiState = uiState,
            showProfileSections = showProfileSections,
            anyProfileAppsVisible = anyProfileAppsVisible,
            focusManager = focusManager,
            onAppClick = onAppClick,
            onAppLongPress = onAppLongPress,
            allowCustomDragReorder = allowCustomDragReorder,
            activeLetter = activeLetter,
            onReorderProfileSection = onReorderDrawerProfileSection,
            onReorderPrivateApps = onReorderPrivateDrawerApps,
    )
}

@Composable
fun AppDrawerScreen(
        modifier: Modifier = Modifier,
        viewModel: AppDrawerViewModel = hiltViewModel(),
        onSettingsClick: () -> Unit = {},
        onEditCategoryApps: (String) -> Unit = {},
        onClose: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val onCloseUpdated = rememberUpdatedState(onClose)
    val closeAndReset = remember(viewModel, keyboardController) {
        {
            // Start IME hide with the drawer exit, not after AnimatedVisibility tears down.
            keyboardController?.hide()
            viewModel.resetSearchState()
            onCloseUpdated.value()
        }
    }
    // Defer closing until after startActivity is processed so the drawer exit animation does not
    // run in the same frame as the launch handoff (smoother transition, avoids perceived "close
    // before open").
    val closeAndResetAfterLaunch = remember(view, viewModel, keyboardController) {
        {
            keyboardController?.hide()
            view.post {
                viewModel.resetSearchState()
                onCloseUpdated.value()
            }
        }
    }

    // Close the drawer after an app is auto-launched from search
    LaunchedEffect(Unit) {
        viewModel.resetSearchStateIfNeeded()
        viewModel.refreshNotificationIndicators()
        viewModel.events.collect { event ->
            if (event is DrawerEvent.AutoLaunch) closeAndResetAfterLaunch()
        }
    }

    AppDrawerContent(
            uiState = uiState,
            onSearchQueryChanged = viewModel::onSearchQueryChanged,
            onCategorySelected = viewModel::onCategorySelected,
            onAppClick = { target ->
                if (viewModel.launchTarget(target)) {
                    closeAndResetAfterLaunch()
                }
            },
            onSettingsClick = {
                viewModel.dismissMenu()
                onSettingsClick()
            },
            modifier = modifier,
            onSearchImeAction = {
                if (uiState.searchQuery.isNotBlank()) {
                    if (viewModel.executeWebSearch()) {
                        closeAndResetAfterLaunch()
                    }
                } else {
                    if (viewModel.tryLaunchFirstSearchResult()) closeAndResetAfterLaunch()
                }
            },
            useSidebarCategoryDrawer = uiState.useSidebarCategoryDrawer,
            drawerCategorySidebarOnRight = uiState.drawerCategorySidebarOnRight,
            categoryDrawerIconOverrides = uiState.categoryDrawerIconOverrides,
            onCategoryLongPress = viewModel::onCategoryLongPress,
            onAppLongPress = viewModel::onAppLongPress,
            onMenuToggle = viewModel::toggleMenu,
            onMenuDismiss = viewModel::dismissMenu,
            onPrivateSpaceToggle = viewModel::togglePrivateSpace,
            onToggleDrawerReorderApps = viewModel::toggleDrawerReorderSession,
            onClose = closeAndReset,
            onReorderDrawerProfileSection = viewModel::reorderDrawerProfileSectionApps,
            onReorderPrivateDrawerApps = viewModel::reorderPrivateDrawerApps
    )

    // Action sheet on long-press
    uiState.selectedApp?.let { app ->
        AppActionSheet(
                app = app,
                categories = uiState.categories,
                onDismiss = viewModel::dismissActionSheet,
                onAddToHome = {
                    viewModel.addToHomeScreen(it)
                    closeAndReset()
                },
                onRename = { newName -> viewModel.renameApp(app, newName) },
                onSetCategory = { category -> viewModel.setAppCategory(app, category) },
                onHide = { viewModel.hideApp(it) },
                onAppInfo = viewModel::openAppInfo,
                onUninstall = viewModel::uninstallApp,
                onRemoveShortcut = { viewModel.removeLauncherShortcut(it) },
                isOnHomeScreen =
                        appMetadataKey(app) in uiState.favoriteAppKeys
        )
    }

    uiState.selectedCategoryForActions?.let { category ->
        CategoryActionSheet(
                category = category,
                categoryDrawerIconOverrides = uiState.categoryDrawerIconOverrides,
                onDismiss = viewModel::dismissCategoryActionSheet,
                onRename = { newName: String -> viewModel.renameCategory(category, newName) },
                onEditApps = {
                    viewModel.dismissCategoryActionSheet()
                    onEditCategoryApps(category)
                },
                onDelete = { viewModel.deleteCategory(category) },
                onSetCategoryIcon = { iconName ->
                    viewModel.setCategoryDrawerIcon(category, iconName)
                },
                onResetCategoryIcon = { viewModel.clearCategoryDrawerIcon(category) }
        )
    }
}

@Composable
fun AppDrawerContent(
        uiState: AppDrawerUiState,
        onSearchQueryChanged: (String) -> Unit,
        onCategorySelected: (String) -> Unit,
        onAppClick: (LaunchTarget) -> Unit,
        onSettingsClick: () -> Unit,
        modifier: Modifier = Modifier,
        onSearchImeAction: () -> Unit = {},
        useSidebarCategoryDrawer: Boolean = false,
        drawerCategorySidebarOnRight: Boolean = true,
        categoryDrawerIconOverrides: Map<String, String> = emptyMap(),
        onCategoryLongPress: (String) -> Unit = {},
        onAppLongPress: (AppInfo) -> Unit = {},
        onMenuToggle: () -> Unit = {},
        onMenuDismiss: () -> Unit = {},
        onPrivateSpaceToggle: () -> Unit = {},
        onToggleDrawerReorderApps: () -> Unit = {},
        onClose: () -> Unit = {},
        onReorderDrawerProfileSection: (sectionId: String, fromIndex: Int, toIndex: Int) -> Unit =
                { _, _, _ -> },
        onReorderPrivateDrawerApps: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> }
) {
    val drawerContext = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val latestCategories = rememberUpdatedState(uiState.categories)
    val latestSelectedCategory = rememberUpdatedState(uiState.selectedCategory)
    var showSearch by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.selectedCategory) { listState.scrollToItem(0) }

    var previousSearchQuery by remember { mutableStateOf("") }
    LaunchedEffect(uiState.searchQuery) {
        val prev = previousSearchQuery
        if (prev.isBlank() && uiState.searchQuery.isNotBlank()) {
            listState.scrollToItem(0, 0)
        }
        previousSearchQuery = uiState.searchQuery
    }

    LaunchedEffect(useSidebarCategoryDrawer) {
        if (!useSidebarCategoryDrawer) showSearch = false
    }

    val showProfileSections =
            !uiState.useSidebarCategoryDrawer ||
                    !uiState.selectedCategory.equals(ReservedCategoryNames.ALL_APPS, ignoreCase = true) ||
                    uiState.searchQuery.isNotBlank()
    val anyProfileAppsVisible =
            uiState.filteredProfileSections.any { it.apps.isNotEmpty() }
    val closeWithFocusReset: () -> Unit = {
        // Hide IME at close start so it animates with the drawer slide-out, not after.
        // clearFocus alone is async on some devices/OEMs and only drops the keyboard when
        // AnimatedVisibility tears down the still-composed search field.
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        onClose()
    }
    val selectCategoryWithFocusReset: (String) -> Unit = { category ->
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        if (useSidebarCategoryDrawer) showSearch = false
        onCategorySelected(category)
    }
    val latestOnCategorySelected = rememberUpdatedState(selectCategoryWithFocusReset)

    val searchFilterBlank =
            remember(uiState.searchQuery) {
                val trimmed = uiState.searchQuery.trimStart()
                val q =
                        if (DotSearchSyntax.isPossibleDotSearchPrefix(trimmed)) ""
                        else trimmed.trim()
                q.isBlank()
            }
    val showDrawerReorderMenuToggle =
            uiState.useSidebarCategoryDrawer &&
                    uiState.drawerAppSortMode == DrawerAppSortMode.CUSTOM
    val allowCustomDragReorder =
            useSidebarCategoryDrawer &&
                    uiState.drawerAppSortMode == DrawerAppSortMode.CUSTOM &&
                    uiState.drawerReorderSessionActive &&
                    searchFilterBlank

    val scope = rememberCoroutineScope()
    var activeLetter by remember { mutableStateOf<String?>(null) }
    var isHoldingIndex by remember { mutableStateOf(false) }
    var activeLetterJob by remember { mutableStateOf<Job?>(null) }
    val dynamicAlphabet = remember {
        listOf("#", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z")
    }
    val hideCharacterScroll = !uiState.drawerShowScrollbar

    val selectLetter: (String, Boolean) -> Unit = { letter, isDrag ->
        activeLetter = letter
        
        var currentIndex = 0
        var targetIndex = -1
        
        // Count Target Chips
        if (uiState.searchQuery.isNotBlank()) {
            currentIndex++
            if (uiState.quickActionResult != null) {
                currentIndex++
            }
        }
        
        if (showProfileSections) {
            var hasEmittedProfileListContent = false
            for (section in uiState.filteredProfileSections) {
                if (section.apps.isEmpty()) continue
                val showSectionLabel = section.id != "owner"
                if (showSectionLabel) {
                    if (hasEmittedProfileListContent) currentIndex++
                    currentIndex++
                }
                hasEmittedProfileListContent = true
                for (app in section.apps) {
                    val appName = app.label
                    val startsWithLetter = if (letter == "#") !appName.firstOrNull()?.isLetter()!! else appName.startsWith(letter, ignoreCase = true)
                    if (targetIndex == -1 && startsWithLetter) {
                        targetIndex = currentIndex
                    }
                    currentIndex++
                }
            }
        }
        if (uiState.isPrivateSpaceUnlocked && uiState.filteredPrivateSpaceApps.isNotEmpty()) {
            if (showProfileSections && anyProfileAppsVisible) {
                currentIndex++
            }
            currentIndex++
            for (app in uiState.filteredPrivateSpaceApps) {
                val appName = app.label
                val startsWithLetter = if (letter == "#") !appName.firstOrNull()?.isLetter()!! else appName.startsWith(letter, ignoreCase = true)
                if (targetIndex == -1 && startsWithLetter) {
                        targetIndex = currentIndex
                }
                currentIndex++
            }
        }
        if (targetIndex != -1) {
            scope.launch { listState.scrollToItem(targetIndex) }
        }
        
        if (!isDrag) {
            activeLetterJob?.cancel()
            activeLetterJob = scope.launch {
                delay(500)
                activeLetter = null
            }
        }
    }
    val holdingScaleMultiplier = if (isHoldingIndex) 1.5f else 1.0f

    BackHandler { closeWithFocusReset() }

    val isAtTop by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 }
    }
    var hasScrolledDown by remember { mutableStateOf(false) }
    // Tracks prior at-top so sidebar search auto-close only runs on scroll-away, not on icon reopen.
    var wasAtTop by remember { mutableStateOf(true) }
    // True only after the user drags the list. Layout/IME/filter relayout can briefly report
    // !isAtTop (e.g. backspacing the last search character) — that must not dismiss the keyboard.
    var userDraggedList by remember { mutableStateOf(false) }

    // Sidebar icon search: focus when opened (independent of list scroll position).
    LaunchedEffect(showSearch, useSidebarCategoryDrawer) {
        if (useSidebarCategoryDrawer && showSearch) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    // Chip layout: auto-focus at top. Keyboard dismiss-on-scroll is handled in nestedScroll.
    // Sidebar: hide empty search only on scroll-away (not when reopening while already scrolled).
    LaunchedEffect(isAtTop, useSidebarCategoryDrawer, uiState.drawerScrollToTopAutoKeyboard) {
        if (isAtTop) {
            userDraggedList = false
        }
        if (useSidebarCategoryDrawer) {
            if (!isAtTop) {
                hasScrolledDown = true
                if (wasAtTop && showSearch && uiState.searchQuery.isBlank() && userDraggedList) {
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true)
                    showSearch = false
                }
            }
            wasAtTop = isAtTop
            return@LaunchedEffect
        }
        val topAutoLaunch = isAtTop && uiState.drawerScrollToTopAutoKeyboard
        if (topAutoLaunch) {
            focusRequester.requestFocus()
            keyboardController?.show()
        } else if (!isAtTop) {
            hasScrolledDown = true
        }
    }

    var overscrollY by remember { mutableFloatStateOf(0f) }
    val latestUseSidebar = rememberUpdatedState(useSidebarCategoryDrawer)
    val latestShowSearch = rememberUpdatedState(showSearch)
    val latestScrollToTopAutoKeyboard =
            rememberUpdatedState(uiState.drawerScrollToTopAutoKeyboard)
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y < 0) {
                    // Finger scrolling the list away from the top.
                    userDraggedList = true
                    // Dismiss keyboard as soon as the user scrolls down while search is open
                    // (chip mode always; sidebar only when the search field is shown).
                    val searchOpen = !latestUseSidebar.value || latestShowSearch.value
                    if (searchOpen) {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                    }
                }
                if (source == NestedScrollSource.UserInput && available.y > 0 && !listState.canScrollBackward) {
                    // Immediate response for pull-down gesture at the boundary
                    if (latestScrollToTopAutoKeyboard.value && !latestUseSidebar.value) {
                        // LaunchedEffect(isAtTop) handles focus when scrolling to top; re-request
                        // if already at top and pulling down at the list boundary.
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }
                    overscrollY += available.y
                    if (overscrollY > 300f) {
                        overscrollY = 0f
                        closeWithFocusReset()
                        return available
                    }
                } else {
                    overscrollY = 0f
                }
                return Offset.Zero
            }
        }
    }
    val contentTopPadding =
            maxOf(
                    DRAWER_MIN_TOP_PADDING,
                    with(density) {
                        (ViewCompat.getRootWindowInsets(view)
                                        ?.getInsets(
                                                WindowInsetsCompat.Type.statusBars() or
                                                        WindowInsetsCompat.Type.displayCutout()
                                        )
                                        ?.top ?: 0)
                                .toDp()
                    } +
                            DRAWER_TOP_INSET_BUFFER
            )

    val hasNonAllAppsCategory =
            uiState.categories.any {
                !it.equals(ReservedCategoryNames.ALL_APPS, ignoreCase = true)
            }
    val categorySwipeModifier =
            if (hasNonAllAppsCategory) {
                Modifier.pointerInput(Unit) {
                    var accumulated = 0f
                    detectHorizontalDragGestures(
                            onDragStart = { accumulated = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                accumulated += dragAmount
                                change.consume()
                            },
                            onDragEnd = {
                                val categories = latestCategories.value
                                if (categories.size <= 1) return@detectHorizontalDragGestures
                                val selected = latestSelectedCategory.value
                                val idx =
                                        categories.indexOfFirst {
                                            it.equals(selected, ignoreCase = true)
                                        }
                                if (idx < 0) return@detectHorizontalDragGestures
                                when {
                                    accumulated <= -DRAWER_CATEGORY_SWIPE_THRESHOLD_PX &&
                                            idx < categories.lastIndex ->
                                            latestOnCategorySelected.value(categories[idx + 1])
                                    accumulated >= DRAWER_CATEGORY_SWIPE_THRESHOLD_PX && idx > 0 ->
                                            latestOnCategorySelected.value(categories[idx - 1])
                                }
                            },
                            onDragCancel = { accumulated = 0f }
                    )
                }
            } else {
                Modifier
            }

    val overflowMenu: @Composable () -> Unit = {
        DrawerOverflowMenu(
                uiState = uiState,
                onMenuToggle = onMenuToggle,
                onMenuDismiss = onMenuDismiss,
                onPrivateSpaceToggle = onPrivateSpaceToggle,
                onSettingsClick = onSettingsClick,
                showReorderMenuItem = showDrawerReorderMenuToggle,
                onToggleReorderApps = onToggleDrawerReorderApps,
        )
    }
    val drawerAppList: @Composable ColumnScope.() -> Unit = {
        DrawerAppListBody(
                listState = listState,
                categorySwipeModifier = categorySwipeModifier,
                uiState = uiState,
                showProfileSections = showProfileSections,
                anyProfileAppsVisible = anyProfileAppsVisible,
                focusManager = focusManager,
                onAppClick = onAppClick,
                onAppLongPress = onAppLongPress,
                allowCustomDragReorder = allowCustomDragReorder,
                activeLetter = activeLetter,
                onReorderDrawerProfileSection = onReorderDrawerProfileSection,
                onReorderPrivateDrawerApps = onReorderPrivateDrawerApps,
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(top = contentTopPadding)
                                .navigationBarsPadding()
                                .testTag("app_drawer_screen")
        ) {
            if (useSidebarCategoryDrawer && uiState.categories.size > 1) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val sidebar: @Composable () -> Unit = {
                        DrawerCategorySidebar(
                                categories = uiState.categories,
                                selectedCategory = uiState.selectedCategory,
                                onCategorySelected = selectCategoryWithFocusReset,
                                onCategoryLongPress = onCategoryLongPress,
                                sidebarOnLeft = !drawerCategorySidebarOnRight,
                                categoryIconOverrides = categoryDrawerIconOverrides,
                                modifier = Modifier.weight(18f).fillMaxHeight()
                        )
                    }
                    val body: @Composable () -> Unit = {
                        Column(
                                modifier =
                                        Modifier.weight(82f)
                                                .fillMaxHeight()
                                                .nestedScroll(nestedScrollConnection)
                        ) {
                            Row(
                                    modifier =
                                            Modifier.fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                        text =
                                                categoryChipDisplayLabel(
                                                        drawerContext,
                                                        uiState.selectedCategory
                                                ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.weight(1f)
                                )
                                FokusIconButton(
                                        onClick = {
                                            if (showSearch) {
                                                showSearch = false
                                                if (uiState.searchQuery.isNotEmpty()) {
                                                    onSearchQueryChanged("")
                                                }
                                                keyboardController?.hide()
                                                focusManager.clearFocus(force = true)
                                            } else {
                                                showSearch = true
                                            }
                                        },
                                        modifier = Modifier.testTag("drawer_search_icon")
                                ) {
                                    LauncherIcon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription =
                                                    stringResource(R.string.search_apps),
                                            tint = MaterialTheme.colorScheme.onBackground,
                                            iconSize = 24.dp,
                                    )
                                }
                                overflowMenu()
                            }
                            if (showSearch) {
                                OutlinedTextField(
                                        value = uiState.searchQuery,
                                        onValueChange = onSearchQueryChanged,
                                        singleLine = true,
                                        keyboardOptions =
                                                KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions =
                                                KeyboardActions(onSearch = { onSearchImeAction() }),
                                        textStyle = MaterialTheme.typography.bodyLarge,
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .padding(horizontal = 16.dp)
                                                        .focusRequester(focusRequester)
                                                        .testTag("search_bar")
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            drawerAppList()
                        }
                    }
                    if (drawerCategorySidebarOnRight) {
                        body()
                        sidebar()
                    } else {
                        sidebar()
                        body()
                    }
                }
            } else {
                Column(
                        modifier =
                                Modifier.fillMaxSize().nestedScroll(nestedScrollConnection)
                ) {
                    Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        SearchBar(
                                query = uiState.searchQuery,
                                onQueryChange = onSearchQueryChanged,
                                focusRequester = focusRequester,
                                onImeAction = onSearchImeAction,
                                modifier =
                                        Modifier.weight(1f).testTag("search_bar")
                        )
                        overflowMenu()
                    }
                    if (hasNonAllAppsCategory) {
                        CategoryChips(
                                categories = uiState.categories,
                                selectedCategory = uiState.selectedCategory,
                                onCategorySelected = selectCategoryWithFocusReset,
                                onCategoryLongPress = onCategoryLongPress,
                                translucent = uiState.usesPhotoWallpaper,
                                modifier =
                                        Modifier.padding(top = DRAWER_CATEGORY_CHIPS_TOP_OFFSET)
                                                .testTag("category_chips")
                        )
                    }
                    drawerAppList()
                }
            }
        }
        
        // Curved A-Z sidebar index on the right with floating letter preview indicator
        var indexHeight by remember { mutableFloatStateOf(0f) }
        val activeIdx = activeLetter?.let { dynamicAlphabet.indexOf(it) } ?: -1
        if (!hideCharacterScroll && uiState.searchQuery.isEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(top = 116.dp, bottom = 16.dp)
                    .fillMaxHeight()
            ) {
                // Floating preview badge shown when dragging / holding index
                if (isHoldingIndex && activeLetter != null) {
                    val badgeYRatio = if (dynamicAlphabet.size > 1 && activeIdx >= 0) {
                        activeIdx.toFloat() / (dynamicAlphabet.size - 1)
                    } else 0.5f

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 80.dp)
                            .graphicsLayer {
                                translationY = badgeYRatio * indexHeight - 24.dp.toPx()
                            }
                            .background(
                                color = Color(0xFF222222),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = activeLetter ?: "",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(64.dp)
                        .onSizeChanged { size ->
                            indexHeight = size.height.toFloat()
                        }
                        .pointerInput(dynamicAlphabet) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    isHoldingIndex = true
                                    if (indexHeight > 0) {
                                        val percentage = (offset.y / indexHeight).coerceIn(0f, 1f)
                                        val targetIdx = (percentage * (dynamicAlphabet.size - 1)).toInt()
                                        dynamicAlphabet.getOrNull(targetIdx)?.let { letter ->
                                            selectLetter(letter, true)
                                        }
                                    }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    if (indexHeight > 0) {
                                        val percentage = (change.position.y / indexHeight).coerceIn(0f, 1f)
                                        val targetIdx = (percentage * (dynamicAlphabet.size - 1)).toInt()
                                        dynamicAlphabet.getOrNull(targetIdx)?.let { letter ->
                                            selectLetter(letter, true)
                                        }
                                    }
                                },
                                onDragEnd = {
                                    isHoldingIndex = false
                                    activeLetterJob?.cancel()
                                    activeLetter = null
                                },
                                onDragCancel = {
                                    isHoldingIndex = false
                                    activeLetterJob?.cancel()
                                    activeLetter = null
                                }
                            )
                        }
                        .pointerInput(dynamicAlphabet) {
                            detectTapGestures(
                                onPress = { offset ->
                                    isHoldingIndex = true
                                    if (indexHeight > 0) {
                                        val percentage = (offset.y / indexHeight).coerceIn(0f, 1f)
                                        val targetIdx = (percentage * (dynamicAlphabet.size - 1)).toInt()
                                        dynamicAlphabet.getOrNull(targetIdx)?.let { letter ->
                                            selectLetter(letter, false)
                                        }
                                    }
                                    try {
                                        awaitRelease()
                                    } finally {
                                        isHoldingIndex = false
                                        activeLetterJob?.cancel()
                                        activeLetter = null
                                    }
                                }
                            )
                        },
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    dynamicAlphabet.forEachIndexed { i, letter ->
                        Text(
                            text = letter,
                            style = TextStyle(
                                color = if (activeLetter == letter) Color.White else Color.LightGray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (activeLetter == letter) FontWeight.Bold else FontWeight.Medium
                            ),
                            modifier = Modifier
                                .graphicsLayer {
                                    val activeIndex = activeIdx
                                    val dist = if (activeIndex != -1) kotlin.math.abs(i - activeIndex).toFloat() else 0f
                                    if (activeIndex != -1) {
                                        val sigma = 3.2f
                                        val factor = kotlin.math.exp(- (dist * dist) / (2f * sigma * sigma)).toFloat()

                                        val tX = factor * -110f
                                        val holdingBonus = if (i == activeIndex) (holdingScaleMultiplier - 1f) else 0f
                                        val scale = 1.0f + (factor * 0.35f) + holdingBonus

                                        val aVal = 0.4f + factor * 0.6f

                                        val dispersionMaxDp = 20f
                                        val dispersionFactor = (dist / 6f).coerceIn(0f, 1f) * kotlin.math.exp(- (dist * dist) / (2f * sigma * sigma)).toFloat()
                                        val yOffsetDp = if (i < activeIndex) {
                                            -dispersionFactor * dispersionMaxDp
                                        } else if (i > activeIndex) {
                                            dispersionFactor * dispersionMaxDp
                                        } else {
                                            0f
                                        }

                                        translationX = tX.dp.toPx()
                                        translationY = yOffsetDp.dp.toPx()
                                        scaleX = scale
                                        scaleY = scale
                                        alpha = aVal
                                    } else {
                                        translationX = 0f
                                        translationY = 0f
                                        scaleX = 1f
                                        scaleY = 1f
                                        alpha = 0.5f
                                    }
                                }
                                .padding(vertical = 1.dp)
                                .padding(start = 4.dp, end = 24.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryActionSheet(
        category: String,
        categoryDrawerIconOverrides: Map<String, String> = emptyMap(),
        onDismiss: () -> Unit,
        onRename: (String) -> Unit,
        onEditApps: () -> Unit,
        onDelete: () -> Unit,
        onSetCategoryIcon: (String) -> Unit,
        onResetCategoryIcon: () -> Unit
) {
    val context = LocalContext.current
    var showIconPickerDialog by remember(category) { mutableStateOf(false) }
    var renameMode by remember(category) { mutableStateOf(false) }
    var renameValue by remember(category) {
        mutableStateOf(categoryChipDisplayLabel(context, category))
    }
    val normalized = renameValue.trim()
    val isReservedDrawerCategory =
            ReservedDrawerActionCategories.any { category.equals(it, ignoreCase = true) }
    val canSaveRename = !isReservedDrawerCategory && normalized.isNotBlank()
    val showEditApps = !isReservedDrawerCategory
    val displayTitle = categoryChipDisplayLabel(context, category)
    val drawerRailIconKey =
            resolvedCategoryDrawerIconName(context, category, categoryDrawerIconOverrides)

    FokusBottomSheet(
            onDismissRequest = onDismiss,
            modifier = Modifier.testTag("category_action_sheet"),
    ) {
            SheetInlineRenameTitleRow(
                    renameMode = renameMode,
                    renameValue = renameValue,
                    onRenameValueChange = { renameValue = it },
                    idleTitle = displayTitle,
                    placeholder = { Text(stringResource(R.string.category_name_label)) },
                    onStartRename = { renameMode = true },
                    onCancelRename = { renameMode = false },
                    onSave = {
                        onRename(normalized)
                        onDismiss()
                    },
                    saveEnabled = canSaveRename,
                    showEditButton = !isReservedDrawerCategory,
                    editIconContentDescription = stringResource(R.string.category_action_rename),
                    textFieldTestTag = "category_rename_inline_input",
                    editButtonTestTag = "category_action_rename",
            )

            if (showEditApps) {
                SheetActionRow(
                        label = stringResource(R.string.category_apps_screen_section_in_category),
                        onClick = onEditApps,
                        icon = Icons.Outlined.Edit,
                        iconContentDescription = stringResource(R.string.category_action_edit_apps),
                        testTag = "category_action_edit_apps",
                )
            }

            SheetActionRow(
                    label = stringResource(R.string.category_icon_picker_title),
                    onClick = { showIconPickerDialog = true },
                    testTag = "category_action_choose_icon",
                    leadingContent = {
                        LauncherIcon(
                                imageVector = MinimalIcons.iconFor(drawerRailIconKey),
                                contentDescription =
                                        stringResource(R.string.icon_picker_current_icon),
                                iconSize = 28.dp,
                                tint = MaterialTheme.colorScheme.onBackground,
                        )
                    },
                    labelModifier = Modifier.weight(1f),
            )
            SheetActionRow(
                    label = stringResource(R.string.category_action_reset_icon),
                    onClick = onResetCategoryIcon,
                    icon = Icons.Default.Restore,
                    testTag = "category_action_reset_icon",
                    destructive = true,
            )
            if (showEditApps) {
                SheetActionRow(
                        label = stringResource(R.string.category_action_remove),
                        onClick = onDelete,
                        icon = Icons.Default.Delete,
                        testTag = "category_action_remove",
                        destructive = true,
                )
            }
    }

    if (showIconPickerDialog) {
        CategoryIconPickerDialog(
                category = category,
                iconOverrides = categoryDrawerIconOverrides,
                onSelect = { name ->
                    onSetCategoryIcon(name)
                    showIconPickerDialog = false
                },
                onDismiss = {
                    showIconPickerDialog = false
                },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListItem(
        app: AppInfo,
        onClick: () -> Unit,
        onLongClick: () -> Unit,
        modifier: Modifier = Modifier,
        hasNotification: Boolean = false,
        notificationIndicatorStyle: NotificationIndicatorStyle = NotificationIndicatorStyle.DOT,
        notificationIndicatorColor: Int = NotificationIndicatorColorPreset.DEFAULT.argb,
        reserveNotificationDotSlot: Boolean = false,
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    val indicatorColor = Color(notificationIndicatorColor)
    val labelColor =
            if (hasNotification &&
                            notificationIndicatorStyle == NotificationIndicatorStyle.COLORED_LABEL
            ) {
                indicatorColor
            } else {
                textColor
            }
    val showDot =
            hasNotification && notificationIndicatorStyle == NotificationIndicatorStyle.DOT
    Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                    modifier.fillMaxWidth()
                            .combinedClickableWithSystemSound(
                                    onClick = onClick,
                                    onLongClick = onLongClick
                            )
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                            .testTag("app_item_${app.packageName}"),
    ) {
        if (reserveNotificationDotSlot || showDot) {
            Box(
                    modifier =
                            Modifier.size(8.dp)
                                    .background(
                                            color = if (showDot) textColor else Color.Transparent,
                                            shape = CircleShape,
                                    ),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge,
                color = labelColor,
        )
    }
}
