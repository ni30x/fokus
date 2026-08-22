package nwd.fokuslauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nwd.fokuslauncher.R
import nwd.fokuslauncher.data.model.appProfileKey
import nwd.fokuslauncher.ui.components.FokusTextButton
import nwd.fokuslauncher.ui.drawer.groupAppsIntoProfileSections
import nwd.fokuslauncher.ui.drawer.sortAppsAlphabeticallyByProfileSection
import nwd.fokuslauncher.ui.theme.FokusBackdrop
import nwd.fokuslauncher.data.local.PreferencesManager

private data class ProfileNameSettingRow(
        val profileKey: String,
        val defaultTitle: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileNamesSettingsScreen(
        viewModel: SettingsViewModel = hiltViewModel(),
        onNavigateBack: () -> Unit,
        backgroundScrim: Color = FokusBackdrop.ScrimColorWithoutBlur
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val rows =
            remember(uiState.allApps, context) {
                val sections =
                        groupAppsIntoProfileSections(
                                context,
                                uiState.allApps,
                                ::sortAppsAlphabeticallyByProfileSection,
                                emptyMap(),
                        )
                sections.map { section ->
                    val pk =
                            section.apps.firstOrNull()?.let { appProfileKey(it.userHandle) } ?: "0"
                    ProfileNameSettingRow(profileKey = pk, defaultTitle = section.title)
                }
            }

    Column(
            modifier =
                    Modifier.fillMaxSize()
                            .background(backgroundScrim)
                            .navigationBarsPadding()
    ) {
        FokusSettingsTopBar(
                titleText = stringResource(R.string.settings_profile_names_title),
                onNavigateBack = onNavigateBack,
                containerColor = MaterialTheme.colorScheme.background,
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item(key = "profile_names_help") {
                Text(
                        text = stringResource(R.string.settings_profile_names_help),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            items(rows, key = { it.profileKey }) { row ->
                ProfileNameRowEditor(
                        row = row,
                        storedOverride = uiState.profileDisplayNameOverrides[row.profileKey],
                        onSave = { viewModel.setProfileDisplayName(row.profileKey, it) },
                )
            }
            item(key = "profile_names_bottom_spacer") { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ProfileNameRowEditor(
        row: ProfileNameSettingRow,
        storedOverride: String?,
        onSave: (String) -> Unit,
) {
    val storedTrimmed = storedOverride?.trim().orEmpty()
    var draft by remember(row.profileKey) { mutableStateOf(storedTrimmed) }
    LaunchedEffect(storedTrimmed) { draft = storedTrimmed }

    Column(
            modifier =
                    Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
                text = stringResource(R.string.settings_profile_names_default_label, row.defaultTitle),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
                value = draft,
                onValueChange = { draft = it.take(PreferencesManager.MAX_PROFILE_DISPLAY_NAME_LENGTH) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.settings_profile_names_field_label)) },
                singleLine = true,
        )
        Spacer(Modifier.height(8.dp))
        Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
        ) {
            FokusTextButton(
                    onClick = { onSave(draft) },
                    enabled = draft.trim() != storedTrimmed,
            ) {
                Text(stringResource(R.string.settings_profile_names_save))
            }
            Spacer(Modifier.padding(horizontal = 4.dp))
            FokusTextButton(
                    onClick = {
                        draft = ""
                        onSave("")
                    },
                    enabled = storedTrimmed.isNotEmpty(),
            ) {
                Text(stringResource(R.string.settings_profile_names_reset))
            }
        }
    }
}
