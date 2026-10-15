// port-lint: source offset/local/tz_info/mod.rs
package io.github.kotlinmania.chrono.offset.local.tzinfo

/**
 * Unified error type for everything in the tz_info module.
 */
internal sealed class Error {
    /** Date time error */
    data class DateTime(
        val description: String,
    ) : Error()

    /** Local time type search error */
    data class FindLocalTimeType(
        val description: String,
    ) : Error()

    /** Local time type error */
    data class LocalTimeType(
        val description: String,
    ) : Error()

    /** Invalid slice for integer conversion */
    data class InvalidSlice(
        val description: String,
    ) : Error()

    /** Invalid Tzif file */
    data class InvalidTzFile(
        val description: String,
    ) : Error()

    /** Invalid TZ string */
    data class InvalidTzString(
        val description: String,
    ) : Error()

    /** I/O error */
    data class Io(
        val description: String,
    ) : Error()

    /** Out of range error */
    data class OutOfRange(
        val description: String,
    ) : Error()

    /** Integer parsing error */
    data class ParseInt(
        val description: String,
    ) : Error()

    /** Date time projection error */
    data class ProjectDateTime(
        val description: String,
    ) : Error()

    /** System time error */
    data class SystemTime(
        val description: String,
    ) : Error()

    /** Time zone error */
    data class TimeZone(
        val description: String,
    ) : Error()

    /** Transition rule error */
    data class TransitionRule(
        val description: String,
    ) : Error()

    /** Unsupported Tzif file */
    data class UnsupportedTzFile(
        val description: String,
    ) : Error()

    /** Unsupported TZ string */
    data class UnsupportedTzString(
        val description: String,
    ) : Error()

    /** UTF-8 error */
    data class Utf8(
        val description: String,
    ) : Error()

    override fun toString(): String =
        when (this) {
            is DateTime -> "invalid date time: $description"
            is FindLocalTimeType -> description
            is LocalTimeType -> "invalid local time type: $description"
            is InvalidSlice -> description
            is InvalidTzString -> "invalid TZ string: $description"
            is InvalidTzFile -> description
            is Io -> description
            is OutOfRange -> description
            is ParseInt -> description
            is ProjectDateTime -> description
            is SystemTime -> description
            is TransitionRule -> "invalid transition rule: $description"
            is TimeZone -> "invalid time zone: $description"
            is UnsupportedTzFile -> description
            is UnsupportedTzString -> "unsupported TZ string: $description"
            is Utf8 -> description
        }
}

internal sealed class TzResult<out T> {
    data class Ok<T>(
        val value: T,
    ) : TzResult<T>()

    data class Err(
        val error: Error,
    ) : TzResult<Nothing>()

    val isOk: Boolean get() = this is Ok
    val isErr: Boolean get() = this is Err

    fun ok(): T? = (this as? Ok)?.value

    fun err(): Error? = (this as? Err)?.error

    fun unwrap(): T =
        when (this) {
            is Ok -> value
            is Err -> throw IllegalStateException(error.toString())
        }

    fun unwrapErr(): Error =
        when (this) {
            is Ok -> throw IllegalStateException("called unwrapErr on Ok: $value")
            is Err -> error
        }

    inline fun <U> map(transform: (T) -> U): TzResult<U> =
        when (this) {
            is Ok -> Ok(transform(value))
            is Err -> this
        }
}

/** Number of hours in one day */
internal const val HOURS_PER_DAY: Long = 24L

/** Number of seconds in one hour */
internal const val SECONDS_PER_HOUR: Long = 3600L

/** Number of seconds in one day */
internal const val SECONDS_PER_DAY: Long = SECONDS_PER_HOUR * HOURS_PER_DAY

/** Number of days in one week */
internal const val DAYS_PER_WEEK: Long = 7L

/** Month days in a normal year */
internal val DAY_IN_MONTHS_NORMAL_YEAR: LongArray =
    longArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

/** Cumulated month days in a normal year */
internal val CUMUL_DAY_IN_MONTHS_NORMAL_YEAR: List<Long> =
    listOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)

internal fun decodeUtf8(bytes: ByteArray): TzResult<String> {
    var i = 0
    while (i < bytes.size) {
        val b0 = bytes[i].toInt() and 0xFF
        when {
            b0 <= 0x7F -> {
                i += 1
            }

            b0 in 0xC2..0xDF -> {
                if (i + 1 >= bytes.size) return TzResult.Err(Error.Utf8("invalid utf-8 sequence"))
                val b1 = bytes[i + 1].toInt() and 0xFF
                if ((b1 and 0xC0) != 0x80) return TzResult.Err(Error.Utf8("invalid utf-8 sequence"))
                i += 2
            }

            b0 in 0xE0..0xEF -> {
                if (i + 2 >= bytes.size) return TzResult.Err(Error.Utf8("invalid utf-8 sequence"))
                val b1 = bytes[i + 1].toInt() and 0xFF
                val b2 = bytes[i + 2].toInt() and 0xFF
                if ((b1 and 0xC0) != 0x80 || (b2 and 0xC0) != 0x80) {
                    return TzResult.Err(Error.Utf8("invalid utf-8 sequence"))
                }
                if (b0 == 0xE0 && b1 < 0xA0) return TzResult.Err(Error.Utf8("invalid utf-8 sequence"))
                if (b0 == 0xED && b1 >= 0xA0) return TzResult.Err(Error.Utf8("invalid utf-8 sequence"))
                i += 3
            }

            b0 in 0xF0..0xF4 -> {
                if (i + 3 >= bytes.size) return TzResult.Err(Error.Utf8("invalid utf-8 sequence"))
                val b1 = bytes[i + 1].toInt() and 0xFF
                val b2 = bytes[i + 2].toInt() and 0xFF
                val b3 = bytes[i + 3].toInt() and 0xFF
                if ((b1 and 0xC0) != 0x80 || (b2 and 0xC0) != 0x80 || (b3 and 0xC0) != 0x80) {
                    return TzResult.Err(Error.Utf8("invalid utf-8 sequence"))
                }
                if (b0 == 0xF0 && b1 < 0x90) return TzResult.Err(Error.Utf8("invalid utf-8 sequence"))
                if (b0 == 0xF4 && b1 >= 0x90) return TzResult.Err(Error.Utf8("invalid utf-8 sequence"))
                i += 4
            }

            else -> {
                return TzResult.Err(Error.Utf8("invalid utf-8 sequence"))
            }
        }
    }
    return TzResult.Ok(bytes.decodeToString())
}
