// port-lint: tests datetime/tests.rs
package io.github.kotlinmania.chrono

import io.github.kotlinmania.chrono.naive.NaiveDate
import io.github.kotlinmania.chrono.naive.NaiveDateTime
import io.github.kotlinmania.chrono.offset.FixedOffset
import io.github.kotlinmania.chrono.offset.MappedLocalTime
import io.github.kotlinmania.chrono.offset.Offset
import io.github.kotlinmania.chrono.offset.TimeZone
import io.github.kotlinmania.chrono.offset.Utc
import io.github.kotlinmania.chrono.serde.TsMicroseconds
import io.github.kotlinmania.chrono.serde.TsMicrosecondsOption
import io.github.kotlinmania.chrono.serde.TsMilliseconds
import io.github.kotlinmania.chrono.serde.TsMillisecondsOption
import io.github.kotlinmania.chrono.serde.TsNanoseconds
import io.github.kotlinmania.chrono.serde.TsNanosecondsOption
import io.github.kotlinmania.chrono.serde.TsSeconds
import io.github.kotlinmania.chrono.serde.TsSecondsOption
import io.github.kotlinmania.serdetest.Token
import io.github.kotlinmania.serdetest.TokenSerializer
import io.github.kotlinmania.serdetest.assertDeTokens
import io.github.kotlinmania.serdetest.assertDeTokensError
import io.github.kotlinmania.serdetest.assertSerTokens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DateTimeTest {
    @Test
    fun testDatetimeFromTimestampMillis() {
        val validMap =
            listOf(
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
        val validMap =
            listOf(
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
        val validMap =
            listOf(
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

    @Test
    fun testSerdeSerialize() {
        assertSerTokens(
            Utc.withYmdAndHms(2014, 7u, 24u, 12u, 34u, 6u).unwrap(),
            listOf(Token.Str("2014-07-24T12:34:06Z")),
        )
        assertSerTokens(
            FixedOffset.eastOpt(3660)!!.withYmdAndHms(2014, 7u, 24u, 12u, 34u, 6u).unwrap(),
            listOf(Token.Str("2014-07-24T12:34:06+01:01")),
        )
        // An offset with seconds is not allowed by RFC 3339, so we round it to the nearest minute.
        // In this case `+01:00:50` becomes `+01:01`
        assertSerTokens(
            FixedOffset.eastOpt(3650)!!.withYmdAndHms(2014, 7u, 24u, 12u, 34u, 6u).unwrap(),
            listOf(Token.Str("2014-07-24T12:34:06+01:01")),
        )
    }

    @Test
    fun testSerdeDeserialize() {
        val expectedUtc =
            NaiveDate
                .fromYmdOpt(2014, 7u, 24u)!!
                .andHmsOpt(12u, 34u, 6u)!!
                .andUtc()
        assertDeTokens(
            expectedUtc,
            DateTime.UtcDeserializer,
            listOf(Token.Str("2014-07-24T12:34:06Z")),
        )
        assertDeTokens(
            expectedUtc,
            DateTime.UtcDeserializer,
            listOf(Token.Str("2014-07-24T13:57:06+01:23")),
        )

        val expectedFixedZero =
            NaiveDate
                .fromYmdOpt(2014, 7u, 24u)!!
                .andHmsOpt(12u, 34u, 6u)!!
                .andLocalTimezone(FixedOffset.eastOpt(0)!!)
                .unwrap()
        assertDeTokens(
            expectedFixedZero,
            DateTime.Companion,
            listOf(Token.Str("2014-07-24T12:34:06Z")),
        )
        val expectedFixedOffset =
            NaiveDate
                .fromYmdOpt(2014, 7u, 24u)!!
                .andHmsOpt(13u, 57u, 6u)!!
                .andLocalTimezone(FixedOffset.eastOpt(60 * 60 + 23 * 60)!!)
                .unwrap()
        assertDeTokens(
            expectedFixedOffset,
            DateTime.Companion,
            listOf(Token.Str("2014-07-24T13:57:06+01:23")),
        )
        assertEquals(expectedFixedOffset.offset(), DateTime.fromStr("2014-07-24T13:57:06+01:23").getOrThrow().offset())

        assertDeTokensError(
            DateTime.UtcDeserializer,
            listOf(Token.Str("2014-07-32T12:34:06Z")),
            "input is out of range",
        )
        assertDeTokensError(
            DateTime.Companion,
            listOf(Token.Str("2014-07-32T12:34:06Z")),
            "input is out of range",
        )
    }

    @Test
    fun testSerdeNoOffsetDebug() {
        val testTz =
            object : TimeZone, Offset {
                override fun toString(): String = "TEST"

                override fun fix(): FixedOffset = FixedOffset.eastOpt(15 * 60 * 60)!!

                override fun fromOffset(state: TimeZone): TimeZone = this

                override fun offsetFromLocalDate(local: NaiveDate): MappedLocalTime<TimeZone> =
                    MappedLocalTime.Single(this)

                override fun offsetFromLocalDatetime(local: NaiveDateTime): MappedLocalTime<TimeZone> =
                    MappedLocalTime.Single(this)

                override fun offsetFromUtcDate(utc: NaiveDate): TimeZone = this

                override fun offsetFromUtcDatetime(utc: NaiveDateTime): TimeZone = this
            }

        assertEquals("TEST", testTz.toString())
        val dt = testTz.withYmdAndHms(2023, 4u, 24u, 21u, 10u, 33u).unwrap()
        assertSerTokens(dt, listOf(Token.Str("2023-04-24T21:10:33+15:00")))
        val decoded = DateTime.fromStr("2023-04-24T21:10:33+15:00").getOrThrow()
        assertEquals(dt.naiveUtc(), decoded.naiveUtc())
        assertEquals((dt.offset() as Offset).fix(), decoded.offset())
    }

    @Test
    fun testSerdeTimestamps() {
        val dtNanos =
            NaiveDate
                .fromYmdOpt(2018, 5u, 17u)!!
                .andHmsNanoOpt(2u, 4u, 59u, 918355733u)!!
                .andUtc()
        val tsNanos = 1526522699918355733L
        val serNanos = TokenSerializer.new(listOf(Token.I64(tsNanos)))
        assertTrue(TsNanoseconds.serialize(dtNanos, serNanos).isSuccess)
        assertDeTokens(dtNanos, TsNanoseconds, listOf(Token.I64(tsNanos)))
        assertDeTokens(
            DateTime.fromTimestamp(-1L, 999_999_999u)!!,
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
                .andUtc()
        val tsMicros = 1526522699918355L
        val serMicros = TokenSerializer.new(listOf(Token.I64(tsMicros)))
        assertTrue(TsMicroseconds.serialize(dtMicros, serMicros).isSuccess)
        assertDeTokens(dtMicros, TsMicroseconds, listOf(Token.I64(tsMicros)))
        assertDeTokens(dtMicros, TsMicrosecondsOption, listOf(Token.Some, Token.I64(tsMicros)))

        val dtMillis =
            NaiveDate
                .fromYmdOpt(2018, 5u, 17u)!!
                .andHmsMilliOpt(2u, 4u, 59u, 918u)!!
                .andUtc()
        val tsMillis = 1526522699918L
        val serMillis = TokenSerializer.new(listOf(Token.I64(tsMillis)))
        assertTrue(TsMilliseconds.serialize(dtMillis, serMillis).isSuccess)
        assertDeTokens(dtMillis, TsMilliseconds, listOf(Token.I64(tsMillis)))
        assertDeTokens(dtMillis, TsMillisecondsOption, listOf(Token.Some, Token.I64(tsMillis)))

        val dtSecs =
            NaiveDate
                .fromYmdOpt(2015, 5u, 15u)!!
                .andHmsOpt(10u, 0u, 0u)!!
                .andUtc()
        val tsSecs = 1431684000L
        val serSecs = TokenSerializer.new(listOf(Token.I64(tsSecs)))
        assertTrue(TsSeconds.serialize(dtSecs, serSecs).isSuccess)
        assertDeTokens(dtSecs, TsSeconds, listOf(Token.I64(tsSecs)))
        assertDeTokens(dtSecs, TsSecondsOption, listOf(Token.Some, Token.I64(tsSecs)))
    }
}
