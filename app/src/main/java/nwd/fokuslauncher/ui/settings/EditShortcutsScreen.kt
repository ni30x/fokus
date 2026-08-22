package nwd.fokuslauncher.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import nwd.fokuslauncher.ui.components.LauncherIcon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nwd.fokuslauncher.R
import nwd.fokuslauncher.data.model.AppInfo
import nwd.fokuslauncher.data.model.AppShortcutAction
import nwd.fokuslauncher.data.model.HomeShortcut
import nwd.fokuslauncher.data.model.ShortcutTarget
import nwd.fokuslauncher.data.model.stableSelectionKey
import nwd.fokuslauncher.ui.components.EditorScreenScaffold
import nwd.fokuslauncher.ui.components.MinimalIconPickerDialog
import nwd.fokuslauncher.ui.components.MinimalIcons
import nwd.fokuslauncher.ui.drawer.DrawerProfileShortcutSectionUi
import nwd.fokuslauncher.ui.drawer.groupShortcutActionsIntoProfileSections
import nwd.fokuslauncher.ui.drawer.profileGroupedShortcutItems
import nwd.fokuslauncher.ui.drawer.profileOriginLabelForHomeShortcut
import nwd.fokuslauncher.ui.home.HomeViewModel
import nwd.fokuslauncher.ui.settings.components.EditorDragHandleReorderIcon
import nwd.fokuslauncher.ui.settings.components.EditorSectionHeader
import nwd.fokuslauncher.ui.settings.components.EditorStandardCheckboxGutter
import nwd.fokuslauncher.ui.settings.components.EditorUncheckedLeadingSpacers
import nwd.fokuslauncher.ui.settings.components.ProfileBadgeSubtitle
import nwd.fokuslauncher.ui.theme.FokusBackdrop
import nwd.fokuslauncher.ui.util.clickableWithSystemSound
import nwd.fokuslauncher.ui.util.rememberBooleanChangeWithSystemSound
import nwd.fokuslauncher.ui.util.rememberVerticalSlotReorderState
import nwd.fokuslauncher.utils.containsNormalizedSearch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditShortcutsScreen(
        viewModel: HomeViewModel,
        onNavigateBack: () -> Unit,
        backgroundScrim: Color = FokusBackdrop.ScrimColorWithoutBlur
) {
    val context = LocalContext.current
    val editShortcuts by viewModel.editRightShortcuts.collectAsStateWithLifecycle()
    val allActions by viewModel.allShortcutActions.collectAsStateWithLifecycle()
    val allApps by viewModel.allInstalledApps.collectAsStateWithLifecycle()
    val profileDisplayNameOverrides by viewModel.profileDisplayNameOverrides.collectAsStateWithLifecycle()
    val iconPickerForIndex = remember { mutableStateOf<Int?>(null) }

    val saveAndBack: () -> Unit = {
        viewModel.saveEditedRightShortcuts()
        onNavigateBack()
    }

    EditorScreenScaffold(
            titleText = stringResource(R.string.edit_shortcuts_title),
            searchPlaceholderResId = R.string.search_apps_and_actions,
            backgroundScrim = backgroundScrim,
            listReadyToScroll = allActions.isNotEmpty(),
            onNavigateBack = saveAndBack,
            onDone = saveAndBack,
    ) { searchQuery, listState ->
        val selectedIds =
                remember(editShortcuts) { editShortcuts.map { it.stableSelectionKey() }.toSet() }
        val uncheckedActions =
                remember(allActions, selectedIds, searchQuery) {
                    allActions
                            .filter { it.id !in selectedIds }
                            .let { list ->
                                if (searchQuery.isBlank()) list
                                else list.filter { it.displayLabel.containsNormalizedSearch(searchQuery) }
                            }
                }
        val uncheckedShortcutSections =
                remember(uncheckedActions, allApps, context, profileDisplayNameOverrides) {
                    groupShortcutActionsIntoProfileSections(
                            context,
                            uncheckedActions,
                            allApps,
                            profileDisplayNameOverrides,
                    )
                }

        ReorderableShortcutList(
                listState = listState,
                editShortcuts = editShortcuts,
                allApps = allApps,
                uncheckedShortcutSections = uncheckedShortcutSections,
                profileDisplayNameOverrides = profileDisplayNameOverrides,
                onToggleChecked = { shortcut ->
                    viewModel.toggleRightShortcut(
                            AppShortcutAction(
                                    appLabel =
                                            viewModel.formatShortcutTarget(
                                                    shortcut.target,
                                                    shortcut.profileKey
                                            ),
                                    actionLabel = AppShortcutAction.OPEN_APP_LABEL,
                                    target = shortcut.target,
                                    profileKey = shortcut.profileKey,
                            )
                    )
                },
                onToggleUnchecked = { action -> viewModel.toggleRightShortcut(action) },
                onReorder = { from, to -> viewModel.reorderRightShortcut(from, to) },
                onOpenIconPicker = { index -> iconPickerForIndex.value = index },
                formatCheckedLabel = { shortcut ->
                    viewModel.formatShortcutTarget(shortcut.target, shortcut.profileKey)
                }
        )
    }

    iconPickerForIndex.value?.let { pickerIndex ->
        MinimalIconPickerDialog(
                storedIconKey = editShortcuts.getOrNull(pickerIndex)?.iconName ?: "circle",
                title = {
                    Text(
                            stringResource(R.string.edit_shortcuts_choose_icon),
                            color = MaterialTheme.colorScheme.onBackground
                    )
                },
                onSelect = { name ->
                    viewModel.updateShortcutIcon(pickerIndex, name)
                    iconPickerForIndex.value = null
                },
                onDismiss = { iconPickerForIndex.value = null }
        )
    }
}

@Composable
private fun ReorderableShortcutList(
        listState: LazyListState,
        editShortcuts: List<HomeShortcut>,
        allApps: List<AppInfo>,
        uncheckedShortcutSections: List<DrawerProfileShortcutSectionUi>,
        profileDisplayNameOverrides: Map<String, String>,
        onToggleChecked: (HomeShortcut) -> Unit,
        onToggleUnchecked: (AppShortcutAction) -> Unit,
        onReorder: (Int, Int) -> Unit,
        onOpenIconPicker: (Int) -> Unit,
        formatCheckedLabel: (HomeShortcut) -> String
) {
    val reorderState = rememberVerticalSlotReorderState()
    val openAppLabel = stringResource(R.string.shortcut_open_app)
    val openDialerLabel = stringResource(R.string.shortcut_open_dialer)
    val context = LocalContext.current

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        if (editShortcuts.isNotEmpty()) {
            item(key = "header_checked_shortcuts") {
                EditorSectionHeader(R.string.edit_shortcuts_section_selected)
            }
        }

        items(
                count = editShortcuts.size,
                key = { "checked_shortcut_${editShortcuts[it].stableSelectionKey()}" }
        ) { index ->
            val shortcut = editShortcuts[index]
            val profileBadge =
                    remember(shortcut, allApps, profileDisplayNameOverrides, context) {
                        profileOriginLabelForHomeShortcut(
                                context,
                                shortcut,
                                allApps,
                                profileDisplayNameOverrides,
                        )
                    }
            Row(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .heightIn(min = 56.dp)
                                    .graphicsLayer { translationY = reorderState.translationYForIndex(index) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                EditorDragHandleReorderIcon(
                        reorderState = reorderState,
                        index = index,
                        lastIndex = editShortcuts.lastIndex,
                        onReorder = onReorder,
                        onReset = { reorderState.reset() },
                        shortcut.target,
                        shortcut.profileKey,
                        editShortcuts.size,
                )
                EditorStandardCheckboxGutter(
                        checked = true,
                        onCheckedChange =
                                rememberBooleanChangeWithSystemSound { _ -> onToggleChecked(shortcut) },
                ) {
                    LauncherIcon(
                            imageVector = MinimalIcons.iconFor(shortcut.iconName),
                            contentDescription = stringResource(R.string.cd_change_icon),
                            tint = MaterialTheme.colorScheme.onBackground,
                            iconSize = 24.dp,
                            modifier =
                                    Modifier.clickableWithSystemSound { onOpenIconPicker(index) },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = formatCheckedLabel(shortcut),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                        )
                        ProfileBadgeSubtitle(profileBadge)
                    }
                }
            }
        }

        item(key = "header_unchecked_shortcuts") {
            EditorSectionHeader(R.string.edit_shortcuts_section_all_actions)
        }

        profileGroupedShortcutItems(
                sections = uncheckedShortcutSections,
                keyPrefix = "unchecked_shortcut",
                horizontalPadding = 16.dp,
        ) { action ->
            val primaryText =
                    when {
                        action.target is ShortcutTarget.PhoneDial -> openDialerLabel
                        action.actionLabel == AppShortcutAction.OPEN_APP_LABEL -> openAppLabel
                        else -> action.actionLabel
                    }
            Row(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .heightIn(min = 56.dp)
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                EditorUncheckedLeadingSpacers()
                EditorStandardCheckboxGutter(
                        checked = false,
                        onCheckedChange =
                                rememberBooleanChangeWithSystemSound { _ -> onToggleUnchecked(action) },
                ) {
                    Text(
                            text = primaryText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
