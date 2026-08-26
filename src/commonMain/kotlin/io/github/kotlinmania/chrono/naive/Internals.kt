// port-lint: source naive/internals.rs
package io.github.kotlinmania.chrono.naive

import kotlin.jvm.JvmInline

/**
 * Year flags (aka the dominical letter).
 *
 * `YearFlags` are used as the last four bits of `NaiveDate`, `Mdf` and `IsoWeek`.
 *
 * There are 14 possible classes of year in the Gregorian calendar:
 * common and leap years starting with Monday through Sunday.
 *
 * The `YearFlags` stores this information into 4 bits `LWWW`. `L` is the leap year flag, with `1`
 * for the common year (this simplifies validating an ordinal in `NaiveDate`). `WWW` is a non-zero
 * `Weekday` of the last day in the preceding year.
 */
@JvmInline
value class YearFlags internal constructor(internal val value: UByte) {
    internal fun ndays(): UInt {
        val flags = value.toUInt()
        return 366u - (flags shr 3)
    }

    internal fun isoweekDelta(): UInt {
        val flags = value.toUInt()
        var delta = flags and 7u
        if (delta < 3u) {
            delta += 7u
        }
        return delta
    }

    internal fun nisoweeks(): UInt {
        val flags = value.toUInt().toInt()
        val bit = (0b0000_0100_0000_0110 ushr flags) and 1
        return (52 + bit).toUInt()
    }

    override fun toString(): String {
        return when (value.toInt()) {
            13 -> "A"
            5 -> "AG"
            12 -> "B"
            4 -> "BA"
            11 -> "C"
            3 -> "CB"
            10 -> "D"
            2 -> "DC"
            9 -> "E"
            1 -> "ED"
            8 -> "F?"
            0 -> "FE?"
            15 -> "F"
            7 -> "FE"
            14 -> "G"
            6 -> "GF"
            else -> "YearFlags($value)"
        }
    }

    companion object {
        private const val YEAR_STARTS_AFTER_MONDAY: UByte = 7u
        private const val YEAR_STARTS_AFTER_TUESDAY: UByte = 1u
        private const val YEAR_STARTS_AFTER_WEDNESDAY: UByte = 2u
        private const val YEAR_STARTS_AFTER_THURSDAY: UByte = 3u
        private const val YEAR_STARTS_AFTER_FRIDAY: UByte = 4u
        private const val YEAR_STARTS_AFTER_SATURDAY: UByte = 5u
        private const val YEAR_STARTS_AFTER_SUNDAY: UByte = 6u

        private const val COMMON_YEAR: UByte = 8u
        private const val LEAP_YEAR: UByte = 0u

        internal val A: YearFlags = YearFlags((COMMON_YEAR.toInt() or YEAR_STARTS_AFTER_SATURDAY.toInt()).toUByte())
        internal val AG: YearFlags = YearFlags((LEAP_YEAR.toInt() or YEAR_STARTS_AFTER_SATURDAY.toInt()).toUByte())
        internal val B: YearFlags = YearFlags((COMMON_YEAR.toInt() or YEAR_STARTS_AFTER_FRIDAY.toInt()).toUByte())
        internal val BA: YearFlags = YearFlags((LEAP_YEAR.toInt() or YEAR_STARTS_AFTER_FRIDAY.toInt()).toUByte())
        internal val C: YearFlags = YearFlags((COMMON_YEAR.toInt() or YEAR_STARTS_AFTER_THURSDAY.toInt()).toUByte())
        internal val CB: YearFlags = YearFlags((LEAP_YEAR.toInt() or YEAR_STARTS_AFTER_THURSDAY.toInt()).toUByte())
        internal val D: YearFlags = YearFlags((COMMON_YEAR.toInt() or YEAR_STARTS_AFTER_WEDNESDAY.toInt()).toUByte())
        internal val DC: YearFlags = YearFlags((LEAP_YEAR.toInt() or YEAR_STARTS_AFTER_WEDNESDAY.toInt()).toUByte())
        internal val E: YearFlags = YearFlags((COMMON_YEAR.toInt() or YEAR_STARTS_AFTER_TUESDAY.toInt()).toUByte())
        internal val ED: YearFlags = YearFlags((LEAP_YEAR.toInt() or YEAR_STARTS_AFTER_TUESDAY.toInt()).toUByte())
        internal val F: YearFlags = YearFlags((COMMON_YEAR.toInt() or YEAR_STARTS_AFTER_MONDAY.toInt()).toUByte())
        internal val FE: YearFlags = YearFlags((LEAP_YEAR.toInt() or YEAR_STARTS_AFTER_MONDAY.toInt()).toUByte())
        internal val G: YearFlags = YearFlags((COMMON_YEAR.toInt() or YEAR_STARTS_AFTER_SUNDAY.toInt()).toUByte())
        internal val GF: YearFlags = YearFlags((LEAP_YEAR.toInt() or YEAR_STARTS_AFTER_SUNDAY.toInt()).toUByte())

        private val YEAR_TO_FLAGS: Array<YearFlags> = arrayOf(
            BA, G, F, E, DC, B, A, G, FE, D, C, B, AG, F, E, D, CB, A, G, F, ED, C, B, A, GF, E, D, C, BA,
            G, F, E, DC, B, A, G, FE, D, C, B, AG, F, E, D, CB, A, G, F, ED, C, B, A, GF, E, D, C, BA, G,
            F, E, DC, B, A, G, FE, D, C, B, AG, F, E, D, CB, A, G, F, ED, C, B, A, GF, E, D, C, BA, G, F,
            E, DC, B, A, G, FE, D, C, B, AG, F, E, D, // 100
            C, B, A, G, FE, D, C, B, AG, F, E, D, CB, A, G, F, ED, C, B, A, GF, E, D, C, BA, G, F, E, DC,
            B, A, G, FE, D, C, B, AG, F, E, D, CB, A, G, F, ED, C, B, A, GF, E, D, C, BA, G, F, E, DC, B,
            A, G, FE, D, C, B, AG, F, E, D, CB, A, G, F, ED, C, B, A, GF, E, D, C, BA, G, F, E, DC, B, A,
            G, FE, D, C, B, AG, F, E, D, CB, A, G, F, // 200
            E, D, C, B, AG, F, E, D, CB, A, G, F, ED, C, B, A, GF, E, D, C, BA, G, F, E, DC, B, A, G, FE,
            D, C, B, AG, F, E, D, CB, A, G, F, ED, C, B, A, GF, E, D, C, BA, G, F, E, DC, B, A, G, FE, D,
            C, B, AG, F, E, D, CB, A, G, F, ED, C, B, A, GF, E, D, C, BA, G, F, E, DC, B, A, G, FE, D, C,
            B, AG, F, E, D, CB, A, G, F, ED, C, B, A, // 300
            G, F, E, D, CB, A, G, F, ED, C, B, A, GF, E, D, C, BA, G, F, E, DC, B, A, G, FE, D, C, B, AG,
            F, E, D, CB, A, G, F, ED, C, B, A, GF, E, D, C, BA, G, F, E, DC, B, A, G, FE, D, C, B, AG, F,
            E, D, CB, A, G, F, ED, C, B, A, GF, E, D, C, BA, G, F, E, DC, B, A, G, FE, D, C, B, AG, F, E,
            D, CB, A, G, F, ED, C, B, A, GF, E, D, C, // 400
        )

        fun fromYear(year: Int): YearFlags {
            val mod = ((year % 400) + 400) % 400
            return fromYearMod400(mod)
        }

        internal fun fromYearMod400(year: Int): YearFlags {
            return YEAR_TO_FLAGS[year]
        }
    }
}

private const val MAX_OL: Int = 366 shl 1
internal const val MAX_MDL: Int = (12 shl 6) or (31 shl 1) or 1

private const val XX: Byte = 0
private val MDL_TO_OL: ByteArray = byteArrayOf(
    XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX,
    XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX,
    XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, // 0
    XX, XX, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64,
    64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64,
    64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, // 1
    XX, XX, 66, 66, 66, 66, 66, 66, 66, 66, 66, 66, 66, 66, 66, 66, 66, 66, 66, 66, 66, 66, 66, 66,
    66, 66, 66, 66, 66, 66, 66, 66, 66, 66, 66, 66, 66, 66, 66, 66, 66, 66, 66, 66, 66, 66, 66, 66,
    66, 66, 66, 66, 66, 66, 66, 66, 66, 66, 66, XX, XX, XX, XX, XX, // 2
    XX, XX, 72, 74, 72, 74, 72, 74, 72, 74, 72, 74, 72, 74, 72, 74, 72, 74, 72, 74, 72, 74, 72, 74,
    72, 74, 72, 74, 72, 74, 72, 74, 72, 74, 72, 74, 72, 74, 72, 74, 72, 74, 72, 74, 72, 74, 72, 74,
    72, 74, 72, 74, 72, 74, 72, 74, 72, 74, 72, 74, 72, 74, 72, 74, // 3
    XX, XX, 74, 76, 74, 76, 74, 76, 74, 76, 74, 76, 74, 76, 74, 76, 74, 76, 74, 76, 74, 76, 74, 76,
    74, 76, 74, 76, 74, 76, 74, 76, 74, 76, 74, 76, 74, 76, 74, 76, 74, 76, 74, 76, 74, 76, 74, 76,
    74, 76, 74, 76, 74, 76, 74, 76, 74, 76, 74, 76, 74, 76, XX, XX, // 4
    XX, XX, 78, 80, 78, 80, 78, 80, 78, 80, 78, 80, 78, 80, 78, 80, 78, 80, 78, 80, 78, 80, 78, 80,
    78, 80, 78, 80, 78, 80, 78, 80, 78, 80, 78, 80, 78, 80, 78, 80, 78, 80, 78, 80, 78, 80, 78, 80,
    78, 80, 78, 80, 78, 80, 78, 80, 78, 80, 78, 80, 78, 80, 78, 80, // 5
    XX, XX, 80, 82, 80, 82, 80, 82, 80, 82, 80, 82, 80, 82, 80, 82, 80, 82, 80, 82, 80, 82, 80, 82,
    80, 82, 80, 82, 80, 82, 80, 82, 80, 82, 80, 82, 80, 82, 80, 82, 80, 82, 80, 82, 80, 82, 80, 82,
    80, 82, 80, 82, 80, 82, 80, 82, 80, 82, 80, 82, 80, 82, XX, XX, // 6
    XX, XX, 84, 86, 84, 86, 84, 86, 84, 86, 84, 86, 84, 86, 84, 86, 84, 86, 84, 86, 84, 86, 84, 86,
    84, 86, 84, 86, 84, 86, 84, 86, 84, 86, 84, 86, 84, 86, 84, 86, 84, 86, 84, 86, 84, 86, 84, 86,
    84, 86, 84, 86, 84, 86, 84, 86, 84, 86, 84, 86, 84, 86, 84, 86, // 7
    XX, XX, 86, 88, 86, 88, 86, 88, 86, 88, 86, 88, 86, 88, 86, 88, 86, 88, 86, 88, 86, 88, 86, 88,
    86, 88, 86, 88, 86, 88, 86, 88, 86, 88, 86, 88, 86, 88, 86, 88, 86, 88, 86, 88, 86, 88, 86, 88,
    86, 88, 86, 88, 86, 88, 86, 88, 86, 88, 86, 88, 86, 88, 86, 88, // 8
    XX, XX, 88, 90, 88, 90, 88, 90, 88, 90, 88, 90, 88, 90, 88, 90, 88, 90, 88, 90, 88, 90, 88, 90,
    88, 90, 88, 90, 88, 90, 88, 90, 88, 90, 88, 90, 88, 90, 88, 90, 88, 90, 88, 90, 88, 90, 88, 90,
    88, 90, 88, 90, 88, 90, 88, 90, 88, 90, 88, 90, 88, 90, XX, XX, // 9
    XX, XX, 92, 94, 92, 94, 92, 94, 92, 94, 92, 94, 92, 94, 92, 94, 92, 94, 92, 94, 92, 94, 92, 94,
    92, 94, 92, 94, 92, 94, 92, 94, 92, 94, 92, 94, 92, 94, 92, 94, 92, 94, 92, 94, 92, 94, 92, 94,
    92, 94, 92, 94, 92, 94, 92, 94, 92, 94, 92, 94, 92, 94, 92, 94, // 10
    XX, XX, 94, 96, 94, 96, 94, 96, 94, 96, 94, 96, 94, 96, 94, 96, 94, 96, 94, 96, 94, 96, 94, 96,
    94, 96, 94, 96, 94, 96, 94, 96, 94, 96, 94, 96, 94, 96, 94, 96, 94, 96, 94, 96, 94, 96, 94, 96,
    94, 96, 94, 96, 94, 96, 94, 96, 94, 96, 94, 96, 94, 96, XX, XX, // 11
    XX, XX, 98, 100, 98, 100, 98, 100, 98, 100, 98, 100, 98, 100, 98, 100, 98, 100, 98, 100, 98,
    100, 98, 100, 98, 100, 98, 100, 98, 100, 98, 100, 98, 100, 98, 100, 98, 100, 98, 100, 98, 100,
    98, 100, 98, 100, 98, 100, 98, 100, 98, 100, 98, 100, 98, 100, 98, 100, 98, 100, 98, 100, 98,
    100, // 12
)

private val OL_TO_MDL: UByteArray = ubyteArrayOf(
    0u, 0u, // 0
    64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u,
    64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u,
    64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, 64u, // 1
    66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u,
    66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u,
    66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u, 66u, // 2
    74u, 72u, 74u, 72u, 74u, 72u, 74u, 72u, 74u, 72u, 74u, 72u, 74u, 72u, 74u, 72u, 74u, 72u, 74u, 72u, 74u, 72u, 74u, 72u,
    74u, 72u, 74u, 72u, 74u, 72u, 74u, 72u, 74u, 72u, 74u, 72u, 74u, 72u, 74u, 72u, 74u, 72u, 74u, 72u, 74u, 72u, 74u, 72u,
    74u, 72u, 74u, 72u, 74u, 72u, 74u, 72u, 74u, 72u, 74u, 72u, 74u, 72u, // 3
    76u, 74u, 76u, 74u, 76u, 74u, 76u, 74u, 76u, 74u, 76u, 74u, 76u, 74u, 76u, 74u, 76u, 74u, 76u, 74u, 76u, 74u, 76u, 74u,
    76u, 74u, 76u, 74u, 76u, 74u, 76u, 74u, 76u, 74u, 76u, 74u, 76u, 74u, 76u, 74u, 76u, 74u, 76u, 74u, 76u, 74u, 76u, 74u,
    76u, 74u, 76u, 74u, 76u, 74u, 76u, 74u, 76u, 74u, 76u, 74u, // 4
    80u, 78u, 80u, 78u, 80u, 78u, 80u, 78u, 80u, 78u, 80u, 78u, 80u, 78u, 80u, 78u, 80u, 78u, 80u, 78u, 80u, 78u, 80u, 78u,
    80u, 78u, 80u, 78u, 80u, 78u, 80u, 78u, 80u, 78u, 80u, 78u, 80u, 78u, 80u, 78u, 80u, 78u, 80u, 78u, 80u, 78u, 80u, 78u,
    80u, 78u, 80u, 78u, 80u, 78u, 80u, 78u, 80u, 78u, 80u, 78u, 80u, 78u, // 5
    82u, 80u, 82u, 80u, 82u, 80u, 82u, 80u, 82u, 80u, 82u, 80u, 82u, 80u, 82u, 80u, 82u, 80u, 82u, 80u, 82u, 80u, 82u, 80u,
    82u, 80u, 82u, 80u, 82u, 80u, 82u, 80u, 82u, 80u, 82u, 80u, 82u, 80u, 82u, 80u, 82u, 80u, 82u, 80u, 82u, 80u, 82u, 80u,
    82u, 80u, 82u, 80u, 82u, 80u, 82u, 80u, 82u, 80u, 82u, 80u, // 6
    86u, 84u, 86u, 84u, 86u, 84u, 86u, 84u, 86u, 84u, 86u, 84u, 86u, 84u, 86u, 84u, 86u, 84u, 86u, 84u, 86u, 84u, 86u, 84u,
    86u, 84u, 86u, 84u, 86u, 84u, 86u, 84u, 86u, 84u, 86u, 84u, 86u, 84u, 86u, 84u, 86u, 84u, 86u, 84u, 86u, 84u, 86u, 84u,
    86u, 84u, 86u, 84u, 86u, 84u, 86u, 84u, 86u, 84u, 86u, 84u, 86u, 84u, // 7
    88u, 86u, 88u, 86u, 88u, 86u, 88u, 86u, 88u, 86u, 88u, 86u, 88u, 86u, 88u, 86u, 88u, 86u, 88u, 86u, 88u, 86u, 88u, 86u,
    88u, 86u, 88u, 86u, 88u, 86u, 88u, 86u, 88u, 86u, 88u, 86u, 88u, 86u, 88u, 86u, 88u, 86u, 88u, 86u, 88u, 86u, 88u, 86u,
    88u, 86u, 88u, 86u, 88u, 86u, 88u, 86u, 88u, 86u, 88u, 86u, 88u, 86u, // 8
    90u, 88u, 90u, 88u, 90u, 88u, 90u, 88u, 90u, 88u, 90u, 88u, 90u, 88u, 90u, 88u, 90u, 88u, 90u, 88u, 90u, 88u, 90u, 88u,
    90u, 88u, 90u, 88u, 90u, 88u, 90u, 88u, 90u, 88u, 90u, 88u, 90u, 88u, 90u, 88u, 90u, 88u, 90u, 88u, 90u, 88u, 90u, 88u,
    90u, 88u, 90u, 88u, 90u, 88u, 90u, 88u, 90u, 88u, 90u, 88u, // 9
    94u, 92u, 94u, 92u, 94u, 92u, 94u, 92u, 94u, 92u, 94u, 92u, 94u, 92u, 94u, 92u, 94u, 92u, 94u, 92u, 94u, 92u, 94u, 92u,
    94u, 92u, 94u, 92u, 94u, 92u, 94u, 92u, 94u, 92u, 94u, 92u, 94u, 92u, 94u, 92u, 94u, 92u, 94u, 92u, 94u, 92u, 94u, 92u,
    94u, 92u, 94u, 92u, 94u, 92u, 94u, 92u, 94u, 92u, 94u, 92u, 94u, 92u, // 10
    96u, 94u, 96u, 94u, 96u, 94u, 96u, 94u, 96u, 94u, 96u, 94u, 96u, 94u, 96u, 94u, 96u, 94u, 96u, 94u, 96u, 94u, 96u, 94u,
    96u, 94u, 96u, 94u, 96u, 94u, 96u, 94u, 96u, 94u, 96u, 94u, 96u, 94u, 96u, 94u, 96u, 94u, 96u, 94u, 96u, 94u, 96u, 94u,
    96u, 94u, 96u, 94u, 96u, 94u, 96u, 94u, 96u, 94u, 96u, 94u, // 11
    100u, 98u, 100u, 98u, 100u, 98u, 100u, 98u, 100u, 98u, 100u, 98u, 100u, 98u, 100u, 98u, 100u, 98u, 100u, 98u, 100u, 98u,
    100u, 98u, 100u, 98u, 100u, 98u, 100u, 98u, 100u, 98u, 100u, 98u, 100u, 98u, 100u, 98u, 100u, 98u, 100u, 98u, 100u, 98u,
    100u, 98u, 100u, 98u, 100u, 98u, 100u, 98u, 100u, 98u, 100u, 98u, 100u, 98u, 100u, 98u, 100u, 98u, 100u, 98u, 100u, 98u,
    100u, // 12
)

/**
 * Month, day of month and year flags: `(month << 9) | (day << 4) | flags`
 */
@JvmInline
internal value class Mdf internal constructor(internal val value: UInt) {
    internal fun month(): UInt = value shr 9

    internal fun withMonth(month: UInt): Mdf? {
        if (month > 12u) return null
        return Mdf((value and 0b1_1111_1111u) or (month shl 9))
    }

    internal fun day(): UInt = (value shr 4) and 0b1_1111u

    internal fun withDay(day: UInt): Mdf? {
        if (day > 31u) return null
        return Mdf((value and 0b1_1111_0000u.inv()) or (day shl 4))
    }

    internal fun withFlags(flags: YearFlags): Mdf {
        return Mdf((value and 0b1111u.inv()) or flags.value.toUInt())
    }

    internal fun ordinal(): UInt? {
        val mdl = (value shr 3).toInt()
        if (mdl !in MDL_TO_OL.indices) return null
        val v = MDL_TO_OL[mdl].toInt()
        if (v == 0) return null
        return ((mdl - v) shr 1).toUInt()
    }

    internal fun yearFlags(): YearFlags = YearFlags((value and 0b1111u).toUByte())

    internal fun ordinalAndFlags(): Int? {
        val mdl = (value shr 3).toInt()
        if (mdl !in MDL_TO_OL.indices) return null
        val v = MDL_TO_OL[mdl].toInt()
        if (v == 0) return null
        return value.toInt() - (v shl 3)
    }

    internal fun valid(): Boolean {
        val mdl = (value shr 3).toInt()
        return mdl in MDL_TO_OL.indices && MDL_TO_OL[mdl] > 0
    }

    companion object {
        internal fun new(month: UInt, day: UInt, flags: YearFlags): Mdf? {
            if (month <= 12u && day <= 31u) {
                return Mdf((month shl 9) or (day shl 4) or flags.value.toUInt())
            }
            return null
        }

        internal fun fromOl(ol: Int, flags: YearFlags): Mdf {
            val adjustment = OL_TO_MDL[ol].toUInt()
            return Mdf(((ol.toUInt() + adjustment) shl 3) or flags.value.toUInt())
        }
    }
}
