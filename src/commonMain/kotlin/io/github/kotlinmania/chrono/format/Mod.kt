// port-lint: source format/mod.rs
package io.github.kotlinmania.chrono.format

import io.github.kotlinmania.chrono.DateTime
import io.github.kotlinmania.chrono.naive.MAX_YEAR
import io.github.kotlinmania.chrono.naive.MIN_YEAR
import io.github.kotlinmania.chrono.naive.NaiveDate
import io.github.kotlinmania.chrono.naive.NaiveDateTime
import io.github.kotlinmania.chrono.naive.NaiveTime
import io.github.kotlinmania.chrono.offset.FixedOffset
import io.github.kotlinmania.chrono.offset.MappedLocalTime

/** The category of parse error. */
enum class ParseErrorKind {
    /** Given field is out of permitted range. */
    OutOfRange,

    /** There is no possible date and time value with given set of fields. */
    Impossible,

    /** Given set of fields is not enough to make a requested date and time value. */
    NotEnough,

    /** The input string has some invalid character sequence for given formatting items. */
    Invalid,

    /** The input string has been prematurely ended. */
    TooShort,

    /** All formatting items have been read but there is a remaining input. */
    TooLong,

    /** There was an error on the formatting string, or there were non-supported formatting items. */
    BadFormat,
}

/** An error from the `parse` function. */
class ParseError internal constructor(
    private val errorKind: ParseErrorKind,
) : Throwable() {
    /** The category of parse error. */
    fun kind(): ParseErrorKind = errorKind

    override fun toString(): String =
        when (errorKind) {
            ParseErrorKind.OutOfRange -> "input is out of range"
            ParseErrorKind.Impossible -> "no possible date and time matching input"
            ParseErrorKind.NotEnough -> "input is not enough for unique date and time"
            ParseErrorKind.Invalid -> "input contains invalid characters"
            ParseErrorKind.TooShort -> "premature end of input"
            ParseErrorKind.TooLong -> "trailing input"
            ParseErrorKind.BadFormat -> "bad or unsupported format string"
        }

    override val message: String get() = toString()

    override fun equals(other: Any?): Boolean = other is ParseError && errorKind == other.errorKind

    override fun hashCode(): Int = errorKind.hashCode()
}

internal val OUT_OF_RANGE = ParseError(ParseErrorKind.OutOfRange)
internal val IMPOSSIBLE = ParseError(ParseErrorKind.Impossible)
internal val NOT_ENOUGH = ParseError(ParseErrorKind.NotEnough)
internal val INVALID = ParseError(ParseErrorKind.Invalid)
internal val TOO_SHORT = ParseError(ParseErrorKind.TooShort)
internal val TOO_LONG = ParseError(ParseErrorKind.TooLong)
internal val BAD_FORMAT = ParseError(ParseErrorKind.BadFormat)

/** Specific formatting options for seconds in RFC 3339 strings. */
enum class SecondsFormat {
    /** Format whole seconds only, with no decimal point nor subseconds. */
    Secs,

    /** Use fixed 3 subsecond digits. */
    Millis,

    /** Use fixed 6 subsecond digits. */
    Micros,

    /** Use fixed 9 subsecond digits. */
    Nanos,

    /** Automatically select one of [Secs], [Millis], [Micros], or [Nanos] to display all available non-zero sub-second digits. */
    AutoSi,
}

private val NANO_SCALES =
    uintArrayOf(
        0u,
        100_000_000u,
        10_000_000u,
        1_000_000u,
        100_000u,
        10_000u,
        1_000u,
        100u,
        10u,
        1u,
    )

internal fun scanNumber(
    s: String,
    min: Int,
    max: Int,
): Result<Pair<String, Long>> {
    if (s.length < min) {
        return Result.failure(TOO_SHORT)
    }
    var n = 0L
    val limit = minOf(max, s.length)
    for (i in 0 until limit) {
        val c = s[i]
        if (c !in '0'..'9') {
            return if (i < min) {
                Result.failure(INVALID)
            } else {
                Result.success(s.substring(i) to n)
            }
        }
        val digit = (c - '0').toLong()
        if (n > (Long.MAX_VALUE - digit) / 10L) {
            return Result.failure(OUT_OF_RANGE)
        }
        n = n * 10L + digit
    }
    return Result.success(s.substring(limit) to n)
}

internal fun scanNanosecond(s: String): Result<Pair<String, UInt>> {
    val origLen = s.length
    val (rest, v) = scanNumber(s, 1, 9).getOrElse { return Result.failure(it) }
    val consumed = origLen - rest.length
    val scaled = v.toUInt() * NANO_SCALES[consumed]
    var idx = 0
    while (idx < rest.length && rest[idx] in '0'..'9') {
        idx++
    }
    return Result.success(rest.substring(idx) to scaled)
}

private fun scanSignedYear(s: String): Result<Pair<String, Int>> {
    val trimmed = s.trimStart()
    val (rest, v) =
        when {
            trimmed.startsWith('-') -> {
                val (r, num) =
                    scanNumber(trimmed.substring(1), 1, Int.MAX_VALUE)
                        .getOrElse { return Result.failure(it) }
                r to -num
            }

            trimmed.startsWith('+') -> {
                scanNumber(trimmed.substring(1), 1, Int.MAX_VALUE)
                    .getOrElse { return Result.failure(it) }
            }

            else -> {
                scanNumber(trimmed, 1, 4)
                    .getOrElse { return Result.failure(it) }
            }
        }
    if (v < MIN_YEAR.toLong() || v > MAX_YEAR.toLong()) {
        return Result.failure(OUT_OF_RANGE)
    }
    return Result.success(rest to v.toInt())
}

private fun parseDatePrefix(s: String): Result<Pair<String, NaiveDate>> {
    val (s1, year) = scanSignedYear(s).getOrElse { return Result.failure(it) }
    val s2 = s1.trimStart()
    if (s2.isEmpty()) return Result.failure(TOO_SHORT)
    if (!s2.startsWith('-')) return Result.failure(INVALID)
    val (s3, month) =
        scanNumber(s2.substring(1).trimStart(), 1, 2)
            .getOrElse { return Result.failure(it) }
    if (month !in 1L..12L) return Result.failure(OUT_OF_RANGE)
    val s4 = s3.trimStart()
    if (s4.isEmpty()) return Result.failure(TOO_SHORT)
    if (!s4.startsWith('-')) return Result.failure(INVALID)
    val (s5, day) =
        scanNumber(s4.substring(1).trimStart(), 1, 2)
            .getOrElse { return Result.failure(it) }
    if (day !in 1L..31L) return Result.failure(OUT_OF_RANGE)
    val date =
        NaiveDate.fromYmdOpt(year, month.toUInt(), day.toUInt())
            ?: return Result.failure(OUT_OF_RANGE)
    return Result.success(s5 to date)
}

private fun parseTimeRequiredSecondsPrefix(s: String): Result<Pair<String, NaiveTime>> {
    val (s1, hour) = scanNumber(s.trimStart(), 1, 2).getOrElse { return Result.failure(it) }
    if (hour !in 0L..23L) return Result.failure(OUT_OF_RANGE)
    val s2 = s1.trimStart()
    if (s2.isEmpty()) return Result.failure(TOO_SHORT)
    if (!s2.startsWith(':')) return Result.failure(INVALID)
    val (s3, minute) =
        scanNumber(s2.substring(1).trimStart(), 1, 2)
            .getOrElse { return Result.failure(it) }
    if (minute !in 0L..59L) return Result.failure(OUT_OF_RANGE)
    val s4 = s3.trimStart()
    if (s4.isEmpty()) return Result.failure(TOO_SHORT)
    if (!s4.startsWith(':')) return Result.failure(INVALID)
    val (s5, second) =
        scanNumber(s4.substring(1).trimStart(), 1, 2)
            .getOrElse { return Result.failure(it) }
    if (second !in 0L..60L) return Result.failure(OUT_OF_RANGE)
    val (s6, nano) =
        if (s5.startsWith('.')) {
            scanNanosecond(s5.substring(1)).getOrElse { return Result.failure(it) }
        } else {
            s5 to 0u
        }
    val (adjSec, adjNano) =
        if (second == 60L) {
            59u to (1_000_000_000u + nano)
        } else {
            second.toUInt() to nano
        }
    val time =
        NaiveTime.fromHmsNanoOpt(hour.toUInt(), minute.toUInt(), adjSec, adjNano)
            ?: return Result.failure(OUT_OF_RANGE)
    return Result.success(s6.trimStart() to time)
}

internal fun parseNaiveDate(s: String): Result<NaiveDate> {
    val (rest, date) = parseDatePrefix(s).getOrElse { return Result.failure(it) }
    if (rest.trimStart().isNotEmpty()) {
        return Result.failure(TOO_LONG)
    }
    return Result.success(date)
}

internal fun parseNaiveTime(s: String): Result<NaiveTime> {
    val (s1, hour) = scanNumber(s.trimStart(), 1, 2).getOrElse { return Result.failure(it) }
    if (hour !in 0L..23L) return Result.failure(OUT_OF_RANGE)
    val s2 = s1.trimStart()
    if (s2.isEmpty()) return Result.failure(TOO_SHORT)
    if (!s2.startsWith(':')) return Result.failure(INVALID)
    val (s3, minute) =
        scanNumber(s2.substring(1).trimStart(), 1, 2)
            .getOrElse { return Result.failure(it) }
    if (minute !in 0L..59L) return Result.failure(OUT_OF_RANGE)
    val s4 = s3.trimStart()
    if (s4.isEmpty()) {
        val time =
            NaiveTime.fromHmsOpt(hour.toUInt(), minute.toUInt(), 0u)
                ?: return Result.failure(OUT_OF_RANGE)
        return Result.success(time)
    }
    if (!s4.startsWith(':')) return Result.failure(TOO_LONG)
    val (s5, second) =
        scanNumber(s4.substring(1).trimStart(), 1, 2)
            .getOrElse { return Result.failure(it) }
    if (second !in 0L..60L) return Result.failure(OUT_OF_RANGE)
    val (s6, nano) =
        if (s5.startsWith('.')) {
            scanNanosecond(s5.substring(1)).getOrElse { return Result.failure(it) }
        } else {
            s5 to 0u
        }
    if (s6.trimStart().isNotEmpty()) return Result.failure(TOO_LONG)
    val (adjSec, adjNano) =
        if (second == 60L) {
            59u to (1_000_000_000u + nano)
        } else {
            second.toUInt() to nano
        }
    val time =
        NaiveTime.fromHmsNanoOpt(hour.toUInt(), minute.toUInt(), adjSec, adjNano)
            ?: return Result.failure(OUT_OF_RANGE)
    return Result.success(time)
}

internal fun parseNaiveDateTime(s: String): Result<NaiveDateTime> {
    val (s1, date) = parseDatePrefix(s).getOrElse { return Result.failure(it) }
    val s2 = s1.trimStart()
    if (s2.isEmpty()) return Result.failure(TOO_SHORT)
    if (!s2.startsWith('T')) return Result.failure(INVALID)
    val (s3, time) =
        parseTimeRequiredSecondsPrefix(s2.substring(1))
            .getOrElse { return Result.failure(it) }
    if (s3.isNotEmpty()) return Result.failure(TOO_LONG)
    return Result.success(NaiveDateTime(date, time))
}

private fun scanTimezoneOffset(s: String): Result<Pair<String, Int>> {
    if (s.isEmpty()) return Result.failure(TOO_SHORT)
    val first = s[0]
    if (first == 'Z' || first == 'z') {
        return Result.success(s.substring(1) to 0)
    }
    val negative =
        when (first) {
            '+' -> false
            '-', '\u2212' -> true
            else -> return Result.failure(INVALID)
        }
    var rest = s.substring(1)
    if (rest.length < 2) return Result.failure(TOO_SHORT)
    val h1 = rest[0]
    val h2 = rest[1]
    if (h1 !in '0'..'9' || h2 !in '0'..'9') return Result.failure(INVALID)
    val hours = (h1 - '0') * 10 + (h2 - '0')
    rest = rest.substring(2).trimStart { it == ':' || it.isWhitespace() }
    if (rest.length < 2) return Result.failure(TOO_SHORT)
    val m1 = rest[0]
    val m2 = rest[1]
    if (m1 !in '0'..'9' || m2 !in '0'..'9') return Result.failure(INVALID)
    if (m1 in '6'..'9') return Result.failure(OUT_OF_RANGE)
    val minutes = (m1 - '0') * 10 + (m2 - '0')
    rest = rest.substring(2)
    val seconds = hours * 3600 + minutes * 60
    return Result.success(rest to (if (negative) -seconds else seconds))
}

internal fun parseRfc3339Relaxed(s: String): Result<DateTime<FixedOffset>> {
    val (s1, date) = parseDatePrefix(s).getOrElse { return Result.failure(it) }
    if (s1.isEmpty()) return Result.failure(TOO_SHORT)
    val sep = s1[0]
    if (sep != 't' && sep != 'T' && sep != ' ') return Result.failure(INVALID)
    val (s2, time) =
        parseTimeRequiredSecondsPrefix(s1.substring(1))
            .getOrElse { return Result.failure(it) }
    val s3 = s2.trimStart()
    val (rest, offsetSecs) =
        if (s3.length >= 3 && s3.substring(0, 3).equals("UTC", ignoreCase = true)) {
            s3.substring(3) to 0
        } else {
            scanTimezoneOffset(s3).getOrElse { return Result.failure(it) }
        }
    if (rest.trimStart().isNotEmpty()) return Result.failure(TOO_LONG)
    val fixedOffset = FixedOffset.eastOpt(offsetSecs) ?: return Result.failure(OUT_OF_RANGE)
    val localDt = NaiveDateTime(date, time)
    return when (val mapped = localDt.andLocalTimezone(fixedOffset)) {
        is MappedLocalTime.Single -> Result.success(mapped.value)
        is MappedLocalTime.None -> Result.failure(IMPOSSIBLE)
        is MappedLocalTime.Ambiguous -> Result.failure(NOT_ENOUGH)
    }
}

internal fun writeRfc3339(
    dt: NaiveDateTime,
    off: FixedOffset,
    secform: SecondsFormat = SecondsFormat.AutoSi,
    useZ: Boolean = true,
): String {
    val sb = StringBuilder()
    sb.append(dt.date().toString())
    sb.append('T')
    val time = dt.time()
    val h = time.hour().toString().padStart(2, '0')
    val m = time.minute().toString().padStart(2, '0')
    var sec = time.second()
    var nano = time.nanosecond()
    if (nano >= 1_000_000_000u) {
        sec += 1u
        nano -= 1_000_000_000u
    }
    val s = sec.toString().padStart(2, '0')
    sb.append("$h:$m:$s")
    when (secform) {
        SecondsFormat.Secs -> {}

        SecondsFormat.Millis -> {
            sb.append('.').append((nano / 1_000_000u).toString().padStart(3, '0'))
        }

        SecondsFormat.Micros -> {
            sb.append('.').append((nano / 1_000u).toString().padStart(6, '0'))
        }

        SecondsFormat.Nanos -> {
            sb.append('.').append(nano.toString().padStart(9, '0'))
        }

        SecondsFormat.AutoSi -> {
            when {
                nano == 0u -> {}

                nano % 1_000_000u == 0u -> {
                    sb.append('.').append((nano / 1_000_000u).toString().padStart(3, '0'))
                }

                nano % 1_000u == 0u -> {
                    sb.append('.').append((nano / 1_000u).toString().padStart(6, '0'))
                }

                else -> {
                    sb.append('.').append(nano.toString().padStart(9, '0'))
                }
            }
        }
    }
    val offsetSecs = off.localMinusUtc
    if (useZ && offsetSecs == 0) {
        sb.append('Z')
    } else {
        val sign = if (offsetSecs < 0) '-' else '+'
        val absOff = if (offsetSecs < 0) -offsetSecs else offsetSecs
        val minutes = (absOff + 30) / 60
        val mins = minutes % 60
        val hours = minutes / 60
        sb.append(sign)
        sb.append(hours.toString().padStart(2, '0'))
        sb.append(':')
        sb.append(mins.toString().padStart(2, '0'))
    }
    return sb.toString()
}
