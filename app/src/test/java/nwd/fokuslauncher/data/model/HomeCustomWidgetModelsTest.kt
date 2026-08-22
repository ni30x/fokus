package nwd.fokuslauncher.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class WorldClockCityTest {

    @Test
    fun `parse blank returns empty list`() {
        assertTrue(parseWorldClockCities("").isEmpty())
        assertTrue(parseWorldClockCities("   ").isEmpty())
    }

    @Test
    fun `serialize and parse round trips cities by position`() {
        val cities =
                listOf(
                        WorldClockCity("b", "LON", "Europe/London", 1),
                        WorldClockCity("a", "NYC", "America/New_York", 0),
                        WorldClockCity("c", "TYO", "Asia/Tokyo", 2),
                        WorldClockCity("d", "Extra", "UTC", 3),
                )

        val parsed = parseWorldClockCities(serializeWorldClockCities(cities))
        assertEquals(4, parsed.size)
        assertEquals(listOf("a", "b", "c", "d"), parsed.map { it.id })
        assertEquals(listOf(0, 1, 2, 3), parsed.map { it.position })
    }

    @Test
    fun `parse invalid json returns empty list`() {
        assertTrue(parseWorldClockCities("{bad").isEmpty())
    }

    @Test
    fun `parse drops invalid entries`() {
        val raw =
                """
                [
                  {"id":"","label":"X","timeZoneId":"UTC"},
                  {"id":"ok","label":"NYC","timeZoneId":"America/New_York","position":0}
                ]
                """.trimIndent()
        val parsed = parseWorldClockCities(raw)
        assertEquals(1, parsed.size)
        assertEquals("ok", parsed[0].id)
    }

    @Test
    fun `formatUtcOffsetLabel formats zero and signed offsets`() {
        assertEquals("UTC", formatUtcOffsetLabel("UTC"))
        // Fixed-offset zones avoid DST flakiness in CI.
        assertEquals("UTC+05:30", formatUtcOffsetLabel("Asia/Kolkata"))
        assertEquals("UTC-05:00", formatUtcOffsetLabel("America/Lima"))
    }

    @Test
    fun `defaultLabelForTimeZoneId prefers city style name`() {
        val name = defaultLabelForTimeZoneId("America/New_York", Locale.US)
        // ICU exemplar ("New York") or IANA leaf fallback — never the raw id with slash.
        assertTrue(name.isNotBlank())
        assertTrue(!name.contains('/'))
        assertTrue(name.contains("New York", ignoreCase = true) || name == "New_York")
        assertEquals("UTC", defaultLabelForTimeZoneId("UTC", Locale.US))
    }

    @Test
    fun `ianaLeafLabel strips region prefix`() {
        assertEquals("New York", ianaLeafLabel("America/New_York"))
        assertEquals("Ho Chi Minh", ianaLeafLabel("Asia/Ho_Chi_Minh"))
        assertEquals("UTC", ianaLeafLabel("UTC"))
    }

    @Test
    fun `listSystemTimeZonePickerEntries includes common zones with offsets`() {
        val entries = listSystemTimeZonePickerEntries(locale = Locale.US)
        assertTrue(entries.any { it.timeZoneId == "Europe/Berlin" })
        assertTrue(entries.any { it.timeZoneId == "UTC" })
        val berlin = entries.first { it.timeZoneId == "Europe/Berlin" }
        assertTrue(berlin.utcOffsetLabel.startsWith("UTC"))
        assertTrue(berlin.displayName.isNotBlank())
        // Sorted by offset ascending.
        assertTrue(entries.zipWithNext().all { (a, b) -> a.rawOffsetMillis <= b.rawOffsetMillis })
    }

    @Test
    fun `filterTimeZonePickerEntries matches id offset and label`() {
        val entries = listSystemTimeZonePickerEntries(locale = Locale.US)
        assertTrue(filterTimeZonePickerEntries(entries, "berlin").any { it.timeZoneId == "Europe/Berlin" })
        assertTrue(filterTimeZonePickerEntries(entries, "UTC+").isNotEmpty())
        assertTrue(filterTimeZonePickerEntries(entries, "zzzz-no-match").isEmpty())
    }
}

@RunWith(RobolectricTestRunner::class)
class CountdownEventTest {

    @Test
    fun `parse blank returns null`() {
        assertNull(parseCountdownEvent(""))
        assertNull(parseCountdownEvent("   "))
    }

    @Test
    fun `serialize and parse round trips`() {
        val event =
                CountdownEvent(
                        id = "trip",
                        title = "Berlin",
                        targetEpochMillis = 1_780_000_000_000L,
                )
        assertEquals(event, parseCountdownEvent(serializeCountdownEvent(event)))
    }

    @Test
    fun `serialize and parse round trips multiple events`() {
        val events =
                listOf(
                        CountdownEvent("a", "One", 1_000L),
                        CountdownEvent("b", "Two", 2_000L),
                )
        assertEquals(events, parseCountdownEvents(serializeCountdownEvents(events)))
    }

    @Test
    fun `serialize null returns empty`() {
        assertEquals("", serializeCountdownEvent(null))
    }

    @Test
    fun `parse invalid json returns null`() {
        assertNull(parseCountdownEvent("{bad"))
    }

    @Test
    fun `parse migrates legacy targetEpochDay to local midnight millis`() {
        val raw =
                """{"id":"legacy","title":"Trip","targetEpochDay":20000}"""
        val parsed = parseCountdownEvent(raw)!!
        assertEquals("legacy", parsed.id)
        assertEquals("Trip", parsed.title)
        assertEquals(legacyEpochDayToLocalMidnightMillis(20_000L), parsed.targetEpochMillis)
    }

    @Test
    fun `parseCountdownEvents migrates legacy single object`() {
        val raw = """{"id":"solo","title":"Trip","targetEpochMillis":123}"""
        val parsed = parseCountdownEvents(raw)
        assertEquals(1, parsed.size)
        assertEquals("solo", parsed[0].id)
    }
}
