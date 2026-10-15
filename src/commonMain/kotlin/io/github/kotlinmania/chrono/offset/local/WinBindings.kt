// port-lint: source offset/local/win_bindings.rs
package io.github.kotlinmania.chrono.offset.local

internal typealias Bool = Int

internal class DynamicTimeZoneInformation(
    var bias: Int = 0,
    var standardName: UShortArray = UShortArray(32),
    var standardDate: Systemtime = Systemtime(),
    var standardBias: Int = 0,
    var daylightName: UShortArray = UShortArray(32),
    var daylightDate: Systemtime = Systemtime(),
    var daylightBias: Int = 0,
    var timeZoneKeyName: UShortArray = UShortArray(128),
    var dynamicDaylightTimeDisabled: Boolean = false,
) {
    companion object {
        fun default(): DynamicTimeZoneInformation = DynamicTimeZoneInformation()
    }
}

internal class Filetime(
    var dwLowDateTime: UInt = 0u,
    var dwHighDateTime: UInt = 0u,
) {
    companion object {
        fun default(): Filetime = Filetime()
    }
}

internal class Systemtime(
    var wYear: UShort = 0u,
    var wMonth: UShort = 0u,
    var wDayOfWeek: UShort = 0u,
    var wDay: UShort = 0u,
    var wHour: UShort = 0u,
    var wMinute: UShort = 0u,
    var wSecond: UShort = 0u,
    var wMilliseconds: UShort = 0u,
) {
    companion object {
        fun default(): Systemtime = Systemtime()
    }
}

internal class TimeZoneInformation(
    var bias: Int = 0,
    var standardName: UShortArray = UShortArray(32),
    var standardDate: Systemtime = Systemtime(),
    var standardBias: Int = 0,
    var daylightName: UShortArray = UShortArray(32),
    var daylightDate: Systemtime = Systemtime(),
    var daylightBias: Int = 0,
) {
    companion object {
        fun default(): TimeZoneInformation = TimeZoneInformation()
    }
}

internal fun getTimeZoneInformationForYear(
    wyear: UShort,
    pdtzi: DynamicTimeZoneInformation?,
    ptzi: TimeZoneInformation,
): Bool = LocalPlatform.getTimeZoneInformationForYear(wyear, pdtzi, ptzi)

internal fun systemTimeToFileTime(
    lpsystemtime: Systemtime,
    lpfiletime: Filetime,
): Bool = LocalPlatform.systemTimeToFileTime(lpsystemtime, lpfiletime)

internal fun systemTimeToTzSpecificLocalTime(
    lptimezoneinformation: TimeZoneInformation?,
    lpuniversaltime: Systemtime,
    lplocaltime: Systemtime,
): Bool = LocalPlatform.systemTimeToTzSpecificLocalTime(lptimezoneinformation, lpuniversaltime, lplocaltime)

internal fun tzSpecificLocalTimeToSystemTime(
    lptimezoneinformation: TimeZoneInformation?,
    lplocaltime: Systemtime,
    lpuniversaltime: Systemtime,
): Bool = LocalPlatform.tzSpecificLocalTimeToSystemTime(lptimezoneinformation, lplocaltime, lpuniversaltime)
