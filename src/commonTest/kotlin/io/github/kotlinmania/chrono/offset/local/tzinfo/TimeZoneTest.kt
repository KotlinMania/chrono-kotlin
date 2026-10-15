// port-lint: tests offset/local/tz_info/timezone.rs
package io.github.kotlinmania.chrono.offset.local.tzinfo

import io.github.kotlinmania.chrono.offset.local.getEnvVar
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TimeZoneTest {
    @Test
    fun testNoDst() {
        val tzString = "HST10".encodeToByteArray()
        val transitionRule = TransitionRule.fromTzString(tzString, false).unwrap()
        assertEquals(
            TransitionRule.Fixed(LocalTimeType.new(-36000, false, "HST".encodeToByteArray()).unwrap()),
            transitionRule,
        )
    }

    @Test
    fun testError() {
        assertIs<Error.UnsupportedTzString>(
            TransitionRule.fromTzString("IST-1GMT0".encodeToByteArray(), false).unwrapErr(),
        )
        assertIs<Error.UnsupportedTzString>(
            TransitionRule.fromTzString("EET-2EEST".encodeToByteArray(), false).unwrapErr(),
        )
    }

    @Test
    fun testV1FileWithLeapSeconds() {
        val bytes =
            byteArrayOf('T'.code.toByte(), 'Z'.code.toByte(), 'i'.code.toByte(), 'f'.code.toByte(), 0) +
                byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0) +
                byteArrayOf(0, 0, 0, 1) +
                byteArrayOf(0, 0, 0, 1) +
                byteArrayOf(0, 0, 0, 0x1b) +
                byteArrayOf(0, 0, 0, 0) +
                byteArrayOf(0, 0, 0, 1) +
                byteArrayOf(0, 0, 0, 4) +
                byteArrayOf(0, 0, 0, 0, 0, 0) +
                byteArrayOf('U'.code.toByte(), 'T'.code.toByte(), 'C'.code.toByte(), 0) +
                byteArrayOf(0x04, 0xb2.toByte(), 0x58, 0x00, 0, 0, 0, 1) +
                byteArrayOf(0x05, 0xa4.toByte(), 0xec.toByte(), 0x01, 0, 0, 0, 2) +
                byteArrayOf(0x07, 0x86.toByte(), 0x1f, 0x82.toByte(), 0, 0, 0, 3) +
                byteArrayOf(0x09, 0x67, 0x53, 0x03, 0, 0, 0, 4) +
                byteArrayOf(0x0b, 0x48, 0x86.toByte(), 0x84.toByte(), 0, 0, 0, 5) +
                byteArrayOf(0x0d, 0x2b, 0x0b, 0x85.toByte(), 0, 0, 0, 6) +
                byteArrayOf(0x0f, 0x0c, 0x3f, 0x06, 0, 0, 0, 7) +
                byteArrayOf(0x10, 0xed.toByte(), 0x72, 0x87.toByte(), 0, 0, 0, 8) +
                byteArrayOf(0x12, 0xce.toByte(), 0xa6.toByte(), 0x08, 0, 0, 0, 9) +
                byteArrayOf(0x15, 0x9f.toByte(), 0xca.toByte(), 0x89.toByte(), 0, 0, 0, 0x0a) +
                byteArrayOf(0x17, 0x80.toByte(), 0xfe.toByte(), 0x0a, 0, 0, 0, 0x0b) +
                byteArrayOf(0x19, 0x62, 0x31, 0x8b.toByte(), 0, 0, 0, 0x0c) +
                byteArrayOf(0x1d, 0x25, 0xea.toByte(), 0x0c, 0, 0, 0, 0x0d) +
                byteArrayOf(0x21, 0xda.toByte(), 0xe5.toByte(), 0x0d, 0, 0, 0, 0x0e) +
                byteArrayOf(0x25, 0x9e.toByte(), 0x9d.toByte(), 0x8e.toByte(), 0, 0, 0, 0x0f) +
                byteArrayOf(0x27, 0x7f, 0xd1.toByte(), 0x0f, 0, 0, 0, 0x10) +
                byteArrayOf(0x2a, 0x50, 0xf5.toByte(), 0x90.toByte(), 0, 0, 0, 0x11) +
                byteArrayOf(0x2c, 0x32, 0x29, 0x11, 0, 0, 0, 0x12) +
                byteArrayOf(0x2e, 0x13, 0x5c, 0x92.toByte(), 0, 0, 0, 0x13) +
                byteArrayOf(0x30, 0xe7.toByte(), 0x24, 0x13, 0, 0, 0, 0x14) +
                byteArrayOf(0x33, 0xb8.toByte(), 0x48, 0x94.toByte(), 0, 0, 0, 0x15) +
                byteArrayOf(0x36, 0x8c.toByte(), 0x10, 0x15, 0, 0, 0, 0x16) +
                byteArrayOf(0x43, 0xb7.toByte(), 0x1b, 0x96.toByte(), 0, 0, 0, 0x17) +
                byteArrayOf(0x49, 0x5c, 0x07, 0x97.toByte(), 0, 0, 0, 0x18) +
                byteArrayOf(0x4f, 0xef.toByte(), 0x93.toByte(), 0x18, 0, 0, 0, 0x19) +
                byteArrayOf(0x55, 0x93.toByte(), 0x2d, 0x99.toByte(), 0, 0, 0, 0x1a) +
                byteArrayOf(0x58, 0x68, 0x46, 0x9a.toByte(), 0, 0, 0, 0x1b) +
                byteArrayOf(0, 0)

        val timeZone = TimeZone.fromTzData(bytes).unwrap()

        val timeZoneResult =
            TimeZone
                .new(
                    emptyList(),
                    listOf(LocalTimeType.new(0, false, "UTC".encodeToByteArray()).unwrap()),
                    listOf(
                        LeapSecond.new(78796800L, 1),
                        LeapSecond.new(94694401L, 2),
                        LeapSecond.new(126230402L, 3),
                        LeapSecond.new(157766403L, 4),
                        LeapSecond.new(189302404L, 5),
                        LeapSecond.new(220924805L, 6),
                        LeapSecond.new(252460806L, 7),
                        LeapSecond.new(283996807L, 8),
                        LeapSecond.new(315532808L, 9),
                        LeapSecond.new(362793609L, 10),
                        LeapSecond.new(394329610L, 11),
                        LeapSecond.new(425865611L, 12),
                        LeapSecond.new(489024012L, 13),
                        LeapSecond.new(567993613L, 14),
                        LeapSecond.new(631152014L, 15),
                        LeapSecond.new(662688015L, 16),
                        LeapSecond.new(709948816L, 17),
                        LeapSecond.new(741484817L, 18),
                        LeapSecond.new(773020818L, 19),
                        LeapSecond.new(820454419L, 20),
                        LeapSecond.new(867715220L, 21),
                        LeapSecond.new(915148821L, 22),
                        LeapSecond.new(1136073622L, 23),
                        LeapSecond.new(1230768023L, 24),
                        LeapSecond.new(1341100824L, 25),
                        LeapSecond.new(1435708825L, 26),
                        LeapSecond.new(1483228826L, 27),
                    ),
                    null,
                ).unwrap()

        assertEquals(timeZoneResult, timeZone)
    }

    @Test
    fun testV2File() {
        val bytes =
            byteArrayOf('T'.code.toByte(), 'Z'.code.toByte(), 'i'.code.toByte(), 'f'.code.toByte(), '2'.code.toByte()) +
                byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0) +
                byteArrayOf(0, 0, 0, 6) +
                byteArrayOf(0, 0, 0, 6) +
                byteArrayOf(0, 0, 0, 0) +
                byteArrayOf(0, 0, 0, 7) +
                byteArrayOf(0, 0, 0, 6) +
                byteArrayOf(0, 0, 0, 0x14) +
                byteArrayOf(0x80.toByte(), 0x00, 0x00, 0x00) +
                byteArrayOf(0xbb.toByte(), 0x05, 0x43, 0x48) +
                byteArrayOf(0xbb.toByte(), 0x21, 0x71, 0x58) +
                byteArrayOf(0xcb.toByte(), 0x89.toByte(), 0x3d, 0xc8.toByte()) +
                byteArrayOf(0xd2.toByte(), 0x23, 0xf4.toByte(), 0x70) +
                byteArrayOf(0xd2.toByte(), 0x61, 0x49, 0x38) +
                byteArrayOf(0xd5.toByte(), 0x8d.toByte(), 0x73, 0x48) +
                byteArrayOf(1, 2, 1, 3, 4, 1, 5) +
                byteArrayOf(0xff.toByte(), 0xff.toByte(), 0x6c, 0x02, 0, 0) +
                byteArrayOf(0xff.toByte(), 0xff.toByte(), 0x6c, 0x58, 0, 4) +
                byteArrayOf(0xff.toByte(), 0xff.toByte(), 0x7a, 0x68, 1, 8) +
                byteArrayOf(0xff.toByte(), 0xff.toByte(), 0x7a, 0x68, 1, 0x0c) +
                byteArrayOf(0xff.toByte(), 0xff.toByte(), 0x7a, 0x68, 1, 0x10) +
                byteArrayOf(0xff.toByte(), 0xff.toByte(), 0x73, 0x60, 0, 4) +
                byteArrayOf('L'.code.toByte(), 'M'.code.toByte(), 'T'.code.toByte(), 0) +
                byteArrayOf('H'.code.toByte(), 'S'.code.toByte(), 'T'.code.toByte(), 0) +
                byteArrayOf('H'.code.toByte(), 'D'.code.toByte(), 'T'.code.toByte(), 0) +
                byteArrayOf('H'.code.toByte(), 'W'.code.toByte(), 'T'.code.toByte(), 0) +
                byteArrayOf('H'.code.toByte(), 'P'.code.toByte(), 'T'.code.toByte(), 0) +
                byteArrayOf(0, 0, 0, 0, 1, 0) +
                byteArrayOf(0, 0, 0, 0, 1, 0) +
                byteArrayOf('T'.code.toByte(), 'Z'.code.toByte(), 'i'.code.toByte(), 'f'.code.toByte(), '2'.code.toByte()) +
                byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0) +
                byteArrayOf(0, 0, 0, 6) +
                byteArrayOf(0, 0, 0, 6) +
                byteArrayOf(0, 0, 0, 0) +
                byteArrayOf(0, 0, 0, 7) +
                byteArrayOf(0, 0, 0, 6) +
                byteArrayOf(0, 0, 0, 0x14) +
                byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x74, 0xe0.toByte(), 0x70, 0xbe.toByte()) +
                byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xbb.toByte(), 0x05, 0x43, 0x48) +
                byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xbb.toByte(), 0x21, 0x71, 0x58) +
                byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xcb.toByte(), 0x89.toByte(), 0x3d, 0xc8.toByte()) +
                byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xd2.toByte(), 0x23, 0xf4.toByte(), 0x70) +
                byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xd2.toByte(), 0x61, 0x49, 0x38) +
                byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xd5.toByte(), 0x8d.toByte(), 0x73, 0x48) +
                byteArrayOf(1, 2, 1, 3, 4, 1, 5) +
                byteArrayOf(0xff.toByte(), 0xff.toByte(), 0x6c, 0x02, 0, 0) +
                byteArrayOf(0xff.toByte(), 0xff.toByte(), 0x6c, 0x58, 0, 4) +
                byteArrayOf(0xff.toByte(), 0xff.toByte(), 0x7a, 0x68, 1, 8) +
                byteArrayOf(0xff.toByte(), 0xff.toByte(), 0x7a, 0x68, 1, 0x0c) +
                byteArrayOf(0xff.toByte(), 0xff.toByte(), 0x7a, 0x68, 1, 0x10) +
                byteArrayOf(0xff.toByte(), 0xff.toByte(), 0x73, 0x60, 0, 4) +
                byteArrayOf('L'.code.toByte(), 'M'.code.toByte(), 'T'.code.toByte(), 0) +
                byteArrayOf('H'.code.toByte(), 'S'.code.toByte(), 'T'.code.toByte(), 0) +
                byteArrayOf('H'.code.toByte(), 'D'.code.toByte(), 'T'.code.toByte(), 0) +
                byteArrayOf('H'.code.toByte(), 'W'.code.toByte(), 'T'.code.toByte(), 0) +
                byteArrayOf('H'.code.toByte(), 'P'.code.toByte(), 'T'.code.toByte(), 0) +
                byteArrayOf(0, 0, 0, 0, 1, 0) +
                byteArrayOf(0, 0, 0, 0, 1, 0) +
                "\nHST10\n".encodeToByteArray()

        val timeZone = TimeZone.fromTzData(bytes).unwrap()

        val timeZoneResult =
            TimeZone
                .new(
                    listOf(
                        Transition.new(-2334101314L, 1),
                        Transition.new(-1157283000L, 2),
                        Transition.new(-1155436200L, 1),
                        Transition.new(-880198200L, 3),
                        Transition.new(-769395600L, 4),
                        Transition.new(-765376200L, 1),
                        Transition.new(-712150200L, 5),
                    ),
                    listOf(
                        LocalTimeType.new(-37886, false, "LMT".encodeToByteArray()).unwrap(),
                        LocalTimeType.new(-37800, false, "HST".encodeToByteArray()).unwrap(),
                        LocalTimeType.new(-34200, true, "HDT".encodeToByteArray()).unwrap(),
                        LocalTimeType.new(-34200, true, "HWT".encodeToByteArray()).unwrap(),
                        LocalTimeType.new(-34200, true, "HPT".encodeToByteArray()).unwrap(),
                        LocalTimeType.new(-36000, false, "HST".encodeToByteArray()).unwrap(),
                    ),
                    emptyList(),
                    TransitionRule.Fixed(LocalTimeType.new(-36000, false, "HST".encodeToByteArray()).unwrap()),
                ).unwrap()

        assertEquals(timeZoneResult, timeZone)

        assertEquals(
            LocalTimeType.new(-34200, true, "HDT".encodeToByteArray()).unwrap(),
            timeZone.findLocalTimeType(-1156939200L).unwrap(),
        )
        assertEquals(
            LocalTimeType.new(-36000, false, "HST".encodeToByteArray()).unwrap(),
            timeZone.findLocalTimeType(1546300800L).unwrap(),
        )
    }

    @Test
    fun testNoTzString() {
        val bytes =
            byteArrayOf('T'.code.toByte(), 'Z'.code.toByte(), 'i'.code.toByte(), 'f'.code.toByte(), 0) +
                byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0) +
                byteArrayOf(0, 0, 0, 2) +
                byteArrayOf(0, 0, 0, 2) +
                byteArrayOf(0, 0, 0, 0) +
                byteArrayOf(0, 0, 0, 1) +
                byteArrayOf(0, 0, 0, 2) +
                byteArrayOf(0, 0, 0, 8) +
                byteArrayOf(0xb6.toByte(), 0xa4.toByte(), 0x42, 0x18) +
                byteArrayOf(1) +
                byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xb6.toByte(), 0x68, 0, 0) +
                byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xb9.toByte(), 0xb0.toByte(), 0, 4) +
                byteArrayOf('Q'.code.toByte(), 'M'.code.toByte(), 'T'.code.toByte(), 0) +
                byteArrayOf('E'.code.toByte(), 'C'.code.toByte(), 'T'.code.toByte(), 0) +
                byteArrayOf(0, 0) +
                byteArrayOf(0, 0)

        val timeZone = TimeZone.fromTzData(bytes).unwrap()

        val timeZoneResult =
            TimeZone
                .new(
                    listOf(Transition.new(-1230749160L, 1)),
                    listOf(
                        LocalTimeType.new(-18840, false, "QMT".encodeToByteArray()).unwrap(),
                        LocalTimeType.new(-18000, false, "ECT".encodeToByteArray()).unwrap(),
                    ),
                    emptyList(),
                    null,
                ).unwrap()

        assertEquals(timeZoneResult, timeZone)

        assertEquals(
            LocalTimeType.new(-18840, false, "QMT".encodeToByteArray()).unwrap(),
            timeZone.findLocalTimeType(-1500000000L).unwrap(),
        )
        assertEquals(
            LocalTimeType.new(-18000, false, "ECT".encodeToByteArray()).unwrap(),
            timeZone.findLocalTimeType(0L).unwrap(),
        )
    }

    @Test
    fun testTzAsciiStr() {
        assertIs<Error.LocalTimeType>(TimeZoneName.new(byteArrayOf()).unwrapErr())
        assertIs<Error.LocalTimeType>(TimeZoneName.new("A".encodeToByteArray()).unwrapErr())
        assertIs<Error.LocalTimeType>(TimeZoneName.new("AB".encodeToByteArray()).unwrapErr())
        assertContentEquals("CET".encodeToByteArray(), TimeZoneName.new("CET".encodeToByteArray()).unwrap().asBytes())
        assertContentEquals("CHADT".encodeToByteArray(), TimeZoneName.new("CHADT".encodeToByteArray()).unwrap().asBytes())
        assertContentEquals("abcdefg".encodeToByteArray(), TimeZoneName.new("abcdefg".encodeToByteArray()).unwrap().asBytes())
        assertContentEquals("UTC+02".encodeToByteArray(), TimeZoneName.new("UTC+02".encodeToByteArray()).unwrap().asBytes())
        assertContentEquals("-1230".encodeToByteArray(), TimeZoneName.new("-1230".encodeToByteArray()).unwrap().asBytes())
        assertIs<Error.LocalTimeType>(TimeZoneName.new("−0330".encodeToByteArray()).unwrapErr())
        assertIs<Error.LocalTimeType>(TimeZoneName.new(byteArrayOf(0, '1'.code.toByte(), '2'.code.toByte(), '3'.code.toByte())).unwrapErr())
        assertIs<Error.LocalTimeType>(TimeZoneName.new("12345678".encodeToByteArray()).unwrapErr())
        assertIs<Error.LocalTimeType>(
            TimeZoneName.new(byteArrayOf('G'.code.toByte(), 'M'.code.toByte(), 'T'.code.toByte(), 0, 0, 0)).unwrapErr(),
        )
    }

    @Test
    fun testTimeZone() {
        val utc = LocalTimeType.UTC
        val cet = LocalTimeType.withOffset(3600).unwrap()

        val utcLocalTimeTypes = listOf(utc)
        val fixedExtraRule = TransitionRule.Fixed(cet)

        val timeZone1 = TimeZone.new(emptyList(), utcLocalTimeTypes, emptyList(), null).unwrap()
        val timeZone2 = TimeZone.new(emptyList(), utcLocalTimeTypes, emptyList(), fixedExtraRule).unwrap()
        val timeZone3 = TimeZone.new(listOf(Transition.new(0L, 0)), utcLocalTimeTypes, emptyList(), null).unwrap()
        val timeZone4 =
            TimeZone
                .new(
                    listOf(Transition.new(Int.MIN_VALUE.toLong(), 0), Transition.new(0L, 1)),
                    listOf(utc, cet),
                    emptyList(),
                    fixedExtraRule,
                ).unwrap()

        assertEquals(utc, timeZone1.findLocalTimeType(0L).unwrap())
        assertEquals(cet, timeZone2.findLocalTimeType(0L).unwrap())

        assertEquals(utc, timeZone3.findLocalTimeType(-1L).unwrap())
        assertEquals(utc, timeZone3.findLocalTimeType(0L).unwrap())

        assertEquals(utc, timeZone4.findLocalTimeType(-1L).unwrap())
        assertEquals(cet, timeZone4.findLocalTimeType(0L).unwrap())

        val timeZoneErr =
            TimeZone.new(
                listOf(Transition.new(0L, 0)),
                utcLocalTimeTypes,
                emptyList(),
                fixedExtraRule,
            )
        assertTrue(timeZoneErr.isErr)
    }

    @Test
    fun testTimeZoneFromPosixTz() {
        val tz = getEnvVar("TZ")
        if (tz != null) {
            val timeZoneLocal = TimeZone.local(tz).unwrap()
            val timeZoneLocal1 = TimeZone.fromPosixTz(tz).unwrap()
            assertEquals(timeZoneLocal, timeZoneLocal1)
        }

        val timeZoneUtc = TimeZone.fromPosixTz("UTC").ok()
        if (timeZoneUtc != null) {
            assertEquals(0, timeZoneUtc.findLocalTimeType(0L).unwrap().offset())
        }

        assertTrue(TimeZone.fromPosixTz("EST5EDT,0/0,J365/25").isErr)
        val emptyPosixTz = TimeZone.fromPosixTz("").unwrap()
        assertEquals(0, emptyPosixTz.findLocalTimeType(0L).unwrap().offset())
    }

    @Test
    fun testLeapSeconds() {
        val timeZone =
            TimeZone
                .new(
                    emptyList(),
                    listOf(LocalTimeType.new(0, false, "UTC".encodeToByteArray()).unwrap()),
                    listOf(
                        LeapSecond.new(78796800L, 1),
                        LeapSecond.new(94694401L, 2),
                        LeapSecond.new(126230402L, 3),
                        LeapSecond.new(157766403L, 4),
                        LeapSecond.new(189302404L, 5),
                        LeapSecond.new(220924805L, 6),
                        LeapSecond.new(252460806L, 7),
                        LeapSecond.new(283996807L, 8),
                        LeapSecond.new(315532808L, 9),
                        LeapSecond.new(362793609L, 10),
                        LeapSecond.new(394329610L, 11),
                        LeapSecond.new(425865611L, 12),
                        LeapSecond.new(489024012L, 13),
                        LeapSecond.new(567993613L, 14),
                        LeapSecond.new(631152014L, 15),
                        LeapSecond.new(662688015L, 16),
                        LeapSecond.new(709948816L, 17),
                        LeapSecond.new(741484817L, 18),
                        LeapSecond.new(773020818L, 19),
                        LeapSecond.new(820454419L, 20),
                        LeapSecond.new(867715220L, 21),
                        LeapSecond.new(915148821L, 22),
                        LeapSecond.new(1136073622L, 23),
                        LeapSecond.new(1230768023L, 24),
                        LeapSecond.new(1341100824L, 25),
                        LeapSecond.new(1435708825L, 26),
                        LeapSecond.new(1483228826L, 27),
                    ),
                    null,
                ).unwrap()

        val timeZoneRef = timeZone.asRef()

        assertEquals(1136073599L, timeZoneRef.unixLeapTimeToUnixTime(1136073621L).unwrap())
        assertEquals(1136073600L, timeZoneRef.unixLeapTimeToUnixTime(1136073622L).unwrap())
        assertEquals(1136073600L, timeZoneRef.unixLeapTimeToUnixTime(1136073623L).unwrap())
        assertEquals(1136073601L, timeZoneRef.unixLeapTimeToUnixTime(1136073624L).unwrap())

        assertEquals(1136073621L, timeZoneRef.unixTimeToUnixLeapTime(1136073599L).unwrap())
        assertEquals(1136073623L, timeZoneRef.unixTimeToUnixLeapTime(1136073600L).unwrap())
        assertEquals(1136073624L, timeZoneRef.unixTimeToUnixLeapTime(1136073601L).unwrap())
    }

    @Test
    fun testLeapSecondsOverflow() {
        val timeZoneErr =
            TimeZone.new(
                listOf(Transition.new(Long.MIN_VALUE, 0)),
                listOf(LocalTimeType.UTC),
                listOf(LeapSecond.new(0L, 1)),
                TransitionRule.Fixed(LocalTimeType.UTC),
            )
        assertTrue(timeZoneErr.isErr)

        val timeZone =
            TimeZone
                .new(
                    listOf(Transition.new(Long.MAX_VALUE, 0)),
                    listOf(LocalTimeType.UTC),
                    listOf(LeapSecond.new(0L, 1)),
                    null,
                ).unwrap()
        assertIs<Error.FindLocalTimeType>(timeZone.findLocalTimeType(Long.MAX_VALUE).unwrapErr())
    }
}
