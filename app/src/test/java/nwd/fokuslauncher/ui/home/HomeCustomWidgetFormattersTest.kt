package nwd.fokuslauncher.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class HomeCustomWidgetFormattersTest {

    @Test
    fun `localDatePartsToEpochDay is stable for known UTC date`() {
        assertEquals(1L, localDatePartsToEpochDay(1970, Calendar.JANUARY, 2))
        assertEquals(0L, localDatePartsToEpochDay(1970, Calendar.JANUARY, 1))
    }

    @Test
    fun `millisUntilCountdown is signed difference`() {
        val target = 1_000_000L
        assertEquals(500L, millisUntilCountdown(target, 999_500L))
        assertEquals(-100L, millisUntilCountdown(target, 1_000_100L))
    }

    @Test
    fun `formatCountdownRemaining uses compact thresholds`() {
        fun fmt(ms: Long) =
                formatCountdownRemaining(ms, nowLabel = "Now", pastLabel = "Passed")

        assertEquals("Passed", fmt(-1))
        assertEquals("Now", fmt(500))
        assertEquals("45s", fmt(TimeUnit.SECONDS.toMillis(45)))
        assertEquals("5m 30s", fmt(TimeUnit.MINUTES.toMillis(5) + TimeUnit.SECONDS.toMillis(30)))
        assertEquals("15m", fmt(TimeUnit.MINUTES.toMillis(15)))
        assertEquals("2h 15m", fmt(TimeUnit.HOURS.toMillis(2) + TimeUnit.MINUTES.toMillis(15)))
        assertEquals("12h", fmt(TimeUnit.HOURS.toMillis(12)))
        assertEquals("3d 4h", fmt(TimeUnit.DAYS.toMillis(3) + TimeUnit.HOURS.toMillis(4)))
        assertEquals("20d", fmt(TimeUnit.DAYS.toMillis(20)))
        assertEquals("7d", fmt(TimeUnit.DAYS.toMillis(7)))
        // Under 7 days still shows hours.
        assertEquals("6d 23h", fmt(TimeUnit.DAYS.toMillis(6) + TimeUnit.HOURS.toMillis(23)))
    }

    @Test
    fun `localDateTimeToEpochMillis round trips through epochMillisToLocalParts`() {
        val tz = TimeZone.getTimeZone("Europe/Berlin")
        val millis = localDateTimeToEpochMillis(2026, Calendar.JULY, 9, 14, 30, tz)
        val parts = epochMillisToLocalParts(millis, tz)
        assertEquals(2026, parts.year)
        assertEquals(Calendar.JULY, parts.monthZeroBased)
        assertEquals(9, parts.dayOfMonth)
        assertEquals(14, parts.hourOfDay)
        assertEquals(30, parts.minute)
    }

    @Test
    fun `epochDay round trip year month day`() {
        val day = localDatePartsToEpochDay(2026, Calendar.MAY, 12)
        val (y, m, d) = epochDayToYearMonthDay(day)
        assertEquals(2026, y)
        assertEquals(Calendar.MAY, m)
        assertEquals(12, d)
    }
}
