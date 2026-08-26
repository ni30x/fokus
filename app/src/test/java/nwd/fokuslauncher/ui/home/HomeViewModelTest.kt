package nwd.fokuslauncher.ui.home

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.BatteryManager
import android.os.Build
import android.text.format.DateFormat
import nwd.fokuslauncher.data.local.HomeWidgetVisibility
import nwd.fokuslauncher.data.local.PreferencesManager
import nwd.fokuslauncher.data.model.FavoriteApp
import nwd.fokuslauncher.data.model.AppInfo
import nwd.fokuslauncher.data.model.HomeDateFormatStyle
import nwd.fokuslauncher.data.model.LauncherFontScale
import nwd.fokuslauncher.data.model.HomeAlignment
import nwd.fokuslauncher.data.model.HomeShortcut
import nwd.fokuslauncher.data.model.HOST_APP_METADATA_SENTINEL
import nwd.fokuslauncher.data.model.ShortcutTarget
import nwd.fokuslauncher.data.model.appMetadataKey
import nwd.fokuslauncher.data.model.favoriteAppStableKey
import nwd.fokuslauncher.data.repository.AppRepository
import nwd.fokuslauncher.data.repository.RemovedApp
import nwd.fokuslauncher.data.repository.WeatherRepository
import nwd.fokuslauncher.media.MediaRepository
import nwd.fokuslauncher.notification.NotificationIndicatorRepository
import nwd.fokuslauncher.usage.ScreenTimeRepository
import nwd.fokuslauncher.utils.LockScreenHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HomeViewModelTest {

    private lateinit var context: Context
    private lateinit var appRepository: AppRepository
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var weatherRepository: WeatherRepository
    private lateinit var mediaRepository: MediaRepository
    private lateinit var screenTimeRepository: ScreenTimeRepository
    private lateinit var notificationIndicatorRepository: NotificationIndicatorRepository
    private lateinit var removedPackages: MutableSharedFlow<RemovedApp>
    private val testDispatcher = StandardTestDispatcher()
    private var originalLocale: Locale = Locale.getDefault()
    private var originalTimeZone: TimeZone = TimeZone.getDefault()

    private val testFavorites = listOf(
        FavoriteApp(label = "Music", packageName = "com.lu4p.music", iconName = "music"),
        FavoriteApp(label = "Work", packageName = "com.lu4p.work", iconName = "work"),
        FavoriteApp(label = "Social", packageName = "com.lu4p.social", iconName = "chat")
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        originalLocale = Locale.getDefault()
        originalTimeZone = TimeZone.getDefault()

        context = mockk(relaxed = true)
        appRepository = mockk(relaxed = true)
        preferencesManager = mockk(relaxed = true)
        weatherRepository = mockk(relaxed = true)
        mediaRepository = mockk(relaxed = true)
        screenTimeRepository = mockk(relaxed = true)
        notificationIndicatorRepository = mockk(relaxed = true)
        every { mediaRepository.state } returns MutableStateFlow(null)
        every { notificationIndicatorRepository.appsWithNotifications } returns
                MutableStateFlow(emptySet())
        every { screenTimeRepository.queryLast24HoursTotalMs() } returns 0L
        removedPackages = MutableSharedFlow(extraBufferCapacity = 1)

        // Mock battery intent
        val batteryIntent = mockk<Intent>(relaxed = true)
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns 75
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1) } returns 100
        stubNullReceiverBatterySticky(batteryIntent)

        // Mock preferences using Fake
        preferencesManager = mockk(relaxed = true)
        every { preferencesManager.favoritesFlow } returns flowOf(testFavorites)
        every { preferencesManager.swipeLeftTargetFlow } returns flowOf(null)
        every { preferencesManager.swipeRightTargetFlow } returns flowOf(null)
        every { preferencesManager.rightSideShortcutsFlow } returns flowOf(emptyList())
        every { preferencesManager.preferredWeatherTapFlow } returns flowOf(null)
        every { preferencesManager.preferredClockTapFlow } returns flowOf(null)
        every { preferencesManager.preferredCalendarTapFlow } returns flowOf(null)
        every { preferencesManager.homeAlignmentFlow } returns flowOf(HomeAlignment.LEFT)
        every { preferencesManager.launcherFontScaleFlow } returns
                flowOf(LauncherFontScale.DEFAULT)
        every { preferencesManager.homeWidgetVisibilityFlow } returns
                flowOf(HomeWidgetVisibility(true, true, true, true))
        every { preferencesManager.showHomeClockFlow } returns flowOf(true)
        every { preferencesManager.showHomeDateFlow } returns flowOf(true)
        every { preferencesManager.showHomeWeatherFlow } returns flowOf(true)
        every { preferencesManager.showWorldClockWeatherFlow } returns flowOf(false)
        every { preferencesManager.showHomeBatteryFlow } returns flowOf(true)
        every { preferencesManager.showHomeMediaFlow } returns flowOf(false)
        every { preferencesManager.showHomeScreenTimeFlow } returns flowOf(false)
        every { preferencesManager.homeExtraWidgetsFlow } returns flowOf(emptyList())
        every { preferencesManager.worldClockCitiesFlow } returns flowOf(emptyList())
        every { preferencesManager.countdownEventsFlow } returns flowOf(emptyList())
        every { preferencesManager.showNotificationIndicatorsFlow } returns flowOf(false)
        every { preferencesManager.notificationIndicatorStyleFlow } returns
                flowOf(nwd.fokuslauncher.data.model.NotificationIndicatorStyle.DOT)
        every { preferencesManager.notificationIndicatorColorFlow } returns
                flowOf(nwd.fokuslauncher.data.model.NotificationIndicatorColorPreset.DEFAULT.argb)
        every { preferencesManager.homeDateFormatStyleFlow } returns
                flowOf(HomeDateFormatStyle.SYSTEM_DEFAULT)
        every { preferencesManager.doubleTapEmptyLockFlow } returns flowOf(false)
        coEvery { preferencesManager.ensureRightSideShortcutsInitialized() } returns Unit
        coEvery { preferencesManager.setFavorites(any()) } returns Unit

        // Mock repository flows used by name resolution
        every { appRepository.getAllRenamedApps() } returns flowOf(emptyList())
        every { appRepository.getHiddenApps() } returns flowOf(emptyList())
        every { appRepository.getInstalledAppsVersion() } returns MutableStateFlow(0L)
        every { appRepository.getAllAppCategories() } returns flowOf(emptyList())
        every { appRepository.getInstalledApps() } returns emptyList()
        every { appRepository.getArchivedApps() } returns emptyList()
        every { appRepository.getAllShortcutActions() } returns emptyList()
        every { appRepository.getLaunchableAppKeys(any()) } returns emptySet()
        every { appRepository.getRemovedPackages() } returns removedPackages
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
        TimeZone.setDefault(originalTimeZone)
        Dispatchers.resetMain()
    }

    private fun createViewModel() = HomeViewModel(
        context,
        appRepository,
        preferencesManager,
        weatherRepository,
        mediaRepository,
        screenTimeRepository,
        notificationIndicatorRepository,
    )

    private fun createViewModel(withContext: Context) = HomeViewModel(
        withContext,
        appRepository,
        preferencesManager,
        weatherRepository,
        mediaRepository,
        screenTimeRepository,
        notificationIndicatorRepository,
    )

    /**
     * Sticky battery read uses [androidx.core.content.ContextCompat.registerReceiver], which maps
     * to different [Context.registerReceiver] overloads by API (including the 5-arg form on API 33+).
     */
    private fun stubNullReceiverBatterySticky(intent: Intent?) {
        every { context.registerReceiver(null, any<IntentFilter>()) } returns intent
        every { context.registerReceiver(null, any<IntentFilter>(), any<Int>()) } returns intent
        every { context.registerReceiver(null, any<IntentFilter>(), any<String>(), any<android.os.Handler>()) } returns intent
        every { context.registerReceiver(null, any<IntentFilter>(), any<String>(), any<android.os.Handler>(), any<Int>()) } returns intent
    }

    private fun contextForClockAndBattery(batterySticky: Intent): Context {
        val mockCtx = spyk(org.robolectric.RuntimeEnvironment.getApplication())
        every { mockCtx.registerReceiver(null, any<IntentFilter>()) } returns batterySticky
        every { mockCtx.registerReceiver(null, any<IntentFilter>(), any<Int>()) } returns batterySticky
        every { mockCtx.registerReceiver(null, any<IntentFilter>(), any<String>(), any<android.os.Handler>()) } returns batterySticky
        every { mockCtx.registerReceiver(null, any<IntentFilter>(), any<String>(), any<android.os.Handler>(), any<Int>()) } returns batterySticky
        return mockCtx
    }

    private fun mockBatteryStickyIntent(level: Int = 75, scale: Int = 100): Intent {
        val batteryIntent = mockk<Intent>(relaxed = true)
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns level
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1) } returns scale
        return batteryIntent
    }

    @Test
    fun `initial state has battery percentage`() {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceTimeBy(100)

        val state = viewModel.clockUiState.value
        assertEquals(75, state.batteryPercent)
    }

    @Test
    fun `initial state has formatted time`() {
        val viewModel = createViewModel(contextForClockAndBattery(mockBatteryStickyIntent()))
        testDispatcher.scheduler.advanceTimeBy(1100)

        val state = viewModel.clockUiState.value
        assertTrue(state.currentTime.isNotEmpty())
    }

    @Test
    fun `applySystemTimeZoneChange updates JVM default timezone`() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
        val viewModel = createViewModel(contextForClockAndBattery(mockBatteryStickyIntent()))
        testDispatcher.scheduler.advanceTimeBy(1100)

        // Same logic as ACTION_TIMEZONE_CHANGED; not asserted via sendBroadcast because that
        // intent is system-sent and may not be delivered from test code on all runners.
        viewModel.applySystemTimeZoneChange("Europe/Paris")
        testDispatcher.scheduler.advanceTimeBy(1100)

        assertEquals("Europe/Paris", TimeZone.getDefault().id)
        assertTrue(viewModel.clockUiState.value.currentTime.isNotEmpty())
    }

    @Test
    fun `refreshBattery handles invalid battery intent gracefully`() {
        val batteryIntent = Intent(Intent.ACTION_BATTERY_CHANGED)
        // Missing EXTRAS will cause getIntExtra to return default (-1)
        stubNullReceiverBatterySticky(batteryIntent)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceTimeBy(100)

        assertEquals(0, viewModel.clockUiState.value.batteryPercent)
    }

    @Test
    fun `checkDefaultLauncher handles exception and sets to false`() {
        every { context.packageManager } throws RuntimeException("Package manager crashed")

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceTimeBy(100)

        assertFalse(viewModel.uiState.value.isDefaultLauncher)
    }

    @Test
    fun `hideApp removes only matching profile favorite`() {
        val workFavorite =
            FavoriteApp(label = "Chrome Work", packageName = "com.lu4p.chrome", profileKey = "42")
        val personalFavorite =
            FavoriteApp(label = "Chrome", packageName = "com.lu4p.chrome", profileKey = "0")
        val favoritesFlow = kotlinx.coroutines.flow.MutableStateFlow(listOf(personalFavorite, workFavorite))
        every { preferencesManager.favoritesFlow } returns favoritesFlow

        val viewModel = createViewModel()
        val collectJob = CoroutineScope(testDispatcher).launch { viewModel.favorites.collect { } }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.hideApp(workFavorite)
        repeat(5) { testDispatcher.scheduler.advanceUntilIdle() }

        coVerify {
            appRepository.hideApp("com.lu4p.chrome", "42", HOST_APP_METADATA_SENTINEL)
        }
        // hideApp() delegates to appRepository.hideApp() — the favorites flow
        // reactively filters hidden apps via Room; setFavorites is NOT called here.
        collectJob.cancel()
    }

    @Test
    fun `initial state has formatted date`() {
        val viewModel = createViewModel(contextForClockAndBattery(mockBatteryStickyIntent()))
        testDispatcher.scheduler.advanceTimeBy(1100)

        val state = viewModel.clockUiState.value
        assertTrue(state.currentDate.isNotEmpty())
    }

    @Test
    fun `formatted date does not duplicate dots in German`() {
        val date = Date()
        val pattern = DateFormat.getBestDateTimePattern(Locale.GERMAN, "EEE d MMM")
        val expected = SimpleDateFormat(pattern, Locale.GERMAN).format(date).replace(",", "").replace(Regex("\\s+"), " ").trim()

        val formatted = formatCompactDate(date, Locale.GERMAN)

        assertEquals(expected, formatted)
    }

    @Test
    fun `formatted date uses locale best pattern in Polish`() {
        val polish = Locale.forLanguageTag("pl")
        val date = Date()
        val pattern = DateFormat.getBestDateTimePattern(polish, "EEE d MMM")
        val expected = SimpleDateFormat(pattern, polish).format(date).replace(",", "").replace(Regex("\\s+"), " ").trim()

        val formatted = formatCompactDate(date, polish)

        assertEquals(expected, formatted)
    }

    @Test
    fun `formatted date uses locale best pattern in English`() {
        val english = Locale.ENGLISH
        val date = Date()
        val pattern = DateFormat.getBestDateTimePattern(english, "EEE d MMM")
        val expected = SimpleDateFormat(pattern, english).format(date).replace(",", "").replace(Regex("\\s+"), " ").trim()

        val formatted = formatCompactDate(date, english)

        assertEquals(expected, formatted)
    }

    @Test
    fun `formatted date does not contain commas`() {
        val formatted = formatCompactDate(Date(), Locale.ENGLISH)

        assertFalse(formatted.contains(","))
    }

    @Test
    fun `formatHomeDate US slashes uses MM slash dd slash yyyy`() {
        val cal =
                Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    set(2026, Calendar.APRIL, 7, 12, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
        val date = cal.time
        val formatted = formatHomeDate(date, Locale.US, HomeDateFormatStyle.US_SLASHES)
        assertEquals("04/07/2026", formatted)
    }

    @Test
    fun `formatHomeDate EU slashes uses dd slash MM slash yyyy`() {
        val cal =
                Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    set(2026, Calendar.APRIL, 7, 12, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
        val date = cal.time
        val formatted = formatHomeDate(date, Locale.UK, HomeDateFormatStyle.EU_SLASHES)
        assertEquals("07/04/2026", formatted)
    }

    @Test
    fun `formatHomeDate EU dots uses dd dot MM dot yyyy`() {
        val cal =
                Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    set(2026, Calendar.APRIL, 7, 12, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
        val date = cal.time
        val formatted = formatHomeDate(date, Locale.GERMANY, HomeDateFormatStyle.EU_DOTS)
        assertEquals("07.04.2026", formatted)
    }

    @Test
    fun `formatHomeDate month long uses full month and comma`() {
        val cal =
                Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    set(2026, Calendar.APRIL, 7, 12, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
        val date = cal.time
        val formatted = formatHomeDate(date, Locale.US, HomeDateFormatStyle.MONTH_LONG)
        assertEquals("April 7, 2026", formatted)
    }

    @Test
    fun `formatHomeDate weekday abbrev matches fixed calendar US locale`() {
        val cal =
                Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    set(2026, Calendar.APRIL, 7, 12, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
        val date = cal.time
        val formatted = formatHomeDate(date, Locale.US, HomeDateFormatStyle.WEEKDAY_MONTH_ABBR)
        assertEquals("Tue Apr 7, 2026", formatted)
    }

    @Test
    fun `favorites flow emits from preferences manager`() {
        // The favorites flow filters by installed apps; mock the repository to return
        // apps matching the test favorites so they survive the filter.
        every { appRepository.getInstalledApps() } returns listOf(
            AppInfo("com.lu4p.music", "Music", null),
            AppInfo("com.lu4p.work", "Work", null),
            AppInfo("com.lu4p.social", "Social", null),
        )
        every { appRepository.getArchivedApps() } returns emptyList()

        val viewModel = createViewModel()
        val collected = mutableListOf<List<FavoriteApp>>()
        val collectJob = CoroutineScope(testDispatcher).launch {
            viewModel.favorites.collect { collected += it }
        }
        // Drain multiple rounds: ViewModel init dispatches refreshInstalledApps on
        // Dispatchers.IO and the favorites flow needs the installed-apps snapshot
        // before it can emit non-empty results.
        repeat(5) { testDispatcher.scheduler.advanceUntilIdle() }
        // Allow the IO-thread refreshInstalledApps to complete.
        Thread.sleep(200)
        repeat(5) { testDispatcher.scheduler.advanceUntilIdle() }

        val favorites = collected.lastOrNull().orEmpty()
        assertTrue("Expected at least 3 favorites but got ${favorites.size}", favorites.size >= 3)
        assertEquals("Music", favorites[0].label)
        assertEquals("com.lu4p.music", favorites[0].packageName)
        collectJob.cancel()
    }

    @Test
    fun `launchFavorite delegates to repository for primary profile app target`() {
        val viewModel = createViewModel()

        viewModel.launchFavorite(
                FavoriteApp(label = "Music", packageName = "com.lu4p.music", iconName = "music")
        )

        verify { appRepository.launchApp("com.lu4p.music") }
    }

    @Test
    fun `refreshBattery updates battery percentage`() {
        val batteryIntent = mockk<Intent>(relaxed = true)
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns 50
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1) } returns 100
        stubNullReceiverBatterySticky(batteryIntent)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceTimeBy(100)

        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns 30
        viewModel.refreshBattery()

        assertEquals(30, viewModel.clockUiState.value.batteryPercent)
    }

    @Test
    fun `battery handles missing intent gracefully`() {
        stubNullReceiverBatterySticky(null)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceTimeBy(100)

        assertEquals(0, viewModel.clockUiState.value.batteryPercent)
    }

    @Test
    fun `initial state reads battery from sticky system broadcast`() {
        val batteryIntent = Intent(Intent.ACTION_BATTERY_CHANGED).apply {
            putExtra(BatteryManager.EXTRA_LEVEL, 42)
            putExtra(BatteryManager.EXTRA_SCALE, 100)
        }
        val mockCtx = mockk<Context>(relaxed = true)
        every { mockCtx.registerReceiver(null, any()) } returns batteryIntent
        every { mockCtx.registerReceiver(null, any(), any()) } returns batteryIntent
        every { mockCtx.registerReceiver(null, any(), any(), any()) } returns batteryIntent
        every { mockCtx.registerReceiver(null, any(), any(), any(), any()) } returns batteryIntent

        val viewModel = createViewModel(mockCtx)
        testDispatcher.scheduler.advanceTimeBy(100)

        assertEquals(42, viewModel.clockUiState.value.batteryPercent)
    }

    @Test
    fun `isDefaultLauncher is false when not the default home app`() {
        // With relaxed mocks, resolveActivity returns a mock whose packageName
        // won't match our package, so isDefaultLauncher should be false.
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceTimeBy(100)

        assertFalse(viewModel.uiState.value.isDefaultLauncher)
    }

    @Test
    fun `isDefaultLauncher is true when package manager resolves home to app package`() {
        val mockCtx = mockk<Context>(relaxed = true)
        val packageManager = mockk<PackageManager>(relaxed = true)
        every { mockCtx.packageManager } returns packageManager
        every { mockCtx.packageName } returns "nwd.fokuslauncher"
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = "nwd.fokuslauncher"
                name = "nwd.fokuslauncher.MainActivity"
            }
        }
        every {
            packageManager.resolveActivity(any(), any<Int>())
        } returns resolveInfo
        val roleManager = mockk<android.app.role.RoleManager>(relaxed = true)
        every { roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_HOME) } returns true
        every { mockCtx.getSystemService(android.app.role.RoleManager::class.java) } returns roleManager
        every { mockCtx.getSystemService(Context.ROLE_SERVICE) } returns roleManager

        val viewModel = createViewModel(mockCtx)
        testDispatcher.scheduler.advanceTimeBy(100)

        assertTrue(viewModel.uiState.value.isDefaultLauncher)
    }

    @Test
    fun `refreshInstalledApps removes uninstalled favorites`() {
        every { appRepository.getInstalledApps() } returns listOf(
            AppInfo(packageName = "com.lu4p.music", label = "Music", icon = null)
        )
        val viewModel = createViewModel()
        val collectJob = CoroutineScope(testDispatcher).launch {
            viewModel.favorites.collect { }
        }
        testDispatcher.scheduler.runCurrent()

        viewModel.refreshInstalledApps()
        verify(timeout = 2_000) { appRepository.invalidateCache() }
        coVerify(timeout = 2_000) {
            preferencesManager.setFavorites(
                match { favorites ->
                    favorites.size == 1 && favorites[0].packageName == "com.lu4p.music"
                }
            )
        }
        collectJob.cancel()
    }

    @Test
    fun `refreshInstalledApps does not clear favorites when launcher query is empty`() {
        every { appRepository.getInstalledApps() } returns emptyList()
        val viewModel = createViewModel()
        val collectJob = CoroutineScope(testDispatcher).launch {
            viewModel.favorites.collect { }
        }
        testDispatcher.scheduler.runCurrent()

        viewModel.refreshInstalledApps()
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 0) { preferencesManager.setFavorites(any()) }
        collectJob.cancel()
    }

    @Test
    fun `init does not prune favorites when launcher query is empty`() {
        every { appRepository.getInstalledApps() } returns emptyList()
        createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 0) {
            preferencesManager.setFavorites(match { it.size < testFavorites.size })
        }
    }

    @Test
    fun `refreshInstalledApps keeps favorites absent from partial snapshot when still launchable`() {
        every { appRepository.getInstalledApps() } returns listOf(
            AppInfo(packageName = "com.lu4p.music", label = "Music", icon = null)
        )
        every { appRepository.getLaunchableAppKeys(setOf("0")) } returns setOf(
            appMetadataKey("com.lu4p.work", "0"),
            appMetadataKey("com.lu4p.social", "0")
        )
        val viewModel = createViewModel()
        val collectJob = CoroutineScope(testDispatcher).launch {
            viewModel.favorites.collect { }
        }
        testDispatcher.scheduler.runCurrent()

        viewModel.refreshInstalledApps()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { preferencesManager.setFavorites(any()) }
        verify(atLeast = 1) { appRepository.invalidateCache() }
        verify { appRepository.getLaunchableAppKeys(setOf("0")) }
        collectJob.cancel()
    }

    @Test
    fun `removed package disappears from favorites immediately`() {
        createViewModel()
        testDispatcher.scheduler.runCurrent()

        removedPackages.tryEmit(RemovedApp(packageName = "com.lu4p.music", profileKey = "0"))
        testDispatcher.scheduler.runCurrent()

        coVerify {
            preferencesManager.setFavorites(
                match { favorites ->
                    favorites.none { it.packageName == "com.lu4p.music" } &&
                        favorites.size == 2
                }
            )
        }
    }

    @Test
    fun `removed package only clears matching favorite profile`() {
        every { preferencesManager.favoritesFlow } returns flowOf(
            listOf(
                FavoriteApp(label = "Music", packageName = "com.lu4p.music", iconName = "music", profileKey = "0"),
                FavoriteApp(label = "Music Work", packageName = "com.lu4p.music", iconName = "music", profileKey = "42")
            )
        )
        createViewModel()
        testDispatcher.scheduler.runCurrent()

        removedPackages.tryEmit(RemovedApp(packageName = "com.lu4p.music", profileKey = "42"))
        testDispatcher.scheduler.runCurrent()

        coVerify {
            preferencesManager.setFavorites(
                match { favorites ->
                    favorites.size == 1 &&
                        favorites.single().packageName == "com.lu4p.music" &&
                        favorites.single().profileKey == "0"
                }
            )
        }
    }

    @Test
    fun `toggleAppOnHomeScreen uses profile aware rename key`() {
        val workHandle = mockk<android.os.UserHandle>()
        every { workHandle.hashCode() } returns 42
        every { appRepository.getAllRenamedApps() } returns
            flowOf(listOf(nwd.fokuslauncher.data.database.entity.RenamedAppEntity(
                packageName = "com.lu4p.chrome",
                profileKey = "42",
                customName = "Chrome Work Custom"
            )))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleAppOnHomeScreen(
            AppInfo(
                packageName = "com.lu4p.chrome",
                label = "Chrome Work",
                icon = null,
                userHandle = workHandle
            )
        )

        assertEquals("Chrome Work Custom", viewModel.editFavorites.value.single().label)
        assertEquals("42", viewModel.editFavorites.value.single().profileKey)
    }

    @Test
    fun `toggleAppOnHomeScreen removes work profile app with componentName`() {
        val workHandle = mockk<android.os.UserHandle>()
        every { workHandle.hashCode() } returns 42
        val workApp =
                AppInfo(
                        packageName = "com.lu4p.chrome",
                        label = "Chrome Work",
                        icon = null,
                        userHandle = workHandle,
                        componentName = ComponentName("com.lu4p.chrome", "MainActivity"),
                )

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleAppOnHomeScreen(workApp)
        assertEquals(1, viewModel.editFavorites.value.size)
        assertEquals("42", viewModel.editFavorites.value.single().profileKey)

        viewModel.toggleAppOnHomeScreen(workApp)
        assertTrue(viewModel.editFavorites.value.isEmpty())
    }

    @Test
    fun `toggleAppOnHomeScreen can add browser and PWA without replacing`() {
        val browser = AppInfo("org.mozilla.firefox", "Firefox", null)
        val pwa =
                AppInfo(
                        packageName = "org.mozilla.firefox",
                        label = "Twitter",
                        icon = null,
                        launcherShortcutId = "pwa-twitter",
                )
        every { appRepository.getInstalledApps() } returns listOf(browser, pwa)

        val viewModel = createViewModel()
        viewModel.startEditingHomeApps()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleAppOnHomeScreen(browser)
        viewModel.toggleAppOnHomeScreen(pwa)

        assertEquals(2, viewModel.editFavorites.value.size)
        assertEquals(
                setOf(
                        favoriteAppStableKey(
                                FavoriteApp(
                                        label = "Firefox",
                                        packageName = "org.mozilla.firefox",
                                        profileKey = "0",
                                )
                        ),
                        favoriteAppStableKey(
                                FavoriteApp(
                                        label = "Twitter",
                                        packageName = "org.mozilla.firefox",
                                        iconPackage =
                                                ShortcutTarget.encode(
                                                        ShortcutTarget.LauncherShortcut(
                                                                "org.mozilla.firefox",
                                                                "pwa-twitter",
                                                        )
                                                ),
                                        profileKey = "0",
                                )
                        ),
                ),
                viewModel.editFavorites.value.map { favoriteAppStableKey(it) }.toSet(),
        )
    }

    @Test
    fun `removeFavoriteFromEdit removes built-in phone favorite without installed app`() {
        val phoneFavorite =
                FavoriteApp(
                        label = "Health",
                        packageName = ShortcutTarget.PHONE_FAVORITE_SENTINEL_PACKAGE,
                        iconName = "call",
                        iconPackage = ShortcutTarget.encode(ShortcutTarget.PhoneDial),
                )
        every { preferencesManager.favoritesFlow } returns
                flowOf(testFavorites + phoneFavorite)

        val viewModel = createViewModel()
        val collectJob = CoroutineScope(testDispatcher).launch { viewModel.favorites.collect { } }
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.startEditingHomeApps()

        assertEquals(4, viewModel.editFavorites.value.size)

        viewModel.removeFavoriteFromEdit(phoneFavorite)

        assertEquals(3, viewModel.editFavorites.value.size)
        assertTrue(
                viewModel.editFavorites.value.none {
                    it.packageName == ShortcutTarget.PHONE_FAVORITE_SENTINEL_PACKAGE
                }
        )
        collectJob.cancel()
    }

    @Test
    fun `refreshInstalledApps prunes favorites missing from one profile only`() {
        val testFavs = listOf(
            FavoriteApp(label = "Chrome", packageName = "com.lu4p.chrome", iconName = "circle", profileKey = "0"),
            FavoriteApp(label = "Chrome Work", packageName = "com.lu4p.chrome", iconName = "circle", profileKey = "42")
        )
        every { preferencesManager.favoritesFlow } returns flowOf(testFavs)
        every { appRepository.getInstalledApps() } returns
            listOf(AppInfo(packageName = "com.lu4p.chrome", label = "Chrome", icon = null))
        every { appRepository.getLaunchableAppKeys(setOf("com.lu4p.chrome#42")) } returns emptySet()

        val viewModel = createViewModel()
        val collectJob = CoroutineScope(testDispatcher).launch { viewModel.favorites.collect { } }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.refreshInstalledApps()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            preferencesManager.setFavorites(
                match { favorites ->
                    favorites.size == 1 &&
                        favorites.single().packageName == "com.lu4p.chrome" &&
                        favorites.single().profileKey == "0"
                }
            )
        }
        collectJob.cancel()
    }

    @Test
    fun `openClockApp launches clock safely`() {
        val viewModel = createViewModel()
        viewModel.openClockApp()
        
        // Either AlarmClock or DeskClock gets started. Since mock is relaxed, it doesn't crash.
        verify(atLeast = 1) { context.startActivity(any()) }
    }

    @Test
    fun `launchShortcut handles App target`() {
        val viewModel = createViewModel()
        viewModel.launchShortcut(HomeShortcut(target = ShortcutTarget.App("com.lu4p.music")))

        verify { appRepository.launchApp("com.lu4p.music") }
    }

    @Test
    fun `onDoubleTapEmptyLock calls lockScreenIfPossible when enabled`() {
        var lockScreenCalled = false
        LockScreenHelper.accessibilityEnabledOverride = { true }
        LockScreenHelper.lockScreenOverride = {
            lockScreenCalled = true
            true
        }
        every { preferencesManager.doubleTapEmptyLockFlow } returns flowOf(true)

        val viewModel = createViewModel()
        viewModel.onDoubleTapEmptyLock()
        testDispatcher.scheduler.runCurrent()

        assertTrue(lockScreenCalled)
        LockScreenHelper.accessibilityEnabledOverride = null
        LockScreenHelper.lockScreenOverride = null
    }
}
