// port-lint: tests naive/date/tests.rs
package io.github.kotlinmania.chrono.naive

import io.github.kotlinmania.chrono.Months
import io.github.kotlinmania.chrono.TimeDelta
import io.github.kotlinmania.chrono.Weekday
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NaiveDateTest {

    @Test
    fun testDateBounds() {
        val calculatedMin = NaiveDate.fromYmdOpt(MIN_YEAR, 1u, 1u)
        val calculatedMax = NaiveDate.fromYmdOpt(MAX_YEAR, 12u, 31u)
        assertEquals(NaiveDate.MIN, calculatedMin)
        assertEquals(NaiveDate.MAX, calculatedMax)
    }

    @Test
    fun testDiffMonths() {
        // identity
        assertEquals(
            NaiveDate.fromYmdOpt(2022, 8u, 3u),
            NaiveDate.fromYmdOpt(2022, 8u, 3u)?.checkedAddMonths(Months.new(0u))
        )

        // add crossing year boundary
        assertEquals(
            NaiveDate.fromYmdOpt(2023, 2u, 3u),
            NaiveDate.fromYmdOpt(2022, 8u, 3u)?.checkedAddMonths(Months.new(6u))
        )

        // sub crossing year boundary
        assertEquals(
            NaiveDate.fromYmdOpt(2021, 10u, 3u),
            NaiveDate.fromYmdOpt(2022, 8u, 3u)?.checkedSubMonths(Months.new(10u))
        )

        // add clamping day, non-leap year
        assertEquals(
            NaiveDate.fromYmdOpt(2022, 2u, 28u),
            NaiveDate.fromYmdOpt(2022, 1u, 29u)?.checkedAddMonths(Months.new(1u))
        )

        // add to leap day
        assertEquals(
            NaiveDate.fromYmdOpt(2024, 2u, 29u),
            NaiveDate.fromYmdOpt(2022, 10u, 29u)?.checkedAddMonths(Months.new(16u))
        )
    }

    @Test
    fun testDateFromYmd() {
        val fromYmd = NaiveDate.Companion::fromYmdOpt

        assertNull(fromYmd(2012, 0u, 1u))
        assertTrue(fromYmd(2012, 1u, 1u) != null)
        assertTrue(fromYmd(2012, 2u, 29u) != null)
        assertNull(fromYmd(2014, 2u, 29u))
        assertNull(fromYmd(2014, 3u, 0u))
        assertTrue(fromYmd(2014, 3u, 1u) != null)
        assertTrue(fromYmd(2014, 3u, 31u) != null)
        assertNull(fromYmd(2014, 3u, 32u))
        assertTrue(fromYmd(2014, 12u, 31u) != null)
        assertNull(fromYmd(2014, 13u, 1u))
    }

    @Test
    fun testDateFromYo() {
        val fromYo = NaiveDate.Companion::fromYoOpt
        fun ymd(y: Int, m: UInt, d: UInt) = NaiveDate.fromYmdOpt(y, m, d)

        assertEquals(null, fromYo(2012, 0u))
        assertEquals(ymd(2012, 1u, 1u), fromYo(2012, 1u))
        assertEquals(ymd(2012, 1u, 2u), fromYo(2012, 2u))
        assertEquals(ymd(2012, 2u, 1u), fromYo(2012, 32u))
        assertEquals(ymd(2012, 2u, 29u), fromYo(2012, 60u))
        assertEquals(ymd(2012, 3u, 1u), fromYo(2012, 61u))
        assertEquals(ymd(2012, 4u, 9u), fromYo(2012, 100u))
        assertEquals(ymd(2012, 7u, 18u), fromYo(2012, 200u))
        assertEquals(ymd(2012, 10u, 26u), fromYo(2012, 300u))
        assertEquals(ymd(2012, 12u, 31u), fromYo(2012, 366u))
        assertEquals(null, fromYo(2012, 367u))

        assertEquals(null, fromYo(2014, 0u))
        assertEquals(ymd(2014, 1u, 1u), fromYo(2014, 1u))
        assertEquals(ymd(2014, 1u, 2u), fromYo(2014, 2u))
        assertEquals(ymd(2014, 2u, 1u), fromYo(2014, 32u))
        assertEquals(ymd(2014, 2u, 28u), fromYo(2014, 59u))
        assertEquals(ymd(2014, 3u, 1u), fromYo(2014, 60u))
        assertEquals(ymd(2014, 4u, 10u), fromYo(2014, 100u))
        assertEquals(ymd(2014, 7u, 19u), fromYo(2014, 200u))
        assertEquals(ymd(2014, 10u, 27u), fromYo(2014, 300u))
        assertEquals(ymd(2014, 12u, 31u), fromYo(2014, 365u))
        assertEquals(null, fromYo(2014, 366u))
    }

    @Test
    fun testDateFromIsoywd() {
        val fromIsoywd = NaiveDate.Companion::fromIsoywdOpt
        fun ymd(y: Int, m: UInt, d: UInt) = NaiveDate.fromYmdOpt(y, m, d)

        assertEquals(null, fromIsoywd(2004, 0u, Weekday.Sun))
        assertEquals(ymd(2003, 12u, 29u), fromIsoywd(2004, 1u, Weekday.Mon))
        assertEquals(ymd(2004, 1u, 4u), fromIsoywd(2004, 1u, Weekday.Sun))
        assertEquals(ymd(2004, 1u, 5u), fromIsoywd(2004, 2u, Weekday.Mon))
        assertEquals(ymd(2004, 1u, 11u), fromIsoywd(2004, 2u, Weekday.Sun))
        assertEquals(ymd(2004, 12u, 20u), fromIsoywd(2004, 52u, Weekday.Mon))
        assertEquals(ymd(2004, 12u, 26u), fromIsoywd(2004, 52u, Weekday.Sun))
        assertEquals(ymd(2004, 12u, 27u), fromIsoywd(2004, 53u, Weekday.Mon))
        assertEquals(ymd(2005, 1u, 2u), fromIsoywd(2004, 53u, Weekday.Sun))
        assertEquals(null, fromIsoywd(2004, 54u, Weekday.Mon))

        assertEquals(null, fromIsoywd(2011, 0u, Weekday.Sun))
        assertEquals(ymd(2011, 1u, 3u), fromIsoywd(2011, 1u, Weekday.Mon))
        assertEquals(ymd(2011, 1u, 9u), fromIsoywd(2011, 1u, Weekday.Sun))
        assertEquals(ymd(2011, 1u, 10u), fromIsoywd(2011, 2u, Weekday.Mon))
        assertEquals(ymd(2011, 1u, 16u), fromIsoywd(2011, 2u, Weekday.Sun))
    }

    @Test
    fun testDateFields() {
        val d1 = NaiveDate.fromYmdOpt(2012, 2u, 29u)!!
        assertEquals(2012, d1.year())
        assertEquals(2u, d1.month())
        assertEquals(29u, d1.day())
        assertEquals(60u, d1.ordinal())

        val d2 = NaiveDate.fromYoOpt(2012, 60u)!!
        assertEquals(d1, d2)
    }

    @Test
    fun testDateWeekday() {
        assertEquals(Weekday.Fri, NaiveDate.fromYmdOpt(1582, 10u, 15u)!!.weekday())
        assertEquals(Weekday.Thu, NaiveDate.fromYmdOpt(1875, 5u, 20u)!!.weekday())
        assertEquals(Weekday.Sat, NaiveDate.fromYmdOpt(2000, 1u, 1u)!!.weekday())
    }

    @Test
    fun testDateSuccPred() {
        val ymd = { y: Int, m: UInt, d: UInt -> NaiveDate.fromYmdOpt(y, m, d)!! }
        assertEquals(ymd(2014, 5u, 7u), ymd(2014, 5u, 6u).succ())
        assertEquals(ymd(2014, 6u, 1u), ymd(2014, 5u, 31u).succ())
        assertEquals(ymd(2015, 1u, 1u), ymd(2014, 12u, 31u).succ())
        assertEquals(ymd(2016, 2u, 29u), ymd(2016, 2u, 28u).succ())

        assertEquals(ymd(2016, 2u, 29u), ymd(2016, 3u, 1u).pred())
        assertEquals(ymd(2014, 12u, 31u), ymd(2015, 1u, 1u).pred())
        assertEquals(ymd(2014, 5u, 31u), ymd(2014, 6u, 1u).pred())
    }

    @Test
    fun testDateSignedDurationSince() {
        fun ymd(y: Int, m: UInt, d: UInt) = NaiveDate.fromYmdOpt(y, m, d)!!
        assertEquals(TimeDelta.zero(), ymd(2014, 1u, 1u).signedDurationSince(ymd(2014, 1u, 1u)))
        assertEquals(TimeDelta.tryDays(1), ymd(2014, 1u, 2u).signedDurationSince(ymd(2014, 1u, 1u)))
        assertEquals(TimeDelta.tryDays(364), ymd(2014, 12u, 31u).signedDurationSince(ymd(2014, 1u, 1u)))
    }

    @Test
    fun testNaiveWeek() {
        val date = NaiveDate.fromYmdOpt(2022, 5u, 18u)!!
        val week = date.week(Weekday.Mon)
        assertEquals(NaiveDate.fromYmdOpt(2022, 5u, 16u), week.firstDay())
        assertEquals(NaiveDate.fromYmdOpt(2022, 5u, 22u), week.lastDay())
        assertTrue(week.days().contains(date))
    }
}
