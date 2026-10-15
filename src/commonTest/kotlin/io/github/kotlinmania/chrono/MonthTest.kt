// port-lint: tests month.rs
package io.github.kotlinmania.chrono

import io.github.kotlinmania.chrono.offset.Utc
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MonthTest {
    @Test
    fun testMonthEnumTryFrom() {
        assertEquals(Result.success(Month.January), Month.tryFrom(1u))
        assertEquals(Result.success(Month.February), Month.tryFrom(2u))
        assertEquals(Result.success(Month.December), Month.tryFrom(12u))
        assertTrue(Month.tryFrom(13u).isFailure)

        val date = Utc.withYmdAndHms(2019, 10u, 28u, 9u, 10u, 11u).unwrap()
        assertEquals(Result.success(Month.October), Month.tryFrom(date.month().toUByte()))

        val month = Month.January
        val dt = Utc.withYmdAndHms(2019, month.numberFromMonth(), 28u, 9u, 10u, 11u).unwrap()
        assertEquals(2019, dt.year())
        assertEquals(1u, dt.month())
        assertEquals(28u, dt.day())
    }

    @Test
    fun testMonthEnumPrimitiveParse() {
        val janOpt = Month.fromU32(1u)
        val febOpt = Month.fromU64(2u)
        val decOpt = Month.fromI64(12L)
        val noMonth = Month.fromU32(13u)
        assertEquals(Month.January, janOpt)
        assertEquals(Month.February, febOpt)
        assertEquals(Month.December, decOpt)
        assertNull(noMonth)

        val date = Utc.withYmdAndHms(2019, 10u, 28u, 9u, 10u, 11u).unwrap()
        assertEquals(Month.October, Month.fromU32(date.month()))

        val month = Month.January
        val dt = Utc.withYmdAndHms(2019, month.numberFromMonth(), 28u, 9u, 10u, 11u).unwrap()
        assertEquals(2019, dt.year())
        assertEquals(1u, dt.month())
        assertEquals(28u, dt.day())
    }

    @Test
    fun testMonthEnumSuccPred() {
        assertEquals(Month.February, Month.January.succ())
        assertEquals(Month.January, Month.December.succ())
        assertEquals(Month.December, Month.January.pred())
        assertEquals(Month.January, Month.February.pred())
    }

    @Test
    fun testMonthPartialOrd() {
        assertTrue(Month.January <= Month.January)
        assertTrue(Month.January < Month.February)
        assertTrue(Month.January < Month.December)
        assertTrue(Month.July >= Month.May)
        assertTrue(Month.September > Month.March)
    }

    @Test
    fun testMonthsAsU32() {
        assertEquals(0u, Months.new(0u).asU32())
        assertEquals(1u, Months.new(1u).asU32())
        assertEquals(UInt.MAX_VALUE, Months.new(UInt.MAX_VALUE).asU32())
    }

    // Upstream test_serde_serialize, test_serde_deserialize, and test_rkyv_validation are unported:
    // serde and rkyv bindings are not available in commonTest without serde serialization dependencies.

    @Test
    fun numDays() {
        assertEquals(31u.toUByte(), Month.January.numDays(2020))
        assertEquals(29u.toUByte(), Month.February.numDays(2020))
        assertEquals(28u.toUByte(), Month.February.numDays(2019))
    }
}
