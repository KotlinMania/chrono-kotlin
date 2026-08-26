// port-lint: source src/datetime/mod.rs
package io.github.kotlinmania.chrono

import io.github.kotlinmania.chrono.naive.Days
import io.github.kotlinmania.chrono.naive.IsoWeek
import io.github.kotlinmania.chrono.naive.NaiveDate
import io.github.kotlinmania.chrono.naive.NaiveDateTime
import io.github.kotlinmania.chrono.naive.NaiveTime
import io.github.kotlinmania.chrono.offset.FixedOffset
import io.github.kotlinmania.chrono.offset.MappedLocalTime
import io.github.kotlinmania.chrono.offset.TimeZone
import io.github.kotlinmania.chrono.offset.Utc

/**
 * ISO 8601 combined date and time with time zone.
 */
class DateTime<Tz : TimeZone<Tz>> internal constructor(
    internal val datetime: NaiveDateTime, // in UTC
    internal val offset: Tz,
) : Datelike<DateTime<Tz>>, Timelike<DateTime<Tz>>, Comparable<DateTime<*>> {

    /** Returns the UTC [NaiveDateTime]. */
    fun naiveUtc(): NaiveDateTime = datetime

    /** Returns the local [NaiveDateTime]. */
    fun naiveLocal(): NaiveDateTime {
        val fixed = (offset as? FixedOffset) ?: offset.offsetFromUtcDatetime(datetime).let {
            if (it is FixedOffset) it else (offset as io.github.kotlinmania.chrono.offset.Offset).fix()
        }
        return datetime.checkedAddOffset(fixed) ?: datetime
    }

    /** Retrieves the date component. */
    fun dateNaive(): NaiveDate = naiveLocal().date()

    /** Retrieves the time component. */
    fun time(): NaiveTime = naiveLocal().time()

    /** Retrieves the associated offset. */
    fun offset(): Tz = offset

    /** Retrieves the associated timezone. */
    fun timezone(): Tz = offset

    /** Returns the UNIX timestamp in seconds. */
    fun timestamp(): Long = datetime.timestamp()

    /** Returns the UNIX timestamp in milliseconds. */
    fun timestampMillis(): Long = datetime.timestampMillis()

    /** Returns the UNIX timestamp in microseconds. */
    fun timestampMicros(): Long = datetime.timestampMicros()

    /** Returns the UNIX timestamp in nanoseconds if in range. */
    fun timestampNanosOpt(): Long? = datetime.timestampNanosOpt()

    /** Returns the subsecond milliseconds (0..999). */
    fun timestampSubsecMillis(): UInt = datetime.time().nanosecond() / 1_000_000u

    /** Returns the subsecond microseconds (0..999999). */
    fun timestampSubsecMicros(): UInt = datetime.time().nanosecond() / 1000u

    /** Returns the subsecond nanoseconds (0..999999999). */
    fun timestampSubsecNanos(): UInt = datetime.time().nanosecond()

    /** Converts to a [DateTime] with [Utc] timezone. */
    fun toUtc(): DateTime<Utc> = DateTime(datetime, Utc)

    /** Converts to a [DateTime] with [FixedOffset] timezone. */
    fun fixedOffset(): DateTime<FixedOffset> {
        val fixed = (offset as io.github.kotlinmania.chrono.offset.Offset).fix()
        return DateTime(datetime, fixed)
    }

    @Deprecated("Use dateNaive() instead")
    @Suppress("DEPRECATION")
    fun date(): Date<Tz> = Date.fromUtc(dateNaive(), offset)

    override fun year(): Int = naiveLocal().year()

    override fun month(): UInt = naiveLocal().month()

    override fun month0(): UInt = naiveLocal().month0()

    fun monthEnum(): Month = Month.fromU32(month())!!

    override fun day(): UInt = naiveLocal().day()

    override fun day0(): UInt = naiveLocal().day0()

    override fun ordinal(): UInt = naiveLocal().ordinal()

    override fun ordinal0(): UInt = naiveLocal().ordinal0()

    override fun weekday(): Weekday = naiveLocal().weekday()

    override fun isoWeek(): IsoWeek = naiveLocal().isoWeek()

    override fun numDaysFromCe(): Int = naiveLocal().numDaysFromCe()

    override fun withYear(year: Int): DateTime<Tz>? =
        naiveLocal().withYear(year)?.let { timezone().fromLocalDatetime(it).single() }

    override fun withMonth(month: UInt): DateTime<Tz>? =
        naiveLocal().withMonth(month)?.let { timezone().fromLocalDatetime(it).single() }

    override fun withMonth0(month0: UInt): DateTime<Tz>? =
        naiveLocal().withMonth0(month0)?.let { timezone().fromLocalDatetime(it).single() }

    override fun withDay(day: UInt): DateTime<Tz>? =
        naiveLocal().withDay(day)?.let { timezone().fromLocalDatetime(it).single() }

    override fun withDay0(day0: UInt): DateTime<Tz>? =
        naiveLocal().withDay0(day0)?.let { timezone().fromLocalDatetime(it).single() }

    override fun withOrdinal(ordinal: UInt): DateTime<Tz>? =
        naiveLocal().withOrdinal(ordinal)?.let { timezone().fromLocalDatetime(it).single() }

    override fun withOrdinal0(ordinal0: UInt): DateTime<Tz>? =
        naiveLocal().withOrdinal0(ordinal0)?.let { timezone().fromLocalDatetime(it).single() }

    override fun hour(): UInt = time().hour()

    override fun minute(): UInt = time().minute()

    override fun second(): UInt = time().second()

    override fun nanosecond(): UInt = time().nanosecond()

    override fun withHour(hour: UInt): DateTime<Tz>? =
        naiveLocal().withHour(hour)?.let { timezone().fromLocalDatetime(it).single() }

    override fun withMinute(min: UInt): DateTime<Tz>? =
        naiveLocal().withMinute(min)?.let { timezone().fromLocalDatetime(it).single() }

    override fun withSecond(sec: UInt): DateTime<Tz>? =
        naiveLocal().withSecond(sec)?.let { timezone().fromLocalDatetime(it).single() }

    override fun withNanosecond(nano: UInt): DateTime<Tz>? =
        naiveLocal().withNanosecond(nano)?.let { timezone().fromLocalDatetime(it).single() }

    /** Adds a signed duration to the datetime. */
    fun checkedAddSigned(rhs: TimeDelta): DateTime<Tz>? =
        datetime.checkedAddSigned(rhs)?.let { DateTime(it, offset) }

    /** Subtracts a signed duration from the datetime. */
    fun checkedSubSigned(rhs: TimeDelta): DateTime<Tz>? =
        datetime.checkedSubSigned(rhs)?.let { DateTime(it, offset) }

    /** Adds months to the datetime. */
    fun checkedAddMonths(months: Months): DateTime<Tz>? =
        naiveLocal().checkedAddMonths(months)?.let { timezone().fromLocalDatetime(it).single() }

    /** Subtracts months from the datetime. */
    fun checkedSubMonths(months: Months): DateTime<Tz>? =
        naiveLocal().checkedSubMonths(months)?.let { timezone().fromLocalDatetime(it).single() }

    /** Subtracts another [DateTime] from this, returning [TimeDelta]. */
    fun signedDurationSince(rhs: DateTime<*>): TimeDelta =
        datetime.signedDurationSince(rhs.datetime)

    operator fun plus(rhs: TimeDelta): DateTime<Tz> =
        checkedAddSigned(rhs) ?: throw ArithmeticException("DateTime + TimeDelta overflowed")

    operator fun minus(rhs: TimeDelta): DateTime<Tz> =
        checkedSubSigned(rhs) ?: throw ArithmeticException("DateTime - TimeDelta overflowed")

    operator fun minus(rhs: DateTime<*>): TimeDelta = signedDurationSince(rhs)

    override fun compareTo(other: DateTime<*>): Int =
        datetime.compareTo(other.datetime)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DateTime<*>) return false
        return datetime == other.datetime && offset == other.offset
    }

    override fun hashCode(): Int = datetime.hashCode() * 31 + offset.hashCode()

    override fun toString(): String {
        val local = naiveLocal()
        val offStr = offset.toString()
        return "${local}$offStr"
    }

    /** Converts this [DateTime] to another timezone with the same UTC time. */
    fun <OtherTz : TimeZone<OtherTz>> withTimezone(tz: OtherTz): DateTime<OtherTz> =
        DateTime(datetime, tz)

    companion object {
        fun <Tz : TimeZone<Tz>> fromNaiveUtcAndOffset(
            datetime: NaiveDateTime,
            offset: Tz,
        ): DateTime<Tz> = DateTime(datetime, offset)

        /** Creates a [DateTime] in [Utc] from UNIX timestamp seconds and nanoseconds. */
        fun fromTimestampOpt(secs: Long, nsecs: UInt): DateTime<Utc>? {
            if (nsecs >= 2_000_000_000u) return null
            val days = secs.floorDiv(86_400L)
            val remSecs = secs.mod(86_400L).toUInt()
            val date = NaiveDate.fromEpochDays(days.toInt()) ?: return null
            val time = NaiveTime.fromNumSecondsFromMidnightOpt(remSecs, nsecs) ?: return null
            return DateTime(NaiveDateTime(date, time), Utc)
        }

        /** Creates a [DateTime] in [Utc] from UNIX timestamp seconds and nanoseconds. */
        fun fromTimestamp(secs: Long, nsecs: UInt): DateTime<Utc>? = fromTimestampOpt(secs, nsecs)

        /** Creates a [DateTime] in [Utc] from UNIX timestamp seconds. */
        fun fromTimestampSecs(secs: Long): DateTime<Utc>? = fromTimestampOpt(secs, 0u)

        /** Creates a [DateTime] in [Utc] from UNIX timestamp milliseconds. */
        fun fromTimestampMillis(millis: Long): DateTime<Utc>? {
            val secs = millis.floorDiv(1000L)
            val remNanos = (millis.mod(1000L) * 1_000_000L).toUInt()
            return fromTimestampOpt(secs, remNanos)
        }

        /** Creates a [DateTime] in [Utc] from UNIX timestamp microseconds. */
        fun fromTimestampMicros(micros: Long): DateTime<Utc>? {
            val secs = micros.floorDiv(1_000_000L)
            val remNanos = (micros.mod(1_000_000L) * 1000L).toUInt()
            return fromTimestampOpt(secs, remNanos)
        }

        /** Creates a [DateTime] in [Utc] from UNIX timestamp nanoseconds. */
        fun fromTimestampNanos(nanos: Long): DateTime<Utc> {
            val secs = nanos.floorDiv(1_000_000_000L)
            val remNanos = nanos.mod(1_000_000_000L).toUInt()
            return fromTimestampOpt(secs, remNanos)
                ?: throw IllegalArgumentException("DateTime.fromTimestampNanos out of bounds")
        }
    }
}
