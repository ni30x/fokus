package nwd.fokuslauncher.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * A JUnit [TestWatcher] that replaces [Dispatchers.Main] with a controlled
 * [UnconfinedTestDispatcher] for the duration of each test.
 *
 * [UnconfinedTestDispatcher] executes coroutines eagerly (no virtual-time
 * advancement required), making it ideal for ViewModel tests that push state
 * through [StateFlow] and [SharedFlow].  State updates are visible immediately
 * after the triggering call, so assertions do not need
 * `advanceUntilIdle()`.
 *
 * Usage:
 * ```kotlin
 * @get:Rule
 * val mainCoroutineRule = MainCoroutineRule()
 *
 * // Now Dispatchers.Main is replaced for the whole test class.
 * // ViewModel constructors that accept a CoroutineDispatcher can receive
 * // mainCoroutineRule.testDispatcher directly.
 * ```
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainCoroutineRule(
    val testDispatcher: UnconfinedTestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
