package nwd.fokuslauncher.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import nwd.fokuslauncher.data.local.PreferencesManager
import nwd.fokuslauncher.data.model.PhotoWallpaperDrawerOverlayIntensity
import nwd.fokuslauncher.ui.util.stateWhileSubscribedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class PhotoWallpaperDrawerOverlayUi(
        val usesPhotoWallpaper: Boolean,
        val intensityMultiplier: Float,
)

@HiltViewModel
class FokusNavGraphViewModel @Inject constructor(
        preferencesManager: PreferencesManager,
) : ViewModel() {

    val hasCompletedOnboarding =
            preferencesManager.hasCompletedOnboardingFlow.stateWhileSubscribedIn(
                    viewModelScope,
                    false,
            )

    val photoWallpaperDrawerOverlayUiState: StateFlow<PhotoWallpaperDrawerOverlayUi> =
            combine(
                    preferencesManager.launcherAppearanceFlow.map { it.usesPhotoWallpaper },
                    preferencesManager.photoWallpaperDrawerOverlayIntensityFlow,
            ) { usesPhoto, intensity ->
                PhotoWallpaperDrawerOverlayUi(usesPhoto, intensity)
            }
                    .stateWhileSubscribedIn(
                            viewModelScope,
                            PhotoWallpaperDrawerOverlayUi(
                                    usesPhotoWallpaper = false,
                                    intensityMultiplier =
                                            PhotoWallpaperDrawerOverlayIntensity.DEFAULT,
                            ),
                    )
}
