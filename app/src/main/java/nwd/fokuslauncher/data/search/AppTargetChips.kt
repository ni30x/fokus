package nwd.fokuslauncher.data.search

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class AppSearchChip(
    val id: String,
    val label: String,
    val targetAppPackage: String?,
    val icon: ImageVector,
    val launchAction: (context: Context, query: String) -> Boolean
)

object AppTargetChips {

    fun getChips(): List<AppSearchChip> {
        return listOf(
            AppSearchChip(
                id = "maps",
                label = "Google Maps",
                targetAppPackage = "com.google.android.apps.maps",
                icon = Icons.Default.Place,
                launchAction = { context, query ->
                    val encoded = Uri.encode(query)
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$encoded")).apply {
                        setPackage("com.google.android.apps.maps")
                    }
                    val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/$encoded"))
                    safelyStartIntent(context, intent, fallbackIntent)
                }
            ),
            AppSearchChip(
                id = "youtube",
                label = "YouTube",
                targetAppPackage = "com.google.android.youtube",
                icon = Icons.Default.PlayArrow,
                launchAction = { context, query ->
                    val encoded = Uri.encode(query)
                    val intent = Intent(Intent.ACTION_SEARCH).apply {
                        setPackage("com.google.android.youtube")
                        putExtra(SearchManager.QUERY, query)
                    }
                    val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded"))
                    safelyStartIntent(context, intent, fallbackIntent)
                }
            ),
            AppSearchChip(
                id = "gemini",
                label = "Gemini",
                targetAppPackage = "com.google.android.apps.bard",
                icon = Icons.Default.AutoAwesome,
                launchAction = { context, query ->
                    val encoded = Uri.encode(query)
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://gemini.google.com/prompt?q=$encoded")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    safelyStartIntent(context, intent, null)
                }
            ),
            AppSearchChip(
                id = "browser",
                label = "Web Search",
                targetAppPackage = null,
                icon = Icons.Default.Language,
                launchAction = { context, query ->
                    val encoded = Uri.encode(query)
                    val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                        putExtra(SearchManager.QUERY, query)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$encoded")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    safelyStartIntent(context, intent, fallbackIntent)
                }
            ),
            AppSearchChip(
                id = "playstore",
                label = "Play Store",
                targetAppPackage = "com.android.vending",
                icon = Icons.Default.ShoppingBag,
                launchAction = { context, query ->
                    val encoded = Uri.encode(query)
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$encoded")).apply {
                        setPackage("com.android.vending")
                    }
                    val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=$encoded"))
                    safelyStartIntent(context, intent, fallbackIntent)
                }
            ),
            AppSearchChip(
                id = "wikipedia",
                label = "Wikipedia",
                targetAppPackage = "org.wikipedia",
                icon = Icons.Default.MenuBook,
                launchAction = { context, query ->
                    val encoded = Uri.encode(query)
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://en.wikipedia.org/wiki/Special:Search?search=$encoded")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    safelyStartIntent(context, intent, null)
                }
            )
        )
    }

    private fun safelyStartIntent(context: Context, primary: Intent, fallback: Intent?): Boolean {
        return try {
            primary.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(primary)
            true
        } catch (e: Exception) {
            if (fallback != null) {
                try {
                    fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(fallback)
                    true
                } catch (e2: Exception) {
                    false
                }
            } else {
                false
            }
        }
    }
}
