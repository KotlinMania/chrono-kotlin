// port-lint: tests datetime/tests.rs
package io.github.kotlinmania.chrono

import io.github.kotlinmania.chrono.naive.NaiveDate
import io.github.kotlinmania.chrono.naive.NaiveDateTime
import io.github.kotlinmania.chrono.offset.FixedOffset
import io.github.kotlinmania.chrono.offset.Utc
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DateTimeTest {
    @Test
    fun testDatetimeFromTimestampMillis() {
        val validMap = listOf(
            Pair(1662921288000L, "2022-09-11 18:34:48"),
            Pair(1662921288123L, "2022-09-11 18:34:48.123"),
            Pair(0L, "1970-01-01 00:00:00"),
            Pair(119731017000L, "1973-10-17 18:36:57"),
            Pair(1234567890000L, "2009-02-13 23:31:30"),
            Pair(2034061609000L, "2034-06-16 09:06:49"),
        )

        for ((timestampMillis, _) in validMap) {
            val dt = DateTime.fromTimestampMillis(timestampMillis)!!
            assertEquals(timestampMillis, dt.timestampMillis())
        }

        assertNull(DateTime.fromTimestampMillis(Long.MAX_VALUE))
        assertNull(DateTime.fromTimestampMillis(Long.MIN_VALUE))

        val secsTest = listOf(0L, 1L, 2L, 1000L, 1234L, 12345678L, -1L, -2L, -1000L, -12345678L)
        for (secs in secsTest) {
            assertEquals(
                DateTime.fromTimestampMillis(secs * 1000L),
                DateTime.fromTimestampSecs(secs),
            )
        }
    }

    @Test
    fun testDatetimeFromTimestampMicros() {
        val validMap = listOf(
            Pair(1662921288000000L, "2022-09-11 18:34:48"),
            Pair(1662921288123456L, "2022-09-11 18:34:48.123456"),
            Pair(0L, "1970-01-01 00:00:00"),
            Pair(119731017000000L, "1973-10-17 18:36:57"),
            Pair(1234567890000000L, "2009-02-13 23:31:30"),
            Pair(2034061609000000L, "2034-06-16 09:06:49"),
        )

        for ((timestampMicros, _) in validMap) {
            val dt = DateTime.fromTimestampMicros(timestampMicros)!!
            assertEquals(timestampMicros, dt.timestampMicros())
        }

        assertNull(DateTime.fromTimestampMicros(Long.MAX_VALUE))
        assertNull(DateTime.fromTimestampMicros(Long.MIN_VALUE))
    }

    @Test
    fun testDatetimeFromTimestampNanos() {
        val validMap = listOf(
            1662921288000000000L,
            1662921288123456000L,
            1662921288123456789L,
            0L,
            1234567890000000000L,
        )

        for (timestampNanos in validMap) {
            val dt = DateTime.fromTimestampNanos(timestampNanos)
            assertEquals(timestampNanos, dt.timestampNanosOpt())
        }
    }

    @Test
    fun testWithTimezone() {
        val dtUtc = DateTime.fromTimestampSecs(0L)!!
        val offset = FixedOffset.eastOpt(5 * 3600)!!
        val dtOffset = dtUtc.withTimezone(offset)

        assertEquals(dtUtc.timestamp(), dtOffset.timestamp())
        assertEquals(5u, dtOffset.hour())
        assertEquals(0u, dtOffset.minute())
        assertEquals(0u, dtOffset.second())
        assertEquals(1970, dtOffset.year())
        assertEquals(1u, dtOffset.month())
        assertEquals(1u, dtOffset.day())
    }

    @Test
    fun testSignedDurationSince() {
        val dt1 = DateTime.fromTimestampSecs(100L)!!
        val dt2 = DateTime.fromTimestampSecs(250L)!!

        assertEquals(TimeDelta.trySeconds(150)!!, dt2.signedDurationSince(dt1))
        assertEquals(TimeDelta.trySeconds(-150)!!, dt1.signedDurationSince(dt2))
        assertEquals(TimeDelta.trySeconds(150)!!, dt2 - dt1)
    }

    @Test
    fun testAddSubTimeDelta() {
        val dt = DateTime.fromTimestampSecs(1000L)!!
        val dtPlus = dt + TimeDelta.trySeconds(500)!!
        assertEquals(1500L, dtPlus.timestamp())

        val dtMinus = dt - TimeDelta.trySeconds(300)!!
        assertEquals(700L, dtMinus.timestamp())
    }
}
