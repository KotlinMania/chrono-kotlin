// port-lint: source naive/date/mod.rs
package io.github.kotlinmania.chrono.naive

import io.github.kotlinmania.chrono.Datelike
import io.github.kotlinmania.chrono.Months
import io.github.kotlinmania.chrono.TimeDelta
import io.github.kotlinmania.chrono.Weekday
import kotlin.math.abs

internal const val UNIX_EPOCH_DAY: Long = 719_163L

internal const val MAX_YEAR: Int = (Int.MAX_VALUE shr 13) - 1
internal const val MIN_YEAR: Int = (Int.MIN_VALUE shr 13) + 1

private const val ORDINAL_MASK: Int = 0b1_1111_1111_0000
private const val LEAP_YEAR_MASK: Int = 0b1000
private const val OL_MASK: Int = ORDINAL_MASK or LEAP_YEAR_MASK
private const val MAX_OL: Int = 366 shl 4
private const val WEEKDAY_FLAGS_MASK: Int = 0b111
private const val YEAR_FLAGS_MASK: Int = LEAP_YEAR_MASK or WEEKDAY_FLAGS_MASK

private val YEAR_DELTAS: ByteArray = byteArrayOf(
    0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 6, 6, 7, 7, 7, 7, 8, 8, 8,
    8, 9, 9, 9, 9, 10, 10, 10, 10, 11, 11, 11, 11, 12, 12, 12, 12, 13, 13, 13, 13, 14, 14, 14, 14,
    15, 15, 15, 15, 16, 16, 16, 16, 17, 17, 17, 17, 18, 18, 18, 18, 19, 19, 19, 19, 20, 20, 20, 20,
    21, 21, 21, 21, 22, 22, 22, 22, 23, 23, 23, 23, 24, 24, 24, 24, 25, 25, 25, // 100
    25, 25, 25, 25, 25, 26, 26, 26, 26, 27, 27, 27, 27, 28, 28, 28, 28, 29, 29, 29, 29, 30, 30, 30,
    30, 31, 31, 31, 31, 32, 32, 32, 32, 33, 33, 33, 33, 34, 34, 34, 34, 35, 35, 35, 35, 36, 36, 36,
    36, 37, 37, 37, 37, 38, 38, 38, 38, 39, 39, 39, 39, 40, 40, 40, 40, 41, 41, 41, 41, 42, 42, 42,
    42, 43, 43, 43, 43, 44, 44, 44, 44, 45, 45, 45, 45, 46, 46, 46, 46, 47, 47, 47, 47, 48, 48, 48,
    48, 49, 49, 49, // 200
    49, 49, 49, 49, 49, 50, 50, 50, 50, 51, 51, 51, 51, 52, 52, 52, 52, 53, 53, 53, 53, 54, 54, 54,
    54, 55, 55, 55, 55, 56, 56, 56, 56, 57, 57, 57, 57, 58, 58, 58, 58, 59, 59, 59, 59, 60, 60, 60,
    60, 61, 61, 61, 61, 62, 62, 62, 62, 63, 63, 63, 63, 64, 64, 64, 64, 65, 65, 65, 65, 66, 66, 66,
    66, 67, 67, 67, 67, 68, 68, 68, 68, 69, 69, 69, 69, 70, 70, 70, 70, 71, 71, 71, 71, 72, 72, 72,
    72, 73, 73, 73, // 300
    73, 73, 73, 73, 73, 74, 74, 74, 74, 75, 75, 75, 75, 76, 76, 76, 76, 77, 77, 77, 77, 78, 78, 78,
    78, 79, 79, 79, 79, 80, 80, 80, 80, 81, 81, 81, 81, 82, 82, 82, 82, 83, 83, 83, 83, 84, 84, 84,
    84, 85, 85, 85, 85, 86, 86, 86, 86, 87, 87, 87, 87, 88, 88, 88, 88, 89, 89, 89, 89, 90, 90, 90,
    90, 91, 91, 91, 91, 92, 92, 92, 92, 93, 93, 93, 93, 94, 94, 94, 94, 95, 95, 95, 95, 96, 96, 96,
    96, 97, 97, 97, 97, // 400+1
)

/**
 * ISO 8601 calendar date without timezone.
 *
 * Allows for every proleptic Gregorian date from Jan 1, 262145 BCE to Dec 31, 262143 CE.
 */
class NaiveDate internal constructor(
    internal val yof: Int,
) : Datelike<NaiveDate>, Comparable<NaiveDate> {

    internal fun weeksFrom(day: Weekday): Int =
        (ordinal().toInt() - weekday().daysSince(day) + 6) / 7

    private fun mdf(): Mdf =
        Mdf.fromOl((yof and OL_MASK) ushr 3, yearFlags())

    private fun withMdf(mdf: Mdf): NaiveDate? {
        val ord = mdf.ordinal() ?: return null
        return fromYof((yof and ORDINAL_MASK.inv()) or ((ord.toInt()) shl 4))
    }

    /**
     * Makes a new [NaiveDate] for the next calendar date.
     */
    fun succ(): NaiveDate =
        succOpt() ?: throw IllegalArgumentException("out of bound")

    /**
     * Makes a new [NaiveDate] for the next calendar date, or null if out of bound.
     */
    fun succOpt(): NaiveDate? {
        val newOl = (yof and OL_MASK) + (1 shl 4)
        return if (newOl <= MAX_OL) {
            fromYof((yof and OL_MASK.inv()) or newOl)
        } else {
            fromYoOpt(year() + 1, 1u)
        }
    }

    /**
     * Makes a new [NaiveDate] for the previous calendar date.
     */
    fun pred(): NaiveDate =
        predOpt() ?: throw IllegalArgumentException("out of bound")

    /**
     * Makes a new [NaiveDate] for the previous calendar date, or null if out of bound.
     */
    fun predOpt(): NaiveDate? {
        val newShiftedOrdinal = (yof and ORDINAL_MASK) - (1 shl 4)
        return if (newShiftedOrdinal > 0) {
            fromYof((yof and ORDINAL_MASK.inv()) or newShiftedOrdinal)
        } else {
            fromYmdOpt(year() - 1, 12u, 31u)
        }
    }

    /**
     * Adds the number of whole days in the given [TimeDelta] to the current date.
     */
    fun checkedAddSigned(rhs: TimeDelta): NaiveDate? {
        val days = rhs.numDays()
        if (days < Int.MIN_VALUE.toLong() || days > Int.MAX_VALUE.toLong()) {
            return null
        }
        return addDays(days.toInt())
    }

    /**
     * Subtracts the number of whole days in the given [TimeDelta] from the current date.
     */
    fun checkedSubSigned(rhs: TimeDelta): NaiveDate? {
        val days = -rhs.numDays()
        if (days < Int.MIN_VALUE.toLong() || days > Int.MAX_VALUE.toLong()) {
            return null
        }
        return addDays(days.toInt())
    }

    /**
     * Subtracts another [NaiveDate] from the current date.
     */
    fun signedDurationSince(rhs: NaiveDate): TimeDelta {
        val year1 = year()
        val year2 = rhs.year()
        val (year1Div400, year1Mod400) = divModFloor(year1, 400)
        val (year2Div400, year2Mod400) = divModFloor(year2, 400)
        val cycle1 = yoToCycle(year1Mod400.toUInt(), ordinal()).toLong()
        val cycle2 = yoToCycle(year2Mod400.toUInt(), rhs.ordinal()).toLong()
        val days = (year1Div400.toLong() - year2Div400.toLong()) * 146_097L + (cycle1 - cycle2)
        return TimeDelta.tryDays(days) ?: throw IllegalArgumentException("always in range")
    }

    /**
     * Returns the absolute difference between two [NaiveDate]s measured as the number of days.
     */
    fun absDiff(rhs: NaiveDate): Days =
        Days.new(abs(numDaysFromCe().toLong() - rhs.numDaysFromCe().toLong()).toULong())

    /**
     * Returns the number of whole years from the given [base] until this date.
     */
    fun yearsSince(base: NaiveDate): UInt? {
        var years = year() - base.year()
        if (((month() shl 5) or day()) < ((base.month() shl 5) or base.day())) {
            years -= 1
        }
        return if (years >= 0) years.toUInt() else null
    }

    /**
     * Add a duration in [Months] to the date.
     */
    fun checkedAddMonths(months: Months): NaiveDate? {
        if (months.asU32() == 0u) return this
        return if (months.asU32() <= Int.MAX_VALUE.toUInt()) {
            diffMonths(months.asU32().toInt())
        } else {
            null
        }
    }

    /**
     * Subtract a duration in [Months] from the date.
     */
    fun checkedSubMonths(months: Months): NaiveDate? {
        if (months.asU32() == 0u) return this
        return if (months.asU32() <= Int.MAX_VALUE.toUInt()) {
            diffMonths(-months.asU32().toInt())
        } else {
            null
        }
    }

    private fun diffMonths(months: Int): NaiveDate? {
        val totalMonthsLong = (year().toLong() * 12L + month().toLong() - 1L) + months.toLong()
        if (totalMonthsLong < Int.MIN_VALUE.toLong() || totalMonthsLong > Int.MAX_VALUE.toLong()) {
            return null
        }
        val totalMonths = totalMonthsLong.toInt()
        val year = totalMonths.floorDiv(12)
        val month = (totalMonths.mod(12) + 1).toUInt()

        val flags = YearFlags.fromYear(year)
        val febDays = if (flags.ndays() == 366u) 29u else 28u
        val daysInMonth = intArrayOf(31, febDays.toInt(), 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val dayMax = daysInMonth[(month.toInt() - 1)].toUInt()
        var day = day()
        if (day > dayMax) {
            day = dayMax
        }
        return fromYmdOpt(year, month, day)
    }

    /**
     * Add a duration in [Days] to the date.
     */
    fun checkedAddDays(days: Days): NaiveDate? {
        return if (days.num <= Int.MAX_VALUE.toULong()) {
            addDays(days.num.toInt())
        } else {
            null
        }
    }

    /**
     * Subtract a duration in [Days] from the date.
     */
    fun checkedSubDays(days: Days): NaiveDate? {
        return if (days.num <= Int.MAX_VALUE.toULong()) {
            addDays(-days.num.toInt())
        } else {
            null
        }
    }

    internal fun addDays(days: Int): NaiveDate? {
        val currentOrdinal = (yof and ORDINAL_MASK) ushr 4
        val newOrdinal = currentOrdinal + days
        val maxDays = 365 + (if (leapYear()) 1 else 0)
        if (newOrdinal in 1..maxDays) {
            val yearAndFlags = yof and ORDINAL_MASK.inv()
            return fromYof(yearAndFlags or (newOrdinal shl 4))
        }

        val yr = year()
        val (div400, mod400) = divModFloor(yr, 400)
        var yearDiv400 = div400
        val cycle = yoToCycle(mod400.toUInt(), ordinal()).toLong() + days.toLong()
        val cycleDiv400y = cycle.floorDiv(146_097L).toInt()
        val cycleRem = cycle.mod(146_097L).toUInt()
        yearDiv400 += cycleDiv400y

        val (yearMod400, ord) = cycleToYo(cycleRem)
        val flags = YearFlags.fromYearMod400(yearMod400.toInt())
        return fromOrdinalAndFlags(yearDiv400 * 400 + yearMod400.toInt(), ord, flags)
    }

    /** Returns true if this is a leap year. */
    fun leapYear(): Boolean = (yof and LEAP_YEAR_MASK) == 0

    override fun year(): Int = yof shr 13

    override fun month(): UInt = mdf().month()

    override fun month0(): UInt = month() - 1u

    override fun day(): UInt = mdf().day()

    override fun day0(): UInt = mdf().day() - 1u

    override fun ordinal(): UInt = ((yof and ORDINAL_MASK) ushr 4).toUInt()

    override fun ordinal0(): UInt = ordinal() - 1u

    override fun weekday(): Weekday {
        return when ((((yof and ORDINAL_MASK) ushr 4) + (yof and WEEKDAY_FLAGS_MASK)) % 7) {
            0 -> Weekday.Mon
            1 -> Weekday.Tue
            2 -> Weekday.Wed
            3 -> Weekday.Thu
            4 -> Weekday.Fri
            5 -> Weekday.Sat
            else -> Weekday.Sun
        }
    }

    override fun isoWeek(): IsoWeek =
        IsoWeek.fromYof(year(), ordinal(), yearFlags())

    override fun withYear(year: Int): NaiveDate? {
        val mdf = mdf()
        val flags = YearFlags.fromYear(year)
        val newMdf = mdf.withFlags(flags)
        return fromMdf(year, newMdf)
    }

    override fun withMonth(month: UInt): NaiveDate? {
        val newMdf = mdf().withMonth(month) ?: return null
        return withMdf(newMdf)
    }

    override fun withMonth0(month0: UInt): NaiveDate? =
        withMonth(month0 + 1u)

    override fun withDay(day: UInt): NaiveDate? {
        val newMdf = mdf().withDay(day) ?: return null
        return withMdf(newMdf)
    }

    override fun withDay0(day0: UInt): NaiveDate? =
        withDay(day0 + 1u)

    override fun withOrdinal(ordinal: UInt): NaiveDate? {
        if (ordinal == 0u || ordinal > 366u) return null
        val yofVal = (yof and ORDINAL_MASK.inv()) or (ordinal.toInt() shl 4)
        return if ((yofVal and OL_MASK) <= MAX_OL) {
            fromYof(yofVal)
        } else {
            null
        }
    }

    override fun withOrdinal0(ordinal0: UInt): NaiveDate? =
        withOrdinal(ordinal0 + 1u)

    internal fun yearFlags(): YearFlags =
        YearFlags((yof and YEAR_FLAGS_MASK).toUByte())

    /** Counts the days in the proleptic Gregorian calendar, with January 1, Year 1 (CE) as day 1. */
    override fun numDaysFromCe(): Int {
        var yr = year() - 1
        var ndays = 0
        if (yr < 0) {
            val excess = 1 + (-yr) / 400
            yr += excess * 400
            ndays -= excess * 146_097
        }
        val div100 = yr / 100
        ndays += ((yr * 1461) shr 2) - div100 + (div100 shr 2)
        return ndays + ordinal().toInt()
    }

    /** Counts the days in the proleptic Gregorian calendar, with January 1, Year 1970 as day 0. */
    fun toEpochDays(): Int =
        numDaysFromCe() - UNIX_EPOCH_DAY.toInt()

    /** Returns the [NaiveWeek] that the date belongs to, starting with the specified [Weekday]. */
    fun week(start: Weekday): NaiveWeek = NaiveWeek.new(this, start)

    /** Makes a new [NaiveDateTime] from date and time components. */
    fun andTime(time: NaiveTime): NaiveDateTime = NaiveDateTime(this, time)

    /** Makes a new [NaiveDateTime] from the current date, hour, minute and second. */
    fun andHmsOpt(hour: UInt, min: UInt, sec: UInt): NaiveDateTime? =
        NaiveTime.fromHmsOpt(hour, min, sec)?.let { andTime(it) }

    /** Makes a new [NaiveDateTime] from the current date, hour, minute and second. */
    fun andHms(hour: UInt, min: UInt, sec: UInt): NaiveDateTime =
        andHmsOpt(hour, min, sec) ?: throw IllegalArgumentException("invalid time")

    /** Makes a new [NaiveDateTime] from the current date, hour, minute, second and millisecond. */
    fun andHmsMilliOpt(hour: UInt, min: UInt, sec: UInt, milli: UInt): NaiveDateTime? =
        NaiveTime.fromHmsMilliOpt(hour, min, sec, milli)?.let { andTime(it) }

    /** Makes a new [NaiveDateTime] from the current date, hour, minute, second and microsecond. */
    fun andHmsMicroOpt(hour: UInt, min: UInt, sec: UInt, micro: UInt): NaiveDateTime? =
        NaiveTime.fromHmsMicroOpt(hour, min, sec, micro)?.let { andTime(it) }

    /** Makes a new [NaiveDateTime] from the current date, hour, minute, second and nanosecond. */
    fun andHmsNanoOpt(hour: UInt, min: UInt, sec: UInt, nano: UInt): NaiveDateTime? =
        NaiveTime.fromHmsNanoOpt(hour, min, sec, nano)?.let { andTime(it) }

    operator fun plus(rhs: TimeDelta): NaiveDate =
        checkedAddSigned(rhs) ?: throw ArithmeticException("NaiveDate + TimeDelta overflowed")

    operator fun minus(rhs: TimeDelta): NaiveDate =
        checkedSubSigned(rhs) ?: throw ArithmeticException("NaiveDate - TimeDelta overflowed")

    operator fun minus(rhs: NaiveDate): TimeDelta = signedDurationSince(rhs)

    operator fun plus(months: Months): NaiveDate =
        checkedAddMonths(months) ?: throw ArithmeticException("NaiveDate + Months out of range")

    operator fun minus(months: Months): NaiveDate =
        checkedSubMonths(months) ?: throw ArithmeticException("NaiveDate - Months out of range")

    operator fun plus(days: Days): NaiveDate =
        checkedAddDays(days) ?: throw ArithmeticException("NaiveDate + Days out of range")

    operator fun minus(days: Days): NaiveDate =
        checkedSubDays(days) ?: throw ArithmeticException("NaiveDate - Days out of range")

    override fun compareTo(other: NaiveDate): Int = yof.compareTo(other.yof)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NaiveDate) return false
        return yof == other.yof
    }

    override fun hashCode(): Int = yof.hashCode()

    override fun toString(): String {
        val yr = year()
        val m = month().toInt()
        val d = day().toInt()
        val yrStr = if (yr in 0..9999) {
            yr.toString().padStart(4, '0')
        } else if (yr < 0) {
            "-" + (-yr).toString().padStart(4, '0')
        } else {
            "+" + yr.toString().padStart(4, '0')
        }
        val mStr = m.toString().padStart(2, '0')
        val dStr = d.toString().padStart(2, '0')
        return "$yrStr-$mStr-$dStr"
    }

    companion object {
        /** Create a new [NaiveDate] from a raw year-ordinal-flags [Int]. */
        internal fun fromYof(yof: Int): NaiveDate = NaiveDate(yof)

        /** The minimum possible [NaiveDate] (January 1, 262145 BCE). */
        val MIN: NaiveDate = fromYof((MIN_YEAR shl 13) or (1 shl 4) or 10)

        /** The maximum possible [NaiveDate] (December 31, 262143 CE). */
        val MAX: NaiveDate = fromYof((MAX_YEAR shl 13) or (365 shl 4) or 14)

        /** Makes a new [NaiveDate] from year, ordinal and flags. */
        internal fun fromOrdinalAndFlags(
            year: Int,
            ordinal: UInt,
            flags: YearFlags,
        ): NaiveDate? {
            if (year < MIN_YEAR || year > MAX_YEAR) return null
            if (ordinal == 0u || ordinal > 366u) return null
            val yofVal = (year shl 13) or (ordinal.toInt() shl 4) or flags.value.toInt()
            return if ((yofVal and OL_MASK) <= MAX_OL) {
                fromYof(yofVal)
            } else {
                null
            }
        }

        /** Makes a new [NaiveDate] from year and packed month-day-flags. */
        internal fun fromMdf(year: Int, mdf: Mdf): NaiveDate? {
            if (year < MIN_YEAR || year > MAX_YEAR) return null
            val ordAndFlags = mdf.ordinalAndFlags() ?: return null
            return fromYof((year shl 13) or ordAndFlags)
        }

        /** Makes a new [NaiveDate] from the calendar date (year, month and day). */
        fun fromYmdOpt(year: Int, month: UInt, day: UInt): NaiveDate? {
            val flags = YearFlags.fromYear(year)
            val mdf = Mdf.new(month, day, flags) ?: return null
            return fromMdf(year, mdf)
        }

        /** Makes a new [NaiveDate] from the calendar date (year, month and day). */
        fun fromYmd(year: Int, month: UInt, day: UInt): NaiveDate =
            fromYmdOpt(year, month, day) ?: throw IllegalArgumentException("invalid or out-of-range date")

        /** Makes a new [NaiveDate] from the ordinal date (year and day of the year). */
        fun fromYoOpt(year: Int, ordinal: UInt): NaiveDate? {
            val flags = YearFlags.fromYear(year)
            return fromOrdinalAndFlags(year, ordinal, flags)
        }

        /** Makes a new [NaiveDate] from the ordinal date (year and day of the year). */
        fun fromYo(year: Int, ordinal: UInt): NaiveDate =
            fromYoOpt(year, ordinal) ?: throw IllegalArgumentException("invalid or out-of-range date")

        /** Makes a new [NaiveDate] from the ISO week date. */
        fun fromIsoywdOpt(year: Int, week: UInt, weekday: Weekday): NaiveDate? {
            val flags = YearFlags.fromYear(year)
            val nweeks = flags.nisoweeks()
            if (week == 0u || week > nweeks) return null
            val weekord = week * 7u + weekday.numDaysFromMonday().toUInt()
            val delta = flags.isoweekDelta()
            val (targetYear, ordinal, targetFlags) = if (weekord <= delta) {
                val prevFlags = YearFlags.fromYear(year - 1)
                Triple(year - 1, weekord + prevFlags.ndays() - delta, prevFlags)
            } else {
                val ord = weekord - delta
                val ndays = flags.ndays()
                if (ord <= ndays) {
                    Triple(year, ord, flags)
                } else {
                    val nextFlags = YearFlags.fromYear(year + 1)
                    Triple(year + 1, ord - ndays, nextFlags)
                }
            }
            return fromOrdinalAndFlags(targetYear, ordinal, targetFlags)
        }

        /** Makes a new [NaiveDate] from the ISO week date. */
        fun fromIsoywd(year: Int, week: UInt, weekday: Weekday): NaiveDate =
            fromIsoywdOpt(year, week, weekday) ?: throw IllegalArgumentException("invalid or out-of-range date")

        /** Makes a new [NaiveDate] from a day's number in the proleptic Gregorian calendar, with January 1, 1 being day 1. */
        fun fromNumDaysFromCeOpt(days: Int): NaiveDate? {
            val adjustedDays = days.toLong() + 365L // make December 31, 1 BCE equal to day 0
            val yearDiv400 = adjustedDays.floorDiv(146_097L).toInt()
            val cycle = adjustedDays.mod(146_097L).toUInt()
            val (yearMod400, ordinal) = cycleToYo(cycle)
            val flags = YearFlags.fromYearMod400(yearMod400.toInt())
            return fromOrdinalAndFlags(yearDiv400 * 400 + yearMod400.toInt(), ordinal, flags)
        }

        /** Makes a new [NaiveDate] from a day's number in the proleptic Gregorian calendar, with January 1, 1 being day 1. */
        fun fromNumDaysFromCe(days: Int): NaiveDate =
            fromNumDaysFromCeOpt(days) ?: throw IllegalArgumentException("out-of-range date")

        /** Makes a new [NaiveDate] from a day's number in the proleptic Gregorian calendar, with January 1, 1970 being day 0. */
        fun fromEpochDays(days: Int): NaiveDate? {
            val ceDays = days.toLong() + UNIX_EPOCH_DAY
            if (ceDays < Int.MIN_VALUE.toLong() || ceDays > Int.MAX_VALUE.toLong()) return null
            return fromNumDaysFromCeOpt(ceDays.toInt())
        }

        /** Makes a new [NaiveDate] by counting occurrences of a day-of-week in the month. */
        fun fromWeekdayOfMonthOpt(year: Int, month: UInt, weekday: Weekday, n: UByte): NaiveDate? {
            if (n == 0.toUByte()) return null
            val first = fromYmdOpt(year, month, 1u)?.weekday() ?: return null
            val firstToDow = (7 + weekday.numberFromMonday() - first.numberFromMonday()) % 7
            val day = (n.toInt() - 1) * 7 + firstToDow + 1
            return fromYmdOpt(year, month, day.toUInt())
        }

        /** Makes a new [NaiveDate] by counting occurrences of a day-of-week in the month. */
        fun fromWeekdayOfMonth(year: Int, month: UInt, weekday: Weekday, n: UByte): NaiveDate =
            fromWeekdayOfMonthOpt(year, month, weekday, n) ?: throw IllegalArgumentException("out-of-range date")
    }
}

private fun cycleToYo(cycle: UInt): Pair<UInt, UInt> {
    var yearMod400 = cycle / 365u
    var ordinal0 = cycle % 365u
    val delta = YEAR_DELTAS[yearMod400.toInt()].toUInt()
    if (ordinal0 < delta) {
        yearMod400 -= 1u
        ordinal0 += 365u - YEAR_DELTAS[yearMod400.toInt()].toUInt()
    } else {
        ordinal0 -= delta
    }
    return yearMod400 to (ordinal0 + 1u)
}

private fun yoToCycle(yearMod400: UInt, ordinal: UInt): UInt =
    yearMod400 * 365u + YEAR_DELTAS[yearMod400.toInt()].toUInt() + ordinal - 1u

private fun divModFloor(v: Int, div: Int): Pair<Int, Int> =
    v.floorDiv(div) to v.mod(div)
