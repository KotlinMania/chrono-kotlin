// port-lint: source src/datetime/mod.rs
package io.github.kotlinmania.chrono

import io.github.kotlinmania.chrono.format.SecondsFormat
import io.github.kotlinmania.chrono.format.parseRfc3339Relaxed
import io.github.kotlinmania.chrono.format.writeRfc3339
import io.github.kotlinmania.chrono.naive.IsoWeek
import io.github.kotlinmania.chrono.naive.NaiveDate
import io.github.kotlinmania.chrono.naive.NaiveDateTime
import io.github.kotlinmania.chrono.naive.NaiveTime
import io.github.kotlinmania.chrono.offset.FixedOffset
import io.github.kotlinmania.chrono.offset.Offset
import io.github.kotlinmania.chrono.offset.TimeZone
import io.github.kotlinmania.chrono.offset.Utc
import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.ser.Serialize
import io.github.kotlinmania.serdecore.ser.Serializer

/**
 * ISO 8601 combined date and time with time zone.
 */
class DateTime<Tz : TimeZone> internal constructor(
    internal val datetime: NaiveDateTime, // in UTC
    internal val offset: Tz,
) : Datelike<DateTime<Tz>>,
    Timelike<DateTime<Tz>>,
    Comparable<DateTime<*>>,
    Serialize {
    /** Returns the UTC [NaiveDateTime]. */
    fun naiveUtc(): NaiveDateTime = datetime

    /** Returns the local [NaiveDateTime]. */
    fun naiveLocal(): NaiveDateTime {
        val fixed =
            (offset as? FixedOffset) ?: offset.offsetFromUtcDatetime(datetime).let {
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
    fun date(): Date<Tz> = Date(dateNaive(), offset)

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

    private fun fromLocal(local: NaiveDateTime): DateTime<Tz>? {
        val fixed = (offset as? io.github.kotlinmania.chrono.offset.Offset)?.fix() ?: FixedOffset.east(0)
        val utc = local.checkedSubOffset(fixed) ?: return null
        return DateTime(utc, offset)
    }

    override fun withYear(year: Int): DateTime<Tz>? =
        naiveLocal().withYear(year)?.let { fromLocal(it) }

    override fun withMonth(month: UInt): DateTime<Tz>? =
        naiveLocal().withMonth(month)?.let { fromLocal(it) }

    override fun withMonth0(month0: UInt): DateTime<Tz>? =
        naiveLocal().withMonth0(month0)?.let { fromLocal(it) }

    override fun withDay(day: UInt): DateTime<Tz>? =
        naiveLocal().withDay(day)?.let { fromLocal(it) }

    override fun withDay0(day0: UInt): DateTime<Tz>? =
        naiveLocal().withDay0(day0)?.let { fromLocal(it) }

    override fun withOrdinal(ordinal: UInt): DateTime<Tz>? =
        naiveLocal().withOrdinal(ordinal)?.let { fromLocal(it) }

    override fun withOrdinal0(ordinal0: UInt): DateTime<Tz>? =
        naiveLocal().withOrdinal0(ordinal0)?.let { fromLocal(it) }

    override fun hour(): UInt = time().hour()

    override fun minute(): UInt = time().minute()

    override fun second(): UInt = time().second()

    override fun nanosecond(): UInt = time().nanosecond()

    override fun withHour(hour: UInt): DateTime<Tz>? =
        naiveLocal().withHour(hour)?.let { fromLocal(it) }

    override fun withMinute(min: UInt): DateTime<Tz>? =
        naiveLocal().withMinute(min)?.let { fromLocal(it) }

    override fun withSecond(sec: UInt): DateTime<Tz>? =
        naiveLocal().withSecond(sec)?.let { fromLocal(it) }

    override fun withNanosecond(nano: UInt): DateTime<Tz>? =
        naiveLocal().withNanosecond(nano)?.let { fromLocal(it) }

    /** Adds a signed duration to the datetime. */
    fun checkedAddSigned(rhs: TimeDelta): DateTime<Tz>? =
        datetime.checkedAddSigned(rhs)?.let { DateTime(it, offset) }

    /** Subtracts a signed duration from the datetime. */
    fun checkedSubSigned(rhs: TimeDelta): DateTime<Tz>? =
        datetime.checkedSubSigned(rhs)?.let { DateTime(it, offset) }

    /** Adds months to the datetime. */
    fun checkedAddMonths(months: Months): DateTime<Tz>? =
        naiveLocal().checkedAddMonths(months)?.let { fromLocal(it) }

    /** Subtracts months from the datetime. */
    fun checkedSubMonths(months: Months): DateTime<Tz>? =
        naiveLocal().checkedSubMonths(months)?.let { fromLocal(it) }

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

    /** Returns an RFC 3339 and ISO 8601 date and time string. */
    fun toRfc3339(): String = toRfc3339Opts(SecondsFormat.AutoSi, false)

    /** Returns an RFC 3339 and ISO 8601 date and time string with the specified options. */
    fun toRfc3339Opts(secform: SecondsFormat, useZ: Boolean): String {
        val fixed =
            (offset as? Offset)?.fix() ?: offset.offsetFromUtcDatetime(datetime).let {
                (it as? Offset)?.fix() ?: FixedOffset.east(0)
            }
        return writeRfc3339(naiveLocal(), fixed, secform, useZ)
    }

    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serializer.collectStr(toRfc3339Opts(SecondsFormat.AutoSi, true))

    /** Converts this [DateTime] to UTC timezone with the same UTC time. */
    fun withTimezone(tz: Utc): DateTime<Utc> =
        DateTime(datetime, tz)

    /** Converts this [DateTime] to a fixed offset timezone with the same UTC time. */
    fun withTimezone(tz: FixedOffset): DateTime<FixedOffset> =
        DateTime(datetime, tz)

    /** Converts this [DateTime] to another timezone with the same UTC time. */
    fun withTimezone(tz: TimeZone): DateTime<TimeZone> =
        DateTime(datetime, tz)

    /** Deserializer for `DateTime<Utc>`. */
    object UtcDeserializer : Deserialize<DateTime<Utc>> {
        override fun <D> deserialize(deserializer: D): SerdeResult<DateTime<Utc>> where D : Deserializer =
            deserializer.deserializeStr(DateTimeVisitor).map { it.withTimezone(Utc) }
    }

    companion object : Deserialize<DateTime<FixedOffset>> {
        fun fromNaiveUtcAndOffset(
            datetime: NaiveDateTime,
            offset: Utc,
        ): DateTime<Utc> = DateTime(datetime, offset)

        fun fromNaiveUtcAndOffset(
            datetime: NaiveDateTime,
            offset: FixedOffset,
        ): DateTime<FixedOffset> = DateTime(datetime, offset)

        fun fromNaiveUtcAndOffset(
            datetime: NaiveDateTime,
            offset: TimeZone,
        ): DateTime<TimeZone> = DateTime(datetime, offset)

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

        /** Parses an RFC 3339 date-and-time string into a `DateTime<FixedOffset>`. */
        fun parseFromRfc3339(s: String): Result<DateTime<FixedOffset>> = parseRfc3339Relaxed(s)

        /** Parses a relaxed RFC 3339 string into a `DateTime<FixedOffset>`. */
        fun fromStr(s: String): Result<DateTime<FixedOffset>> = parseRfc3339Relaxed(s)

        /** Parses a relaxed RFC 3339 string into a `DateTime<Utc>`. */
        fun fromStrUtc(s: String): Result<DateTime<Utc>> =
            parseRfc3339Relaxed(s).map { it.withTimezone(Utc) }

        override fun <D> deserialize(deserializer: D): SerdeResult<DateTime<FixedOffset>> where D : Deserializer =
            deserializer.deserializeStr(DateTimeVisitor)
    }
}

private object DateTimeVisitor : Visitor<DateTime<FixedOffset>> {
    override fun expecting(): String = "an RFC 3339 formatted date and time string"

    override fun visitStr(v: String): SerdeResult<DateTime<FixedOffset>> =
        DateTime.fromStr(v).fold(
            onSuccess = { SerdeResult.Success(it) },
            onFailure = { SerdeResult.Failure(SerdeError.custom(it.toString())) },
        )
}
