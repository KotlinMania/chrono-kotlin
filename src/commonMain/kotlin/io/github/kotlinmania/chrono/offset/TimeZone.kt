// port-lint: source offset/mod.rs
package io.github.kotlinmania.chrono.offset

import io.github.kotlinmania.chrono.Date
import io.github.kotlinmania.chrono.DateTime
import io.github.kotlinmania.chrono.naive.NaiveDate
import io.github.kotlinmania.chrono.naive.NaiveDateTime

/**
 * The time zone, which calculates offsets from local time to UTC.
 */
interface TimeZone<Tz : TimeZone<Tz>> {
    fun fromOffset(state: Tz): Tz

    fun offsetFromLocalDate(local: NaiveDate): MappedLocalTime<Tz>

    fun offsetFromLocalDatetime(local: NaiveDateTime): MappedLocalTime<Tz>

    fun offsetFromUtcDate(utc: NaiveDate): Tz

    fun offsetFromUtcDatetime(utc: NaiveDateTime): Tz

    fun fromLocalDatetime(local: NaiveDateTime): MappedLocalTime<DateTime<Tz>> {
        return offsetFromLocalDatetime(local).andThen { offset ->
            val fixed = (offset as? Offset)?.fix() ?: (this as? Offset)?.fix() ?: FixedOffset.east(0)
            val opt = local.checkedSubOffset(fixed)
            if (opt != null) {
                @Suppress("UNCHECKED_CAST")
                MappedLocalTime.Single(DateTime.fromNaiveUtcAndOffset(opt, this as Tz))
            } else {
                MappedLocalTime.None
            }
        }
    }

    fun fromUtcDatetime(utc: NaiveDateTime): DateTime<Tz> {
        val offset = offsetFromUtcDatetime(utc)
        @Suppress("UNCHECKED_CAST")
        return DateTime.fromNaiveUtcAndOffset(utc, this as Tz)
    }

    fun withYmdAndHms(
        year: Int,
        month: UInt,
        day: UInt,
        hour: UInt,
        min: UInt,
        sec: UInt,
    ): MappedLocalTime<DateTime<Tz>> {
        val d = NaiveDate.fromYmdOpt(year, month, day) ?: return MappedLocalTime.None
        val dt = d.andHmsOpt(hour, min, sec) ?: return MappedLocalTime.None
        return fromLocalDatetime(dt)
    }
}
