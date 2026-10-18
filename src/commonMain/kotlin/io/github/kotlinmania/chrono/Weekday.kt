// port-lint: source src/weekday.rs
package io.github.kotlinmania.chrono

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.ser.Serialize
import io.github.kotlinmania.serdecore.ser.Serializer

/**
 * The day of week.
 *
 * The order of the days of week depends on the context.
 * (This is why this type does *not* implement [Comparable].)
 * One should prefer `*FromMonday` or `*FromSunday` methods to get the correct result.
 *
 * # Example
 * ```
 * val monday = "Monday".let { /* parse via the format module */ ... }
 * check(monday == Weekday.Mon)
 *
 * val sunday = Weekday.tryFrom(6).getOrThrow()
 * check(sunday == Weekday.Sun)
 *
 * check(sunday.numDaysFromMonday() == 6) // starts counting with Monday = 0
 * check(sunday.numberFromMonday() == 7) // starts counting with Monday = 1
 * check(sunday.numDaysFromSunday() == 0) // starts counting with Sunday = 0
 * check(sunday.numberFromSunday() == 1) // starts counting with Sunday = 1
 *
 * check(sunday.succ() == monday)
 * check(sunday.pred() == Weekday.Sat)
 * ```
 */
enum class Weekday(
    val value: Int,
) : Serialize {
    /** Monday. */
    Mon(0),

    /** Tuesday. */
    Tue(1),

    /** Wednesday. */
    Wed(2),

    /** Thursday. */
    Thu(3),

    /** Friday. */
    Fri(4),

    /** Saturday. */
    Sat(5),

    /** Sunday. */
    Sun(6),
    ;

    /**
     * The next day in the week.
     *
     * | `w`:        | `Mon` | `Tue` | `Wed` | `Thu` | `Fri` | `Sat` | `Sun` |
     * | ----------- | ----- | ----- | ----- | ----- | ----- | ----- | ----- |
     * | `w.succ()`: | `Tue` | `Wed` | `Thu` | `Fri` | `Sat` | `Sun` | `Mon` |
     */
    fun succ(): Weekday =
        when (this) {
            Mon -> Tue
            Tue -> Wed
            Wed -> Thu
            Thu -> Fri
            Fri -> Sat
            Sat -> Sun
            Sun -> Mon
        }

    /**
     * The previous day in the week.
     *
     * | `w`:        | `Mon` | `Tue` | `Wed` | `Thu` | `Fri` | `Sat` | `Sun` |
     * | ----------- | ----- | ----- | ----- | ----- | ----- | ----- | ----- |
     * | `w.pred()`: | `Sun` | `Mon` | `Tue` | `Wed` | `Thu` | `Fri` | `Sat` |
     */
    fun pred(): Weekday =
        when (this) {
            Mon -> Sun
            Tue -> Mon
            Wed -> Tue
            Thu -> Wed
            Fri -> Thu
            Sat -> Fri
            Sun -> Sat
        }

    /**
     * Returns a day-of-week number starting from Monday = 1. (ISO 8601 weekday number)
     *
     * | `w`:                      | `Mon` | `Tue` | `Wed` | `Thu` | `Fri` | `Sat` | `Sun` |
     * | ------------------------- | ----- | ----- | ----- | ----- | ----- | ----- | ----- |
     * | `w.numberFromMonday()`:   | 1     | 2     | 3     | 4     | 5     | 6     | 7     |
     */
    fun numberFromMonday(): Int = daysSince(Mon) + 1

    /**
     * Returns a day-of-week number starting from Sunday = 1.
     *
     * | `w`:                      | `Mon` | `Tue` | `Wed` | `Thu` | `Fri` | `Sat` | `Sun` |
     * | ------------------------- | ----- | ----- | ----- | ----- | ----- | ----- | ----- |
     * | `w.numberFromSunday()`:   | 2     | 3     | 4     | 5     | 6     | 7     | 1     |
     */
    fun numberFromSunday(): Int = daysSince(Sun) + 1

    /**
     * Returns a day-of-week number starting from Monday = 0.
     *
     * | `w`:                        | `Mon` | `Tue` | `Wed` | `Thu` | `Fri` | `Sat` | `Sun` |
     * | --------------------------- | ----- | ----- | ----- | ----- | ----- | ----- | ----- |
     * | `w.numDaysFromMonday()`:    | 0     | 1     | 2     | 3     | 4     | 5     | 6     |
     *
     * # Example
     *
     * ```
     * // MTWRFSU is occasionally used as a single-letter abbreviation of the weekdays.
     * // Use `numDaysFromMonday` to index into the list.
     * val mtwrfsu = listOf('M', 'T', 'W', 'R', 'F', 'S', 'U')
     *
     * val today = Local.now().weekday()
     * println(mtwrfsu[today.numDaysFromMonday()])
     * ```
     */
    fun numDaysFromMonday(): Int = daysSince(Mon)

    /**
     * Returns a day-of-week number starting from Sunday = 0.
     *
     * | `w`:                        | `Mon` | `Tue` | `Wed` | `Thu` | `Fri` | `Sat` | `Sun` |
     * | --------------------------- | ----- | ----- | ----- | ----- | ----- | ----- | ----- |
     * | `w.numDaysFromSunday()`:    | 1     | 2     | 3     | 4     | 5     | 6     | 0     |
     */
    fun numDaysFromSunday(): Int = daysSince(Sun)

    /**
     * The number of days since the given day.
     *
     * # Examples
     *
     * ```
     * check(Weekday.Mon.daysSince(Weekday.Mon) == 0)
     * check(Weekday.Sun.daysSince(Weekday.Tue) == 5)
     * check(Weekday.Wed.daysSince(Weekday.Sun) == 3)
     * ```
     */
    fun daysSince(other: Weekday): Int {
        val lhs = this.value
        val rhs = other.value
        return if (lhs < rhs) 7 + lhs - rhs else lhs - rhs
    }

    override fun toString(): String =
        when (this) {
            Mon -> "Mon"
            Tue -> "Tue"
            Wed -> "Wed"
            Thu -> "Thu"
            Fri -> "Fri"
            Sat -> "Sat"
            Sun -> "Sun"
        }

    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serializer.collectStr(toString())

    companion object : Deserialize<Weekday> {
        private val LONG_WEEKDAY_SUFFIXES =
            arrayOf(
                "day",
                "sday",
                "nesday",
                "rsday",
                "day",
                "urday",
                "day",
            )

        /**
         * Any weekday can be represented as an integer from 0 to 6, which equals to
         * [numDaysFromMonday] in this implementation.
         * Do not heavily depend on this though; use explicit methods whenever possible.
         */
        fun tryFrom(value: Int): Result<Weekday> =
            when (value) {
                0 -> Result.success(Mon)
                1 -> Result.success(Tue)
                2 -> Result.success(Wed)
                3 -> Result.success(Thu)
                4 -> Result.success(Fri)
                5 -> Result.success(Sat)
                6 -> Result.success(Sun)
                else -> Result.failure(OutOfRange())
            }

        /**
         * Any weekday can be represented as an integer from 0 to 6, which equals to
         * [numDaysFromMonday] in this implementation.
         * Do not heavily depend on this though; use explicit methods whenever possible.
         */
        fun fromI64(n: Long): Weekday? =
            when (n) {
                0L -> Mon
                1L -> Tue
                2L -> Wed
                3L -> Thu
                4L -> Fri
                5L -> Sat
                6L -> Sun
                else -> null
            }

        fun fromU64(n: ULong): Weekday? =
            when (n) {
                0uL -> Mon
                1uL -> Tue
                2uL -> Wed
                3uL -> Thu
                4uL -> Fri
                5uL -> Sat
                6uL -> Sun
                else -> null
            }

        /**
         * Parses a string into a [Weekday].
         *
         * The parsing is case-insensitive and accepts only the shortest form (e.g. `sun`)
         * and the longest form (e.g. `sunday`).
         */
        fun fromStr(s: String): Result<Weekday> {
            if (s.length < 3) {
                return Result.failure(ParseWeekdayError())
            }
            val prefix = s.substring(0, 3).lowercase()
            val weekday =
                when (prefix) {
                    "mon" -> Mon
                    "tue" -> Tue
                    "wed" -> Wed
                    "thu" -> Thu
                    "fri" -> Fri
                    "sat" -> Sat
                    "sun" -> Sun
                    else -> return Result.failure(ParseWeekdayError())
                }
            var rest = s.substring(3)
            val suffix = LONG_WEEKDAY_SUFFIXES[weekday.numDaysFromMonday()]
            if (rest.length >= suffix.length &&
                rest.substring(0, suffix.length).equals(suffix, ignoreCase = true)
            ) {
                rest = rest.substring(suffix.length)
            }
            if (rest.isNotEmpty()) {
                return Result.failure(ParseWeekdayError())
            }
            return Result.success(weekday)
        }

        override fun <D> deserialize(deserializer: D): SerdeResult<Weekday> where D : Deserializer =
            deserializer.deserializeStr(WeekdayVisitor)
    }
}

private object WeekdayVisitor : Visitor<Weekday> {
    override fun expecting(): String = "Weekday"

    override fun visitStr(v: String): SerdeResult<Weekday> =
        Weekday.fromStr(v).fold(
            onSuccess = { SerdeResult.Success(it) },
            onFailure = {
                SerdeResult.Failure(
                    SerdeError.custom("short or long weekday names expected"),
                )
            },
        )
}

/** An error resulting from reading [Weekday] value from a string. */
class ParseWeekdayError internal constructor() : Throwable() {
    override fun toString(): String = "ParseWeekdayError { .. }"

    override val message: String get() = toString()

    override fun equals(other: Any?): Boolean = other is ParseWeekdayError

    override fun hashCode(): Int = 0
}
