// port-lint: source naive/time/mod.rs
package io.github.kotlinmania.chrono.naive

import io.github.kotlinmania.chrono.TimeDelta
import io.github.kotlinmania.chrono.Timelike

/**
 * ISO 8601 time without timezone.
 *
 * Allows for nanosecond precision and optional leap second representation.
 */
class NaiveTime internal constructor(
    internal val secs: UInt, // 0..86399
    internal val frac: UInt, // 0..1_999_999_999 (>= 1_000_000_000 indicates leap second)
) : Timelike<NaiveTime>, Comparable<NaiveTime> {

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
        val s = second().toString().padStart(2, '0')
        return if (frac == 0u) {
            "$h:$m:$s"
        } else {
            val nanoStr = frac.toString().padStart(9, '0').trimEnd('0')
            "$h:$m:$s.$nanoStr"
        }
    }

    companion object {
        /** The earliest possible [NaiveTime]: 00:00:00. */
        val MIN: NaiveTime = NaiveTime(0u, 0u)

        /** The latest possible non-leap [NaiveTime]: 23:59:59.999999999. */
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
            val isLeap = secs == 86399u && nano >= 1_000_000_000u
            if (nano >= 2_000_000_000u || (!isLeap && nano >= 1_000_000_000u)) return null
            return NaiveTime(secs, nano)
        }
    }
}
