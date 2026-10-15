// port-lint: source offset/local/tz_info/rule.rs
package io.github.kotlinmania.chrono.offset.local.tzinfo

import io.github.kotlinmania.chrono.naive.NaiveDateTime
import io.github.kotlinmania.chrono.offset.MappedLocalTime
import kotlin.math.abs
import kotlin.math.min

/**
 * Transition rule
 */
internal sealed class TransitionRule {
    /** Fixed local time type */
    data class Fixed(
        val localTimeType: LocalTimeType,
    ) : TransitionRule()

    /** Alternate local time types */
    data class Alternate(
        val alternateTime: AlternateTime,
    ) : TransitionRule()

    /**
     * Find the local time type associated to the transition rule at the specified Unix time in seconds.
     */
    fun findLocalTimeType(unixTime: Long): TzResult<LocalTimeType> =
        when (this) {
            is Fixed -> TzResult.Ok(localTimeType)
            is Alternate -> alternateTime.findLocalTimeType(unixTime)
        }

    /**
     * Find the local time type associated to the transition rule at the specified local time.
     */
    fun findLocalTimeTypeFromLocal(localTime: NaiveDateTime): TzResult<MappedLocalTime<LocalTimeType>> =
        when (this) {
            is Fixed -> TzResult.Ok(MappedLocalTime.Single(localTimeType))
            is Alternate -> alternateTime.findLocalTimeTypeFromLocal(localTime)
        }

    companion object {
        /**
         * Parse a POSIX TZ string containing a time zone description.
         */
        fun fromTzString(tzString: ByteArray, useStringExtensions: Boolean): TzResult<TransitionRule> {
            val cursor = Cursor.new(tzString)

            val stdTimeZone =
                when (val res = parseName(cursor)) {
                    is TzResult.Ok -> res.value
                    is TzResult.Err -> return res
                }
            val stdOffset =
                when (val res = parseOffset(cursor)) {
                    is TzResult.Ok -> res.value
                    is TzResult.Err -> return res
                }

            if (cursor.isEmpty()) {
                return LocalTimeType.new(-stdOffset, false, stdTimeZone).map { Fixed(it) }
            }

            val dstTimeZone =
                when (val res = parseName(cursor)) {
                    is TzResult.Ok -> res.value
                    is TzResult.Err -> return res
                }

            val dstOffset =
                when (cursor.peek()) {
                    ','.code.toByte() -> {
                        stdOffset - 3600
                    }

                    null -> {
                        return TzResult.Err(Error.UnsupportedTzString("DST start and end rules must be provided"))
                    }

                    else -> {
                        when (val res = parseOffset(cursor)) {
                            is TzResult.Ok -> res.value
                            is TzResult.Err -> return res
                        }
                    }
                }

            if (cursor.isEmpty()) {
                return TzResult.Err(Error.UnsupportedTzString("DST start and end rules must be provided"))
            }

            when (val res = cursor.readTag(byteArrayOf(','.code.toByte()))) {
                is TzResult.Ok -> Unit
                is TzResult.Err -> return res
            }
            val (dstStart, dstStartTime) =
                when (val res = RuleDay.parse(cursor, useStringExtensions)) {
                    is TzResult.Ok -> res.value
                    is TzResult.Err -> return res
                }

            when (val res = cursor.readTag(byteArrayOf(','.code.toByte()))) {
                is TzResult.Ok -> Unit
                is TzResult.Err -> return res
            }
            val (dstEnd, dstEndTime) =
                when (val res = RuleDay.parse(cursor, useStringExtensions)) {
                    is TzResult.Ok -> res.value
                    is TzResult.Err -> return res
                }

            if (!cursor.isEmpty()) {
                return TzResult.Err(Error.InvalidTzString("remaining data after parsing TZ string"))
            }

            val stdType =
                when (val res = LocalTimeType.new(-stdOffset, false, stdTimeZone)) {
                    is TzResult.Ok -> res.value
                    is TzResult.Err -> return res
                }
            val dstType =
                when (val res = LocalTimeType.new(-dstOffset, true, dstTimeZone)) {
                    is TzResult.Ok -> res.value
                    is TzResult.Err -> return res
                }

            return AlternateTime
                .new(
                    std = stdType,
                    dst = dstType,
                    dstStart = dstStart,
                    dstStartTime = dstStartTime,
                    dstEnd = dstEnd,
                    dstEndTime = dstEndTime,
                ).map { Alternate(it) }
        }
    }
}

/**
 * Transition rule representing alternate local time types
 */
internal data class AlternateTime(
    val std: LocalTimeType,
    val dst: LocalTimeType,
    val dstStart: RuleDay,
    val dstStartTime: Int,
    val dstEnd: RuleDay,
    val dstEndTime: Int,
) {
    /**
     * Find the local time type associated to the alternate transition rule at the specified Unix time in seconds.
     */
    fun findLocalTimeType(unixTime: Long): TzResult<LocalTimeType> {
        val dstStartTimeInUtc = dstStartTime.toLong() - std.utOffset.toLong()
        val dstEndTimeInUtc = dstEndTime.toLong() - dst.utOffset.toLong()

        val currentYear =
            when (val res = UtcDateTime.fromTimespec(unixTime)) {
                is TzResult.Ok -> res.value.year
                is TzResult.Err -> return res
            }

        if (currentYear !in (Int.MIN_VALUE + 2)..(Int.MAX_VALUE - 2)) {
            return TzResult.Err(Error.OutOfRange("out of range date time"))
        }

        val currentYearDstStartUnixTime = dstStart.unixTime(currentYear, dstStartTimeInUtc)
        val currentYearDstEndUnixTime = dstEnd.unixTime(currentYear, dstEndTimeInUtc)

        val isDst =
            if (currentYearDstStartUnixTime <= currentYearDstEndUnixTime) {
                if (unixTime < currentYearDstStartUnixTime) {
                    val previousYearDstEndUnixTime = dstEnd.unixTime(currentYear - 1, dstEndTimeInUtc)
                    if (unixTime < previousYearDstEndUnixTime) {
                        val previousYearDstStartUnixTime = dstStart.unixTime(currentYear - 1, dstStartTimeInUtc)
                        previousYearDstStartUnixTime <= unixTime
                    } else {
                        false
                    }
                } else if (unixTime < currentYearDstEndUnixTime) {
                    true
                } else {
                    val nextYearDstStartUnixTime = dstStart.unixTime(currentYear + 1, dstStartTimeInUtc)
                    if (nextYearDstStartUnixTime <= unixTime) {
                        val nextYearDstEndUnixTime = dstEnd.unixTime(currentYear + 1, dstEndTimeInUtc)
                        unixTime < nextYearDstEndUnixTime
                    } else {
                        false
                    }
                }
            } else {
                if (unixTime < currentYearDstEndUnixTime) {
                    val previousYearDstStartUnixTime = dstStart.unixTime(currentYear - 1, dstStartTimeInUtc)
                    if (unixTime < previousYearDstStartUnixTime) {
                        val previousYearDstEndUnixTime = dstEnd.unixTime(currentYear - 1, dstEndTimeInUtc)
                        unixTime < previousYearDstEndUnixTime
                    } else {
                        true
                    }
                } else if (unixTime < currentYearDstStartUnixTime) {
                    false
                } else {
                    val nextYearDstEndUnixTime = dstEnd.unixTime(currentYear + 1, dstEndTimeInUtc)
                    if (nextYearDstEndUnixTime <= unixTime) {
                        val nextYearDstStartUnixTime = dstStart.unixTime(currentYear + 1, dstStartTimeInUtc)
                        nextYearDstStartUnixTime <= unixTime
                    } else {
                        true
                    }
                }
            }

        return TzResult.Ok(if (isDst) dst else std)
    }

    fun findLocalTimeTypeFromLocal(localTime: NaiveDateTime): TzResult<MappedLocalTime<LocalTimeType>> {
        val currentYear = localTime.year()
        val localTimestamp = localTime.andUtc().timestamp()

        val dstStartTransitionStart = dstStart.unixTime(currentYear, 0L) + dstStartTime.toLong()
        val dstStartTransitionEnd =
            dstStart.unixTime(currentYear, 0L) +
                dstStartTime.toLong() +
                dst.utOffset.toLong() -
                std.utOffset.toLong()

        val dstEndTransitionStart = dstEnd.unixTime(currentYear, 0L) + dstEndTime.toLong()
        val dstEndTransitionEnd =
            dstEnd.unixTime(currentYear, 0L) +
                dstEndTime.toLong() +
                std.utOffset.toLong() -
                dst.utOffset.toLong()

        return when {
            std.utOffset == dst.utOffset -> {
                TzResult.Ok(MappedLocalTime.Single(std))
            }

            std.utOffset < dst.utOffset -> {
                if (dstStart.transitionDate(currentYear).first < dstEnd.transitionDate(currentYear).first) {
                    if (localTimestamp <= dstStartTransitionStart) {
                        TzResult.Ok(MappedLocalTime.Single(std))
                    } else if (localTimestamp > dstStartTransitionStart && localTimestamp < dstStartTransitionEnd) {
                        TzResult.Ok(MappedLocalTime.None)
                    } else if (localTimestamp >= dstStartTransitionEnd && localTimestamp < dstEndTransitionEnd) {
                        TzResult.Ok(MappedLocalTime.Single(dst))
                    } else if (localTimestamp >= dstEndTransitionEnd && localTimestamp <= dstEndTransitionStart) {
                        TzResult.Ok(MappedLocalTime.Ambiguous(std, dst))
                    } else {
                        TzResult.Ok(MappedLocalTime.Single(std))
                    }
                } else {
                    if (localTimestamp < dstEndTransitionEnd) {
                        TzResult.Ok(MappedLocalTime.Single(dst))
                    } else if (localTimestamp >= dstEndTransitionEnd && localTimestamp <= dstEndTransitionStart) {
                        TzResult.Ok(MappedLocalTime.Ambiguous(std, dst))
                    } else if (localTimestamp > dstEndTransitionEnd && localTimestamp < dstStartTransitionStart) {
                        TzResult.Ok(MappedLocalTime.Single(std))
                    } else if (localTimestamp >= dstStartTransitionStart && localTimestamp < dstStartTransitionEnd) {
                        TzResult.Ok(MappedLocalTime.None)
                    } else {
                        TzResult.Ok(MappedLocalTime.Single(dst))
                    }
                }
            }

            else -> {
                if (dstStart.transitionDate(currentYear).first < dstEnd.transitionDate(currentYear).first) {
                    if (localTimestamp < dstStartTransitionEnd) {
                        TzResult.Ok(MappedLocalTime.Single(std))
                    } else if (localTimestamp >= dstStartTransitionEnd && localTimestamp <= dstStartTransitionStart) {
                        TzResult.Ok(MappedLocalTime.Ambiguous(dst, std))
                    } else if (localTimestamp > dstStartTransitionStart && localTimestamp < dstEndTransitionStart) {
                        TzResult.Ok(MappedLocalTime.Single(dst))
                    } else if (localTimestamp >= dstEndTransitionStart && localTimestamp < dstEndTransitionEnd) {
                        TzResult.Ok(MappedLocalTime.None)
                    } else {
                        TzResult.Ok(MappedLocalTime.Single(std))
                    }
                } else {
                    if (localTimestamp <= dstEndTransitionStart) {
                        TzResult.Ok(MappedLocalTime.Single(dst))
                    } else if (localTimestamp > dstEndTransitionStart && localTimestamp < dstEndTransitionEnd) {
                        TzResult.Ok(MappedLocalTime.None)
                    } else if (localTimestamp >= dstEndTransitionEnd && localTimestamp < dstStartTransitionEnd) {
                        TzResult.Ok(MappedLocalTime.Single(std))
                    } else if (localTimestamp >= dstStartTransitionEnd && localTimestamp <= dstStartTransitionStart) {
                        TzResult.Ok(MappedLocalTime.Ambiguous(dst, std))
                    } else {
                        TzResult.Ok(MappedLocalTime.Single(dst))
                    }
                }
            }
        }
    }

    companion object {
        fun new(
            std: LocalTimeType,
            dst: LocalTimeType,
            dstStart: RuleDay,
            dstStartTime: Int,
            dstEnd: RuleDay,
            dstEndTime: Int,
        ): TzResult<AlternateTime> {
            if (!(abs(dstStartTime.toLong()) < SECONDS_PER_WEEK && abs(dstEndTime.toLong()) < SECONDS_PER_WEEK)) {
                return TzResult.Err(Error.TransitionRule("invalid DST start or end time"))
            }
            return TzResult.Ok(AlternateTime(std, dst, dstStart, dstStartTime, dstEnd, dstEndTime))
        }
    }
}

private fun parseName(cursor: Cursor): TzResult<ByteArray> {
    if (cursor.peek() != '<'.code.toByte()) {
        return cursor.readWhile { b ->
            val c = b.toInt() and 0xFF
            (c in 'A'.code..'Z'.code) || (c in 'a'.code..'z'.code)
        }
    }

    when (val res = cursor.readExact(1)) {
        is TzResult.Ok -> Unit
        is TzResult.Err -> return res
    }
    val unquoted =
        when (val res = cursor.readUntil { b -> b == '>'.code.toByte() }) {
            is TzResult.Ok -> res.value
            is TzResult.Err -> return res
        }
    when (val res = cursor.readExact(1)) {
        is TzResult.Ok -> Unit
        is TzResult.Err -> return res
    }
    return TzResult.Ok(unquoted)
}

internal fun parseOffset(cursor: Cursor): TzResult<Int> {
    val parsed =
        when (val res = parseSignedHhmmss(cursor)) {
            is TzResult.Ok -> res.value
            is TzResult.Err -> return res
        }

    if (parsed.hour !in 0..24) {
        return TzResult.Err(Error.InvalidTzString("invalid offset hour"))
    }
    if (parsed.minute !in 0..59) {
        return TzResult.Err(Error.InvalidTzString("invalid offset minute"))
    }
    if (parsed.second !in 0..59) {
        return TzResult.Err(Error.InvalidTzString("invalid offset second"))
    }

    return TzResult.Ok(parsed.sign * (parsed.hour * 3600 + parsed.minute * 60 + parsed.second))
}

internal fun parseRuleTime(cursor: Cursor): TzResult<Int> {
    val (hour, minute, second) =
        when (val res = parseHhmmss(cursor)) {
            is TzResult.Ok -> res.value
            is TzResult.Err -> return res
        }

    if (hour !in 0..24) {
        return TzResult.Err(Error.InvalidTzString("invalid day time hour"))
    }
    if (minute !in 0..59) {
        return TzResult.Err(Error.InvalidTzString("invalid day time minute"))
    }
    if (second !in 0..59) {
        return TzResult.Err(Error.InvalidTzString("invalid day time second"))
    }

    return TzResult.Ok(hour * 3600 + minute * 60 + second)
}

internal fun parseRuleTimeExtended(cursor: Cursor): TzResult<Int> {
    val parsed =
        when (val res = parseSignedHhmmss(cursor)) {
            is TzResult.Ok -> res.value
            is TzResult.Err -> return res
        }

    if (parsed.hour !in -167..167) {
        return TzResult.Err(Error.InvalidTzString("invalid day time hour"))
    }
    if (parsed.minute !in 0..59) {
        return TzResult.Err(Error.InvalidTzString("invalid day time minute"))
    }
    if (parsed.second !in 0..59) {
        return TzResult.Err(Error.InvalidTzString("invalid day time second"))
    }

    return TzResult.Ok(parsed.sign * (parsed.hour * 3600 + parsed.minute * 60 + parsed.second))
}

private fun parseHhmmss(cursor: Cursor): TzResult<Triple<Int, Int, Int>> {
    val hour =
        when (val res = cursor.readInt()) {
            is TzResult.Ok -> res.value
            is TzResult.Err -> return res
        }

    var minute = 0
    var second = 0

    val hasColon1 =
        when (val res = cursor.readOptionalTag(byteArrayOf(':'.code.toByte()))) {
            is TzResult.Ok -> res.value
            is TzResult.Err -> return res
        }
    if (hasColon1) {
        minute =
            when (val res = cursor.readInt()) {
                is TzResult.Ok -> res.value
                is TzResult.Err -> return res
            }

        val hasColon2 =
            when (val res = cursor.readOptionalTag(byteArrayOf(':'.code.toByte()))) {
                is TzResult.Ok -> res.value
                is TzResult.Err -> return res
            }
        if (hasColon2) {
            second =
                when (val res = cursor.readInt()) {
                    is TzResult.Ok -> res.value
                    is TzResult.Err -> return res
                }
        }
    }

    return TzResult.Ok(Triple(hour, minute, second))
}

private data class SignedHhmmss(
    val sign: Int,
    val hour: Int,
    val minute: Int,
    val second: Int,
)

private fun parseSignedHhmmss(cursor: Cursor): TzResult<SignedHhmmss> {
    var sign = 1
    val c = cursor.peek()
    if (c != null && (c == '+'.code.toByte() || c == '-'.code.toByte())) {
        when (val res = cursor.readExact(1)) {
            is TzResult.Ok -> Unit
            is TzResult.Err -> return res
        }
        if (c == '-'.code.toByte()) {
            sign = -1
        }
    }

    val (hour, minute, second) =
        when (val res = parseHhmmss(cursor)) {
            is TzResult.Ok -> res.value
            is TzResult.Err -> return res
        }
    return TzResult.Ok(SignedHhmmss(sign, hour, minute, second))
}

/**
 * Transition rule day
 */
internal sealed class RuleDay {
    /** Julian day in `[1, 365]`, without taking occasional Feb 29 into account */
    data class Julian1WithoutLeap(
        val julianDay1: UShort,
    ) : RuleDay()

    /** Zero-based Julian day in `[0, 365]`, taking occasional Feb 29 into account */
    data class Julian0WithLeap(
        val julianDay0: UShort,
    ) : RuleDay()

    /** Day represented by a month, a month week and a week day */
    data class MonthWeekday(
        val month: UByte,
        val week: UByte,
        val weekDay: UByte,
    ) : RuleDay()

    fun transitionDate(year: Int): Pair<Int, Long> =
        when (this) {
            is Julian1WithoutLeap -> {
                val yearDay = julianDay1.toLong()
                val idx = CUMUL_DAY_IN_MONTHS_NORMAL_YEAR.binarySearch(yearDay - 1L)
                val month = if (idx >= 0) idx + 1 else -(idx + 1)
                val monthDay = yearDay - CUMUL_DAY_IN_MONTHS_NORMAL_YEAR[month - 1]
                month to monthDay
            }

            is Julian0WithLeap -> {
                val leap = if (isLeapYear(year)) 1L else 0L
                val cumulDayInMonths =
                    listOf(
                        0L,
                        31L,
                        59L + leap,
                        90L + leap,
                        120L + leap,
                        151L + leap,
                        181L + leap,
                        212L + leap,
                        243L + leap,
                        273L + leap,
                        304L + leap,
                        334L + leap,
                    )
                val yearDay = julianDay0.toLong()
                val idx = cumulDayInMonths.binarySearch(yearDay)
                val month = if (idx >= 0) idx + 1 else -(idx + 1)
                val monthDay = 1L + yearDay - cumulDayInMonths[month - 1]
                month to monthDay
            }

            is MonthWeekday -> {
                val leap = if (isLeapYear(year)) 1L else 0L
                val m = month.toInt()
                var dayInMonth = DAY_IN_MONTHS_NORMAL_YEAR[m - 1]
                if (m == 2) {
                    dayInMonth += leap
                }

                val weekDayOfFirstMonthDay = (4L + daysSinceUnixEpoch(year, m, 1L)).mod(DAYS_PER_WEEK)
                val firstWeekDayOccurrenceInMonth =
                    1L + (weekDay.toLong() - weekDayOfFirstMonthDay).mod(DAYS_PER_WEEK)

                var monthDay = firstWeekDayOccurrenceInMonth + (week.toLong() - 1L) * DAYS_PER_WEEK
                if (monthDay > dayInMonth) {
                    monthDay -= DAYS_PER_WEEK
                }
                m to monthDay
            }
        }

    fun unixTime(year: Int, dayTimeInUtc: Long): Long {
        val (month, monthDay) = transitionDate(year)
        return daysSinceUnixEpoch(year, month, monthDay) * SECONDS_PER_DAY + dayTimeInUtc
    }

    companion object {
        fun parse(cursor: Cursor, useStringExtensions: Boolean): TzResult<Pair<RuleDay, Int>> {
            val date =
                when (cursor.peek()) {
                    'M'.code.toByte() -> {
                        when (val res = cursor.readExact(1)) {
                            is TzResult.Ok -> Unit
                            is TzResult.Err -> return res
                        }
                        val month =
                            when (val res = cursor.readU8()) {
                                is TzResult.Ok -> res.value
                                is TzResult.Err -> return res
                            }
                        when (val res = cursor.readTag(byteArrayOf('.'.code.toByte()))) {
                            is TzResult.Ok -> Unit
                            is TzResult.Err -> return res
                        }
                        val week =
                            when (val res = cursor.readU8()) {
                                is TzResult.Ok -> res.value
                                is TzResult.Err -> return res
                            }
                        when (val res = cursor.readTag(byteArrayOf('.'.code.toByte()))) {
                            is TzResult.Ok -> Unit
                            is TzResult.Err -> return res
                        }
                        val weekDay =
                            when (val res = cursor.readU8()) {
                                is TzResult.Ok -> res.value
                                is TzResult.Err -> return res
                            }
                        when (val res = monthWeekday(month, week, weekDay)) {
                            is TzResult.Ok -> res.value
                            is TzResult.Err -> return res
                        }
                    }

                    'J'.code.toByte() -> {
                        when (val res = cursor.readExact(1)) {
                            is TzResult.Ok -> Unit
                            is TzResult.Err -> return res
                        }
                        val day1 =
                            when (val res = cursor.readU16()) {
                                is TzResult.Ok -> res.value
                                is TzResult.Err -> return res
                            }
                        when (val res = julian1(day1)) {
                            is TzResult.Ok -> res.value
                            is TzResult.Err -> return res
                        }
                    }

                    else -> {
                        val day0 =
                            when (val res = cursor.readU16()) {
                                is TzResult.Ok -> res.value
                                is TzResult.Err -> return res
                            }
                        when (val res = julian0(day0)) {
                            is TzResult.Ok -> res.value
                            is TzResult.Err -> return res
                        }
                    }
                }

            val hasSlash =
                when (val res = cursor.readOptionalTag(byteArrayOf('/'.code.toByte()))) {
                    is TzResult.Ok -> res.value
                    is TzResult.Err -> return res
                }
            val time =
                if (!hasSlash) {
                    2 * 3600
                } else if (useStringExtensions) {
                    when (val res = parseRuleTimeExtended(cursor)) {
                        is TzResult.Ok -> res.value
                        is TzResult.Err -> return res
                    }
                } else {
                    when (val res = parseRuleTime(cursor)) {
                        is TzResult.Ok -> res.value
                        is TzResult.Err -> return res
                    }
                }
            return TzResult.Ok(date to time)
        }

        fun julian1(julianDay1: UShort): TzResult<RuleDay> {
            if (julianDay1 < 1u || julianDay1 > 365u) {
                return TzResult.Err(Error.TransitionRule("invalid rule day julian day"))
            }
            return TzResult.Ok(Julian1WithoutLeap(julianDay1))
        }

        fun julian0(julianDay0: UShort): TzResult<RuleDay> {
            if (julianDay0 > 365u) {
                return TzResult.Err(Error.TransitionRule("invalid rule day julian day"))
            }
            return TzResult.Ok(Julian0WithLeap(julianDay0))
        }

        fun monthWeekday(month: UByte, week: UByte, weekDay: UByte): TzResult<RuleDay> {
            if (month < 1u || month > 12u) {
                return TzResult.Err(Error.TransitionRule("invalid rule day month"))
            }
            if (week < 1u || week > 5u) {
                return TzResult.Err(Error.TransitionRule("invalid rule day week"))
            }
            if (weekDay > 6u) {
                return TzResult.Err(Error.TransitionRule("invalid rule day week day"))
            }
            return TzResult.Ok(MonthWeekday(month, week, weekDay))
        }
    }
}

/**
 * UTC date time expressed in the proleptic gregorian calendar
 */
internal data class UtcDateTime(
    val year: Int,
    val month: UByte,
    val monthDay: UByte,
    val hour: UByte,
    val minute: UByte,
    val second: UByte,
) : Comparable<UtcDateTime> {
    override fun compareTo(other: UtcDateTime): Int {
        var cmp = year.compareTo(other.year)
        if (cmp != 0) return cmp
        cmp = month.compareTo(other.month)
        if (cmp != 0) return cmp
        cmp = monthDay.compareTo(other.monthDay)
        if (cmp != 0) return cmp
        cmp = hour.compareTo(other.hour)
        if (cmp != 0) return cmp
        cmp = minute.compareTo(other.minute)
        if (cmp != 0) return cmp
        return second.compareTo(other.second)
    }

    companion object {
        fun fromTimespec(unixTime: Long): TzResult<UtcDateTime> {
            val seconds = unixTime - UNIX_OFFSET_SECS
            if ((unixTime xor UNIX_OFFSET_SECS) < 0L && (unixTime xor seconds) < 0L) {
                return TzResult.Err(Error.OutOfRange("out of range operation"))
            }

            var remainingDays = seconds / SECONDS_PER_DAY
            var remainingSeconds = seconds % SECONDS_PER_DAY
            if (remainingSeconds < 0L) {
                remainingSeconds += SECONDS_PER_DAY
                remainingDays -= 1L
            }

            var cycles400Years = remainingDays / DAYS_PER_400_YEARS
            remainingDays %= DAYS_PER_400_YEARS
            if (remainingDays < 0L) {
                remainingDays += DAYS_PER_400_YEARS
                cycles400Years -= 1L
            }

            val cycles100Years = min(remainingDays / DAYS_PER_100_YEARS, 3L)
            remainingDays -= cycles100Years * DAYS_PER_100_YEARS

            val cycles4Years = min(remainingDays / DAYS_PER_4_YEARS, 24L)
            remainingDays -= cycles4Years * DAYS_PER_4_YEARS

            val remainingYears = min(remainingDays / DAYS_PER_NORMAL_YEAR, 3L)
            remainingDays -= remainingYears * DAYS_PER_NORMAL_YEAR

            var year =
                OFFSET_YEAR +
                    remainingYears +
                    cycles4Years * 4L +
                    cycles100Years * 100L +
                    cycles400Years * 400L

            var month = 0
            while (month < DAY_IN_MONTHS_LEAP_YEAR_FROM_MARCH.size) {
                val days = DAY_IN_MONTHS_LEAP_YEAR_FROM_MARCH[month]
                if (remainingDays < days) {
                    break
                }
                remainingDays -= days
                month += 1
            }
            month += 2

            if (month >= MONTHS_PER_YEAR.toInt()) {
                month -= MONTHS_PER_YEAR.toInt()
                year += 1L
            }
            month += 1

            val monthDay = 1L + remainingDays

            val hour = remainingSeconds / SECONDS_PER_HOUR
            val minute = (remainingSeconds / SECONDS_PER_MINUTE) % MINUTES_PER_HOUR
            val second = remainingSeconds % SECONDS_PER_MINUTE

            if (year < Int.MIN_VALUE.toLong() || year > Int.MAX_VALUE.toLong()) {
                return TzResult.Err(Error.OutOfRange("i64 is out of range for i32"))
            }

            return TzResult.Ok(
                UtcDateTime(
                    year = year.toInt(),
                    month = month.toUByte(),
                    monthDay = monthDay.toUByte(),
                    hour = hour.toUByte(),
                    minute = minute.toUByte(),
                    second = second.toUByte(),
                ),
            )
        }
    }
}

internal const val NANOSECONDS_PER_SECOND: UInt = 1_000_000_000u
private const val SECONDS_PER_MINUTE: Long = 60L
private const val MINUTES_PER_HOUR: Long = 60L
private const val MONTHS_PER_YEAR: Long = 12L
private const val DAYS_PER_NORMAL_YEAR: Long = 365L
private const val DAYS_PER_4_YEARS: Long = DAYS_PER_NORMAL_YEAR * 4L + 1L
private const val DAYS_PER_100_YEARS: Long = DAYS_PER_NORMAL_YEAR * 100L + 24L
private const val DAYS_PER_400_YEARS: Long = DAYS_PER_NORMAL_YEAR * 400L + 97L
private const val UNIX_OFFSET_SECS: Long = 951868800L
private const val OFFSET_YEAR: Long = 2000L

private val DAY_IN_MONTHS_LEAP_YEAR_FROM_MARCH: LongArray =
    longArrayOf(31, 30, 31, 30, 31, 31, 30, 31, 30, 31, 31, 29)

/**
 * Compute the number of days since Unix epoch (`1970-01-01T00:00:00Z`).
 */
internal fun daysSinceUnixEpoch(year: Int, month: Int, monthDay: Long): Long {
    val isLeap = isLeapYear(year)
    val yr = year.toLong()

    var result = (yr - 1970L) * 365L

    if (yr >= 1970L) {
        result += (yr - 1968L) / 4L
        result -= (yr - 1900L) / 100L
        result += (yr - 1600L) / 400L

        if (isLeap && month < 3) {
            result -= 1L
        }
    } else {
        result += (yr - 1972L) / 4L
        result -= (yr - 2000L) / 100L
        result += (yr - 2000L) / 400L

        if (isLeap && month >= 3) {
            result += 1L
        }
    }

    result += CUMUL_DAY_IN_MONTHS_NORMAL_YEAR[month - 1] + monthDay - 1L
    return result
}

/**
 * Check if a year is a leap year.
 */
internal fun isLeapYear(year: Int): Boolean =
    year % 400 == 0 || (year % 4 == 0 && year % 100 != 0)
