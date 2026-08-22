package nwd.fokuslauncher

import android.app.Application
import nwd.fokuslauncher.data.util.AppLocaleHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FokusLauncherApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // After super: Hilt / Application init is ready; still before any activity is created.
        AppLocaleHelper.applyStoredLocaleFromDisk(this)
    }
}
