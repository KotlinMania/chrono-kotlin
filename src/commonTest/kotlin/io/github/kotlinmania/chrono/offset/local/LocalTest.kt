// port-lint: tests offset/local/mod.rs
package io.github.kotlinmania.chrono.offset.local

import io.github.kotlinmania.chrono.naive.Days
import io.github.kotlinmania.chrono.naive.NaiveDate
import io.github.kotlinmania.chrono.naive.NaiveDateTime
import io.github.kotlinmania.chrono.offset.FixedOffset
import io.github.kotlinmania.chrono.offset.MappedLocalTime
import io.github.kotlinmania.chrono.offset.Utc
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalTest {
    @Test
    fun verifyCorrectOffsets() {
        val now = Local.now()
        val fromLocal = Local.fromLocalDatetime(now.naiveLocal()).unwrap()
        val fromUtc = Local.fromUtcDatetime(now.naiveUtc())

        assertEquals(
            now.offset().localMinusUtc(),
            fromLocal.offset().localMinusUtc(),
        )
        assertEquals(now.offset().localMinusUtc(), fromUtc.offset().localMinusUtc())

        assertEquals(now, fromLocal)
        assertEquals(now, fromUtc)
    }

    @Test
    fun verifyCorrectOffsetsDistantPast() {
        val distantPast = Local.now() - Days.new(365uL * 500uL)
        val fromLocal = Local.fromLocalDatetime(distantPast.naiveLocal()).unwrap()
        val fromUtc = Local.fromUtcDatetime(distantPast.naiveUtc())

        assertEquals(
            distantPast.offset().localMinusUtc(),
            fromLocal.offset().localMinusUtc(),
        )
        assertEquals(
            distantPast.offset().localMinusUtc(),
            fromUtc.offset().localMinusUtc(),
        )

        assertEquals(distantPast, fromLocal)
        assertEquals(distantPast, fromUtc)
    }

    @Test
    fun verifyCorrectOffsetsDistantFuture() {
        val distantFuture = Local.now() + Days.new(365uL * 35000uL)
        val fromLocal = Local.fromLocalDatetime(distantFuture.naiveLocal()).unwrap()
        val fromUtc = Local.fromUtcDatetime(distantFuture.naiveUtc())

        assertEquals(
            distantFuture.offset().localMinusUtc(),
            fromLocal.offset().localMinusUtc(),
        )
        assertEquals(
            distantFuture.offset().localMinusUtc(),
            fromUtc.offset().localMinusUtc(),
        )

        assertEquals(distantFuture, fromLocal)
        assertEquals(distantFuture, fromUtc)
    }

    @Test
    fun testLocalDateSanityCheck() {
        // issue #27
        assertEquals(28u, Local.withYmdAndHms(2999, 12u, 28u, 0u, 0u, 0u).unwrap().day())
    }

    @Test
    fun testLeapSecond() {
        // issue #123
        val today = Utc.now().dateNaive()

        val dt = today.andHmsMilliOpt(15u, 2u, 59u, 1000u)
        if (dt != null) {
            val timestr = dt.time().toString()
            // the OS API may or may not support the leap second,
            // but there are only two sensible options.
            assertTrue(
                timestr == "15:02:60" || timestr == "15:03:00",
                "unexpected timestr $timestr",
            )
        }

        val dt2 = today.andHmsMilliOpt(15u, 2u, 3u, 1234u)
        if (dt2 != null) {
            val timestr = dt2.time().toString()
            assertTrue(
                timestr == "15:02:03.234" || timestr == "15:02:04.234",
                "unexpected timestr $timestr",
            )
        }
    }

    @Test
    fun testLookupWithDstTransitions() {
        fun ymdhms(
            y: Int,
            m: UInt,
            d: UInt,
            h: UInt,
            n: UInt,
            s: UInt,
        ): NaiveDateTime =
            NaiveDate.fromYmdOpt(y, m, d)!!.andHmsOpt(h, n, s)!!

        fun compareLookup(
            transitions: Array<Transition>,
            y: Int,
            m: UInt,
            d: UInt,
            h: UInt,
            n: UInt,
            s: UInt,
            result: MappedLocalTime<FixedOffset>,
        ) {
            val dt = NaiveDate.fromYmdOpt(y, m, d)!!.andHmsOpt(h, n, s)!!
            assertEquals(result, lookupWithDstTransitions(transitions, dt))
        }

        // dst transition before std transition
        // dst offset > std offset
        val std1 = FixedOffset.eastOpt(3 * 60 * 60)!!
        val dst1 = FixedOffset.eastOpt(4 * 60 * 60)!!
        val transitions1 =
            arrayOf(
                Transition.new(ymdhms(2023, 3u, 26u, 2u, 0u, 0u), std1, dst1),
                Transition.new(ymdhms(2023, 10u, 29u, 3u, 0u, 0u), dst1, std1),
            )
        compareLookup(transitions1, 2023, 3u, 26u, 1u, 0u, 0u, MappedLocalTime.Single(std1))
        compareLookup(transitions1, 2023, 3u, 26u, 2u, 0u, 0u, MappedLocalTime.Single(std1))
        compareLookup(transitions1, 2023, 3u, 26u, 2u, 30u, 0u, MappedLocalTime.None)
        compareLookup(transitions1, 2023, 3u, 26u, 3u, 0u, 0u, MappedLocalTime.Single(dst1))
        compareLookup(transitions1, 2023, 3u, 26u, 4u, 0u, 0u, MappedLocalTime.Single(dst1))

        compareLookup(transitions1, 2023, 10u, 29u, 1u, 0u, 0u, MappedLocalTime.Single(dst1))
        compareLookup(transitions1, 2023, 10u, 29u, 2u, 0u, 0u, MappedLocalTime.Ambiguous(dst1, std1))
        compareLookup(transitions1, 2023, 10u, 29u, 2u, 30u, 0u, MappedLocalTime.Ambiguous(dst1, std1))
        compareLookup(transitions1, 2023, 10u, 29u, 3u, 0u, 0u, MappedLocalTime.Ambiguous(dst1, std1))
        compareLookup(transitions1, 2023, 10u, 29u, 4u, 0u, 0u, MappedLocalTime.Single(std1))

        // std transition before dst transition
        // dst offset > std offset
        val std2 = FixedOffset.eastOpt(-5 * 60 * 60)!!
        val dst2 = FixedOffset.eastOpt(-4 * 60 * 60)!!
        val transitions2 =
            arrayOf(
                Transition.new(ymdhms(2023, 3u, 24u, 3u, 0u, 0u), dst2, std2),
                Transition.new(ymdhms(2023, 10u, 27u, 2u, 0u, 0u), std2, dst2),
            )
        compareLookup(transitions2, 2023, 3u, 24u, 1u, 0u, 0u, MappedLocalTime.Single(dst2))
        compareLookup(transitions2, 2023, 3u, 24u, 2u, 0u, 0u, MappedLocalTime.Ambiguous(dst2, std2))
        compareLookup(transitions2, 2023, 3u, 24u, 2u, 30u, 0u, MappedLocalTime.Ambiguous(dst2, std2))
        compareLookup(transitions2, 2023, 3u, 24u, 3u, 0u, 0u, MappedLocalTime.Ambiguous(dst2, std2))
        compareLookup(transitions2, 2023, 3u, 24u, 4u, 0u, 0u, MappedLocalTime.Single(std2))

        compareLookup(transitions2, 2023, 10u, 27u, 1u, 0u, 0u, MappedLocalTime.Single(std2))
        compareLookup(transitions2, 2023, 10u, 27u, 2u, 0u, 0u, MappedLocalTime.Single(std2))
        compareLookup(transitions2, 2023, 10u, 27u, 2u, 30u, 0u, MappedLocalTime.None)
        compareLookup(transitions2, 2023, 10u, 27u, 3u, 0u, 0u, MappedLocalTime.Single(dst2))
        compareLookup(transitions2, 2023, 10u, 27u, 4u, 0u, 0u, MappedLocalTime.Single(dst2))

        // dst transition before std transition
        // dst offset < std offset
        val std3 = FixedOffset.eastOpt(3 * 60 * 60)!!
        val dst3 = FixedOffset.eastOpt((2 * 60 + 30) * 60)!!
        val transitions3 =
            arrayOf(
                Transition.new(ymdhms(2023, 3u, 26u, 2u, 30u, 0u), std3, dst3),
                Transition.new(ymdhms(2023, 10u, 29u, 2u, 0u, 0u), dst3, std3),
            )
        compareLookup(transitions3, 2023, 3u, 26u, 1u, 0u, 0u, MappedLocalTime.Single(std3))
        compareLookup(transitions3, 2023, 3u, 26u, 2u, 0u, 0u, MappedLocalTime.Ambiguous(std3, dst3))
        compareLookup(transitions3, 2023, 3u, 26u, 2u, 15u, 0u, MappedLocalTime.Ambiguous(std3, dst3))
        compareLookup(transitions3, 2023, 3u, 26u, 2u, 30u, 0u, MappedLocalTime.Ambiguous(std3, dst3))
        compareLookup(transitions3, 2023, 3u, 26u, 3u, 0u, 0u, MappedLocalTime.Single(dst3))

        compareLookup(transitions3, 2023, 10u, 29u, 1u, 0u, 0u, MappedLocalTime.Single(dst3))
        compareLookup(transitions3, 2023, 10u, 29u, 2u, 0u, 0u, MappedLocalTime.Single(dst3))
        compareLookup(transitions3, 2023, 10u, 29u, 2u, 15u, 0u, MappedLocalTime.None)
        compareLookup(transitions3, 2023, 10u, 29u, 2u, 30u, 0u, MappedLocalTime.Single(std3))
        compareLookup(transitions3, 2023, 10u, 29u, 3u, 0u, 0u, MappedLocalTime.Single(std3))

        // std transition before dst transition
        // dst offset < std offset
        val std4 = FixedOffset.eastOpt(-(4 * 60 + 30) * 60)!!
        val dst4 = FixedOffset.eastOpt(-5 * 60 * 60)!!
        val transitions4 =
            arrayOf(
                Transition.new(ymdhms(2023, 3u, 24u, 2u, 0u, 0u), dst4, std4),
                Transition.new(ymdhms(2023, 10u, 27u, 2u, 30u, 0u), std4, dst4),
            )
        compareLookup(transitions4, 2023, 3u, 24u, 1u, 0u, 0u, MappedLocalTime.Single(dst4))
        compareLookup(transitions4, 2023, 3u, 24u, 2u, 0u, 0u, MappedLocalTime.Single(dst4))
        compareLookup(transitions4, 2023, 3u, 24u, 2u, 15u, 0u, MappedLocalTime.None)
        compareLookup(transitions4, 2023, 3u, 24u, 2u, 30u, 0u, MappedLocalTime.Single(std4))
        compareLookup(transitions4, 2023, 3u, 24u, 3u, 0u, 0u, MappedLocalTime.Single(std4))

        compareLookup(transitions4, 2023, 10u, 27u, 1u, 0u, 0u, MappedLocalTime.Single(std4))
        compareLookup(transitions4, 2023, 10u, 27u, 2u, 0u, 0u, MappedLocalTime.Ambiguous(std4, dst4))
        compareLookup(transitions4, 2023, 10u, 27u, 2u, 15u, 0u, MappedLocalTime.Ambiguous(std4, dst4))
        compareLookup(transitions4, 2023, 10u, 27u, 2u, 30u, 0u, MappedLocalTime.Ambiguous(std4, dst4))
        compareLookup(transitions4, 2023, 10u, 27u, 3u, 0u, 0u, MappedLocalTime.Single(dst4))

        // offset stays the same
        val std5 = FixedOffset.eastOpt(3 * 60 * 60)!!
        val transitions5 =
            arrayOf(
                Transition.new(ymdhms(2023, 3u, 26u, 2u, 0u, 0u), std5, std5),
                Transition.new(ymdhms(2023, 10u, 29u, 3u, 0u, 0u), std5, std5),
            )
        compareLookup(transitions5, 2023, 3u, 26u, 2u, 0u, 0u, MappedLocalTime.Single(std5))
        compareLookup(transitions5, 2023, 10u, 29u, 3u, 0u, 0u, MappedLocalTime.Single(std5))

        // single transition
        val std6 = FixedOffset.eastOpt(3 * 60 * 60)!!
        val dst6 = FixedOffset.eastOpt(4 * 60 * 60)!!
        val transitions6 = arrayOf(Transition.new(ymdhms(2023, 3u, 26u, 2u, 0u, 0u), std6, dst6))
        compareLookup(transitions6, 2023, 3u, 26u, 1u, 0u, 0u, MappedLocalTime.Single(std6))
        compareLookup(transitions6, 2023, 3u, 26u, 2u, 0u, 0u, MappedLocalTime.Single(std6))
        compareLookup(transitions6, 2023, 3u, 26u, 2u, 30u, 0u, MappedLocalTime.None)
        compareLookup(transitions6, 2023, 3u, 26u, 3u, 0u, 0u, MappedLocalTime.Single(dst6))
        compareLookup(transitions6, 2023, 3u, 26u, 4u, 0u, 0u, MappedLocalTime.Single(dst6))
    }

    @Test
    fun testLookupWithDstTransitionsLimits() {
        // Transition beyond UTC year end doesn't panic in year of `NaiveDate::MAX`
        val stdEast = FixedOffset.eastOpt(3 * 60 * 60)!!
        val dstEast = FixedOffset.eastOpt(4 * 60 * 60)!!
        val transitionsMax =
            arrayOf(
                Transition.new(NaiveDateTime.MAX.withMonth(7u)!!, stdEast, dstEast),
                Transition.new(NaiveDateTime.MAX, dstEast, stdEast),
            )
        assertEquals(
            MappedLocalTime.Single(stdEast),
            lookupWithDstTransitions(transitionsMax, NaiveDateTime.MAX.withMonth(3u)!!),
        )
        assertEquals(
            MappedLocalTime.Single(dstEast),
            lookupWithDstTransitions(transitionsMax, NaiveDateTime.MAX.withMonth(8u)!!),
        )
        // Doesn't panic with `NaiveDateTime::MAX` as argument (which would be out of range when
        // converted to UTC).
        assertEquals(
            MappedLocalTime.Ambiguous(dstEast, stdEast),
            lookupWithDstTransitions(transitionsMax, NaiveDateTime.MAX),
        )

        // Transition before UTC year end doesn't panic in year of `NaiveDate::MIN`
        val stdWest = FixedOffset.westOpt(3 * 60 * 60)!!
        val dstWest = FixedOffset.westOpt(4 * 60 * 60)!!
        val transitionsMin =
            arrayOf(
                Transition.new(NaiveDateTime.MIN, stdWest, dstWest),
                Transition.new(NaiveDateTime.MIN.withMonth(6u)!!, dstWest, stdWest),
            )
        assertEquals(
            MappedLocalTime.Single(dstWest),
            lookupWithDstTransitions(transitionsMin, NaiveDateTime.MIN.withMonth(3u)!!),
        )
        assertEquals(
            MappedLocalTime.Single(stdWest),
            lookupWithDstTransitions(transitionsMin, NaiveDateTime.MIN.withMonth(8u)!!),
        )
        // Doesn't panic with `NaiveDateTime::MIN` as argument (which would be out of range when
        // converted to UTC).
        assertEquals(
            MappedLocalTime.Ambiguous(stdWest, dstWest),
            lookupWithDstTransitions(transitionsMin, NaiveDateTime.MIN),
        )
    }

    // test_rkyv_validation: rkyv zero-copy archival is a Rust-specific serialization crate not applicable to Kotlin.
}
