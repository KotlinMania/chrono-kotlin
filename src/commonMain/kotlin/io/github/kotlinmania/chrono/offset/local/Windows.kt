// port-lint: source offset/local/windows.rs
package io.github.kotlinmania.chrono.offset.local

import io.github.kotlinmania.chrono.Weekday
import io.github.kotlinmania.chrono.naive.NaiveDate
import io.github.kotlinmania.chrono.naive.NaiveDateTime
import io.github.kotlinmania.chrono.naive.NaiveTime
import io.github.kotlinmania.chrono.offset.FixedOffset
import io.github.kotlinmania.chrono.offset.MappedLocalTime

// We don't use `SystemTimeToTzSpecificLocalTime` because it doesn't support the same range of dates
// as Chrono. Also it really isn't that difficult to work out the correct offset from the provided
// DST rules.
//
// This method uses `overflowingSubOffset` because it is no problem if the transition time in UTC
// falls a couple of hours inside the buffer space around the `NaiveDateTime` range (although it is
// very theoretical to have a transition at midnight around `NaiveDate.MIN` or `NaiveDate.MAX`).
internal fun offsetFromUtcDatetimeWindows(utc: NaiveDateTime): MappedLocalTime<FixedOffset> {
    // Using a `TzInfo` based on the year of an UTC datetime is technically wrong, we should be
    // using the rules for the year of the corresponding local time. But this matches what
    // `SystemTimeToTzSpecificLocalTime` is documented to do.
    val tzInfo = TzInfo.forYear(utc.year()) ?: return MappedLocalTime.None
    val stdTransition = tzInfo.stdTransition
    val dstTransition = tzInfo.dstTransition
    val offset =
        when {
            stdTransition != null && dstTransition != null -> {
                val stdTransitionUtc = stdTransition.overflowingSubOffset(tzInfo.dstOffset)
                val dstTransitionUtc = dstTransition.overflowingSubOffset(tzInfo.stdOffset)
                if (dstTransitionUtc < stdTransitionUtc) {
                    if (utc >= dstTransitionUtc && utc < stdTransitionUtc) {
                        tzInfo.dstOffset
                    } else {
                        tzInfo.stdOffset
                    }
                } else {
                    if (utc >= stdTransitionUtc && utc < dstTransitionUtc) {
                        tzInfo.stdOffset
                    } else {
                        tzInfo.dstOffset
                    }
                }
            }

            stdTransition != null -> {
                val stdTransitionUtc = stdTransition.overflowingSubOffset(tzInfo.dstOffset)
                if (utc < stdTransitionUtc) {
                    tzInfo.dstOffset
                } else {
                    tzInfo.stdOffset
                }
            }

            dstTransition != null -> {
                val dstTransitionUtc = dstTransition.overflowingSubOffset(tzInfo.stdOffset)
                if (utc < dstTransitionUtc) {
                    tzInfo.stdOffset
                } else {
                    tzInfo.dstOffset
                }
            }

            else -> {
                tzInfo.stdOffset
            }
        }
    return MappedLocalTime.Single(offset)
}

// We don't use `TzSpecificLocalTimeToSystemTime` because it doesn't let us choose how to handle
// ambiguous cases (during a DST transition). Instead we get the timezone information for the
// current year and compute it ourselves, like we do on Unix.
internal fun offsetFromLocalDatetimeWindows(local: NaiveDateTime): MappedLocalTime<FixedOffset> {
    val tzInfo = TzInfo.forYear(local.year()) ?: return MappedLocalTime.None
    val stdTransition = tzInfo.stdTransition
    val dstTransition = tzInfo.dstTransition
    // Create a sorted array of transitions and use `lookupWithDstTransitions`.
    return when {
        stdTransition != null && dstTransition != null -> {
            val stdTr = Transition.new(stdTransition, tzInfo.dstOffset, tzInfo.stdOffset)
            val dstTr = Transition.new(dstTransition, tzInfo.stdOffset, tzInfo.dstOffset)
            val cmp = stdTr.compareTo(dstTr)
            val transitions =
                when {
                    cmp < 0 -> {
                        arrayOf(stdTr, dstTr)
                    }

                    cmp > 0 -> {
                        arrayOf(dstTr, stdTr)
                    }

                    else -> {
                        // This doesn't make sense. Let's just return the standard offset.
                        return MappedLocalTime.Single(tzInfo.stdOffset)
                    }
                }
            lookupWithDstTransitions(transitions, local)
        }

        stdTransition != null -> {
            val transitions = arrayOf(Transition.new(stdTransition, tzInfo.dstOffset, tzInfo.stdOffset))
            lookupWithDstTransitions(transitions, local)
        }

        dstTransition != null -> {
            val transitions = arrayOf(Transition.new(dstTransition, tzInfo.stdOffset, tzInfo.dstOffset))
            lookupWithDstTransitions(transitions, local)
        }

        else -> {
            MappedLocalTime.Single(tzInfo.stdOffset)
        }
    }
}

// The basis for Windows timezone and DST support has been in place since Windows 2000. It does not
// allow for complex rules like the IANA timezone database:
// - A timezone has the same base offset the whole year.
// - There seem to be either zero or two DST transitions (but we support having just one).
// - As of Vista(?) only years from 2004 until a few years into the future are supported.
// - All other years get the base settings, which seem to be that of the current year.
//
// These details don't matter much, we just work with the offsets and transition dates Windows
// returns through `GetTimeZoneInformationForYear` for a particular year.
internal class TzInfo(
    // Offset from UTC during standard time.
    val stdOffset: FixedOffset,
    // Offset from UTC during daylight saving time.
    val dstOffset: FixedOffset,
    // Transition from standard time to daylight saving time, given in local standard time.
    val stdTransition: NaiveDateTime?,
    // Transition from daylight saving time to standard time, given in local daylight saving time.
    val dstTransition: NaiveDateTime?,
) {
    companion object {
        fun forYear(year: Int): TzInfo? {
            // The API limits years to 1601..=30827.
            // Working with timezones and daylight saving time this far into the past or future makes
            // little sense. But whatever is extrapolated for 1601 or 30827 is what can be extrapolated
            // for years beyond.
            val refYear = year.coerceIn(1601, 30827).toUShort()
            val tzInfo = TimeZoneInformation.default()
            if (getTimeZoneInformationForYear(refYear, null, tzInfo) == 0) {
                return null
            }
            val stdOffset =
                checkedAddBias(tzInfo.bias, tzInfo.standardBias)
                    ?.let { checkedMulBias(it, 60) }
                    ?.let { FixedOffset.westOpt(it) }
                    ?: return null
            val dstOffset =
                checkedAddBias(tzInfo.bias, tzInfo.daylightBias)
                    ?.let { checkedMulBias(it, 60) }
                    ?.let { FixedOffset.westOpt(it) }
                    ?: return null
            val stdTransitionRes = naiveDateTimeFromSystemTime(tzInfo.standardDate, year)
            if (!stdTransitionRes.ok) return null
            val dstTransitionRes = naiveDateTimeFromSystemTime(tzInfo.daylightDate, year)
            if (!dstTransitionRes.ok) return null
            return TzInfo(
                stdOffset = stdOffset,
                dstOffset = dstOffset,
                stdTransition = stdTransitionRes.value,
                dstTransition = dstTransitionRes.value,
            )
        }
    }
}

private fun checkedAddBias(a: Int, b: Int): Int? {
    val res = a.toLong() + b.toLong()
    if (res < Int.MIN_VALUE.toLong() || res > Int.MAX_VALUE.toLong()) return null
    return res.toInt()
}

private fun checkedMulBias(a: Int, b: Int): Int? {
    val res = a.toLong() * b.toLong()
    if (res < Int.MIN_VALUE.toLong() || res > Int.MAX_VALUE.toLong()) return null
    return res.toInt()
}

internal class SystemTimeResolveResult(
    val ok: Boolean,
    val value: NaiveDateTime?,
)

/**
 * Resolve a `SYSTEMTIME` object to an optional `NaiveDateTime`.
 *
 * A `SYSTEMTIME` within a `TIME_ZONE_INFORMATION` struct can be zero to indicate there is no
 * transition.
 * If it has year, month and day values it is a concrete date.
 * If the year is missing the `SYSTEMTIME` is a rule, which this method resolves for the provided
 * year. A rule has a month, weekday, and nth weekday of the month as components.
 *
 * Returns `ok = false` if any of the values is invalid, which should never happen.
 */
internal fun naiveDateTimeFromSystemTime(
    st: Systemtime,
    year: Int,
): SystemTimeResolveResult {
    if (st.wYear == 0.toUShort() && st.wMonth == 0.toUShort()) {
        return SystemTimeResolveResult(true, null)
    }
    val time =
        NaiveTime.fromHmsMilliOpt(
            st.wHour.toUInt(),
            st.wMinute.toUInt(),
            st.wSecond.toUInt(),
            st.wMilliseconds.toUInt(),
        ) ?: return SystemTimeResolveResult(false, null)

    if (st.wYear != 0.toUShort()) {
        // We have a concrete date.
        val date =
            NaiveDate.fromYmdOpt(st.wYear.toInt(), st.wMonth.toUInt(), st.wDay.toUInt())
                ?: return SystemTimeResolveResult(false, null)
        return SystemTimeResolveResult(true, date.andTime(time))
    }

    // Resolve a rule with month, weekday, and nth weekday of the month to a date in the current
    // year.
    val weekday =
        when (st.wDayOfWeek.toInt()) {
            0 -> Weekday.Sun
            1 -> Weekday.Mon
            2 -> Weekday.Tue
            3 -> Weekday.Wed
            4 -> Weekday.Thu
            5 -> Weekday.Fri
            6 -> Weekday.Sat
            else -> return SystemTimeResolveResult(false, null)
        }
    val nthDay =
        if (st.wDay in 1u..5u) {
            st.wDay.toUByte()
        } else {
            return SystemTimeResolveResult(false, null)
        }
    val date =
        NaiveDate.fromWeekdayOfMonthOpt(year, st.wMonth.toUInt(), weekday, nthDay)
            ?: NaiveDate.fromWeekdayOfMonthOpt(year, st.wMonth.toUInt(), weekday, 4u)
            ?: return SystemTimeResolveResult(false, null)
    return SystemTimeResolveResult(true, date.andTime(time))
}
