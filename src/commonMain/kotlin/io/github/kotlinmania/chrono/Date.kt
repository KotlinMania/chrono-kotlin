// port-lint: source src/date.rs
package io.github.kotlinmania.chrono

import io.github.kotlinmania.chrono.naive.IsoWeek
import io.github.kotlinmania.chrono.naive.NaiveDate
import io.github.kotlinmania.chrono.naive.NaiveTime
import io.github.kotlinmania.chrono.offset.TimeZone
import io.github.kotlinmania.chrono.offset.Utc

/**
 * ISO 8601 calendar date with time zone.
 */
@Deprecated("Use NaiveDate or DateTime<Tz> instead")
@Suppress("DEPRECATION")
class Date<Tz : TimeZone<Tz>> internal constructor(
    internal val date: NaiveDate,
    internal val offset: Tz,
) : Datelike<Date<Tz>>, Comparable<Date<*>> {

    fun naiveUtc(): NaiveDate = date

    fun naiveLocal(): NaiveDate = date

    fun timezone(): Tz = offset

    fun andTime(time: NaiveTime): DateTime<Tz>? {
        val localDt = naiveLocal().andTime(time)
        return timezone().fromLocalDatetime(localDt).single()
    }

    fun andHmsOpt(hour: UInt, min: UInt, sec: UInt): DateTime<Tz>? {
        val time = NaiveTime.fromHmsOpt(hour, min, sec) ?: return null
        return andTime(time)
    }

    fun andHms(hour: UInt, min: UInt, sec: UInt): DateTime<Tz> =
        andHmsOpt(hour, min, sec) ?: throw IllegalArgumentException("invalid time")

    fun andHmsMilliOpt(hour: UInt, min: UInt, sec: UInt, milli: UInt): DateTime<Tz>? {
        val time = NaiveTime.fromHmsMilliOpt(hour, min, sec, milli) ?: return null
        return andTime(time)
    }

    fun andHmsMicroOpt(hour: UInt, min: UInt, sec: UInt, micro: UInt): DateTime<Tz>? {
        val time = NaiveTime.fromHmsMicroOpt(hour, min, sec, micro) ?: return null
        return andTime(time)
    }

    fun andHmsNanoOpt(hour: UInt, min: UInt, sec: UInt, nano: UInt): DateTime<Tz>? {
        val time = NaiveTime.fromHmsNanoOpt(hour, min, sec, nano) ?: return null
        return andTime(time)
    }

    override fun year(): Int = date.year()

    override fun month(): UInt = date.month()

    override fun month0(): UInt = date.month0()

    fun monthEnum(): Month = Month.fromU32(month())!!

    override fun day(): UInt = date.day()

    override fun day0(): UInt = date.day0()

    override fun ordinal(): UInt = date.ordinal()

    override fun ordinal0(): UInt = date.ordinal0()

    override fun weekday(): Weekday = date.weekday()

    override fun isoWeek(): IsoWeek = date.isoWeek()

    override fun numDaysFromCe(): Int = date.numDaysFromCe()

    override fun withYear(year: Int): Date<Tz>? =
        date.withYear(year)?.let { Date(it, offset) }

    override fun withMonth(month: UInt): Date<Tz>? =
        date.withMonth(month)?.let { Date(it, offset) }

    override fun withMonth0(month0: UInt): Date<Tz>? =
        date.withMonth0(month0)?.let { Date(it, offset) }

    override fun withDay(day: UInt): Date<Tz>? =
        date.withDay(day)?.let { Date(it, offset) }

    override fun withDay0(day0: UInt): Date<Tz>? =
        date.withDay0(day0)?.let { Date(it, offset) }

    override fun withOrdinal(ordinal: UInt): Date<Tz>? =
        date.withOrdinal(ordinal)?.let { Date(it, offset) }

    override fun withOrdinal0(ordinal0: UInt): Date<Tz>? =
        date.withOrdinal0(ordinal0)?.let { Date(it, offset) }

    override fun compareTo(other: Date<*>): Int = date.compareTo(other.date)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Date<*>) return false
        return date == other.date && offset == other.offset
    }

    override fun hashCode(): Int = date.hashCode() * 31 + offset.hashCode()

    override fun toString(): String = "$date$offset"

    companion object {
        fun <Tz : TimeZone<Tz>> fromUtc(date: NaiveDate, offset: Tz): Date<Tz> =
            Date(date, offset)
    }
}
