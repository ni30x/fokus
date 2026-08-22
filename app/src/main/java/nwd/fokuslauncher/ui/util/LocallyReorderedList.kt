package nwd.fokuslauncher.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue

/**
 * Local copy of a reorderable list for drag gestures.
 *
 * Updates immediately on each adjacent swap (so [VerticalSlotReorderState] indices stay
 * consistent), then commits the full order when the drag ends. Syncs from [source] when
 * not dragging so external changes still apply.
 *
 * Same pattern as Edit home (in-memory list during drag) and category reorder — do not
 * write prefs on every mid-drag swap or the list will jump under the drag index.
 */
@Stable
class LocallyReorderedListState<T> internal constructor(
        initial: List<T>,
) {
    var items by mutableStateOf(initial)
        private set

    var isDragging by mutableStateOf(false)
        private set

    fun syncFromSource(source: List<T>) {
        if (!isDragging) items = source
    }

    fun onDragStart() {
        isDragging = true
    }

    fun reorder(from: Int, to: Int) {
        if (from == to) return
        if (from !in items.indices || to !in items.indices) return
        val next = items.toMutableList()
        val item = next.removeAt(from)
        next.add(to, item)
        items = next
    }

    fun onDragEnd(commit: (List<T>) -> Unit) {
        val snapshot = items
        isDragging = false
        commit(snapshot)
    }
}

@Composable
fun <T> rememberLocallyReorderedList(source: List<T>): LocallyReorderedListState<T> {
    val state = remember { LocallyReorderedListState(source) }
    val latestSource by rememberUpdatedState(source)
    LaunchedEffect(source) { state.syncFromSource(latestSource) }
    return state
}
