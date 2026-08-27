// port-lint: source naive/datetime/mod.rs
package io.github.kotlinmania.chrono.naive

import io.github.kotlinmania.chrono.Datelike
import io.github.kotlinmania.chrono.DateTime
import io.github.kotlinmania.chrono.Month
import io.github.kotlinmania.chrono.Months
import io.github.kotlinmania.chrono.TimeDelta
import io.github.kotlinmania.chrono.Timelike
import io.github.kotlinmania.chrono.Weekday
import io.github.kotlinmania.chrono.offset.FixedOffset
import io.github.kotlinmania.chrono.offset.MappedLocalTime
import io.github.kotlinmania.chrono.offset.TimeZone
import io.github.kotlinmania.chrono.offset.Utc

/**
 * ISO 8601 combined date and time without timezone.
 */
class NaiveDateTime(
    internal val date: NaiveDate,
    internal val time: NaiveTime,
) : Datelike<NaiveDateTime>, Timelike<NaiveDateTime>, Comparable<NaiveDateTime> {

    /** Retrieves the date component. */
    fun date(): NaiveDate = date

    /** Retrieves the time component. */
    fun time(): NaiveTime = time

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

    override fun withYear(year: Int): NaiveDateTime? =
        date.withYear(year)?.let { NaiveDateTime(it, time) }

    override fun withMonth(month: UInt): NaiveDateTime? =
        date.withMonth(month)?.let { NaiveDateTime(it, time) }

    override fun withMonth0(month0: UInt): NaiveDateTime? =
        date.withMonth0(month0)?.let { NaiveDateTime(it, time) }

    override fun withDay(day: UInt): NaiveDateTime? =
        date.withDay(day)?.let { NaiveDateTime(it, time) }

    override fun withDay0(day0: UInt): NaiveDateTime? =
        date.withDay0(day0)?.let { NaiveDateTime(it, time) }

    override fun withOrdinal(ordinal: UInt): NaiveDateTime? =
        date.withOrdinal(ordinal)?.let { NaiveDateTime(it, time) }

    override fun withOrdinal0(ordinal0: UInt): NaiveDateTime? =
        date.withOrdinal0(ordinal0)?.let { NaiveDateTime(it, time) }

    override fun hour(): UInt = time.hour()

    override fun minute(): UInt = time.minute()

    override fun second(): UInt = time.second()

    override fun nanosecond(): UInt = time.nanosecond()

    override fun withHour(hour: UInt): NaiveDateTime? =
        time.withHour(hour)?.let { NaiveDateTime(date, it) }

    override fun withMinute(min: UInt): NaiveDateTime? =
        time.withMinute(min)?.let { NaiveDateTime(date, it) }

    override fun withSecond(sec: UInt): NaiveDateTime? =
        time.withSecond(sec)?.let { NaiveDateTime(date, it) }

    override fun withNanosecond(nano: UInt): NaiveDateTime? =
        time.withNanosecond(nano)?.let { NaiveDateTime(date, it) }

    /** Adds a [FixedOffset] to this datetime, preserving leap seconds. */
    fun checkedAddOffset(rhs: FixedOffset): NaiveDateTime? {
        val (newTime, days) = time.overflowingAddOffset(rhs)
        val newDate = when (days) {
            -1 -> date.predOpt() ?: return null
            1 -> date.succOpt() ?: return null
            else -> date
        }
        return NaiveDateTime(newDate, newTime)
    }

    /** Subtracts a [FixedOffset] from this datetime, preserving leap seconds. */
    fun checkedSubOffset(rhs: FixedOffset): NaiveDateTime? {
        val (newTime, days) = time.overflowingSubOffset(rhs)
        val newDate = when (days) {
            -1 -> date.predOpt() ?: return null
            1 -> date.succOpt() ?: return null
            else -> date
        }
        return NaiveDateTime(newDate, newTime)
    }

    /** Adds [TimeDelta] to the current date and time. */
    fun checkedAddSigned(rhs: TimeDelta): NaiveDateTime? {
        val (newTime, remainder) = time.overflowingAddSigned(rhs)
        val remainderDelta = TimeDelta.trySeconds(remainder) ?: return null
        val newDate = date.checkedAddSigned(remainderDelta) ?: return null
        return NaiveDateTime(newDate, newTime)
    }

    /** Subtracts [TimeDelta] from the current date and time. */
    fun checkedSubSigned(rhs: TimeDelta): NaiveDateTime? {
        val (newTime, remainder) = time.overflowingSubSigned(rhs)
        val remainderDelta = TimeDelta.trySeconds(remainder) ?: return null
        val newDate = date.checkedSubSigned(remainderDelta) ?: return null
        return NaiveDateTime(newDate, newTime)
    }

    operator fun plus(rhs: TimeDelta): NaiveDateTime =
        checkedAddSigned(rhs) ?: throw ArithmeticException("NaiveDateTime + TimeDelta out of range")

    operator fun minus(rhs: TimeDelta): NaiveDateTime =
        checkedSubSigned(rhs) ?: throw ArithmeticException("NaiveDateTime - TimeDelta out of range")

    operator fun minus(rhs: NaiveDateTime): TimeDelta = signedDurationSince(rhs)

    operator fun plus(offset: FixedOffset): NaiveDateTime =
        checkedAddOffset(offset) ?: throw IllegalArgumentException("NaiveDateTime + FixedOffset out of range")

    operator fun minus(offset: FixedOffset): NaiveDateTime =
        checkedSubOffset(offset) ?: throw IllegalArgumentException("NaiveDateTime - FixedOffset out of range")

    /** Adds [Months] to the current date and time. */
    fun checkedAddMonths(rhs: Months): NaiveDateTime? =
        date.checkedAddMonths(rhs)?.let { NaiveDateTime(it, time) }

    /** Subtracts [Months] from the current date and time. */
    fun checkedSubMonths(rhs: Months): NaiveDateTime? =
        date.checkedSubMonths(rhs)?.let { NaiveDateTime(it, time) }

    /** Subtracts another [NaiveDateTime] from this, returning [TimeDelta]. */
    fun signedDurationSince(rhs: NaiveDateTime): TimeDelta {
        val dateDelta = date.signedDurationSince(rhs.date)
        val timeDelta = time.signedDurationSince(rhs.time)
        return dateDelta + timeDelta
    }

    /** Creates a [DateTime] with [Utc] timezone. */
    fun andUtc(): DateTime<Utc> = DateTime.fromNaiveUtcAndOffset(this, Utc)

    /** Maps this local datetime to a [DateTime] in Utc timezone. */
    @Suppress("UNCHECKED_CAST")
    fun andLocalTimezone(tz: Utc): MappedLocalTime<DateTime<Utc>> =
        tz.fromLocalDatetime(this) as MappedLocalTime<DateTime<Utc>>

    /** Maps this local datetime to a [DateTime] in FixedOffset timezone. */
    @Suppress("UNCHECKED_CAST")
    fun andLocalTimezone(tz: io.github.kotlinmania.chrono.offset.FixedOffset): MappedLocalTime<DateTime<io.github.kotlinmania.chrono.offset.FixedOffset>> =
        tz.fromLocalDatetime(this) as MappedLocalTime<DateTime<io.github.kotlinmania.chrono.offset.FixedOffset>>

    /** Maps this local datetime to a [DateTime] in the specified timezone. */
    fun andLocalTimezone(tz: TimeZone): MappedLocalTime<DateTime<TimeZone>> =
        tz.fromLocalDatetime(this)

    /** Returns the UNIX timestamp in seconds. */
    fun timestamp(): Long {
        val epochDays = date.toEpochDays().toLong()
        return epochDays * 86_400L + time.numSecondsFromMidnight().toLong()
    }

    /** Returns the UNIX timestamp in milliseconds. */
    fun timestampMillis(): Long =
        timestamp() * 1000L + (time.nanosecond() / 1_000_000u).toLong()

    /** Returns the UNIX timestamp in microseconds. */
    fun timestampMicros(): Long =
        timestamp() * 1_000_000L + (time.nanosecond() / 1000u).toLong()

    /** Returns the UNIX timestamp in nanoseconds if in range. */
    fun timestampNanosOpt(): Long? {
        var secs = timestamp()
        var subsecNanos = time.nanosecond().toLong()
        if (secs < 0) {
            subsecNanos -= 1_000_000_000L
            secs += 1L
        }
        if (secs < -9_223_372_036L || secs > 9_223_372_036L) return null
        val mult = secs * 1_000_000_000L
        if (mult / 1_000_000_000L != secs) return null
        val res = mult + subsecNanos
        if (secs > 0 && res < mult) return null
        if (secs < 0 && res > mult) return null
        return res
    }

    override fun compareTo(other: NaiveDateTime): Int {
        val cmp = date.compareTo(other.date)
        return if (cmp != 0) cmp else time.compareTo(other.time)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NaiveDateTime) return false
        return date == other.date && time == other.time
    }

    override fun hashCode(): Int = date.hashCode() * 31 + time.hashCode()

    override fun toString(): String = "${date}T$time"

    companion object {
        val MIN: NaiveDateTime = NaiveDateTime(NaiveDate.MIN, NaiveTime.MIN)
        val MAX: NaiveDateTime = NaiveDateTime(NaiveDate.MAX, NaiveTime.MAX)

        fun new(date: NaiveDate, time: NaiveTime): NaiveDateTime = NaiveDateTime(date, time)
    }
}
