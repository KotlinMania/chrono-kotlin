// port-lint: source offset/local/unix.rs
package io.github.kotlinmania.chrono.offset.local

import io.github.kotlinmania.chrono.naive.NaiveDateTime
import io.github.kotlinmania.chrono.offset.FixedOffset
import io.github.kotlinmania.chrono.offset.MappedLocalTime
import io.github.kotlinmania.chrono.offset.local.tzinfo.TimeZone
import io.github.kotlinmania.ianatimezone.TimezoneResult
import io.github.kotlinmania.ianatimezone.getTimezone
import kotlin.time.Clock

internal fun offsetFromUtcDatetimeUnix(utc: NaiveDateTime): MappedLocalTime<FixedOffset> =
    offsetUnix(utc, false)

internal fun offsetFromLocalDatetimeUnix(local: NaiveDateTime): MappedLocalTime<FixedOffset> =
    offsetUnix(local, true)

private var cachedTzInfo: Cache? = null

private fun offsetUnix(d: NaiveDateTime, local: Boolean): MappedLocalTime<FixedOffset> {
    val cache = cachedTzInfo ?: Cache.default().also { cachedTzInfo = it }
    return cache.offset(d, local)
}

private sealed class Source {
    data class LocalTime(
        val mtimeEpochSeconds: Long,
    ) : Source()

    data class Environment(
        val hash: Int,
    ) : Source()

    companion object {
        fun new(envTz: String?): Source =
            if (envTz != null) {
                Environment(envTz.hashCode())
            } else {
                val mtime = LocalPlatform.getLocalTimeMtimeSeconds() ?: Clock.System.now().epochSeconds
                LocalTime(mtime)
            }
    }
}

private const val TZDB_LOCATION: String = "/usr/share/zoneinfo"

private fun fallbackTimezone(): TimeZone? {
    val tzName = (getTimezone() as? TimezoneResult.Ok)?.name ?: return null
    val androidBytes = forZone(tzName).ok()
    if (androidBytes != null) {
        return TimeZone.fromTzData(androidBytes).ok()
    }
    val bytes = LocalPlatform.readTzFileBytes("$TZDB_LOCATION/$tzName") ?: return null
    return TimeZone.fromTzData(bytes).ok()
}

private fun currentZone(envTz: String?): TimeZone =
    TimeZone.local(envTz).ok() ?: fallbackTimezone() ?: TimeZone.utc()

private class Cache(
    var zone: TimeZone,
    var source: Source,
    var lastCheckedEpochSeconds: Long,
) {
    fun offset(d: NaiveDateTime, local: Boolean): MappedLocalTime<FixedOffset> {
        val nowSecs = Clock.System.now().epochSeconds
        val diff = nowSecs - lastCheckedEpochSeconds
        if (diff < 0L || diff >= 1L) {
            val envTz = LocalPlatform.getEnvVar("TZ")
            val newSource = Source.new(envTz)

            val outOfDate =
                when {
                    source is Source.Environment && newSource is Source.LocalTime -> {
                        true
                    }

                    source is Source.LocalTime && newSource is Source.Environment -> {
                        true
                    }

                    source is Source.LocalTime && newSource is Source.LocalTime -> {
                        (source as Source.LocalTime).mtimeEpochSeconds != newSource.mtimeEpochSeconds
                    }

                    source is Source.Environment && newSource is Source.Environment -> {
                        (source as Source.Environment).hash != newSource.hash
                    }

                    else -> {
                        false
                    }
                }

            if (outOfDate) {
                zone = currentZone(envTz)
            }

            lastCheckedEpochSeconds = nowSecs
            source = newSource
        }

        if (!local) {
            val offsetSecs =
                zone
                    .findLocalTimeType(d.andUtc().timestamp())
                    .unwrap()
                    .offset()
            val fixed = FixedOffset.eastOpt(offsetSecs)
            return if (fixed != null) MappedLocalTime.Single(fixed) else MappedLocalTime.None
        }

        return zone
            .findLocalTimeTypeFromLocal(d)
            .unwrap()
            .andThen { ltt ->
                val fixed = FixedOffset.eastOpt(ltt.offset())
                if (fixed != null) MappedLocalTime.Single(fixed) else MappedLocalTime.None
            }
    }

    companion object {
        fun default(): Cache {
            val envTz = LocalPlatform.getEnvVar("TZ")
            return Cache(
                zone = currentZone(envTz),
                source = Source.new(envTz),
                lastCheckedEpochSeconds = Clock.System.now().epochSeconds,
            )
        }
    }
}
