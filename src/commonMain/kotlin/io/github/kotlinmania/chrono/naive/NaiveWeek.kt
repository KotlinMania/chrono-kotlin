// port-lint: source naive/mod.rs
package io.github.kotlinmania.chrono.naive

import io.github.kotlinmania.chrono.Weekday

/**
 * A week represented by a [NaiveDate] and a [Weekday] which is the first day of the week.
 */
class NaiveWeek internal constructor(
    private val date: NaiveDate,
    private val start: Weekday,
) {
    /**
     * Returns a date representing the first day of the week.
     */
    fun firstDay(): NaiveDate =
        checkedFirstDay() ?: throw IllegalArgumentException("first weekday out of range for NaiveDate")

    /**
     * Returns a date representing the first day of the week or null if out of range.
     */
    fun checkedFirstDay(): NaiveDate? {
        val startDay = start.numDaysFromMonday()
        val refDay = date.weekday().numDaysFromMonday()
        val days = startDay - refDay - if (startDay > refDay) 7 else 0
        return date.addDays(days)
    }

    /**
     * Returns a date representing the last day of the week.
     */
    fun lastDay(): NaiveDate =
        checkedLastDay() ?: throw IllegalArgumentException("last weekday out of range for NaiveDate")

    /**
     * Returns a date representing the last day of the week or null if out of range.
     */
    fun checkedLastDay(): NaiveDate? {
        val end = start.pred().numDaysFromMonday()
        val refDay = date.weekday().numDaysFromMonday()
        val days = end - refDay + if (end < refDay) 7 else 0
        return date.addDays(days)
    }

    /**
     * Returns a [ClosedRange] representing the whole week bounded by [firstDay] and [lastDay].
     */
    fun days(): ClosedRange<NaiveDate> =
        checkedDays() ?: throw IllegalArgumentException("first or last weekday is out of range for NaiveDate")

    /**
     * Returns an optional [ClosedRange] representing the whole week.
     */
    fun checkedDays(): ClosedRange<NaiveDate>? {
        val first = checkedFirstDay() ?: return null
        val last = checkedLastDay() ?: return null
        return first..last
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NaiveWeek) return false
        return firstDay() == other.firstDay()
    }

    override fun hashCode(): Int = firstDay().hashCode()

    override fun toString(): String = "NaiveWeek(firstDay=${firstDay()}, lastDay=${lastDay()})"

    companion object {
        /** Create a new [NaiveWeek]. */
        fun new(date: NaiveDate, start: Weekday): NaiveWeek = NaiveWeek(date, start)
    }
}
