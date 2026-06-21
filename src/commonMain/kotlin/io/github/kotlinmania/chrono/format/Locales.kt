// port-lint: source format/locales.rs
package io.github.kotlinmania.chrono.format

/** Locale marker used when locale data is not enabled. */
class Locale private constructor() {
    companion object {
        /** The default POSIX-style locale. */
        val POSIX: Locale = Locale()
    }
}

internal fun defaultLocale(): Locale = Locale.POSIX

internal fun shortMonths(locale: Locale): List<String> =
    listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

internal fun longMonths(locale: Locale): List<String> =
    listOf(
        "January",
        "February",
        "March",
        "April",
        "May",
        "June",
        "July",
        "August",
        "September",
        "October",
        "November",
        "December",
    )

internal fun shortWeekdays(locale: Locale): List<String> =
    listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

internal fun longWeekdays(locale: Locale): List<String> =
    listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

internal fun amPm(locale: Locale): List<String> = listOf("AM", "PM")

internal fun decimalPoint(locale: Locale): String = "."
