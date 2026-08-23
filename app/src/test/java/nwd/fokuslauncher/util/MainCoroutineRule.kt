package nwd.fokuslauncher.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * A JUnit [TestWatcher] that replaces [Dispatchers.Main] with a
 * [TestDispatcher] for the duration of each test.
 *
 * By default uses [UnconfinedTestDispatcher], which executes coroutines
 * eagerly so state updates are visible immediately after the triggering
 * call — assertions do not need `advanceUntilIdle()`.
 *
 * Usage:
 * ```kotlin
 * @get:Rule
 * val mainCoroutineRule = MainCoroutineRule()
 *
 * // Dispatchers.Main is replaced for the whole test class.
 * // ViewModel constructors that accept a CoroutineDispatcher can receive
 * // mainCoroutineRule.testDispatcher directly.
 * ```
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainCoroutineRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) {
        super.starting(description)
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        super.finished(description)
        Dispatchers.resetMain()
    }
}
