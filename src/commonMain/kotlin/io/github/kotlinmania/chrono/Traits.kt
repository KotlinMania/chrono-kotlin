// port-lint: source traits.rs
package io.github.kotlinmania.chrono

import io.github.kotlinmania.chrono.naive.IsoWeek

/**
 * The common set of methods for date components.
 *
 * Methods such as [year], [month], [day], and [weekday] can be used to get
 * basic information about the date.
 *
 * The `with` methods can change the date.
 *
 * The `with` methods can be convenient for changing a single component of a
 * date, but they must be used with care. Examples to watch out for:
 *
 * - [withYear] changes the year component of a year-month-day value. Do not
 *   use this method if you want the ordinal to stay the same after changing
 *   the year, or if you want the week and weekday values to stay the same.
 * - Do not combine two `with` methods to change two date components. For
 *   example, changing both the year and month components could fail because an
 *   intermediate value does not exist, while the final date would be valid.
 *
 * For more complex changes to a date, it is best to use the methods on
 * `NaiveDate` to create a new value instead of altering an existing date.
 */
interface Datelike<Self : Datelike<Self>> {
    /** Returns the year number in the calendar date. */
    fun year(): Int

    /**
     * Returns the absolute year number starting from 1 with a flag that is
     * false when the year predates the epoch and true otherwise.
     */
    fun yearCe(): Pair<Boolean, UInt> {
        val year = year()
        return if (year < 1) {
            false to (1 - year).toUInt()
        } else {
            true to year.toUInt()
        }
    }

    /**
     * Returns the quarter number starting from 1.
     *
     * The return value ranges from 1 to 4.
     */
    fun quarter(): UInt = (Math.floorDiv(month().toInt() - 1, 3) + 1).toUInt()

    /**
     * Returns the month number starting from 1.
     *
     * The return value ranges from 1 to 12.
     */
    fun month(): UInt

    /**
     * Returns the month number starting from 0.
     *
     * The return value ranges from 0 to 11.
     */
    fun month0(): UInt

    /**
     * Returns the day of month starting from 1.
     *
     * The return value ranges from 1 to 31. The last day of month differs by
     * month.
     */
    fun day(): UInt

    /**
     * Returns the day of month starting from 0.
     *
     * The return value ranges from 0 to 30. The last day of month differs by
     * month.
     */
    fun day0(): UInt

    /**
     * Returns the day of year starting from 1.
     *
     * The return value ranges from 1 to 366. The last day of year differs by
     * year.
     */
    fun ordinal(): UInt

    /**
     * Returns the day of year starting from 0.
     *
     * The return value ranges from 0 to 365. The last day of year differs by
     * year.
     */
    fun ordinal0(): UInt

    /** Returns the day of week. */
    fun weekday(): Weekday

    /** Returns the ISO week. */
    fun isoWeek(): IsoWeek

    /**
     * Makes a new value with the year number changed, while keeping the same
     * month and day.
     *
     * This method assumes you want to work on the date as a year-month-day
     * value. Do not use it if you want the ordinal to stay the same after
     * changing the year, or if you want the week and weekday values to stay
     * the same.
     *
     * Returns `null` when:
     *
     * - The resulting date does not exist, such as February 29 in a non-leap
     *   year.
     * - The year is out of range for `NaiveDate`.
     * - For a date-time value, the resulting date and time fall within a time
     *   zone transition such as from daylight saving time to standard time.
     */
    fun withYear(year: Int): Self?

    /**
     * Makes a new value with the month number, starting from 1, changed.
     *
     * Returns `null` when:
     *
     * - The resulting date does not exist, such as April 31.
     * - For a date-time value, the resulting date and time fall within a time
     *   zone transition such as from daylight saving time to standard time.
     * - The month value is out of range.
     */
    fun withMonth(month: UInt): Self?

    /**
     * Makes a new value with the month number, starting from 0, changed.
     *
     * Returns `null` when:
     *
     * - The resulting date does not exist, such as a zero-based April value
     *   when the day of month is 31.
     * - For a date-time value, the resulting date and time fall within a time
     *   zone transition such as from daylight saving time to standard time.
     * - The zero-based month value is out of range.
     */
    fun withMonth0(month0: UInt): Self?

    /**
     * Makes a new value with the day of month, starting from 1, changed.
     *
     * Returns `null` when:
     *
     * - The resulting date does not exist, such as April 31.
     * - For a date-time value, the resulting date and time fall within a time
     *   zone transition such as from daylight saving time to standard time.
     * - The day value is out of range.
     */
    fun withDay(day: UInt): Self?

    /**
     * Makes a new value with the day of month, starting from 0, changed.
     *
     * Returns `null` when:
     *
     * - The resulting date does not exist, such as zero-based April 30.
     * - For a date-time value, the resulting date and time fall within a time
     *   zone transition such as from daylight saving time to standard time.
     * - The zero-based day value is out of range.
     */
    fun withDay0(day0: UInt): Self?

    /**
     * Makes a new value with the day of year, starting from 1, changed.
     *
     * Returns `null` when:
     *
     * - The resulting date does not exist, such as day 366 in a non-leap year.
     * - For a date-time value, the resulting date and time fall within a time
     *   zone transition such as from daylight saving time to standard time.
     * - The ordinal value is out of range.
     */
    fun withOrdinal(ordinal: UInt): Self?

    /**
     * Makes a new value with the day of year, starting from 0, changed.
     *
     * Returns `null` when:
     *
     * - The resulting date does not exist, such as zero-based day 365 in a
     *   non-leap year.
     * - For a date-time value, the resulting date and time fall within a time
     *   zone transition such as from daylight saving time to standard time.
     * - The zero-based ordinal value is out of range.
     */
    fun withOrdinal0(ordinal0: UInt): Self?

    /**
     * Counts the days in the proleptic Gregorian calendar, with January 1,
     * year 1 as day 1.
     */
    fun numDaysFromCe(): Int {
        var year = year() - 1
        var ndays = 0
        if (year < 0) {
            val excess = 1 + (-year) / 400
            year += excess * 400
            ndays -= excess * 146_097
        }
        val div100 = year / 100
        ndays += ((year * 1461) shr 2) - div100 + (div100 shr 2)
        return ndays + ordinal().toInt()
    }

    /** Gets the length in days of the month. */
    fun numDaysInMonth(): UByte {
        val month = Month.fromU32(month())!!
        return month.numDays(year())!!
    }
}

/** The common set of methods for time components. */
interface Timelike<Self : Timelike<Self>> {
    /** Returns the hour number from 0 to 23. */
    fun hour(): UInt

    /**
     * Returns the hour number from 1 to 12 with a flag that is false for AM
     * and true for PM.
     */
    fun hour12(): Pair<Boolean, UInt> {
        val hour = hour()
        var hour12 = hour % 12u
        if (hour12 == 0u) {
            hour12 = 12u
        }
        return (hour >= 12u) to hour12
    }

    /** Returns the minute number from 0 to 59. */
    fun minute(): UInt

    /** Returns the second number from 0 to 59. */
    fun second(): UInt

    /**
     * Returns the number of nanoseconds since the whole non-leap second.
     *
     * The range from 1,000,000,000 to 1,999,999,999 represents the leap
     * second.
     */
    fun nanosecond(): UInt

    /**
     * Makes a new value with the hour number changed.
     *
     * Returns `null` when the resulting value would be invalid.
     */
    fun withHour(hour: UInt): Self?

    /**
     * Makes a new value with the minute number changed.
     *
     * Returns `null` when the resulting value would be invalid.
     */
    fun withMinute(min: UInt): Self?

    /**
     * Makes a new value with the second number changed.
     *
     * Returns `null` when the resulting value would be invalid. As with
     * [second], the input range is restricted to 0 through 59.
     */
    fun withSecond(sec: UInt): Self?

    /**
     * Makes a new value with nanoseconds since the whole non-leap second
     * changed.
     *
     * Returns `null` when the resulting value would be invalid. As with
     * [nanosecond], the input range can exceed 1,000,000,000 for leap seconds.
     */
    fun withNanosecond(nano: UInt): Self?

    /**
     * Returns the number of non-leap seconds past the last midnight.
     *
     * Every value in 00:00:00-23:59:59 maps to an integer in 0-86399.
     *
     * This method is not intended to provide the real number of seconds since
     * midnight on a given day. It does not take things like daylight saving
     * transitions into account.
     */
    fun numSecondsFromMidnight(): UInt = hour() * 3600u + minute() * 60u + second()
}
