// port-lint: tests naive/time/tests.rs
package io.github.kotlinmania.chrono.naive

import io.github.kotlinmania.chrono.TimeDelta
import io.github.kotlinmania.chrono.offset.FixedOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NaiveTimeTest {
    @Test
    fun testTimeFromHmsMilli() {
        assertEquals(
            NaiveTime.fromHmsMilliOpt(3u, 5u, 7u, 0u),
            NaiveTime.fromHmsNanoOpt(3u, 5u, 7u, 0u),
        )
        assertEquals(
            NaiveTime.fromHmsMilliOpt(3u, 5u, 7u, 777u),
            NaiveTime.fromHmsNanoOpt(3u, 5u, 7u, 777_000_000u),
        )
        assertEquals(
            NaiveTime.fromHmsMilliOpt(3u, 5u, 59u, 1_999u),
            NaiveTime.fromHmsNanoOpt(3u, 5u, 59u, 1_999_000_000u),
        )
        assertNull(NaiveTime.fromHmsMilliOpt(3u, 5u, 59u, 2_000u))
        assertNull(NaiveTime.fromHmsMilliOpt(3u, 5u, 59u, 5_000u)) // overflow check
        assertNull(NaiveTime.fromHmsMilliOpt(3u, 5u, 59u, UInt.MAX_VALUE))
    }

    @Test
    fun testTimeFromHmsMicro() {
        assertEquals(
            NaiveTime.fromHmsMicroOpt(3u, 5u, 7u, 0u),
            NaiveTime.fromHmsNanoOpt(3u, 5u, 7u, 0u),
        )
        assertEquals(
            NaiveTime.fromHmsMicroOpt(3u, 5u, 7u, 333u),
            NaiveTime.fromHmsNanoOpt(3u, 5u, 7u, 333_000u),
        )
        assertEquals(
            NaiveTime.fromHmsMicroOpt(3u, 5u, 7u, 777_777u),
            NaiveTime.fromHmsNanoOpt(3u, 5u, 7u, 777_777_000u),
        )
        assertEquals(
            NaiveTime.fromHmsMicroOpt(3u, 5u, 59u, 1_999_999u),
            NaiveTime.fromHmsNanoOpt(3u, 5u, 59u, 1_999_999_000u),
        )
        assertNull(NaiveTime.fromHmsMicroOpt(3u, 5u, 59u, 2_000_000u))
        assertNull(NaiveTime.fromHmsMicroOpt(3u, 5u, 59u, 5_000_000u)) // overflow check
        assertNull(NaiveTime.fromHmsMicroOpt(3u, 5u, 59u, UInt.MAX_VALUE))
    }

    @Test
    fun testTimeHms() {
        val t = NaiveTime.fromHmsOpt(3u, 5u, 7u)!!
        assertEquals(3u, t.hour())
        assertEquals(NaiveTime.fromHmsOpt(0u, 5u, 7u), t.withHour(0u))
        assertEquals(NaiveTime.fromHmsOpt(23u, 5u, 7u), t.withHour(23u))
        assertNull(t.withHour(24u))
        assertNull(t.withHour(UInt.MAX_VALUE))

        assertEquals(5u, t.minute())
        assertEquals(NaiveTime.fromHmsOpt(3u, 0u, 7u), t.withMinute(0u))
        assertEquals(NaiveTime.fromHmsOpt(3u, 59u, 7u), t.withMinute(59u))
        assertNull(t.withMinute(60u))
        assertNull(t.withMinute(UInt.MAX_VALUE))

        assertEquals(7u, t.second())
        assertEquals(NaiveTime.fromHmsOpt(3u, 5u, 0u), t.withSecond(0u))
        assertEquals(NaiveTime.fromHmsOpt(3u, 5u, 59u), t.withSecond(59u))
        assertNull(t.withSecond(60u))
        assertNull(t.withSecond(UInt.MAX_VALUE))
    }

    @Test
    fun testTimeAdd() {
        fun hmsm(h: UInt, m: UInt, s: UInt, ms: UInt): NaiveTime =
            NaiveTime.fromHmsMilliOpt(h, m, s, ms)!!

        assertEquals(hmsm(3u, 5u, 59u, 900u), hmsm(3u, 5u, 59u, 900u) + TimeDelta.zero())
        assertEquals(hmsm(3u, 6u, 0u, 0u), hmsm(3u, 5u, 59u, 900u) + TimeDelta.tryMilliseconds(100)!!)
        assertEquals(hmsm(3u, 5u, 58u, 500u), hmsm(3u, 5u, 59u, 1300u) + TimeDelta.tryMilliseconds(-1800)!!)
        assertEquals(hmsm(3u, 5u, 59u, 500u), hmsm(3u, 5u, 59u, 1300u) + TimeDelta.tryMilliseconds(-800)!!)
        assertEquals(hmsm(3u, 5u, 59u, 1200u), hmsm(3u, 5u, 59u, 1300u) + TimeDelta.tryMilliseconds(-100)!!)
        assertEquals(hmsm(3u, 5u, 59u, 1400u), hmsm(3u, 5u, 59u, 1300u) + TimeDelta.tryMilliseconds(100)!!)
        assertEquals(hmsm(3u, 6u, 0u, 100u), hmsm(3u, 5u, 59u, 1300u) + TimeDelta.tryMilliseconds(800)!!)
        assertEquals(hmsm(3u, 6u, 1u, 100u), hmsm(3u, 5u, 59u, 1300u) + TimeDelta.tryMilliseconds(1800)!!)
        assertEquals(hmsm(3u, 5u, 58u, 900u), hmsm(3u, 5u, 59u, 900u) + TimeDelta.trySeconds(86399)!!)
        assertEquals(hmsm(3u, 6u, 0u, 900u), hmsm(3u, 5u, 59u, 900u) + TimeDelta.trySeconds(-86399)!!)
        assertEquals(hmsm(3u, 5u, 59u, 900u), hmsm(3u, 5u, 59u, 900u) + TimeDelta.tryDays(12345)!!)
        assertEquals(hmsm(3u, 5u, 59u, 300u), hmsm(3u, 5u, 59u, 1300u) + TimeDelta.tryDays(1)!!)
        assertEquals(hmsm(3u, 6u, 0u, 300u), hmsm(3u, 5u, 59u, 1300u) + TimeDelta.tryDays(-1)!!)

        // regression tests for #37
        assertEquals(hmsm(23u, 59u, 59u, 10u), hmsm(0u, 0u, 0u, 0u) + TimeDelta.tryMilliseconds(-990)!!)
        assertEquals(hmsm(23u, 59u, 50u, 10u), hmsm(0u, 0u, 0u, 0u) + TimeDelta.tryMilliseconds(-9990)!!)
    }

    @Test
    fun testTimeOverflowingAdd() {
        fun hmsm(h: UInt, m: UInt, s: UInt, ms: UInt): NaiveTime =
            NaiveTime.fromHmsMilliOpt(h, m, s, ms)!!

        assertEquals(
            Pair(hmsm(14u, 4u, 5u, 678u), 0L),
            hmsm(3u, 4u, 5u, 678u).overflowingAddSigned(TimeDelta.tryHours(11)!!),
        )
        assertEquals(
            Pair(hmsm(2u, 4u, 5u, 678u), 86_400L),
            hmsm(3u, 4u, 5u, 678u).overflowingAddSigned(TimeDelta.tryHours(23)!!),
        )
        assertEquals(
            Pair(hmsm(20u, 4u, 5u, 678u), -86_400L),
            hmsm(3u, 4u, 5u, 678u).overflowingAddSigned(TimeDelta.tryHours(-7)!!),
        )

        assertEquals(
            Pair(hmsm(3u, 4u, 59u, 678u), 86_400L),
            hmsm(3u, 4u, 59u, 1678u).overflowingAddSigned(TimeDelta.tryDays(1)!!),
        )
        assertEquals(
            Pair(hmsm(3u, 5u, 0u, 678u), -86_400L),
            hmsm(3u, 4u, 59u, 1678u).overflowingAddSigned(TimeDelta.tryDays(-1)!!),
        )
    }

    @Test
    fun testTimeSub() {
        fun hmsm(h: UInt, m: UInt, s: UInt, ms: UInt): NaiveTime =
            NaiveTime.fromHmsMilliOpt(h, m, s, ms)!!

        fun check(lhs: NaiveTime, rhs: NaiveTime, diff: TimeDelta) {
            assertEquals(diff, lhs.signedDurationSince(rhs))
            assertEquals(-diff, rhs.signedDurationSince(lhs))
        }

        check(hmsm(3u, 5u, 7u, 900u), hmsm(3u, 5u, 7u, 900u), TimeDelta.zero())
        check(hmsm(3u, 5u, 7u, 900u), hmsm(3u, 5u, 7u, 600u), TimeDelta.tryMilliseconds(300)!!)
        check(hmsm(3u, 5u, 7u, 200u), hmsm(2u, 4u, 6u, 200u), TimeDelta.trySeconds(3600 + 60 + 1)!!)
        check(
            hmsm(3u, 5u, 7u, 200u),
            hmsm(2u, 4u, 6u, 300u),
            TimeDelta.trySeconds(3600 + 60)!! + TimeDelta.tryMilliseconds(900)!!,
        )

        check(hmsm(3u, 6u, 0u, 200u), hmsm(3u, 5u, 59u, 1800u), TimeDelta.tryMilliseconds(400)!!)
        assertEquals(hmsm(3u, 5u, 7u, 200u), hmsm(3u, 5u, 6u, 800u) + TimeDelta.tryMilliseconds(400)!!)
    }

    @Test
    fun testOverflowingOffset() {
        fun hmsm(h: UInt, m: UInt, s: UInt, n: UInt): NaiveTime =
            NaiveTime.fromHmsMilliOpt(h, m, s, n)!!

        val positiveOffset = FixedOffset.eastOpt(4 * 60 * 60)!!
        val t = hmsm(5u, 6u, 7u, 890u)
        assertEquals(Pair(hmsm(9u, 6u, 7u, 890u), 0), t.overflowingAddOffset(positiveOffset))
        assertEquals(Pair(hmsm(1u, 6u, 7u, 890u), 0), t.overflowingSubOffset(positiveOffset))

        val tLeap = hmsm(23u, 59u, 59u, 1000u)
        assertEquals(Pair(hmsm(3u, 59u, 59u, 1000u), 1), tLeap.overflowingAddOffset(positiveOffset))
        assertEquals(Pair(hmsm(19u, 59u, 59u, 1000u), 0), tLeap.overflowingSubOffset(positiveOffset))

        val tPrev = hmsm(1u, 2u, 3u, 456u)
        assertEquals(Pair(hmsm(21u, 2u, 3u, 456u), -1), tPrev.overflowingSubOffset(positiveOffset))

        val negativeOffset = FixedOffset.westOpt(((2 * 60) + 3) * 60 + 4)!!
        assertEquals(Pair(hmsm(3u, 3u, 3u, 890u), 0), t.overflowingAddOffset(negativeOffset))
        assertEquals(Pair(hmsm(7u, 9u, 11u, 890u), 0), t.overflowingSubOffset(negativeOffset))
    }
}
