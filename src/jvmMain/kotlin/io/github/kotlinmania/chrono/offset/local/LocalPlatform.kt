package io.github.kotlinmania.chrono.offset.local

import io.github.kotlinmania.chrono.naive.NaiveDateTime
import io.github.kotlinmania.chrono.offset.FixedOffset
import io.github.kotlinmania.chrono.offset.MappedLocalTime
import java.io.File

internal actual object LocalPlatform {
    actual fun offsetFromUtcDatetime(utc: NaiveDateTime): MappedLocalTime<FixedOffset> =
        offsetFromUtcDatetimeUnix(utc)

    actual fun offsetFromLocalDatetime(local: NaiveDateTime): MappedLocalTime<FixedOffset> =
        offsetFromLocalDatetimeUnix(local)

    actual fun readTzFileBytes(path: String): ByteArray? {
        val file = File(path)
        if (!file.exists() || !file.isFile || !file.canRead()) return null
        return file.readBytes()
    }

    actual fun getEnvVar(name: String): String? =
        System.getenv(name)

    actual fun getLocalTimeMtimeSeconds(): Long? {
        val file = File("/etc/localtime")
        if (!file.exists()) return null
        val mtimeMillis = file.lastModified()
        return if (mtimeMillis > 0L) mtimeMillis / 1000L else null
    }

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
