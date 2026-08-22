package nwd.fokuslauncher.data.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * A home countdown target (title + absolute instant).
 *
 * [targetEpochMillis] is the absolute instant (Unix epoch millis) of the event when the user picked
 * date + time of day in the device's default timezone.
 */
data class CountdownEvent(
        val id: String,
        val title: String,
        val targetEpochMillis: Long,
)

const val COUNTDOWN_TITLE_MAX_LENGTH = 40

fun normalizeCountdownEvents(events: List<CountdownEvent>): List<CountdownEvent> {
    val seen = mutableSetOf<String>()
    return events.mapNotNull { event ->
        val id = event.id.trim()
        val title = event.title.trim().take(COUNTDOWN_TITLE_MAX_LENGTH)
        if (id.isBlank() || title.isBlank() || !seen.add(id)) null
        else event.copy(id = id, title = title)
    }
}

fun serializeCountdownEvents(events: List<CountdownEvent>): String {
    val normalized = normalizeCountdownEvents(events)
    if (normalized.isEmpty()) return ""
    val array = JSONArray()
    normalized.forEach { event ->
        array.put(
                JSONObject()
                        .put("id", event.id)
                        .put("title", event.title)
                        .put("targetEpochMillis", event.targetEpochMillis),
        )
    }
    return array.toString()
}

/**
 * Parses countdown storage. Supports a JSON array of events and a legacy single-object value.
 */
fun parseCountdownEvents(raw: String): List<CountdownEvent> {
    if (raw.isBlank()) return emptyList()
    return try {
        val trimmed = raw.trim()
        when {
            trimmed.startsWith("[") -> {
                val array = JSONArray(trimmed)
                buildList {
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        parseCountdownEventObject(obj)?.let { add(it) }
                    }
                }.let(::normalizeCountdownEvents)
            }
            trimmed.startsWith("{") -> {
                parseCountdownEventObject(JSONObject(trimmed))?.let { listOf(it) }.orEmpty()
            }
            else -> emptyList()
        }
    } catch (_: Exception) {
        emptyList()
    }
}

/** @deprecated Prefer [parseCountdownEvents]; kept for single-event call sites/tests. */
fun parseCountdownEvent(raw: String): CountdownEvent? = parseCountdownEvents(raw).firstOrNull()

/** @deprecated Prefer [serializeCountdownEvents]. */
fun serializeCountdownEvent(event: CountdownEvent?): String =
        if (event == null) "" else serializeCountdownEvents(listOf(event))

private fun parseCountdownEventObject(obj: JSONObject): CountdownEvent? {
    val id = obj.optString("id").trim()
    val title = obj.optString("title").trim().take(COUNTDOWN_TITLE_MAX_LENGTH)
    if (id.isBlank() || title.isBlank()) return null
    val millis = obj.optLong("targetEpochMillis", Long.MIN_VALUE)
    if (millis != Long.MIN_VALUE) {
        return CountdownEvent(id = id, title = title, targetEpochMillis = millis)
    }
    // Legacy day-only events → local midnight of that civil day.
    val epochDay = obj.optLong("targetEpochDay", Long.MIN_VALUE)
    if (epochDay == Long.MIN_VALUE) return null
    return CountdownEvent(
            id = id,
            title = title,
            targetEpochMillis = legacyEpochDayToLocalMidnightMillis(epochDay),
    )
}

/** Converts a UTC civil epoch-day to local midnight millis (legacy migration). */
internal fun legacyEpochDayToLocalMidnightMillis(epochDay: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    utc.timeInMillis = TimeUnit.DAYS.toMillis(epochDay)
    val year = utc.get(Calendar.YEAR)
    val month = utc.get(Calendar.MONTH)
    val day = utc.get(Calendar.DAY_OF_MONTH)
    val local = Calendar.getInstance()
    local.clear()
    local.set(year, month, day, 0, 0, 0)
    local.set(Calendar.MILLISECOND, 0)
    return local.timeInMillis
}
