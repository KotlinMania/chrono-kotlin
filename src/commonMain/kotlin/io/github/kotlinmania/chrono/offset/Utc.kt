// port-lint: source offset/utc.rs
package io.github.kotlinmania.chrono.offset

import io.github.kotlinmania.chrono.Date
import io.github.kotlinmania.chrono.DateTime
import io.github.kotlinmania.chrono.naive.NaiveDate
import io.github.kotlinmania.chrono.naive.NaiveDateTime
import kotlin.time.Clock

/**
 * The UTC, Coordinated Universal Time, time zone.
 *
 * This is the most efficient time zone when you do not need the local time. It
 * is also used as an offset, which is a dummy type.
 *
 * Using the [TimeZone] methods on [Utc] is the preferred way to construct
 * UTC date-time instances.
 */
data object Utc : TimeZone<Utc>, Offset {
    /**
     * Returns a `Date` which corresponds to the current date.
     */
    @Deprecated("Use Utc.now() instead, potentially with dateNaive().")
    @Suppress("DEPRECATION")
    fun today(): Date<Utc> = now().date()

    /**
     * Returns a `DateTime` which corresponds to the current date and time in
     * UTC.
     *
     * See also the similar `Local.now()` which returns the local date and time
     * including the offset from UTC.
     */
    fun now(): DateTime<Utc> {
        val now = Clock.System.now()
        return DateTime.fromTimestamp(now.epochSeconds, now.nanosecondsOfSecond.toUInt())!!
    }

    override fun fromOffset(state: Utc): Utc = Utc

    override fun offsetFromLocalDate(local: NaiveDate): MappedLocalTime<Utc> = MappedLocalTime.Single(Utc)

    override fun offsetFromLocalDatetime(local: NaiveDateTime): MappedLocalTime<Utc> = MappedLocalTime.Single(Utc)

    override fun offsetFromUtcDate(utc: NaiveDate): Utc = Utc

    override fun offsetFromUtcDatetime(utc: NaiveDateTime): Utc = Utc

    override fun fix(): FixedOffset = FixedOffset.eastOpt(0)!!

    /** The debugging representation for UTC. */
    fun debugString(): String = "Z"

    override fun toString(): String = "UTC"
}
