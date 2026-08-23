package nwd.fokuslauncher.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * A JUnit [TestWatcher] that replaces [Dispatchers.Main] with a
 * [StandardTestDispatcher] for the duration of each test.
 *
 * [StandardTestDispatcher] queues coroutines without executing them eagerly.
 * You must call [advanceUntilIdle][kotlinx.coroutines.test.TestCoroutineScheduler.advanceUntilIdle]
 * (or [runTest][kotlinx.coroutines.test.runTest]) to flush pending work
 * before asserting.
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
    val testDispatcher: StandardTestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
