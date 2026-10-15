package io.github.kotlinmania.chrono.offset.local

import io.github.kotlinmania.chrono.naive.NaiveDateTime
import io.github.kotlinmania.chrono.offset.FixedOffset
import io.github.kotlinmania.chrono.offset.MappedLocalTime

@OptIn(ExperimentalWasmJsInterop::class)
private fun jsTimezoneOffsetFromMillis(millis: Double): Int =
    js("new Date(millis).getTimezoneOffset()")

@OptIn(ExperimentalWasmJsInterop::class)
private fun jsTimezoneOffsetFromLocal(
    year: Int,
    month0: Int,
    day: Int,
    hour: Int,
    minute: Int,
    second: Int,
): Int = js("new Date(year, month0, day, hour, minute, second).getTimezoneOffset()")

internal actual object LocalPlatform {
    actual fun offsetFromUtcDatetime(utc: NaiveDateTime): MappedLocalTime<FixedOffset> {
        val offsetMinutes = jsTimezoneOffsetFromMillis(utc.timestampMillis().toDouble())
        return MappedLocalTime.Single(FixedOffset.westOpt(offsetMinutes * 60)!!)
    }

    actual fun offsetFromLocalDatetime(local: NaiveDateTime): MappedLocalTime<FixedOffset> {
        var year = local.year()
        if (year < 100) {
            val shiftCycles = (year - 100).floorDiv(400)
            year -= shiftCycles * 400
        }
        val offsetMinutes =
            jsTimezoneOffsetFromLocal(
                year = year,
                month0 = local.month0().toInt(),
                day = local.day().toInt(),
                hour = local.hour().toInt(),
                minute = local.minute().toInt(),
                second = local.second().toInt(),
            )
        return MappedLocalTime.Single(FixedOffset.westOpt(offsetMinutes * 60)!!)
    }

    actual fun readTzFileBytes(path: String): ByteArray? = null

    actual fun getEnvVar(name: String): String? = null

    actual fun getLocalTimeMtimeSeconds(): Long? = null

    actual fun getTimeZoneInformationForYear(
        wyear: UShort,
        pdtzi: DynamicTimeZoneInformation?,
        ptzi: TimeZoneInformation,
    ): Bool = 0

    actual fun systemTimeToFileTime(
        lpsystemtime: Systemtime,
        lpfiletime: Filetime,
    ): Bool = 0

    actual fun systemTimeToTzSpecificLocalTime(
        lptimezoneinformation: TimeZoneInformation?,
        lpuniversaltime: Systemtime,
        lplocaltime: Systemtime,
    ): Bool = 0

    actual fun tzSpecificLocalTimeToSystemTime(
        lptimezoneinformation: TimeZoneInformation?,
        lplocaltime: Systemtime,
        lpuniversaltime: Systemtime,
    ): Bool = 0
}
