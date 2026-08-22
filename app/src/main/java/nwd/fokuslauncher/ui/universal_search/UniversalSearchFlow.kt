package nwd.fokuslauncher.ui.universal_search

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.app.SearchManager
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- Models ---

data class PermissionState(
    val id: String,
    val title: String,
    val description: String,
    var isGranted: Boolean = false
)

data class SearchEngineConfig(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val color: Color,
    var isEnabled: Boolean = true,
    var order: Int = 0,
    val searchUrlTemplate: String
)

sealed class SearchResultItem {
    data class LocalApp(val packageName: String, val appName: String, val icon: ImageVector = Icons.Default.Apps) : SearchResultItem()
    data class ExternalSearch(val engine: SearchEngineConfig, val query: String) : SearchResultItem()
}

// --- Intent Resolver ---

object UniversalSearchIntentResolver {
    fun openExternalSearch(context: Context, engine: SearchEngineConfig, query: String) {
        val url = engine.searchUrlTemplate.replace("{query}", Uri.encode(query))
        
        if (engine.id == "youtube") {
            val ytIntent = Intent(Intent.ACTION_SEARCH)
            ytIntent.setPackage("com.google.android.youtube")
            ytIntent.putExtra(SearchManager.QUERY, query)
            ytIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            try {
                context.startActivity(ytIntent)
                return
            } catch (e: Exception) { }
        } else if (engine.id == "playstore") {
             val playIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$query"))
             playIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
             try {
                context.startActivity(playIntent)
                return
             } catch(e: Exception) {}
        }
        
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// --- UI Components ---

@Composable
fun UniversalSearchApp() {
    var isOnboardingComplete by remember { mutableStateOf(false) }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E24),
            primary = Color(0xFFB39DDB),
            onPrimary = Color.White
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (!isOnboardingComplete) {
                OnboardingFlow(onComplete = { isOnboardingComplete = true })
            } else {
                MainSearchScreen()
            }
        }
    }
}

@Composable
fun OnboardingFlow(onComplete: () -> Unit) {
    var step by remember { mutableStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val title = when (step) {
                1 -> "Permissions"
                2 -> "Search Engines"
                else -> "Almost Done!"
            }
            Text(title, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Surface(
                color = Color(0xFF2A2A35),
                shape = CircleShape
            ) {
                Text(
                    text = "$step/3",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (step) {
                1 -> OnboardingStep1()
                2 -> OnboardingStep2()
                3 -> OnboardingStep3()
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { if (step < 3) step++ else onComplete() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = if (step < 3) "Next" else "Start Searching!",
                color = Color(0xFF121212),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun OnboardingStep1() {
    val permissions = remember {
        listOf(
            PermissionState("usage", "Usage Access", "Required to show your recently used apps in suggestions."),
            PermissionState("contacts", "Contacts", "Required to search your contacts."),
            PermissionState("files", "Files Access", "Required to search through your documents and other files."),
            PermissionState("calendar", "Calendar", "Required to search your calendar events.")
        )
    }

    Column {
        Text("These permissions unlock additional features. They are optional. Your data stays on your device.", color = Color.Gray)
        Spacer(modifier = Modifier.height(24.dp))
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                permissions.forEach { perm ->
                    var isGranted by remember { mutableStateOf(perm.isGranted) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isGranted = !isGranted }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(perm.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Text(perm.description, color = Color.Gray, fontSize = 14.sp)
                        }
                        // Simple check icon replacement for demo
                        Icon(
                            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isGranted) Color(0xFF4CAF50) else Color.Gray,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingStep2() {
    val engines = remember {
        listOf(
            SearchEngineConfig("google", "Google", Icons.Default.Search, Color(0xFF4285F4), searchUrlTemplate = "https://www.google.com/search?q={query}"),
            SearchEngineConfig("chatgpt", "ChatGPT", Icons.Default.ChatBubbleOutline, Color(0xFF10A37F), searchUrlTemplate = "https://chat.openai.com/?q={query}"),
            SearchEngineConfig("gemini", "Gemini", Icons.Default.AutoAwesome, Color(0xFF8E24AA), searchUrlTemplate = "https://gemini.google.com/app?q={query}"),
            SearchEngineConfig("perplexity", "Perplexity", Icons.Default.Explore, Color(0xFF00C6BA), searchUrlTemplate = "https://www.perplexity.ai/search?q={query}"),
            SearchEngineConfig("playstore", "Google Play", Icons.Default.PlayArrow, Color(0xFF34A853), searchUrlTemplate = ""),
            SearchEngineConfig("youtube", "YouTube", Icons.Default.PlayCircle, Color(0xFFFF0000), searchUrlTemplate = "https://www.youtube.com/results?search_query={query}"),
            SearchEngineConfig("maps", "Google Maps", Icons.Default.Map, Color(0xFFFBBC05), searchUrlTemplate = "https://www.google.com/maps/search/{query}")
        )
    }

    Column {
        Text("Enable search engines you need and reorder them as you like.", color = Color.Gray)
        Spacer(modifier = Modifier.height(24.dp))
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                engines.forEach { engine ->
                    var isEnabled by remember { mutableStateOf(engine.isEnabled) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Menu, contentDescription = "Reorder", tint = Color.Gray)
                        Spacer(modifier = Modifier.width(16.dp))
                        Icon(engine.icon, contentDescription = null, tint = engine.color, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(engine.name, color = Color.White, fontSize = 18.sp, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { isEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingStep3() {
    Column {
        Text("Default Messaging App", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            var selectedApp by remember { mutableStateOf("messages") }
            MessagingAppCard(
                name = "Messages",
                icon = Icons.Default.Message,
                isSelected = selectedApp == "messages",
                onClick = { selectedApp = "messages" },
                modifier = Modifier.weight(1f)
            )
            MessagingAppCard(
                name = "WhatsApp",
                icon = Icons.Default.Phone, // Placeholder
                isSelected = selectedApp == "whatsapp",
                onClick = { selectedApp = "whatsapp" },
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Text("Files & Folders", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                listOf("Folders" to Icons.Default.Folder, "Documents" to Icons.Default.Description, "Pictures" to Icons.Default.Image, "Videos" to Icons.Default.VideoLibrary, "Audio" to Icons.Default.AudioFile).forEach { (name, icon) ->
                    var isEnabled by remember { mutableStateOf(name != "Folders") }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, contentDescription = null, tint = Color.LightGray)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(name, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { isEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessagingAppCard(name: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(name, color = Color.White, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun MainSearchScreen() {
    var query by remember { mutableStateOf("") }
    val context = LocalContext.current
    
    val allEngines = listOf(
        SearchEngineConfig("google", "Google", Icons.Default.Search, Color(0xFF4285F4), searchUrlTemplate = "https://www.google.com/search?q={query}"),
        SearchEngineConfig("chatgpt", "ChatGPT", Icons.Default.ChatBubbleOutline, Color(0xFF10A37F), searchUrlTemplate = "https://chat.openai.com/?q={query}"),
        SearchEngineConfig("gemini", "Gemini", Icons.Default.AutoAwesome, Color(0xFF8E24AA), searchUrlTemplate = "https://gemini.google.com/app?q={query}"),
        SearchEngineConfig("perplexity", "Perplexity", Icons.Default.Explore, Color(0xFF00C6BA), searchUrlTemplate = "https://www.perplexity.ai/search?q={query}"),
        SearchEngineConfig("playstore", "Google Play", Icons.Default.PlayArrow, Color(0xFF34A853), searchUrlTemplate = ""),
        SearchEngineConfig("youtube", "YouTube", Icons.Default.PlayCircle, Color(0xFFFF0000), searchUrlTemplate = "https://www.youtube.com/results?search_query={query}")
    )
    
    val localApps = listOf(
        SearchResultItem.LocalApp("com.spotify.music", "Spotify"),
        SearchResultItem.LocalApp("com.spotify.premium", "Spotify premium"),
        SearchResultItem.LocalApp("com.spotify.web", "Spotify web")
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 16.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(32.dp)),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true,
            shape = RoundedCornerShape(32.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        if (query.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Local Results
                val matchedApps = localApps.filter { it.appName.contains(query, ignoreCase = true) || "spotifry".contains(query.lowercase()) } // simulating fuzzy match
                
                if (matchedApps.isNotEmpty()) {
                    item {
                        Icon(Icons.Default.Apps, contentDescription = null, tint = Color(0xFF1DB954), modifier = Modifier.padding(vertical = 12.dp).size(32.dp))
                        Text("Spotify", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    items(matchedApps) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ArrowOutward, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(app.appName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                // External Search Engine Cards
                items(allEngines) { engine ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                            .clickable {
                                UniversalSearchIntentResolver.openExternalSearch(context, engine, query)
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(engine.icon, contentDescription = null, tint = engine.color, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Search on ${engine.name}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Open keyboard hint
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            Text("Open keyboard", color = Color.Gray, fontSize = 14.sp)
        }
    }
}
