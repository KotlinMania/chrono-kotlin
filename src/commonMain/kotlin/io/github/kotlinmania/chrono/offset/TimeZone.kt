// port-lint: source offset/mod.rs
package io.github.kotlinmania.chrono.offset

import io.github.kotlinmania.chrono.DateTime
import io.github.kotlinmania.chrono.naive.NaiveDate
import io.github.kotlinmania.chrono.naive.NaiveDateTime

/**
 * The time zone, which calculates offsets from local time to UTC.
 */
interface TimeZone {
    fun fromOffset(state: TimeZone): TimeZone

    fun offsetFromLocalDate(local: NaiveDate): MappedLocalTime<TimeZone>

    fun offsetFromLocalDatetime(local: NaiveDateTime): MappedLocalTime<TimeZone>

    fun offsetFromUtcDate(utc: NaiveDate): TimeZone

    fun offsetFromUtcDatetime(utc: NaiveDateTime): TimeZone

    fun fromLocalDatetime(local: NaiveDateTime): MappedLocalTime<DateTime<TimeZone>> =
        offsetFromLocalDatetime(local).andThen { offset ->
            val fixed = (offset as? Offset)?.fix() ?: (this as? Offset)?.fix() ?: FixedOffset.east(0)
            val opt = local.checkedSubOffset(fixed)
            if (opt != null) {
                MappedLocalTime.Single(DateTime.fromNaiveUtcAndOffset(opt, offset))
            } else {
                MappedLocalTime.None
            }
        }

    fun fromUtcDatetime(utc: NaiveDateTime): DateTime<TimeZone> =
        DateTime.fromNaiveUtcAndOffset(utc, offsetFromUtcDatetime(utc))

    fun withYmdAndHms(
        year: Int,
        month: UInt,
        day: UInt,
        hour: UInt,
        min: UInt,
        sec: UInt,
    ): MappedLocalTime<DateTime<TimeZone>> {
        val d = NaiveDate.fromYmdOpt(year, month, day) ?: return MappedLocalTime.None
        val dt = d.andHmsOpt(hour, min, sec) ?: return MappedLocalTime.None
        return fromLocalDatetime(dt)
    }
}
