// port-lint: source src/weekday_set.rs
package io.github.kotlinmania.chrono

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WeekdaySetTest {
    @Test
    fun bitwiseSetOperationsPreserveEighthBitInvariant() {
        for (lhs in iterAll()) {
            for (rhs in iterAll()) {
                assertEighthBitInvariant(lhs.union(rhs))
                assertEighthBitInvariant(lhs.intersection(rhs))
                assertEighthBitInvariant(lhs.symmetricDifference(rhs))
                assertEighthBitInvariant(lhs.difference(rhs))
            }
        }
    }

    // `split_at()` is used in `iter()`, so we must not iterate
    // over all days with `WeekdaySet.ALL.iter(Weekday.Mon)`.
    @Test
    fun splitAtIsEquivalentToIterating() {
        val week = listOf(
            Weekday.Mon, Weekday.Tue, Weekday.Wed, Weekday.Thu, Weekday.Fri, Weekday.Sat, Weekday.Sun,
        )

        for (weekdays in iterAll()) {
            for (splitDay in week) {
                val expectedBefore = WeekdaySet.fromIterable(
                    week.takeWhile { it != splitDay }.filter { weekdays.contains(it) },
                )
                val expectedAfter = WeekdaySet.fromIterable(
                    week.dropWhile { it != splitDay }.filter { weekdays.contains(it) },
                )

                val (actualBefore, actualAfter) = weekdays.splitAt(splitDay)
                assertEquals(
                    expectedBefore,
                    actualBefore,
                    "splitAt($splitDay) `before` failed for $weekdays",
                )
                assertEquals(
                    expectedAfter,
                    actualAfter,
                    "splitAt($splitDay) `after` failed for $weekdays",
                )
            }
        }
    }

    @Test
    fun singleDayMatchesSingleConstructor() {
        for (day in Weekday.entries) {
            assertEquals(day, WeekdaySet.single(day).singleDay())
        }
        assertNull(WeekdaySet.EMPTY.singleDay())
        assertNull(WeekdaySet.ALL.singleDay())
        assertNull(WeekdaySet.fromArray(Weekday.Mon, Weekday.Tue).singleDay())
    }

    @Test
    fun firstAndLastAgreeWithIteration() {
        for (weekdays in iterAll()) {
            val asList = weekdays.iter(Weekday.Mon).asSequence().toList()
            assertEquals(asList.firstOrNull(), weekdays.first())
            assertEquals(asList.lastOrNull(), weekdays.last())
        }
    }

    @Test
    fun iterStartsFromGivenWeekdayAndWraps() {
        val weekdays = WeekdaySet.fromArray(Weekday.Mon, Weekday.Wed, Weekday.Fri)
        val iter = weekdays.iter(Weekday.Wed)
        assertEquals(Weekday.Wed, iter.next())
        assertEquals(Weekday.Fri, iter.next())
        assertEquals(Weekday.Mon, iter.next())
        assertFalse(iter.hasNext())
    }

    @Test
    fun insertAndRemoveReportNovelty() {
        val weekdays = WeekdaySet.single(Weekday.Mon)
        assertTrue(weekdays.insert(Weekday.Tue))
        assertFalse(weekdays.insert(Weekday.Tue))
        assertTrue(weekdays.remove(Weekday.Mon))
        assertFalse(weekdays.remove(Weekday.Mon))
    }

    @Test
    fun displayMatchesUpstreamFormat() {
        assertEquals("[]", WeekdaySet.EMPTY.toString())
        assertEquals("[Mon]", WeekdaySet.single(Weekday.Mon).toString())
        assertEquals(
            "[Mon, Fri, Sun]",
            WeekdaySet.fromArray(Weekday.Mon, Weekday.Fri, Weekday.Sun).toString(),
        )
    }

    /** Iterate over all 128 possible sets, from EMPTY to ALL. */
    private fun iterAll(): Sequence<WeekdaySet> = sequence {
        for (bits in 0b0000_0000 until 0b1000_0000) {
            yield(WeekdaySet(bits))
        }
    }

    /** Asserts that the 8-th bit of `days` is not set. */
    private fun assertEighthBitInvariant(days: WeekdaySet) {
        assertEquals(0, days.bits and 0b1000_0000, "the 8-th bit of $days is not 0")
    }
}
