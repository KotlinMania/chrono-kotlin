// port-lint: tests naive/datetime/tests.rs
package io.github.kotlinmania.chrono.naive

import io.github.kotlinmania.chrono.DateTime
import io.github.kotlinmania.chrono.TimeDelta
import io.github.kotlinmania.chrono.naive.serde.TsMicroseconds
import io.github.kotlinmania.chrono.naive.serde.TsMicrosecondsOption
import io.github.kotlinmania.chrono.naive.serde.TsMilliseconds
import io.github.kotlinmania.chrono.naive.serde.TsMillisecondsOption
import io.github.kotlinmania.chrono.naive.serde.TsNanoseconds
import io.github.kotlinmania.chrono.naive.serde.TsNanosecondsOption
import io.github.kotlinmania.chrono.naive.serde.TsSeconds
import io.github.kotlinmania.chrono.naive.serde.TsSecondsOption
import io.github.kotlinmania.chrono.offset.FixedOffset
import io.github.kotlinmania.chrono.offset.MappedLocalTime
import io.github.kotlinmania.chrono.offset.Utc
import io.github.kotlinmania.serdetest.Token
import io.github.kotlinmania.serdetest.TokenSerializer
import io.github.kotlinmania.serdetest.assertDeTokens
import io.github.kotlinmania.serdetest.assertDeTokensError
import io.github.kotlinmania.serdetest.assertSerTokens
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
            val sum =
                if (expectedY != null) {
                    NaiveDate.fromYmdOpt(expectedY, expectedM!!, expectedD!!)!!.andHmsOpt(
                        expectedH!!,
                        expectedN!!,
                        expectedS!!,
                    )
                } else {
                    null
                }
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

    @Test
    fun testSerdeSerialize() {
        assertSerTokens(
            NaiveDate.fromYmdOpt(2016, 7u, 8u)!!.andHmsMilliOpt(9u, 10u, 48u, 90u)!!,
            listOf(Token.Str("2016-07-08T09:10:48.090")),
        )
        assertSerTokens(
            NaiveDate.fromYmdOpt(2014, 7u, 24u)!!.andHmsOpt(12u, 34u, 6u)!!,
            listOf(Token.Str("2014-07-24T12:34:06")),
        )
        assertSerTokens(
            NaiveDate.fromYmdOpt(0, 1u, 1u)!!.andHmsMilliOpt(0u, 0u, 59u, 1_000u)!!,
            listOf(Token.Str("0000-01-01T00:00:60")),
        )
        assertSerTokens(
            NaiveDate.fromYmdOpt(-1, 12u, 31u)!!.andHmsNanoOpt(23u, 59u, 59u, 7u)!!,
            listOf(Token.Str("-0001-12-31T23:59:59.000000007")),
        )
        assertSerTokens(
            NaiveDate.MIN.andHmsOpt(0u, 0u, 0u)!!,
            listOf(Token.Str("-262143-01-01T00:00:00")),
        )
        assertSerTokens(
            NaiveDate.MAX.andHmsNanoOpt(23u, 59u, 59u, 1_999_999_999u)!!,
            listOf(Token.Str("+262142-12-31T23:59:60.999999999")),
        )
    }

    @Test
    fun testSerdeDeserialize() {
        assertDeTokens(
            NaiveDate.fromYmdOpt(2016, 7u, 8u)!!.andHmsMilliOpt(9u, 10u, 48u, 90u)!!,
            NaiveDateTime.Companion,
            listOf(Token.Str("2016-07-08T09:10:48.090")),
        )
        assertDeTokens(
            NaiveDate.fromYmdOpt(2016, 7u, 8u)!!.andHmsMilliOpt(9u, 10u, 48u, 90u)!!,
            NaiveDateTime.Companion,
            listOf(Token.Str("2016-7-8T9:10:48.09")),
        )
        assertDeTokens(
            NaiveDate.fromYmdOpt(2014, 7u, 24u)!!.andHmsOpt(12u, 34u, 6u)!!,
            NaiveDateTime.Companion,
            listOf(Token.Str("2014-07-24T12:34:06")),
        )
        assertDeTokens(
            NaiveDate.fromYmdOpt(0, 1u, 1u)!!.andHmsMilliOpt(0u, 0u, 59u, 1_000u)!!,
            NaiveDateTime.Companion,
            listOf(Token.Str("0000-01-01T00:00:60")),
        )
        assertDeTokens(
            NaiveDate.fromYmdOpt(0, 1u, 1u)!!.andHmsMilliOpt(0u, 0u, 59u, 1_000u)!!,
            NaiveDateTime.Companion,
            listOf(Token.Str("0-1-1T0:0:60")),
        )
        assertDeTokens(
            NaiveDate.fromYmdOpt(-1, 12u, 31u)!!.andHmsNanoOpt(23u, 59u, 59u, 7u)!!,
            NaiveDateTime.Companion,
            listOf(Token.Str("-0001-12-31T23:59:59.000000007")),
        )
        assertDeTokens(
            NaiveDate.MIN.andHmsOpt(0u, 0u, 0u)!!,
            NaiveDateTime.Companion,
            listOf(Token.Str("-262143-01-01T00:00:00")),
        )
        assertDeTokens(
            NaiveDate.MAX.andHmsNanoOpt(23u, 59u, 59u, 1_999_999_999u)!!,
            NaiveDateTime.Companion,
            listOf(Token.Str("+262142-12-31T23:59:60.999999999")),
        )
        assertDeTokens(
            NaiveDate.MAX.andHmsNanoOpt(23u, 59u, 59u, 1_999_999_999u)!!,
            NaiveDateTime.Companion,
            listOf(Token.Str("+262142-12-31T23:59:60.9999999999997")),
        )

        // bad formats
        assertDeTokensError(
            NaiveDateTime.Companion,
            listOf(Token.Str("")),
            "premature end of input",
        )
        assertDeTokensError(
            NaiveDateTime.Companion,
            listOf(Token.Str("2016-07-08")),
            "premature end of input",
        )
        assertDeTokensError(
            NaiveDateTime.Companion,
            listOf(Token.Str("09:10:48.090")),
            "input contains invalid characters",
        )
        assertDeTokensError(
            NaiveDateTime.Companion,
            listOf(Token.Str("20160708T091048.090")),
            "input contains invalid characters",
        )
        assertDeTokensError(
            NaiveDateTime.Companion,
            listOf(Token.Str("2000-00-00T00:00:00")),
            "input is out of range",
        )
        assertDeTokensError(
            NaiveDateTime.Companion,
            listOf(Token.Str("2000-02-30T00:00:00")),
            "input is out of range",
        )
        assertDeTokensError(
            NaiveDateTime.Companion,
            listOf(Token.Str("2001-02-29T00:00:00")),
            "input is out of range",
        )
        assertDeTokensError(
            NaiveDateTime.Companion,
            listOf(Token.Str("2002-02-28T24:00:00")),
            "input is out of range",
        )
        assertDeTokensError(
            NaiveDateTime.Companion,
            listOf(Token.Str("2002-02-28T23:60:00")),
            "input is out of range",
        )
        assertDeTokensError(
            NaiveDateTime.Companion,
            listOf(Token.Str("2002-02-28T23:59:61")),
            "input is out of range",
        )
        assertDeTokensError(
            NaiveDateTime.Companion,
            listOf(Token.Str("2016-07-08T09:10:48,090")),
            "trailing input",
        )
        assertDeTokensError(
            NaiveDateTime.Companion,
            listOf(Token.Str("2016-07-08 09:10:48.090")),
            "input contains invalid characters",
        )
        assertDeTokensError(
            NaiveDateTime.Companion,
            listOf(Token.Str("2016-007-08T09:10:48.090")),
            "input is out of range",
        )
        assertDeTokensError(
            NaiveDateTime.Companion,
            listOf(Token.Str("yyyy-mm-ddThh:mm:ss.fffffffff")),
            "input contains invalid characters",
        )
        assertDeTokensError(
            NaiveDateTime.Companion,
            listOf(Token.I64(20160708000000L)),
            "invalid type: Signed(value=20160708000000), expected a formatted date and time string",
        )
    }

    @Test
    fun testNaiveSerdeTimestamps() {
        val dtNanos =
            NaiveDate
                .fromYmdOpt(2018, 5u, 17u)!!
                .andHmsNanoOpt(2u, 4u, 59u, 918355733u)!!
        val tsNanos = 1526522699918355733L
        val serNanos = TokenSerializer.new(listOf(Token.I64(tsNanos)))
        assertTrue(TsNanoseconds.serialize(dtNanos, serNanos).isSuccess)
        assertDeTokens(dtNanos, TsNanoseconds, listOf(Token.I64(tsNanos)))
        assertDeTokens(
            DateTime.fromTimestamp(-1L, 999_999_999u)!!.naiveUtc(),
            TsNanoseconds,
            listOf(Token.I64(-1L)),
        )

        val serNanosOpt = TokenSerializer.new(listOf(Token.Some, Token.I64(tsNanos)))
        assertTrue(TsNanosecondsOption.serialize(dtNanos, serNanosOpt).isSuccess)
        val serNanosNone = TokenSerializer.new(listOf(Token.None))
        assertTrue(TsNanosecondsOption.serialize(null, serNanosNone).isSuccess)
        assertDeTokens(dtNanos, TsNanosecondsOption, listOf(Token.Some, Token.I64(tsNanos)))
        assertDeTokens(null, TsNanosecondsOption, listOf(Token.None))

        val dtMicros =
            NaiveDate
                .fromYmdOpt(2018, 5u, 17u)!!
                .andHmsMicroOpt(2u, 4u, 59u, 918355u)!!
        val tsMicros = 1526522699918355L
        val serMicros = TokenSerializer.new(listOf(Token.I64(tsMicros)))
        assertTrue(TsMicroseconds.serialize(dtMicros, serMicros).isSuccess)
        assertDeTokens(dtMicros, TsMicroseconds, listOf(Token.I64(tsMicros)))
        assertDeTokens(dtMicros, TsMicrosecondsOption, listOf(Token.Some, Token.I64(tsMicros)))

        val dtMillis =
            NaiveDate
                .fromYmdOpt(2018, 5u, 17u)!!
                .andHmsMilliOpt(2u, 4u, 59u, 918u)!!
        val tsMillis = 1526522699918L
        val serMillis = TokenSerializer.new(listOf(Token.I64(tsMillis)))
        assertTrue(TsMilliseconds.serialize(dtMillis, serMillis).isSuccess)
        assertDeTokens(dtMillis, TsMilliseconds, listOf(Token.I64(tsMillis)))
        assertDeTokens(dtMillis, TsMillisecondsOption, listOf(Token.Some, Token.I64(tsMillis)))

        val dtSecs =
            NaiveDate
                .fromYmdOpt(2015, 5u, 15u)!!
                .andHmsOpt(10u, 0u, 0u)!!
        val tsSecs = 1431684000L
        val serSecs = TokenSerializer.new(listOf(Token.I64(tsSecs)))
        assertTrue(TsSeconds.serialize(dtSecs, serSecs).isSuccess)
        assertDeTokens(dtSecs, TsSeconds, listOf(Token.I64(tsSecs)))
        assertDeTokens(dtSecs, TsSecondsOption, listOf(Token.Some, Token.I64(tsSecs)))
    }
}
