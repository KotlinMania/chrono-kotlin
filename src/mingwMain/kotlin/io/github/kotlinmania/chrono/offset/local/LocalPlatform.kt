package io.github.kotlinmania.chrono.offset.local

import io.github.kotlinmania.chrono.naive.NaiveDateTime
import io.github.kotlinmania.chrono.offset.FixedOffset
import io.github.kotlinmania.chrono.offset.MappedLocalTime
import io.github.kotlinmania.windowssys.windows.win32.foundation.FILETIME
import io.github.kotlinmania.windowssys.windows.win32.foundation.SYSTEMTIME
import io.github.kotlinmania.windowssys.windows.win32.system.time.DYNAMIC_TIME_ZONE_INFORMATION
import io.github.kotlinmania.windowssys.windows.win32.system.time.GetTimeZoneInformationForYear
import io.github.kotlinmania.windowssys.windows.win32.system.time.SystemTimeToFileTime
import io.github.kotlinmania.windowssys.windows.win32.system.time.SystemTimeToTzSpecificLocalTime
import io.github.kotlinmania.windowssys.windows.win32.system.time.TIME_ZONE_INFORMATION
import io.github.kotlinmania.windowssys.windows.win32.system.time.TzSpecificLocalTimeToSystemTime
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.toKString
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.getenv

internal actual object LocalPlatform {
    actual fun offsetFromUtcDatetime(utc: NaiveDateTime): MappedLocalTime<FixedOffset> =
        offsetFromUtcDatetimeWindows(utc)

    actual fun offsetFromLocalDatetime(local: NaiveDateTime): MappedLocalTime<FixedOffset> =
        offsetFromLocalDatetimeWindows(local)

    @OptIn(ExperimentalForeignApi::class)
    actual fun readTzFileBytes(path: String): ByteArray? {
        val file = fopen(path, "rb") ?: return null
        val out = ArrayList<ByteArray>()
        var totalLen = 0
        memScoped {
            val buf = allocArray<ByteVar>(4096)
            while (true) {
                val readCount = fread(buf, 1u, 4096u, file).toInt()
                if (readCount <= 0) break
                val chunk = buf.readBytes(readCount)
                out.add(chunk)
                totalLen += readCount
            }
        }
        fclose(file)
        val result = ByteArray(totalLen)
        var offset = 0
        for (chunk in out) {
            chunk.copyInto(result, offset)
            offset += chunk.size
        }
        return result
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun getEnvVar(name: String): String? =
        getenv(name)?.toKString()

    actual fun getLocalTimeMtimeSeconds(): Long? = null

    actual fun getTimeZoneInformationForYear(
        wyear: UShort,
        pdtzi: DynamicTimeZoneInformation?,
        ptzi: TimeZoneInformation,
    ): Bool {
        val sysDtzi = pdtzi?.toWinSys()
        val sysTzi = TIME_ZONE_INFORMATION()
        val res = GetTimeZoneInformationForYear(wyear, sysDtzi, sysTzi)
        if (res != 0) {
            ptzi.copyFromWinSys(sysTzi)
        }
        return res
    }

    actual fun systemTimeToFileTime(
        lpsystemtime: Systemtime,
        lpfiletime: Filetime,
    ): Bool {
        val sysSt = lpsystemtime.toWinSys()
        val sysFt = FILETIME()
        val res = SystemTimeToFileTime(sysSt, sysFt)
        if (res != 0) {
            lpfiletime.dwLowDateTime = sysFt.dwLowDateTime
            lpfiletime.dwHighDateTime = sysFt.dwHighDateTime
        }
        return res
    }

    actual fun systemTimeToTzSpecificLocalTime(
        lptimezoneinformation: TimeZoneInformation?,
        lpuniversaltime: Systemtime,
        lplocaltime: Systemtime,
    ): Bool {
        val sysTzi = lptimezoneinformation?.toWinSys()
        val sysUtc = lpuniversaltime.toWinSys()
        val sysLocal = SYSTEMTIME()
        val res = SystemTimeToTzSpecificLocalTime(sysTzi, sysUtc, sysLocal)
        if (res != 0) {
            lplocaltime.copyFromWinSys(sysLocal)
        }
        return res
    }

    actual fun tzSpecificLocalTimeToSystemTime(
        lptimezoneinformation: TimeZoneInformation?,
        lplocaltime: Systemtime,
        lpuniversaltime: Systemtime,
    ): Bool {
        val sysTzi = lptimezoneinformation?.toWinSys()
        val sysLocal = lplocaltime.toWinSys()
        val sysUtc = SYSTEMTIME()
        val res = TzSpecificLocalTimeToSystemTime(sysTzi, sysLocal, sysUtc)
        if (res != 0) {
            lpuniversaltime.copyFromWinSys(sysUtc)
        }
        return res
    }
}

private fun Systemtime.toWinSys(): SYSTEMTIME =
    SYSTEMTIME(
        wYear = wYear,
        wMonth = wMonth,
        wDayOfWeek = wDayOfWeek,
        wDay = wDay,
        wHour = wHour,
        wMinute = wMinute,
        wSecond = wSecond,
        wMilliseconds = wMilliseconds,
    )

private fun Systemtime.copyFromWinSys(src: SYSTEMTIME) {
    wYear = src.wYear
    wMonth = src.wMonth
    wDayOfWeek = src.wDayOfWeek
    wDay = src.wDay
    wHour = src.wHour
    wMinute = src.wMinute
    wSecond = src.wSecond
    wMilliseconds = src.wMilliseconds
}

private fun TimeZoneInformation.toWinSys(): TIME_ZONE_INFORMATION =
    TIME_ZONE_INFORMATION(
        Bias = bias,
        StandardName = standardName.copyOf(),
        StandardDate = standardDate.toWinSys(),
        StandardBias = standardBias,
        DaylightName = daylightName.copyOf(),
        DaylightDate = daylightDate.toWinSys(),
        DaylightBias = daylightBias,
    )

private fun TimeZoneInformation.copyFromWinSys(src: TIME_ZONE_INFORMATION) {
    bias = src.Bias
    src.StandardName.copyInto(standardName)
    standardDate.copyFromWinSys(src.StandardDate)
    standardBias = src.StandardBias
    src.DaylightName.copyInto(daylightName)
    daylightDate.copyFromWinSys(src.DaylightDate)
    daylightBias = src.DaylightBias
}

private fun DynamicTimeZoneInformation.toWinSys(): DYNAMIC_TIME_ZONE_INFORMATION =
    DYNAMIC_TIME_ZONE_INFORMATION(
        Bias = bias,
        StandardName = standardName.copyOf(),
        StandardDate = standardDate.toWinSys(),
        StandardBias = standardBias,
        DaylightName = daylightName.copyOf(),
        DaylightDate = daylightDate.toWinSys(),
        DaylightBias = daylightBias,
        TimeZoneKeyName = timeZoneKeyName.copyOf(),
        DynamicDaylightTimeDisabled = dynamicDaylightTimeDisabled,
    )
