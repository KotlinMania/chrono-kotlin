// port-lint: tests offset/local/windows.rs
package io.github.kotlinmania.chrono.offset.local

import io.github.kotlinmania.chrono.DateTime
import io.github.kotlinmania.chrono.TimeDelta
import io.github.kotlinmania.chrono.naive.NaiveDate
import io.github.kotlinmania.chrono.naive.NaiveDateTime
import io.github.kotlinmania.chrono.offset.FixedOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WindowsTest {
    @Test
    fun verifyAgainstTzSpecificLocalTimeToSystemTime() {
        // The implementation in Windows itself is the source of truth on how to work with the OS
        // timezone information. This test compares for every hour over a period of 125 years our
        // implementation to `TzSpecificLocalTimeToSystemTime`.
        //
        // This uses parts of a previous Windows `Local` implementation in chrono.
        fun systemTimeFromNaiveDateTime(dt: NaiveDateTime): Systemtime =
            Systemtime(
                // Valid values: 1601-30827
                wYear = dt.year().toUShort(),
                // Valid values: 1-12
                wMonth = dt.month().toUShort(),
                // Valid values: 0-6, starting Sunday.
                wDayOfWeek = dt.weekday().numDaysFromSunday().toUShort(),
                // Valid values: 1-31
                wDay = dt.day().toUShort(),
                // Valid values: 0-23
                wHour = dt.hour().toUShort(),
                // Valid values: 0-59
                wMinute = dt.minute().toUShort(),
                // Valid values: 0-59
                wSecond = dt.second().toUShort(),
                // Valid values: 0-999
                wMilliseconds = 0u,
            )

        fun localToUtcTime(local: Systemtime): Systemtime {
            val sysTime = Systemtime.default()
            tzSpecificLocalTimeToSystemTime(null, local, sysTime)
            return sysTime
        }

        val hectonanosecsInSec = 10_000_000L
        val hectonanosecToUnixEpoch = 11_644_473_600L * hectonanosecsInSec

        fun systemTimeAsUnixSeconds(st: Systemtime): Long {
            val filetime = Filetime.default()
            systemTimeToFileTime(st, filetime)
            val bitShift =
                (filetime.dwHighDateTime.toULong() shl 32) or filetime.dwLowDateTime.toULong()
            return (bitShift.toLong() - hectonanosecToUnixEpoch) / hectonanosecsInSec
        }

        fun fromLocalTime(dt: NaiveDateTime): DateTime<Local> {
            val st = systemTimeFromNaiveDateTime(dt)
            val utcTime = localToUtcTime(st)
            val utcSecs = systemTimeAsUnixSeconds(utcTime)
            val localSecs = systemTimeAsUnixSeconds(st)
            val offsetSecs = (localSecs - utcSecs).toInt()
            val offset = FixedOffset.eastOpt(offsetSecs)!!
            return DateTime.fromNaiveUtcAndOffset(dt - offset, Local(offset))
        }

        // Verify naiveDateTimeFromSystemTime directly for concrete and rule-based SYSTEMTIME
        val sampleDt = NaiveDate.fromYmdOpt(2020, 6u, 15u)!!.andHmsOpt(12u, 30u, 45u)!!
        val sampleSt = systemTimeFromNaiveDateTime(sampleDt)
        val resolvedConcrete = naiveDateTimeFromSystemTime(sampleSt, 2020)
        assertTrue(resolvedConcrete.ok)
        assertEquals(sampleDt, resolvedConcrete.value)

        val dstRule =
            Systemtime(
                wYear = 0u,
                wMonth = 3u,
                wDayOfWeek = 0u,
                wDay = 5u,
                wHour = 2u,
                wMinute = 0u,
                wSecond = 0u,
                wMilliseconds = 0u,
            )
        val resolvedRule = naiveDateTimeFromSystemTime(dstRule, 2020)
        assertTrue(resolvedRule.ok)
        assertEquals(NaiveDate.fromYmdOpt(2020, 3u, 29u)!!.andHmsOpt(2u, 0u, 0u)!!, resolvedRule.value)

        var date = NaiveDate.fromYmdOpt(1975, 1u, 1u)!!.andHmsOpt(0u, 30u, 0u)!!
        val probe = Systemtime.default()
        if (tzSpecificLocalTimeToSystemTime(null, systemTimeFromNaiveDateTime(date), probe) != 0) {
            while (date.year() < 2078) {
                // Windows doesn't handle non-existing dates, it just treats it as valid.
                val ourResult = Local.fromLocalDatetime(date).earliest()
                if (ourResult != null) {
                    assertEquals(fromLocalTime(date), ourResult)
                }
                date += TimeDelta.tryHours(1L)!!
            }
        }
    }
}
