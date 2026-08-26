// port-lint: tests naive/datetime/tests.rs
package io.github.kotlinmania.chrono.naive

import io.github.kotlinmania.chrono.TimeDelta
import io.github.kotlinmania.chrono.offset.FixedOffset
import io.github.kotlinmania.chrono.offset.MappedLocalTime
import io.github.kotlinmania.chrono.offset.Utc
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NaiveDateTimeTest {
    @Test
    fun testDatetimeAdd() {
        fun check(
            y: Int,
            m: UInt,
            d: UInt,
            h: UInt,
            n: UInt,
            s: UInt,
            rhs: TimeDelta,
            expectedY: Int?,
            expectedM: UInt?,
            expectedD: UInt?,
            expectedH: UInt?,
            expectedN: UInt?,
            expectedS: UInt?,
        ) {
            val lhs = NaiveDate.fromYmdOpt(y, m, d)!!.andHmsOpt(h, n, s)!!
            val sum = if (expectedY != null) {
                NaiveDate.fromYmdOpt(expectedY, expectedM!!, expectedD!!)!!.andHmsOpt(
                    expectedH!!,
                    expectedN!!,
                    expectedS!!,
                )
            } else null
            assertEquals(sum, lhs.checkedAddSigned(rhs))
            assertEquals(sum, lhs.checkedSubSigned(-rhs))
        }

        fun seconds(s: Long) = TimeDelta.trySeconds(s)!!

        check(2014, 5u, 6u, 7u, 8u, 9u, seconds(3600 + 60 + 1), 2014, 5u, 6u, 8u, 9u, 10u)
        check(2014, 5u, 6u, 7u, 8u, 9u, seconds(-(3600 + 60 + 1)), 2014, 5u, 6u, 6u, 7u, 8u)
        check(2014, 5u, 6u, 7u, 8u, 9u, seconds(86399), 2014, 5u, 7u, 7u, 8u, 8u)
        check(2014, 5u, 6u, 7u, 8u, 9u, seconds(86_400 * 10), 2014, 5u, 16u, 7u, 8u, 9u)
        check(2014, 5u, 6u, 7u, 8u, 9u, seconds(-86_400 * 10), 2014, 4u, 26u, 7u, 8u, 9u)

        val maxDaysFromYear0 = NaiveDate.MAX.signedDurationSince(NaiveDate.fromYmdOpt(0, 1u, 1u)!!)
        check(0, 1u, 1u, 0u, 0u, 0u, maxDaysFromYear0, NaiveDate.MAX.year(), 12u, 31u, 0u, 0u, 0u)
        check(
            0,
            1u,
            1u,
            0u,
            0u,
            0u,
            maxDaysFromYear0 + seconds(86399),
            NaiveDate.MAX.year(),
            12u,
            31u,
            23u,
            59u,
            59u,
        )
        check(0, 1u, 1u, 0u, 0u, 0u, maxDaysFromYear0 + seconds(86_400), null, null, null, null, null, null)
        check(0, 1u, 1u, 0u, 0u, 0u, TimeDelta.MAX, null, null, null, null, null, null)

        val minDaysFromYear0 = NaiveDate.MIN.signedDurationSince(NaiveDate.fromYmdOpt(0, 1u, 1u)!!)
        check(0, 1u, 1u, 0u, 0u, 0u, minDaysFromYear0, NaiveDate.MIN.year(), 1u, 1u, 0u, 0u, 0u)
        check(0, 1u, 1u, 0u, 0u, 0u, minDaysFromYear0 - seconds(1), null, null, null, null, null, null)
        check(0, 1u, 1u, 0u, 0u, 0u, TimeDelta.MIN, null, null, null, null, null, null)
    }

    @Test
    fun testDatetimeSub() {
        fun ymdhms(y: Int, m: UInt, d: UInt, h: UInt, n: UInt, s: UInt): NaiveDateTime =
            NaiveDate.fromYmdOpt(y, m, d)!!.andHmsOpt(h, n, s)!!

        val since = { lhs: NaiveDateTime, rhs: NaiveDateTime -> lhs.signedDurationSince(rhs) }
        assertEquals(TimeDelta.zero(), since(ymdhms(2014, 5u, 6u, 7u, 8u, 9u), ymdhms(2014, 5u, 6u, 7u, 8u, 9u)))
        assertEquals(
            TimeDelta.trySeconds(1)!!,
            since(ymdhms(2014, 5u, 6u, 7u, 8u, 10u), ymdhms(2014, 5u, 6u, 7u, 8u, 9u)),
        )
        assertEquals(
            TimeDelta.trySeconds(-1)!!,
            since(ymdhms(2014, 5u, 6u, 7u, 8u, 9u), ymdhms(2014, 5u, 6u, 7u, 8u, 10u)),
        )
        assertEquals(
            TimeDelta.trySeconds(86399)!!,
            since(ymdhms(2014, 5u, 7u, 7u, 8u, 9u), ymdhms(2014, 5u, 6u, 7u, 8u, 10u)),
        )
        assertEquals(
            TimeDelta.trySeconds(999_999_999)!!,
            since(ymdhms(2001, 9u, 9u, 1u, 46u, 39u), ymdhms(1970, 1u, 1u, 0u, 0u, 0u)),
        )
    }

    @Test
    fun testDatetimeAddSubInvariant() {
        val base = NaiveDate.fromYmdOpt(2000, 1u, 1u)!!.andHmsOpt(0u, 0u, 0u)!!
        val t = -946684799990000L
        val time = base + TimeDelta.microseconds(t)
        assertEquals(t, time.signedDurationSince(base).numMicroseconds())
    }

    @Test
    fun testAndLocalTimezone() {
        val ndt = NaiveDate.fromYmdOpt(2022, 6u, 15u)!!.andHmsOpt(18u, 59u, 36u)!!
        val dtUtc = ndt.andUtc()
        assertEquals(ndt, dtUtc.naiveLocal())
        assertEquals(Utc, dtUtc.timezone())

        val offsetTz = FixedOffset.westOpt(4 * 3600)!!
        val dtOffset = ndt.andLocalTimezone(offsetTz).unwrap()
        assertEquals(ndt, dtOffset.naiveLocal())
        assertEquals(offsetTz, dtOffset.timezone())
    }

    @Test
    fun testAndUtc() {
        val ndt = NaiveDate.fromYmdOpt(2023, 1u, 30u)!!.andHmsOpt(19u, 32u, 33u)!!
        val dtUtc = ndt.andUtc()
        assertEquals(ndt, dtUtc.naiveLocal())
        assertEquals(Utc, dtUtc.timezone())
    }

    @Test
    fun testCheckedAddOffset() {
        fun ymdhmsm(y: Int, m: UInt, d: UInt, h: UInt, mn: UInt, s: UInt, mi: UInt): NaiveDateTime? =
            NaiveDate.fromYmdOpt(y, m, d)?.andHmsMilliOpt(h, mn, s, mi)

        val positiveOffset = FixedOffset.eastOpt(2 * 60 * 60)!!
        val dt = ymdhmsm(2023, 5u, 5u, 20u, 10u, 0u, 0u)!!
        assertEquals(ymdhmsm(2023, 5u, 5u, 22u, 10u, 0u, 0u), dt.checkedAddOffset(positiveOffset))

        val dtLeap = ymdhmsm(2023, 6u, 30u, 23u, 59u, 59u, 1000u)!!
        assertEquals(ymdhmsm(2023, 7u, 1u, 1u, 59u, 59u, 1000u), dtLeap.checkedAddOffset(positiveOffset))

        assertNull(NaiveDateTime.MAX.checkedAddOffset(positiveOffset))

        val negativeOffset = FixedOffset.westOpt(2 * 60 * 60)!!
        assertEquals(ymdhmsm(2023, 5u, 5u, 18u, 10u, 0u, 0u), dt.checkedAddOffset(negativeOffset))
        assertEquals(ymdhmsm(2023, 6u, 30u, 21u, 59u, 59u, 1000u), dtLeap.checkedAddOffset(negativeOffset))
        assertNull(NaiveDateTime.MIN.checkedAddOffset(negativeOffset))
    }

    @Test
    fun testCheckedSubOffset() {
        fun ymdhmsm(y: Int, m: UInt, d: UInt, h: UInt, mn: UInt, s: UInt, mi: UInt): NaiveDateTime? =
            NaiveDate.fromYmdOpt(y, m, d)?.andHmsMilliOpt(h, mn, s, mi)

        val positiveOffset = FixedOffset.eastOpt(2 * 60 * 60)!!
        val dt = ymdhmsm(2023, 5u, 5u, 20u, 10u, 0u, 0u)!!
        assertEquals(ymdhmsm(2023, 5u, 5u, 18u, 10u, 0u, 0u), dt.checkedSubOffset(positiveOffset))

        val dtLeap = ymdhmsm(2023, 6u, 30u, 23u, 59u, 59u, 1000u)!!
        assertEquals(ymdhmsm(2023, 6u, 30u, 21u, 59u, 59u, 1000u), dtLeap.checkedSubOffset(positiveOffset))
        assertNull(NaiveDateTime.MIN.checkedSubOffset(positiveOffset))

        val negativeOffset = FixedOffset.westOpt(2 * 60 * 60)!!
        assertEquals(ymdhmsm(2023, 5u, 5u, 22u, 10u, 0u, 0u), dt.checkedSubOffset(negativeOffset))
        assertEquals(ymdhmsm(2023, 7u, 1u, 1u, 59u, 59u, 1000u), dtLeap.checkedSubOffset(negativeOffset))
        assertNull(NaiveDateTime.MAX.checkedSubOffset(negativeOffset))

        assertEquals(dt + positiveOffset, dt.checkedAddOffset(positiveOffset))
        assertEquals(dt - positiveOffset, dt.checkedSubOffset(positiveOffset))
    }

    @Test
    fun testAndTimezoneMinMaxDates() {
        for (offsetHour in -23..23) {
            val offset = FixedOffset.eastOpt(offsetHour * 60 * 60)!!

            val localMax = NaiveDateTime.MAX.andLocalTimezone(offset)
            if (offsetHour >= 0) {
                assertEquals(NaiveDateTime.MAX, localMax.unwrap().naiveLocal())
            } else {
                assertEquals(MappedLocalTime.None, localMax)
            }

            val localMin = NaiveDateTime.MIN.andLocalTimezone(offset)
            if (offsetHour <= 0) {
                assertEquals(NaiveDateTime.MIN, localMin.unwrap().naiveLocal())
            } else {
                assertEquals(MappedLocalTime.None, localMin)
            }
        }
    }
}
