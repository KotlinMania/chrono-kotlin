package io.github.kotlinmania.chrono.offset.local

import io.github.kotlinmania.chrono.naive.NaiveDateTime
import io.github.kotlinmania.chrono.offset.FixedOffset
import io.github.kotlinmania.chrono.offset.MappedLocalTime
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
        offsetFromUtcDatetimeUnix(utc)

    actual fun offsetFromLocalDatetime(local: NaiveDateTime): MappedLocalTime<FixedOffset> =
        offsetFromLocalDatetimeUnix(local)

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
