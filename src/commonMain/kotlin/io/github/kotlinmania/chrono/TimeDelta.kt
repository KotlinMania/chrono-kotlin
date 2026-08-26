// port-lint: source time_delta.rs
package io.github.kotlinmania.chrono

import kotlin.math.abs

private const val NANOS_PER_MICRO: Int = 1000
private const val NANOS_PER_MILLI: Int = 1_000_000
internal const val NANOS_PER_SEC: Int = 1_000_000_000
private const val MICROS_PER_SEC: Long = 1_000_000L
private const val MILLIS_PER_SEC: Long = 1000L
private const val SECS_PER_MINUTE: Long = 60L
private const val SECS_PER_HOUR: Long = 3600L
private const val SECS_PER_DAY: Long = 86_400L
private const val SECS_PER_WEEK: Long = 604_800L

/**
 * Time duration with nanosecond precision.
 *
 * This also allows for negative durations.
 */
data class TimeDelta(
    val secs: Long,
    val nanos: Int, // Always 0 <= nanos < NANOS_PER_SEC
) : Comparable<TimeDelta> {

    /** Returns the total number of whole weeks in the [TimeDelta]. */
    fun numWeeks(): Long = numDays() / 7L

    /** Returns the total number of whole days in the [TimeDelta]. */
    fun numDays(): Long = numSeconds() / SECS_PER_DAY

    /** Returns the total number of whole hours in the [TimeDelta]. */
    fun numHours(): Long = numSeconds() / SECS_PER_HOUR

    /** Returns the total number of whole minutes in the [TimeDelta]. */
    fun numMinutes(): Long = numSeconds() / SECS_PER_MINUTE

    /** Returns the total number of whole seconds in the [TimeDelta]. */
    fun numSeconds(): Long =
        if (secs < 0 && nanos > 0) secs + 1 else secs

    /** Returns the fractional number of seconds in the [TimeDelta]. */
    fun asSecondsF64(): Double =
        secs.toDouble() + nanos.toDouble() / NANOS_PER_SEC.toDouble()

    /** Returns the fractional number of seconds in the [TimeDelta]. */
    fun asSecondsF32(): Float =
        secs.toFloat() + nanos.toFloat() / NANOS_PER_SEC.toFloat()

    /** Returns the total number of whole milliseconds in the [TimeDelta]. */
    fun numMilliseconds(): Long {
        val secsPart = numSeconds() * MILLIS_PER_SEC
        val nanosPart = subsecMillis().toLong()
        return secsPart + nanosPart
    }

    /** Returns the number of milliseconds in the fractional part of the duration. */
    fun subsecMillis(): Int = subsecNanos() / NANOS_PER_MILLI

    /** Returns the total number of whole microseconds in the [TimeDelta], or null on overflow. */
    fun numMicroseconds(): Long? {
        val secsPart = checkedMul(numSeconds(), MICROS_PER_SEC) ?: return null
        val nanosPart = (subsecNanos() / NANOS_PER_MICRO).toLong()
        return checkedAdd(secsPart, nanosPart)
    }

    /** Returns the number of microseconds in the fractional part of the duration. */
    fun subsecMicros(): Int = subsecNanos() / NANOS_PER_MICRO

    /** Returns the total number of whole nanoseconds in the [TimeDelta], or null on overflow. */
    fun numNanoseconds(): Long? {
        val secsPart = checkedMul(numSeconds(), NANOS_PER_SEC.toLong()) ?: return null
        val nanosPart = subsecNanos().toLong()
        return checkedAdd(secsPart, nanosPart)
    }

    /** Returns the number of nanoseconds in the fractional part of the duration. */
    fun subsecNanos(): Int =
        if (secs < 0 && nanos > 0) nanos - NANOS_PER_SEC else nanos

    /** Add two [TimeDelta]s, returning null if overflow occurred. */
    fun checkedAdd(rhs: TimeDelta): TimeDelta? {
        var newSecs = secs + rhs.secs
        var newNanos = nanos + rhs.nanos
        if (newNanos >= NANOS_PER_SEC) {
            newNanos -= NANOS_PER_SEC
            newSecs += 1
        }
        return new(newSecs, newNanos.toUInt())
    }

    /** Subtract two [TimeDelta]s, returning null if overflow occurred. */
    fun checkedSub(rhs: TimeDelta): TimeDelta? {
        var newSecs = secs - rhs.secs
        var newNanos = nanos - rhs.nanos
        if (newNanos < 0) {
            newNanos += NANOS_PER_SEC
            newSecs -= 1
        }
        return new(newSecs, newNanos.toUInt())
    }

    operator fun plus(rhs: TimeDelta): TimeDelta =
        checkedAdd(rhs) ?: throw ArithmeticException("TimeDelta + TimeDelta overflowed")

    operator fun minus(rhs: TimeDelta): TimeDelta =
        checkedSub(rhs) ?: throw ArithmeticException("TimeDelta - TimeDelta overflowed")

    operator fun unaryMinus(): TimeDelta {
        return if (nanos == 0) {
            new(-secs, 0u) ?: throw ArithmeticException("TimeDelta negation overflow")
        } else {
            new(-secs - 1, (NANOS_PER_SEC - nanos).toUInt())
                ?: throw ArithmeticException("TimeDelta negation overflow")
        }
    }

    /** Returns the absolute value of the [TimeDelta]. */
    fun abs(): TimeDelta = if (secs < 0) -this else this

    override fun compareTo(other: TimeDelta): Int {
        val cmp = secs.compareTo(other.secs)
        return if (cmp != 0) cmp else nanos.compareTo(other.nanos)
    }

    override fun toString(): String {
        val totalSecs = numSeconds()
        val subNanos = abs(subsecNanos())
        return if (subNanos == 0) {
            "PT${totalSecs}S"
        } else {
            val nanoStr = subNanos.toString().padStart(9, '0').trimEnd('0')
            "PT$totalSecs.${nanoStr}S"
        }
    }

    companion object {
        /** The minimum possible [TimeDelta]: `-Long.MAX_VALUE` milliseconds. */
        val MIN: TimeDelta = TimeDelta(
            -Long.MAX_VALUE / MILLIS_PER_SEC - 1,
            NANOS_PER_SEC + ((-Long.MAX_VALUE % MILLIS_PER_SEC).toInt() * NANOS_PER_MILLI),
        )

        /** The maximum possible [TimeDelta]: `Long.MAX_VALUE` milliseconds. */
        val MAX: TimeDelta = TimeDelta(
            Long.MAX_VALUE / MILLIS_PER_SEC,
            (Long.MAX_VALUE % MILLIS_PER_SEC).toInt() * NANOS_PER_MILLI,
        )

        /** Makes a new [TimeDelta] with given number of seconds and nanoseconds. */
        fun new(secs: Long, nanos: UInt): TimeDelta? {
            if (secs < MIN.secs ||
                secs > MAX.secs ||
                nanos >= 1_000_000_000u ||
                (secs == MAX.secs && nanos > MAX.nanos.toUInt()) ||
                (secs == MIN.secs && nanos < MIN.nanos.toUInt())
            ) {
                return null
            }
            return TimeDelta(secs, nanos.toInt())
        }

        /** Makes a new [TimeDelta] with the given number of weeks. */
        fun tryWeeks(weeks: Long): TimeDelta? {
            val secs = checkedMul(weeks, SECS_PER_WEEK) ?: return null
            return trySeconds(secs)
        }

        /** Makes a new [TimeDelta] with the given number of weeks. */
        fun weeks(weeks: Long): TimeDelta =
            tryWeeks(weeks) ?: throw IllegalArgumentException("TimeDelta.weeks out of bounds")

        /** Makes a new [TimeDelta] with the given number of days. */
        fun tryDays(days: Long): TimeDelta? {
            val secs = checkedMul(days, SECS_PER_DAY) ?: return null
            return trySeconds(secs)
        }

        /** Makes a new [TimeDelta] with the given number of days. */
        fun days(days: Long): TimeDelta =
            tryDays(days) ?: throw IllegalArgumentException("TimeDelta.days out of bounds")

        /** Makes a new [TimeDelta] with the given number of hours. */
        fun tryHours(hours: Long): TimeDelta? {
            val secs = checkedMul(hours, SECS_PER_HOUR) ?: return null
            return trySeconds(secs)
        }

        /** Makes a new [TimeDelta] with the given number of hours. */
        fun hours(hours: Long): TimeDelta =
            tryHours(hours) ?: throw IllegalArgumentException("TimeDelta.hours out of bounds")

        /** Makes a new [TimeDelta] with the given number of minutes. */
        fun tryMinutes(minutes: Long): TimeDelta? {
            val secs = checkedMul(minutes, SECS_PER_MINUTE) ?: return null
            return trySeconds(secs)
        }

        /** Makes a new [TimeDelta] with the given number of minutes. */
        fun minutes(minutes: Long): TimeDelta =
            tryMinutes(minutes) ?: throw IllegalArgumentException("TimeDelta.minutes out of bounds")

        /** Makes a new [TimeDelta] with the given number of seconds. */
        fun trySeconds(seconds: Long): TimeDelta? = new(seconds, 0u)

        /** Makes a new [TimeDelta] with the given number of seconds. */
        fun seconds(seconds: Long): TimeDelta =
            trySeconds(seconds) ?: throw IllegalArgumentException("TimeDelta.seconds out of bounds")

        /** Makes a new [TimeDelta] with the given number of milliseconds. */
        fun tryMilliseconds(milliseconds: Long): TimeDelta? {
            if (milliseconds < -Long.MAX_VALUE) return null
            val (secs, millis) = divModFloor64(milliseconds, MILLIS_PER_SEC)
            return TimeDelta(secs, millis.toInt() * NANOS_PER_MILLI)
        }

        /** Makes a new [TimeDelta] with the given number of milliseconds. */
        fun milliseconds(milliseconds: Long): TimeDelta =
            tryMilliseconds(milliseconds) ?: throw IllegalArgumentException("TimeDelta.milliseconds out of bounds")

        /** Makes a new [TimeDelta] with the given number of microseconds. */
        fun microseconds(microseconds: Long): TimeDelta {
            val (secs, micros) = divModFloor64(microseconds, MICROS_PER_SEC)
            val nanos = micros.toInt() * NANOS_PER_MICRO
            return TimeDelta(secs, nanos)
        }

        /** Makes a new [TimeDelta] with the given number of nanoseconds. */
        fun nanoseconds(nanos: Long): TimeDelta {
            val (secs, remNanos) = divModFloor64(nanos, NANOS_PER_SEC.toLong())
            return TimeDelta(secs, remNanos.toInt())
        }

        /** Makes a new [TimeDelta] with a value of zero. */
        fun zero(): TimeDelta = TimeDelta(0L, 0)
    }
}

private fun divModFloor64(a: Long, b: Long): Pair<Long, Long> {
    val div = a.floorDiv(b)
    val rem = a.mod(b)
    return div to rem
}

private fun checkedAdd(a: Long, b: Long): Long? {
    val res = a + b
    if (((a xor res) and (b xor res)) < 0) return null
    return res
}

private fun checkedMul(a: Long, b: Long): Long? {
    if (a == 0L || b == 0L) return 0L
    val res = a * b
    if (a == Long.MIN_VALUE && b == -1L) return null
    if (res / a != b) return null
    return res
}
