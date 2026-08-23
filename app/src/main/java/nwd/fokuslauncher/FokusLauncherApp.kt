package nwd.fokuslauncher

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import nwd.fokuslauncher.data.util.AppLocaleHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FokusLauncherApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        // After super: Hilt / Application init is ready; still before any activity is created.
        AppLocaleHelper.applyStoredLocaleFromDisk(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
}
