// port-lint: source naive/time/mod.rs
package io.github.kotlinmania.chrono.naive

import io.github.kotlinmania.chrono.TimeDelta
import io.github.kotlinmania.chrono.Timelike
import io.github.kotlinmania.chrono.format.parseNaiveTime
import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.ser.Serialize
import io.github.kotlinmania.serdecore.ser.Serializer

/**
 * ISO 8601 time without timezone.
 *
 * Allows for nanosecond precision and optional leap second representation.
 */
class NaiveTime internal constructor(
    internal val secs: UInt, // 0..86399
    internal val frac: UInt, // 0..1_999_999_999 (>= 1_000_000_000 indicates leap second)
) : Timelike<NaiveTime>,
    Comparable<NaiveTime>,
    Serialize {
    override fun hour(): UInt = secs / 3600u

    override fun minute(): UInt = (secs % 3600u) / 60u

    override fun second(): UInt = secs % 60u

    override fun nanosecond(): UInt = frac

    override fun withHour(hour: UInt): NaiveTime? {
        if (hour >= 24u) return null
        return fromHmsNanoOpt(hour, minute(), second(), frac)
    }

    override fun withMinute(min: UInt): NaiveTime? {
        if (min >= 60u) return null
        return fromHmsNanoOpt(hour(), min, second(), frac)
    }

    override fun withSecond(sec: UInt): NaiveTime? {
        if (sec >= 60u) return null
        return fromHmsNanoOpt(hour(), minute(), sec, frac)
    }

    override fun withNanosecond(nano: UInt): NaiveTime? {
        val isLeap = second() == 59u
        val maxNano = if (isLeap) 1_999_999_999u else 999_999_999u
        if (nano > maxNano) return null
        return NaiveTime(secs, nano)
    }

    override fun numSecondsFromMidnight(): UInt = secs

    /** Returns the number of non-leap seconds and nanoseconds from midnight. */
    fun numSecondsAndNanosecondsFromMidnight(): Pair<UInt, UInt> = secs to frac

    /**
     * Adds [TimeDelta] to the time, returning the result and the number of seconds in integral days overflowing.
     */
    fun overflowingAddSigned(rhs: TimeDelta): Pair<NaiveTime, Long> {
        var s = secs.toLong()
        var f = frac.toInt()
        val secsToAdd = rhs.numSeconds()
        val fracToAdd = rhs.subsecNanos()

        if (f >= 1_000_000_000) {
            if (secsToAdd > 0 || (fracToAdd > 0 && f >= 2_000_000_000 - fracToAdd)) {
                f -= 1_000_000_000
            } else if (secsToAdd < 0) {
                f -= 1_000_000_000
                s += 1
            } else {
                return Pair(NaiveTime(secs, (f + fracToAdd).toUInt()), 0L)
            }
        }

        s += secsToAdd
        f += fracToAdd

        if (f < 0) {
            f += 1_000_000_000
            s -= 1
        } else if (f >= 1_000_000_000) {
            f -= 1_000_000_000
            s += 1
        }

        val secsInDay = s.mod(86_400L)
        val remaining = s - secsInDay
        return Pair(NaiveTime(secsInDay.toUInt(), f.toUInt()), remaining)
    }

    /**
     * Subtracts [TimeDelta] from the time, returning the result and the number of seconds in integral days underflowing.
     */
    fun overflowingSubSigned(rhs: TimeDelta): Pair<NaiveTime, Long> {
        val (time, rem) = overflowingAddSigned(-rhs)
        return Pair(time, -rem)
    }

    /** Subtracts another [NaiveTime] from this, returning a [TimeDelta]. */
    fun signedDurationSince(rhs: NaiveTime): TimeDelta {
        var s = secs.toLong() - rhs.secs.toLong()
        val f = frac.toLong() - rhs.frac.toLong()

        if (secs > rhs.secs && rhs.frac >= 1_000_000_000u) {
            s += 1
        } else if (secs < rhs.secs && frac >= 1_000_000_000u) {
            s -= 1
        }

        val secsFromFrac = f.floorDiv(1_000_000_000L)
        val remFrac = f.mod(1_000_000_000L).toUInt()
        return TimeDelta.new(s + secsFromFrac, remFrac) ?: throw IllegalArgumentException("out of range")
    }

    operator fun plus(rhs: TimeDelta): NaiveTime = overflowingAddSigned(rhs).first

    operator fun minus(rhs: TimeDelta): NaiveTime = overflowingSubSigned(rhs).first

    fun overflowingAddOffset(offset: io.github.kotlinmania.chrono.offset.FixedOffset): Pair<NaiveTime, Int> {
        val s = secs.toInt() + offset.localMinusUtc
        val days = s.floorDiv(86_400)
        val remSecs = s.mod(86_400).toUInt()
        return Pair(NaiveTime(remSecs, frac), days)
    }

    fun overflowingSubOffset(offset: io.github.kotlinmania.chrono.offset.FixedOffset): Pair<NaiveTime, Int> {
        val s = secs.toInt() - offset.localMinusUtc
        val days = s.floorDiv(86_400)
        val remSecs = s.mod(86_400).toUInt()
        return Pair(NaiveTime(remSecs, frac), days)
    }

    operator fun plus(offset: io.github.kotlinmania.chrono.offset.FixedOffset): NaiveTime =
        overflowingAddOffset(offset).first

    operator fun minus(offset: io.github.kotlinmania.chrono.offset.FixedOffset): NaiveTime =
        overflowingSubOffset(offset).first

    operator fun minus(rhs: NaiveTime): TimeDelta = signedDurationSince(rhs)

    override fun compareTo(other: NaiveTime): Int {
        val cmp = secs.compareTo(other.secs)
        return if (cmp != 0) cmp else frac.compareTo(other.frac)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NaiveTime) return false
        return secs == other.secs && frac == other.frac
    }

    override fun hashCode(): Int = (secs * 31u + frac).toInt()

    override fun toString(): String {
        val h = hour().toString().padStart(2, '0')
        val m = minute().toString().padStart(2, '0')
        val (sec, nano) =
            if (frac >= 1_000_000_000u) {
                (second() + 1u) to (frac - 1_000_000_000u)
            } else {
                second() to frac
            }
        val s = sec.toString().padStart(2, '0')
        return when {
            nano == 0u -> {
                "$h:$m:$s"
            }

            nano % 1_000_000u == 0u -> {
                val milliStr = (nano / 1_000_000u).toString().padStart(3, '0')
                "$h:$m:$s.$milliStr"
            }

            nano % 1_000u == 0u -> {
                val microStr = (nano / 1_000u).toString().padStart(6, '0')
                "$h:$m:$s.$microStr"
            }

            else -> {
                val nanoStr = nano.toString().padStart(9, '0')
                "$h:$m:$s.$nanoStr"
            }
        }
    }

    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serializer.collectStr(toString())

    companion object : Deserialize<NaiveTime> {
        /** The earliest possible [NaiveTime]: 00:00:00. */
        val MIN: NaiveTime = NaiveTime(0u, 0u)

        /** The latest possible [NaiveTime]: 23:59:59.999999999 (or 23:59:60.999999999 leap). */
        val MAX: NaiveTime = NaiveTime(86399u, 1_999_999_999u)

        /** Makes a new [NaiveTime] from hour, minute and second. */
        fun fromHmsOpt(hour: UInt, min: UInt, sec: UInt): NaiveTime? =
            fromHmsNanoOpt(hour, min, sec, 0u)

        /** Makes a new [NaiveTime] from hour, minute and second. */
        fun fromHms(hour: UInt, min: UInt, sec: UInt): NaiveTime =
            fromHmsOpt(hour, min, sec) ?: throw IllegalArgumentException("invalid time")

        /** Makes a new [NaiveTime] from hour, minute, second and millisecond. */
        fun fromHmsMilliOpt(hour: UInt, min: UInt, sec: UInt, milli: UInt): NaiveTime? {
            val nanoLong = milli.toLong() * 1_000_000L
            if (nanoLong > UInt.MAX_VALUE.toLong()) return null
            return fromHmsNanoOpt(hour, min, sec, nanoLong.toUInt())
        }

        /** Makes a new [NaiveTime] from hour, minute, second and microsecond. */
        fun fromHmsMicroOpt(hour: UInt, min: UInt, sec: UInt, micro: UInt): NaiveTime? {
            val nanoLong = micro.toLong() * 1_000L
            if (nanoLong > UInt.MAX_VALUE.toLong()) return null
            return fromHmsNanoOpt(hour, min, sec, nanoLong.toUInt())
        }

        /** Makes a new [NaiveTime] from hour, minute, second and nanosecond. */
        fun fromHmsNanoOpt(hour: UInt, min: UInt, sec: UInt, nano: UInt): NaiveTime? {
            if (hour >= 24u || min >= 60u) return null
            val isLeap = sec == 59u && nano >= 1_000_000_000u
            if (sec >= 60u && !isLeap) return null
            if (nano >= 2_000_000_000u || (!isLeap && nano >= 1_000_000_000u)) return null
            val secsVal = hour * 3600u + min * 60u + sec
            return NaiveTime(secsVal, nano)
        }

        /** Makes a new [NaiveTime] from seconds and nanoseconds from midnight. */
        fun fromNumSecondsFromMidnightOpt(secs: UInt, nano: UInt): NaiveTime? {
            if (secs >= 86_400u) return null
            val isLeap = secs % 60u == 59u && nano >= 1_000_000_000u
            if (nano >= 2_000_000_000u || (!isLeap && nano >= 1_000_000_000u)) return null
            return NaiveTime(secs, nano)
        }

        /** Parses a string into a [NaiveTime] using the `%H:%M:%S%.f` format. */
        fun fromStr(s: String): Result<NaiveTime> = parseNaiveTime(s)

        override fun <D> deserialize(deserializer: D): SerdeResult<NaiveTime> where D : Deserializer =
            deserializer.deserializeStr(NaiveTimeVisitor)
    }
}

private object NaiveTimeVisitor : Visitor<NaiveTime> {
    override fun expecting(): String = "a formatted time string"

    override fun visitStr(v: String): SerdeResult<NaiveTime> =
        NaiveTime.fromStr(v).fold(
            onSuccess = { SerdeResult.Success(it) },
            onFailure = { SerdeResult.Failure(SerdeError.custom(it.toString())) },
        )
}
