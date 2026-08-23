package nwd.fokuslauncher.data.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import nwd.fokuslauncher.data.local.APP_LOCALE_TAG_KEY
import nwd.fokuslauncher.data.local.fokusLauncherPreferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object AppLocaleHelper {

    private const val PREFS_NAME = "fokus_locale_cache"
    private const val KEY_CACHED_LOCALE = "cached_locale_tag"

    fun cacheLocaleTag(context: Context, tag: String) {
        try {
            val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sp.edit().putString(KEY_CACHED_LOCALE, tag.trim()).apply()
        } catch (_: Exception) {
            // Ignore cache write failures
        }
    }

    fun getCachedLocaleTag(context: Context): String? {
        return try {
            val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (sp.contains(KEY_CACHED_LOCALE)) {
                sp.getString(KEY_CACHED_LOCALE, "") ?: ""
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun applyLocaleTag(tag: String) {
        val locales =
                if (tag.isBlank()) LocaleListCompat.getEmptyLocaleList()
                else LocaleListCompat.forLanguageTags(tag.trim())
        AppCompatDelegate.setApplicationLocales(locales)
    }

    /**
     * Non-blocking locale initialization for cold start.
     *
     * 1. Synchronously reads the cached locale tag from SharedPreferences for an instant, zero-cost fast path.
     * 2. Asynchronously queries the DataStore source of truth on a background coroutine and applies corrections if needed.
     */
    fun applyStoredLocaleFromDisk(context: Context) {
        val appContext = context.applicationContext

        // Fast-path: Synchronously read cached locale tag from lightweight SharedPreferences
        val cachedTag = getCachedLocaleTag(appContext)
        if (cachedTag != null) {
            try {
                applyLocaleTag(cachedTag)
            } catch (_: Exception) {
                // Avoid taking down the process if AppCompat locale APIs fail on a specific device.
            }
        }

        // Asynchronous path: Read DataStore source of truth without blocking onCreate or first frame
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            val dataStoreTag = try {
                appContext.fokusLauncherPreferencesDataStore
                        .data
                        .first()[APP_LOCALE_TAG_KEY] ?: ""
            } catch (_: Exception) {
                null
            }

            if (dataStoreTag != null) {
                cacheLocaleTag(appContext, dataStoreTag)
                if (dataStoreTag != cachedTag) {
                    withContext(Dispatchers.Main) {
                        try {
                            applyLocaleTag(dataStoreTag)
                        } catch (_: Exception) {
                            // Ignore failure
                        }
                    }
                }
            }
        }
    }
}
