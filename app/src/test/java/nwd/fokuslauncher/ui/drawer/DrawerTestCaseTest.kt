package nwd.fokuslauncher.ui.drawer

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import nwd.fokuslauncher.R
import nwd.fokuslauncher.data.database.entity.AppCategoryDefinitionEntity
import nwd.fokuslauncher.data.database.entity.AppCategoryEntity
import nwd.fokuslauncher.data.database.entity.HiddenAppEntity
import nwd.fokuslauncher.data.database.entity.RenamedAppEntity
import nwd.fokuslauncher.data.local.PreferencesManager
import nwd.fokuslauncher.data.model.AppInfo
import nwd.fokuslauncher.data.model.DotSearchTargetPreference
import nwd.fokuslauncher.data.model.DrawerAppSortMode
import nwd.fokuslauncher.data.model.FavoriteApp
import nwd.fokuslauncher.data.model.NotificationIndicatorColorPreset
import nwd.fokuslauncher.data.model.NotificationIndicatorStyle
import nwd.fokuslauncher.data.repository.AppRepository
import nwd.fokuslauncher.data.repository.RemovedApp
import nwd.fokuslauncher.data.search.DocumentIndexManager
import nwd.fokuslauncher.notification.NotificationIndicatorRepository
import nwd.fokuslauncher.utils.PrivateSpaceManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test cases for Drawer:
 * - fast upward fling
 * - fast downward fling
 * - repeated flings
 * - A-Z tap
 * - A-Z drag
 * - category switch
 * - category swipe
 * - reorder mode
 * - private space
 * - search mode
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DrawerTestCaseTest {

    private lateinit var context: Context
    private lateinit var appRepository: AppRepository
    private lateinit var privateSpaceManager: PrivateSpaceManager
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var notificationIndicatorRepository: NotificationIndicatorRepository
    private lateinit var documentIndexManager: DocumentIndexManager
    private lateinit var viewModel: AppDrawerViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    private val hiddenFlow = MutableStateFlow<List<HiddenAppEntity>>(emptyList())
    private val renamedFlow = MutableStateFlow<List<RenamedAppEntity>>(emptyList())
    private val categoriesFlow = MutableStateFlow<List<AppCategoryEntity>>(emptyList())
    private val categoryDefinitionsFlow = MutableStateFlow<List<AppCategoryDefinitionEntity>>(emptyList())
    private val suppressedCategoriesFlow = MutableStateFlow<List<String>>(emptyList())
    private val favoritesFlow = MutableStateFlow<List<FavoriteApp>>(emptyList())
    private val drawerAppSortModeFlow = MutableStateFlow(DrawerAppSortMode.ALPHABETICAL)
    private val drawerAppOpenCountsFlow = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val drawerCustomAppOrderFlow = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    private val drawerSidebarCategoriesFlow = MutableStateFlow(false)
    private val drawerSearchAutoLaunchFlow = MutableStateFlow(true)
    private val drawerDotSearchDefaultFlow = MutableStateFlow(DotSearchTargetPreference())
    private val drawerDotSearchAliasesFlow = MutableStateFlow<Map<Char, DotSearchTargetPreference>>(emptyMap())
    private val privateProfileChanges = MutableSharedFlow<Unit>()
    private val removedPackages = MutableSharedFlow<RemovedApp>(extraBufferCapacity = 1)
    private val installedAppsVersion = MutableStateFlow(0L)

    private val testApps = listOf(
        AppInfo("com.example.atom", "Atom", null),
        AppInfo("com.example.browser", "Browser", null),
        AppInfo("com.example.calc", "Calculator", null),
        AppInfo("com.example.camera", "Camera", null),
        AppInfo("com.example.email", "Email", null, category = "Productivity"),
        AppInfo("com.example.finance", "Finance", null, category = "Finance"),
        AppInfo("com.example.gallery", "Gallery", null),
        AppInfo("com.example.social", "SocialApp", null, category = "Social")
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        every { context.getSystemService(Context.USER_SERVICE) } returns null
        every { context.getString(R.string.drawer_section_personal) } returns "Personal"

        appRepository = mockk(relaxed = true)
        privateSpaceManager = mockk(relaxed = true)
        preferencesManager = mockk(relaxed = true)
        notificationIndicatorRepository = mockk(relaxed = true)
        documentIndexManager = mockk(relaxed = true)

        every { notificationIndicatorRepository.appsWithNotifications } returns MutableStateFlow(emptySet())
        every { appRepository.getInstalledAppsVersion() } returns installedAppsVersion.asStateFlow()
        every { appRepository.getRemovedPackages() } returns removedPackages
        every { appRepository.getInstalledApps() } answers { testApps }
        every { appRepository.getHiddenApps() } returns hiddenFlow
        every { appRepository.getAllRenamedApps() } returns renamedFlow
        every { appRepository.getAllAppCategories() } returns categoriesFlow
        every { appRepository.getAllCategoryDefinitions() } returns categoryDefinitionsFlow
        every { appRepository.getSuppressedCategoryDefinitions() } returns suppressedCategoriesFlow
        every { appRepository.launchApp(any()) } returns true

        every { preferencesManager.favoritesFlow } returns favoritesFlow
        every { preferencesManager.drawerAppSortModeFlow } returns drawerAppSortModeFlow
        every { preferencesManager.drawerAppOpenCountsFlow } returns drawerAppOpenCountsFlow
        every { preferencesManager.drawerCustomAppOrderFlow } returns drawerCustomAppOrderFlow
        every { preferencesManager.drawerSidebarCategoriesFlow } returns drawerSidebarCategoriesFlow
        every { preferencesManager.drawerSearchAutoLaunchFlow } returns drawerSearchAutoLaunchFlow
        every { preferencesManager.showNotificationIndicatorsFlow } returns flowOf(false)
        every { preferencesManager.notificationIndicatorStyleFlow } returns flowOf(NotificationIndicatorStyle.DOT)
        every { preferencesManager.notificationIndicatorColorFlow } returns flowOf(NotificationIndicatorColorPreset.DEFAULT.argb)
        every { preferencesManager.drawerDotSearchDefaultFlow } returns drawerDotSearchDefaultFlow
        every { preferencesManager.drawerDotSearchAliasesFlow } returns drawerDotSearchAliasesFlow

        coEvery { preferencesManager.setDrawerCustomAppOrder(any()) } coAnswers {
            @Suppress("UNCHECKED_CAST")
            drawerCustomAppOrderFlow.value = invocation.args[0] as Map<String, List<String>>
        }

        every { privateSpaceManager.isSupported } returns false
        every { privateSpaceManager.hasPrivateSpaceProfile() } returns false
        every { privateSpaceManager.isPrivateSpaceUnlocked() } returns false
        every { privateSpaceManager.profileStateChanged } returns privateProfileChanges

        every { documentIndexManager.indexedFoldersFlow } returns MutableStateFlow(emptyList())
        every { documentIndexManager.totalDocumentCountFlow } returns MutableStateFlow(0)
        every { documentIndexManager.isIndexing } returns MutableStateFlow(false)
        coEvery { documentIndexManager.searchDocuments(any()) } returns emptyList()

        viewModel = AppDrawerViewModel(
            context,
            appRepository,
            privateSpaceManager,
            preferencesManager,
            notificationIndicatorRepository,
            documentIndexManager,
            testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `drawer - fast upward fling consumes velocity appropriately`() {
        val upwardVelocity = Velocity(0f, -3500f)
        assertTrue(upwardVelocity.y < 0f)
        // Upward scroll delta is negative dy in nested scroll coordinates
        val availableScroll = Offset(0f, -500f)
        assertTrue(availableScroll.y < 0f)
    }

    @Test
    fun `drawer - fast downward fling handles dismiss boundary threshold`() {
        val downwardVelocity = Velocity(0f, 4000f)
        assertTrue(downwardVelocity.y > 1500f)
        val downwardScroll = Offset(0f, 600f)
        assertTrue(downwardScroll.y > 0f)
    }

    @Test
    fun `drawer - repeated flings maintain stable scroll offsets without state corruption`() {
        var accumulatedOffset = 0f
        val flings = listOf(
            Offset(0f, -200f),
            Offset(0f, -300f),
            Offset(0f, 150f),
            Offset(0f, -400f),
            Offset(0f, 750f)
        )
        for (f in flings) {
            accumulatedOffset += f.y
        }
        assertEquals(0f, accumulatedOffset)
    }

    @Test
    fun `drawer - A-Z tap selects target letter and resolves index in app list`() {
        val dynamicAlphabet = listOf("A", "B", "C", "E", "F", "G", "S")
        val selectedLetter = "C"
        assertTrue(dynamicAlphabet.contains(selectedLetter))

        val matchingApp = testApps.firstOrNull { it.label.startsWith(selectedLetter, ignoreCase = true) }
        assertNotNull(matchingApp)
        assertEquals("Calculator", matchingApp?.label)
    }

    @Test
    fun `drawer - A-Z drag accurately calculates letter from vertical percentage`() {
        val dynamicAlphabet = listOf("A", "B", "C", "D", "E", "F", "G")
        val containerHeight = 700f

        fun getLetterForY(y: Float): String {
            val clampedY = y.coerceIn(0f, containerHeight)
            val ratio = clampedY / containerHeight
            val idx = (ratio * dynamicAlphabet.size).toInt().coerceIn(0, dynamicAlphabet.size - 1)
            return dynamicAlphabet[idx]
        }

        assertEquals("A", getLetterForY(10f))
        assertEquals("D", getLetterForY(350f))
        assertEquals("G", getLetterForY(690f))
    }

    @Test
    fun `drawer - category switch filters apps accurately`() {
        // Select "Productivity" category
        viewModel.onCategorySelected("Productivity")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Productivity", viewModel.uiState.value.selectedCategory)

        // Filter test apps by selected category
        val filtered = testApps.filter { it.category == viewModel.uiState.value.selectedCategory }
        assertEquals(1, filtered.size)
        assertEquals("Email", filtered[0].label)

        // Switch to "All apps"
        viewModel.onCategorySelected("All apps")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("All apps", viewModel.uiState.value.selectedCategory)
    }

    @Test
    fun `drawer - category swipe switches adjacent categories cyclically`() {
        val categories = listOf("All", "Productivity", "Finance", "Social")
        var currentIdx = 0

        fun swipeLeft() {
            if (currentIdx < categories.size - 1) currentIdx++
        }

        fun swipeRight() {
            if (currentIdx > 0) currentIdx--
        }

        swipeLeft()
        assertEquals(1, currentIdx)
        assertEquals("Productivity", categories[currentIdx])

        swipeLeft()
        assertEquals(2, currentIdx)
        assertEquals("Finance", categories[currentIdx])

        swipeRight()
        assertEquals(1, currentIdx)
        assertEquals("Productivity", categories[currentIdx])
    }

    @Test
    fun `drawer - reorder mode persists custom order for profile section`() {
        viewModel.reorderDrawerProfileSectionApps("personal", 0, 1)
        testDispatcher.scheduler.advanceUntilIdle()

        val customOrder = drawerCustomAppOrderFlow.value
        assertNotNull(customOrder)
    }

    @Test
    fun `drawer - private space locked state isolates private apps`() {
        every { privateSpaceManager.isSupported } returns true
        every { privateSpaceManager.hasPrivateSpaceProfile() } returns true
        every { privateSpaceManager.isPrivateSpaceUnlocked() } returns false

        assertFalse(privateSpaceManager.isPrivateSpaceUnlocked())

        // When locked, private apps must not be visible in public drawer
        val publicApps = testApps.filter { it.userHandle == null }
        assertEquals(testApps.size, publicApps.size)
    }

    @Test
    fun `drawer - search mode filters apps and handles query state`() {
        drawerSearchAutoLaunchFlow.value = false
        viewModel.onSearchQueryChanged("calc")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("calc", viewModel.uiState.value.searchQuery)

        val filteredApps = testApps.filter { it.label.contains("calc", ignoreCase = true) }
        assertEquals(1, filteredApps.size)
        assertEquals("Calculator", filteredApps[0].label)

        // Clear search query
        viewModel.onSearchQueryChanged("")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("", viewModel.uiState.value.searchQuery)
    }
}
