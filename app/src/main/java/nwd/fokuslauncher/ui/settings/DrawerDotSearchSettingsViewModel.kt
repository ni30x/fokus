package nwd.fokuslauncher.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import nwd.fokuslauncher.data.local.PreferencesManager
import nwd.fokuslauncher.data.model.AppInfo
import nwd.fokuslauncher.data.model.AppShortcutAction
import nwd.fokuslauncher.data.model.DotSearchTargetMode
import nwd.fokuslauncher.data.model.DotSearchTargetPreference
import nwd.fokuslauncher.data.model.ShortcutTarget
import nwd.fokuslauncher.data.model.appProfileKey
import nwd.fokuslauncher.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DrawerDotSearchSettingsUiState(
        val defaultTarget: DotSearchTargetPreference = DotSearchTargetPreference(),
        val aliases: Map<Char, DotSearchTargetPreference> = emptyMap(),
        val allApps: List<AppInfo> = emptyList(),
        val allShortcutActions: List<AppShortcutAction> = emptyList(),
        /** Apps that resolve [android.content.Intent.ACTION_WEB_SEARCH] or [android.content.Intent.ACTION_SEARCH] for their package. */
        val webSearchCapableApps: List<AppInfo> = emptyList(),
        val profileDisplayNameOverrides: Map<String, String> = emptyMap(),
)

@HiltViewModel
class DrawerDotSearchSettingsViewModel
@Inject
constructor(
        private val appRepository: AppRepository,
        private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DrawerDotSearchSettingsUiState())
    val uiState: StateFlow<DrawerDotSearchSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                            preferencesManager.drawerDotSearchDefaultFlow,
                            preferencesManager.drawerDotSearchAliasesFlow,
                            preferencesManager.profileDisplayNameOverridesFlow,
                            appRepository.getInstalledAppsVersion()
                    ) { defaultTarget, aliases, profileNames, _ ->
                        Triple(defaultTarget, aliases, profileNames)
                    }
                    .collectLatest { (defaultTarget, aliases, profileNames) ->
                        val apps = appRepository.getInstalledAppsOnBackground()
                        val shortcuts = appRepository.getAllShortcutActionsOnBackground()
                        val webSearchApps =
                                withContext(Dispatchers.IO) {
                                    appRepository.filterAppsForDotSearchAppPicker(apps)
                                }
                        _uiState.value =
                                DrawerDotSearchSettingsUiState(
                                        defaultTarget = defaultTarget,
                                        aliases = aliases,
                                        allApps = apps,
                                        allShortcutActions = shortcuts,
                                        webSearchCapableApps = webSearchApps,
                                        profileDisplayNameOverrides = profileNames,
                                )
                    }
        }
    }

    fun setDefaultFromApp(app: AppInfo) {
        viewModelScope.launch {
            preferencesManager.setDrawerDotSearchDefault(
                    DotSearchTargetPreference(
                            profileKey = appProfileKey(app.userHandle),
                            target = ShortcutTarget.App(app.packageName)
                    )
            )
        }
    }

    fun setDefaultFromUrlTemplate(template: String) {
        val t = template.trim()
        require(isValidDotSearchUrlTemplate(t)) { "URL template must contain %q" }
        viewModelScope.launch {
            preferencesManager.setDrawerDotSearchDefault(
                    DotSearchTargetPreference(
                            profileKey = "0",
                            target = ShortcutTarget.DeepLink(t)
                    )
            )
        }
    }

    fun clearDefaultTarget() {
        viewModelScope.launch { preferencesManager.clearDrawerDotSearchDefault() }
    }

    fun setAlias(alias: Char, action: AppShortcutAction) {
        val key = alias.lowercaseChar()
        require(isValidAliasChar(key)) { "invalid alias" }
        viewModelScope.launch {
            preferencesManager.setDrawerDotSearchAlias(
                    key,
                    DotSearchTargetPreference(
                            profileKey = action.profileKey,
                            target = action.target,
                            mode = DotSearchTargetMode.SHORTCUT,
                    )
            )
        }
    }

    fun setAliasFromSearchApp(alias: Char, app: AppInfo) {
        val key = alias.lowercaseChar()
        require(isValidAliasChar(key)) { "invalid alias" }
        viewModelScope.launch {
            preferencesManager.setDrawerDotSearchAlias(
                    key,
                    DotSearchTargetPreference(
                            profileKey = appProfileKey(app.userHandle),
                            target = ShortcutTarget.App(app.packageName),
                            mode = DotSearchTargetMode.SEARCH,
                    )
            )
        }
    }

    fun setAliasFromUrlTemplate(alias: Char, template: String) {
        val key = alias.lowercaseChar()
        require(isValidAliasChar(key)) { "invalid alias" }
        val t = template.trim()
        require(isValidDotSearchUrlTemplate(t)) { "URL template must contain %q" }
        viewModelScope.launch {
            preferencesManager.setDrawerDotSearchAlias(
                    key,
                    DotSearchTargetPreference(
                            profileKey = "0",
                            target = ShortcutTarget.DeepLink(t),
                            mode = DotSearchTargetMode.SEARCH,
                    )
            )
        }
    }

    fun removeAlias(alias: Char) {
        viewModelScope.launch { preferencesManager.removeDrawerDotSearchAlias(alias) }
    }

    fun aliasCharTaken(alias: Char): Boolean =
            alias.lowercaseChar() in _uiState.value.aliases

    companion object {
        fun isValidAliasChar(c: Char): Boolean = c in 'a'..'z'

        fun isValidDotSearchUrlTemplate(template: String): Boolean {
            val t = template.trim()
            return t.isNotEmpty() && t.contains("%q")
        }
    }
}
