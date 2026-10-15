// port-lint: source month.rs
package io.github.kotlinmania.chrono

import io.github.kotlinmania.chrono.naive.NaiveDate
import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.ser.Serialize
import io.github.kotlinmania.serdecore.ser.Serializer
import kotlin.jvm.JvmInline

/**
 * The month of the year.
 *
 * This enum is a convenience implementation. The month in dates created by
 * [Datelike] objects does not return this enum.
 *
 * It is possible to convert from a date to a month independently, or from a
 * [Month] to an integer usable by dates. Mapping is from January as 1 through
 * December as 12.
 */
enum class Month : Serialize {
    /** January. */
    January,

    /** February. */
    February,

    /** March. */
    March,

    /** April. */
    April,

    /** May. */
    May,

    /** June. */
    June,

    /** July. */
    July,

    /** August. */
    August,

    /** September. */
    September,

    /** October. */
    October,

    /** November. */
    November,

    /** December. */
    December,

    ;

    /**
     * The next month.
     *
     * | `m` | `January` | `February` | `...` | `December` |
     * | --- | --------- | ---------- | ----- | ---------- |
     * | `m.succ()` | `February` | `March` | `...` | `January` |
     */
    fun succ(): Month =
        when (this) {
            January -> February
            February -> March
            March -> April
            April -> May
            May -> June
            June -> July
            July -> August
            August -> September
            September -> October
            October -> November
            November -> December
            December -> January
        }

    /**
     * The previous month.
     *
     * | `m` | `January` | `February` | `...` | `December` |
     * | --- | --------- | ---------- | ----- | ---------- |
     * | `m.pred()` | `December` | `January` | `...` | `November` |
     */
    fun pred(): Month =
        when (this) {
            January -> December
            February -> January
            March -> February
            April -> March
            May -> April
            June -> May
            July -> June
            August -> July
            September -> August
            October -> September
            November -> October
            December -> November
        }

    /**
     * Returns a month-of-year number starting from January = 1.
     *
     * | `m` | `January` | `February` | `...` | `December` |
     * | --- | --------- | ---------- | ----- | ---------- |
     * | `m.numberFromMonth()` | 1 | 2 | `...` | 12 |
     */
    fun numberFromMonth(): UInt = (ordinal + 1).toUInt()

    /** Gets the name of the month. */
    @kotlin.jvm.JvmName("monthName")
    fun name(): String =
        when (this) {
            January -> "January"
            February -> "February"
            March -> "March"
            April -> "April"
            May -> "May"
            June -> "June"
            July -> "July"
            August -> "August"
            September -> "September"
            October -> "October"
            November -> "November"
            December -> "December"
        }

    /**
     * Gets the length in days of the month.
     *
     * Yields `null` if [year] is out of range for `NaiveDate`.
     */
    fun numDays(year: Int): UByte? =
        when (this) {
            January -> {
                31u
            }

            February -> {
                when (NaiveDate.fromYmdOpt(year, 2u, 1u)?.leapYear()) {
                    true -> 29u
                    false -> 28u
                    null -> return null
                }
            }

            March -> {
                31u
            }

            April -> {
                30u
            }

            May -> {
                31u
            }

            June -> {
                30u
            }

            July -> {
                31u
            }

            August -> {
                31u
            }

            September -> {
                30u
            }

            October -> {
                31u
            }

            November -> {
                30u
            }

            December -> {
                31u
            }
        }.toUByte()

    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serializer.collectStr(name())

    companion object : Deserialize<Month> {
        private val LONG_MONTH_SUFFIXES =
            arrayOf(
                "uary",
                "ruary",
                "ch",
                "il",
                "",
                "e",
                "y",
                "ust",
                "tember",
                "ober",
                "ember",
                "ember",
            )

        /** Converts a one-based month number to a [Month]. */
        fun tryFrom(value: UByte): Result<Month> =
            when (value.toUInt()) {
                1u -> Result.success(January)
                2u -> Result.success(February)
                3u -> Result.success(March)
                4u -> Result.success(April)
                5u -> Result.success(May)
                6u -> Result.success(June)
                7u -> Result.success(July)
                8u -> Result.success(August)
                9u -> Result.success(September)
                10u -> Result.success(October)
                11u -> Result.success(November)
                12u -> Result.success(December)
                else -> Result.failure(OutOfRange())
            }

        /** Returns a [Month] from a non-negative integer, assuming January = 1. */
        fun fromU64(n: ULong): Month? = fromU32(n.toUInt())

        /** Returns a [Month] from an integer, assuming January = 1. */
        fun fromI64(n: Long): Month? = fromU32(n.toUInt())

        /** Returns a [Month] from a non-negative integer, assuming January = 1. */
        fun fromU32(n: UInt): Month? =
            when (n) {
                1u -> January
                2u -> February
                3u -> March
                4u -> April
                5u -> May
                6u -> June
                7u -> July
                8u -> August
                9u -> September
                10u -> October
                11u -> November
                12u -> December
                else -> null
            }

        /**
         * Parses a string into a [Month].
         *
         * The parsing is case-insensitive and accepts either the 3-letter short form
         * (e.g. `jan`) or the full month name (e.g. `january`).
         */
        fun fromStr(s: String): Result<Month> {
            if (s.length < 3) {
                return Result.failure(ParseMonthError())
            }
            val prefix = s.substring(0, 3).lowercase()
            val month0 =
                when (prefix) {
                    "jan" -> 0
                    "feb" -> 1
                    "mar" -> 2
                    "apr" -> 3
                    "may" -> 4
                    "jun" -> 5
                    "jul" -> 6
                    "aug" -> 7
                    "sep" -> 8
                    "oct" -> 9
                    "nov" -> 10
                    "dec" -> 11
                    else -> return Result.failure(ParseMonthError())
                }
            var rest = s.substring(3)
            val suffix = LONG_MONTH_SUFFIXES[month0]
            if (rest.length >= suffix.length &&
                rest.substring(0, suffix.length).equals(suffix, ignoreCase = true)
            ) {
                rest = rest.substring(suffix.length)
            }
            if (rest.isNotEmpty()) {
                return Result.failure(ParseMonthError())
            }
            return Result.success(entries[month0])
        }

        override fun <D> deserialize(deserializer: D): SerdeResult<Month> where D : Deserializer =
            deserializer.deserializeStr(MonthVisitor)
    }
}

private object MonthVisitor : Visitor<Month> {
    override fun expecting(): String = "Month"

    override fun visitStr(v: String): SerdeResult<Month> =
        Month.fromStr(v).fold(
            onSuccess = { SerdeResult.Success(it) },
            onFailure = {
                SerdeResult.Failure(
                    SerdeError.custom("short (3-letter) or full month names expected"),
                )
            },
        )
}

/** A duration in calendar months. */
@JvmInline
value class Months(
    private val value: UInt,
) : Comparable<Months> {
    /** Returns the total number of months in the [Months] instance. */
    fun asU32(): UInt = value

    override fun compareTo(other: Months): Int = value.compareTo(other.value)

    companion object {
        /** Constructs a new [Months] from a number of months. */
        fun new(num: UInt): Months = Months(num)
    }
}

/** An error resulting from reading a [Month] value from a string. */
class ParseMonthError internal constructor() : Throwable() {
    override fun toString(): String = "ParseMonthError { .. }"

    override val message: String get() = toString()

    override fun equals(other: Any?): Boolean = other is ParseMonthError

    override fun hashCode(): Int = 0
}
