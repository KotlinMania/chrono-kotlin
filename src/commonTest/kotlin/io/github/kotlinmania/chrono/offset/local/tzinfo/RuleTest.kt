// port-lint: tests offset/local/tz_info/rule.rs
package io.github.kotlinmania.chrono.offset.local.tzinfo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RuleTest {
    @Test
    fun testQuoted() {
        val transitionRule =
            TransitionRule.fromTzString("<-03>+3<+03>-3,J1,J365".encodeToByteArray(), false).unwrap()
        val expected =
            TransitionRule.Alternate(
                AlternateTime
                    .new(
                        LocalTimeType.new(-10800, false, "-03".encodeToByteArray()).unwrap(),
                        LocalTimeType.new(10800, true, "+03".encodeToByteArray()).unwrap(),
                        RuleDay.julian1(1u).unwrap(),
                        7200,
                        RuleDay.julian1(365u).unwrap(),
                        7200,
                    ).unwrap(),
            )
        assertEquals(expected, transitionRule)
    }

    @Test
    fun testFull() {
        val tzString = "NZST-12:00:00NZDT-13:00:00,M10.1.0/02:00:00,M3.3.0/02:00:00".encodeToByteArray()
        val transitionRule = TransitionRule.fromTzString(tzString, false).unwrap()
        val expected =
            TransitionRule.Alternate(
                AlternateTime
                    .new(
                        LocalTimeType.new(43200, false, "NZST".encodeToByteArray()).unwrap(),
                        LocalTimeType.new(46800, true, "NZDT".encodeToByteArray()).unwrap(),
                        RuleDay.monthWeekday(10u, 1u, 0u).unwrap(),
                        7200,
                        RuleDay.monthWeekday(3u, 3u, 0u).unwrap(),
                        7200,
                    ).unwrap(),
            )
        assertEquals(expected, transitionRule)
    }

    @Test
    fun testNegativeDst() {
        val tzString = "IST-1GMT0,M10.5.0,M3.5.0/1".encodeToByteArray()
        val transitionRule = TransitionRule.fromTzString(tzString, false).unwrap()
        val expected =
            TransitionRule.Alternate(
                AlternateTime
                    .new(
                        LocalTimeType.new(3600, false, "IST".encodeToByteArray()).unwrap(),
                        LocalTimeType.new(0, true, "GMT".encodeToByteArray()).unwrap(),
                        RuleDay.monthWeekday(10u, 5u, 0u).unwrap(),
                        7200,
                        RuleDay.monthWeekday(3u, 5u, 0u).unwrap(),
                        3600,
                    ).unwrap(),
            )
        assertEquals(expected, transitionRule)
    }

    @Test
    fun testNegativeHour() {
        val tzString = "<-03>3<-02>,M3.5.0/-2,M10.5.0/-1".encodeToByteArray()
        assertTrue(TransitionRule.fromTzString(tzString, false).isErr)

        val expected =
            TransitionRule.Alternate(
                AlternateTime
                    .new(
                        LocalTimeType.new(-10800, false, "-03".encodeToByteArray()).unwrap(),
                        LocalTimeType.new(-7200, true, "-02".encodeToByteArray()).unwrap(),
                        RuleDay.monthWeekday(3u, 5u, 0u).unwrap(),
                        -7200,
                        RuleDay.monthWeekday(10u, 5u, 0u).unwrap(),
                        -3600,
                    ).unwrap(),
            )
        assertEquals(expected, TransitionRule.fromTzString(tzString, true).unwrap())
    }

    @Test
    fun testAllYearDst() {
        val tzString = "EST5EDT,0/0,J365/25".encodeToByteArray()
        assertTrue(TransitionRule.fromTzString(tzString, false).isErr)

        val expected =
            TransitionRule.Alternate(
                AlternateTime
                    .new(
                        LocalTimeType.new(-18000, false, "EST".encodeToByteArray()).unwrap(),
                        LocalTimeType.new(-14400, true, "EDT".encodeToByteArray()).unwrap(),
                        RuleDay.julian0(0u).unwrap(),
                        0,
                        RuleDay.julian1(365u).unwrap(),
                        90000,
                    ).unwrap(),
            )
        assertEquals(expected, TransitionRule.fromTzString(tzString, true).unwrap())
    }

    @Test
    fun testV3File() {
        val bytes =
            byteArrayOf('T'.code.toByte(), 'Z'.code.toByte(), 'i'.code.toByte(), 'f'.code.toByte(), '3'.code.toByte()) +
                byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0) +
                byteArrayOf(0, 0, 0, 0) +
                byteArrayOf(0, 0, 0, 0) +
                byteArrayOf(0, 0, 0, 0) +
                byteArrayOf(0, 0, 0, 0) +
                byteArrayOf(0, 0, 0, 1) +
                byteArrayOf(0, 0, 0, 4) +
                byteArrayOf(0, 0, 0x1c, 0x20, 0, 0) +
                byteArrayOf('I'.code.toByte(), 'S'.code.toByte(), 'T'.code.toByte(), 0) +
                byteArrayOf('T'.code.toByte(), 'Z'.code.toByte(), 'i'.code.toByte(), 'f'.code.toByte(), '3'.code.toByte()) +
                byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0) +
                byteArrayOf(0, 0, 0, 1) +
                byteArrayOf(0, 0, 0, 1) +
                byteArrayOf(0, 0, 0, 0) +
                byteArrayOf(0, 0, 0, 1) +
                byteArrayOf(0, 0, 0, 1) +
                byteArrayOf(0, 0, 0, 4) +
                byteArrayOf(0, 0, 0, 0, 0x7f, 0xe8.toByte(), 0x17, 0x80.toByte()) +
                byteArrayOf(0) +
                byteArrayOf(0, 0, 0x1c, 0x20, 0, 0) +
                byteArrayOf('I'.code.toByte(), 'S'.code.toByte(), 'T'.code.toByte(), 0) +
                byteArrayOf(1, 1) +
                "\nIST-2IDT,M3.4.4/26,M10.5.0\n".encodeToByteArray()

        val timeZone = TimeZone.fromTzData(bytes).unwrap()

        val timeZoneResult =
            TimeZone
                .new(
                    listOf(Transition.new(2145916800L, 0)),
                    listOf(LocalTimeType.new(7200, false, "IST".encodeToByteArray()).unwrap()),
                    emptyList(),
                    TransitionRule.Alternate(
                        AlternateTime
                            .new(
                                LocalTimeType.new(7200, false, "IST".encodeToByteArray()).unwrap(),
                                LocalTimeType.new(10800, true, "IDT".encodeToByteArray()).unwrap(),
                                RuleDay.monthWeekday(3u, 4u, 4u).unwrap(),
                                93600,
                                RuleDay.monthWeekday(10u, 5u, 0u).unwrap(),
                                7200,
                            ).unwrap(),
                    ),
                ).unwrap()

        assertEquals(timeZoneResult, timeZone)
    }

    @Test
    fun testRuleDay() {
        val ruleDayJ1 = RuleDay.julian1(60u).unwrap()
        assertEquals(3 to 1L, ruleDayJ1.transitionDate(2000))
        assertEquals(3 to 1L, ruleDayJ1.transitionDate(2001))
        assertEquals(951912000L, ruleDayJ1.unixTime(2000, 43200L))

        val ruleDayJ0 = RuleDay.julian0(59u).unwrap()
        assertEquals(2 to 29L, ruleDayJ0.transitionDate(2000))
        assertEquals(3 to 1L, ruleDayJ0.transitionDate(2001))
        assertEquals(951825600L, ruleDayJ0.unixTime(2000, 43200L))

        val ruleDayMwd = RuleDay.monthWeekday(2u, 5u, 2u).unwrap()
        assertEquals(2 to 29L, ruleDayMwd.transitionDate(2000))
        assertEquals(2 to 27L, ruleDayMwd.transitionDate(2001))
        assertEquals(951825600L, ruleDayMwd.unixTime(2000, 43200L))
        assertEquals(983275200L, ruleDayMwd.unixTime(2001, 43200L))
    }

    @Test
    fun testTransitionRule() {
        val transitionRuleFixed =
            TransitionRule.Fixed(LocalTimeType.new(-36000, false, null).unwrap())
        assertEquals(-36000, transitionRuleFixed.findLocalTimeType(0L).unwrap().offset())

        val transitionRuleDst =
            TransitionRule.Alternate(
                AlternateTime
                    .new(
                        LocalTimeType.new(43200, false, "NZST".encodeToByteArray()).unwrap(),
                        LocalTimeType.new(46800, true, "NZDT".encodeToByteArray()).unwrap(),
                        RuleDay.monthWeekday(10u, 1u, 0u).unwrap(),
                        7200,
                        RuleDay.monthWeekday(3u, 3u, 0u).unwrap(),
                        7200,
                    ).unwrap(),
            )

        assertEquals(46800, transitionRuleDst.findLocalTimeType(953384399L).unwrap().offset())
        assertEquals(43200, transitionRuleDst.findLocalTimeType(953384400L).unwrap().offset())
        assertEquals(43200, transitionRuleDst.findLocalTimeType(970322399L).unwrap().offset())
        assertEquals(46800, transitionRuleDst.findLocalTimeType(970322400L).unwrap().offset())

        val transitionRuleNegativeDst =
            TransitionRule.Alternate(
                AlternateTime
                    .new(
                        LocalTimeType.new(3600, false, "IST".encodeToByteArray()).unwrap(),
                        LocalTimeType.new(0, true, "GMT".encodeToByteArray()).unwrap(),
                        RuleDay.monthWeekday(10u, 5u, 0u).unwrap(),
                        7200,
                        RuleDay.monthWeekday(3u, 5u, 0u).unwrap(),
                        3600,
                    ).unwrap(),
            )

        assertEquals(0, transitionRuleNegativeDst.findLocalTimeType(954032399L).unwrap().offset())
        assertEquals(3600, transitionRuleNegativeDst.findLocalTimeType(954032400L).unwrap().offset())
        assertEquals(3600, transitionRuleNegativeDst.findLocalTimeType(972781199L).unwrap().offset())
        assertEquals(0, transitionRuleNegativeDst.findLocalTimeType(972781200L).unwrap().offset())

        val transitionRuleNegativeTime1 =
            TransitionRule.Alternate(
                AlternateTime
                    .new(
                        LocalTimeType.new(0, false, null).unwrap(),
                        LocalTimeType.new(0, true, null).unwrap(),
                        RuleDay.julian0(100u).unwrap(),
                        0,
                        RuleDay.julian0(101u).unwrap(),
                        -86500,
                    ).unwrap(),
            )

        assertTrue(transitionRuleNegativeTime1.findLocalTimeType(8639899L).unwrap().isDst())
        assertFalse(transitionRuleNegativeTime1.findLocalTimeType(8639900L).unwrap().isDst())
        assertFalse(transitionRuleNegativeTime1.findLocalTimeType(8639999L).unwrap().isDst())
        assertTrue(transitionRuleNegativeTime1.findLocalTimeType(8640000L).unwrap().isDst())

        val transitionRuleNegativeTime2 =
            TransitionRule.Alternate(
                AlternateTime
                    .new(
                        LocalTimeType.new(-10800, false, "-03".encodeToByteArray()).unwrap(),
                        LocalTimeType.new(-7200, true, "-02".encodeToByteArray()).unwrap(),
                        RuleDay.monthWeekday(3u, 5u, 0u).unwrap(),
                        -7200,
                        RuleDay.monthWeekday(10u, 5u, 0u).unwrap(),
                        -3600,
                    ).unwrap(),
            )

        assertEquals(-10800, transitionRuleNegativeTime2.findLocalTimeType(954032399L).unwrap().offset())
        assertEquals(-7200, transitionRuleNegativeTime2.findLocalTimeType(954032400L).unwrap().offset())
        assertEquals(-7200, transitionRuleNegativeTime2.findLocalTimeType(972781199L).unwrap().offset())
        assertEquals(-10800, transitionRuleNegativeTime2.findLocalTimeType(972781200L).unwrap().offset())

        val transitionRuleAllYearDst =
            TransitionRule.Alternate(
                AlternateTime
                    .new(
                        LocalTimeType.new(-18000, false, "EST".encodeToByteArray()).unwrap(),
                        LocalTimeType.new(-14400, true, "EDT".encodeToByteArray()).unwrap(),
                        RuleDay.julian0(0u).unwrap(),
                        0,
                        RuleDay.julian1(365u).unwrap(),
                        90000,
                    ).unwrap(),
            )

        assertEquals(-14400, transitionRuleAllYearDst.findLocalTimeType(946702799L).unwrap().offset())
        assertEquals(-14400, transitionRuleAllYearDst.findLocalTimeType(946702800L).unwrap().offset())
    }

    @Test
    fun testTransitionRuleOverflow() {
        val transitionRule1 =
            TransitionRule.Alternate(
                AlternateTime
                    .new(
                        LocalTimeType.new(-1, false, null).unwrap(),
                        LocalTimeType.new(-1, true, null).unwrap(),
                        RuleDay.julian1(365u).unwrap(),
                        0,
                        RuleDay.julian1(1u).unwrap(),
                        0,
                    ).unwrap(),
            )

        val transitionRule2 =
            TransitionRule.Alternate(
                AlternateTime
                    .new(
                        LocalTimeType.new(1, false, null).unwrap(),
                        LocalTimeType.new(1, true, null).unwrap(),
                        RuleDay.julian1(365u).unwrap(),
                        0,
                        RuleDay.julian1(1u).unwrap(),
                        0,
                    ).unwrap(),
            )

        val minUnixTime = -67768100567971200L
        val maxUnixTime = 67767976233532799L

        assertIs<Error.OutOfRange>(transitionRule1.findLocalTimeType(minUnixTime).unwrapErr())
        assertIs<Error.OutOfRange>(transitionRule2.findLocalTimeType(maxUnixTime).unwrapErr())
    }
}
