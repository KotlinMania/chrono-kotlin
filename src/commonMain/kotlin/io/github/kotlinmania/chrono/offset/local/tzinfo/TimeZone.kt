// port-lint: source offset/local/tz_info/timezone.rs
package io.github.kotlinmania.chrono.offset.local.tzinfo

import io.github.kotlinmania.chrono.naive.NaiveDateTime
import io.github.kotlinmania.chrono.offset.MappedLocalTime
import io.github.kotlinmania.chrono.offset.local.forZone
import io.github.kotlinmania.chrono.offset.local.readTzFileBytes
import kotlin.math.abs

/**
 * Time zone
 */
internal data class TimeZone(
    val transitions: List<Transition>,
    val localTimeTypes: List<LocalTimeType>,
    val leapSeconds: List<LeapSecond>,
    val extraRule: TransitionRule?,
) {
    /**
     * Find the local time type associated to the time zone at the specified Unix time in seconds.
     */
    fun findLocalTimeType(unixTime: Long): TzResult<LocalTimeType> =
        asRef().findLocalTimeType(unixTime)

    fun findLocalTimeTypeFromLocal(localTime: NaiveDateTime): TzResult<MappedLocalTime<LocalTimeType>> =
        asRef().findLocalTimeTypeFromLocal(localTime)

    /**
     * Returns a reference to the time zone.
     */
    fun asRef(): TimeZoneRef =
        TimeZoneRef(
            transitions = transitions,
            localTimeTypes = localTimeTypes,
            leapSeconds = leapSeconds,
            extraRule = extraRule,
        )

    companion object {
        /**
         * Returns local time zone.
         */
        fun local(envTz: String?): TzResult<TimeZone> =
            when (envTz) {
                null -> fromPosixTz("localtime")
                else -> fromPosixTz(envTz)
            }

        /**
         * Construct a time zone from a POSIX TZ string.
         */
        fun fromPosixTz(tzString: String): TzResult<TimeZone> {
            if (tzString.isEmpty()) {
                return TzResult.Ok(utc())
            }

            if (tzString == "localtime") {
                val bytes = readTzFileBytes("/etc/localtime") ?: return TzResult.Err(Error.Io("file not found"))
                return fromTzData(bytes)
            }

            val androidTzRes = forZone(tzString)
            if (androidTzRes is TzResult.Ok && androidTzRes.value != null) {
                return fromTzData(androidTzRes.value)
            }

            if (tzString.startsWith(':')) {
                val fileBytes =
                    when (val res = findTzFile(tzString.substring(1))) {
                        is TzResult.Ok -> res.value
                        is TzResult.Err -> return res
                    }
                return fromTzData(fileBytes)
            }

            val maybeFileBytes = findTzFile(tzString)
            if (maybeFileBytes is TzResult.Ok) {
                return fromTzData(maybeFileBytes.value)
            }

            val trimmed = tzString.trim { c -> c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\u000C' }
            val rule =
                when (val res = TransitionRule.fromTzString(trimmed.encodeToByteArray(), false)) {
                    is TzResult.Ok -> res.value
                    is TzResult.Err -> return res
                }
            return new(
                transitions = emptyList(),
                localTimeTypes =
                    when (rule) {
                        is TransitionRule.Fixed -> listOf(rule.localTimeType)
                        is TransitionRule.Alternate -> listOf(rule.alternateTime.std, rule.alternateTime.dst)
                    },
                leapSeconds = emptyList(),
                extraRule = rule,
            )
        }

        /**
         * Construct a time zone.
         */
        fun new(
            transitions: List<Transition>,
            localTimeTypes: List<LocalTimeType>,
            leapSeconds: List<LeapSecond>,
            extraRule: TransitionRule?,
        ): TzResult<TimeZone> {
            val tz = TimeZone(transitions, localTimeTypes, leapSeconds, extraRule)
            when (val res = tz.asRef().validate()) {
                is TzResult.Ok -> Unit
                is TzResult.Err -> return res
            }
            return TzResult.Ok(tz)
        }

        /**
         * Construct a time zone from the contents of a time zone file.
         */
        fun fromTzData(bytes: ByteArray): TzResult<TimeZone> = parse(bytes)

        /**
         * Construct a time zone with the specified UTC offset in seconds.
         */
        fun fixed(utOffset: Int): TzResult<TimeZone> =
            LocalTimeType.withOffset(utOffset).map { ltt ->
                TimeZone(
                    transitions = emptyList(),
                    localTimeTypes = listOf(ltt),
                    leapSeconds = emptyList(),
                    extraRule = null,
                )
            }

        /**
         * Construct the time zone associated to UTC.
         */
        fun utc(): TimeZone =
            TimeZone(
                transitions = emptyList(),
                localTimeTypes = listOf(LocalTimeType.UTC),
                leapSeconds = emptyList(),
                extraRule = null,
            )
    }
}

/**
 * Reference to a time zone
 */
internal data class TimeZoneRef(
    val transitions: List<Transition>,
    val localTimeTypes: List<LocalTimeType>,
    val leapSeconds: List<LeapSecond>,
    val extraRule: TransitionRule?,
) {
    /**
     * Find the local time type associated to the time zone at the specified Unix time in seconds.
     */
    fun findLocalTimeType(unixTime: Long): TzResult<LocalTimeType> {
        val rule =
            if (transitions.isEmpty()) {
                extraRule ?: return TzResult.Ok(localTimeTypes[0])
            } else {
                val lastTransition = transitions.last()
                val unixLeapTime =
                    when (val res = unixTimeToUnixLeapTime(unixTime)) {
                        is TzResult.Ok -> {
                            res.value
                        }

                        is TzResult.Err -> {
                            return when (val err = res.error) {
                                is Error.OutOfRange -> TzResult.Err(Error.FindLocalTimeType(err.description))
                                else -> res
                            }
                        }
                    }

                if (unixLeapTime >= lastTransition.unixLeapTime) {
                    extraRule ?: return TzResult.Ok(localTimeTypes[lastTransition.localTimeTypeIndex])
                } else {
                    val idx = transitions.binarySearch { it.unixLeapTime.compareTo(unixLeapTime) }
                    val index = if (idx >= 0) idx + 1 else -(idx + 1)
                    val localTimeTypeIndex =
                        if (index > 0) {
                            transitions[index - 1].localTimeTypeIndex
                        } else {
                            0
                        }
                    return TzResult.Ok(localTimeTypes[localTimeTypeIndex])
                }
            }

        return when (val res = rule.findLocalTimeType(unixTime)) {
            is TzResult.Ok -> {
                res
            }

            is TzResult.Err -> {
                when (val err = res.error) {
                    is Error.OutOfRange -> TzResult.Err(Error.FindLocalTimeType(err.description))
                    else -> res
                }
            }
        }
    }

    fun findLocalTimeTypeFromLocal(localTime: NaiveDateTime): TzResult<MappedLocalTime<LocalTimeType>> {
        val localLeapTime = localTime.andUtc().timestamp()

        val offsetAfterLast =
            if (transitions.isNotEmpty()) {
                var prev = localTimeTypes[0]

                for (transition in transitions) {
                    val afterLtt = localTimeTypes[transition.localTimeTypeIndex]

                    val transitionEnd = transition.unixLeapTime + afterLtt.utOffset.toLong()
                    val transitionStart = transition.unixLeapTime + prev.utOffset.toLong()

                    when {
                        transitionStart > transitionEnd -> {
                            if (localLeapTime < transitionEnd) {
                                return TzResult.Ok(MappedLocalTime.Single(prev))
                            } else if (localLeapTime in transitionEnd..transitionStart) {
                                return if (prev.utOffset < afterLtt.utOffset) {
                                    TzResult.Ok(MappedLocalTime.Ambiguous(prev, afterLtt))
                                } else {
                                    TzResult.Ok(MappedLocalTime.Ambiguous(afterLtt, prev))
                                }
                            }
                        }

                        transitionStart == transitionEnd -> {
                            if (localLeapTime < transitionStart) {
                                return TzResult.Ok(MappedLocalTime.Single(prev))
                            } else if (localLeapTime == transitionEnd) {
                                return if (prev.utOffset < afterLtt.utOffset) {
                                    TzResult.Ok(MappedLocalTime.Ambiguous(prev, afterLtt))
                                } else {
                                    TzResult.Ok(MappedLocalTime.Ambiguous(afterLtt, prev))
                                }
                            }
                        }

                        else -> {
                            if (localLeapTime <= transitionStart) {
                                return TzResult.Ok(MappedLocalTime.Single(prev))
                            } else if (localLeapTime < transitionEnd) {
                                return TzResult.Ok(MappedLocalTime.None)
                            } else if (localLeapTime == transitionEnd) {
                                return TzResult.Ok(MappedLocalTime.Single(afterLtt))
                            }
                        }
                    }

                    prev = afterLtt
                }

                prev
            } else {
                localTimeTypes[0]
            }

        return if (extraRule != null) {
            when (val res = extraRule.findLocalTimeTypeFromLocal(localTime)) {
                is TzResult.Ok -> {
                    res
                }

                is TzResult.Err -> {
                    when (val err = res.error) {
                        is Error.OutOfRange -> TzResult.Err(Error.FindLocalTimeType(err.description))
                        else -> res
                    }
                }
            }
        } else {
            TzResult.Ok(MappedLocalTime.Single(offsetAfterLast))
        }
    }

    /**
     * Check time zone inputs.
     */
    fun validate(): TzResult<Unit> {
        val localTimeTypesSize = localTimeTypes.size
        if (localTimeTypesSize == 0) {
            return TzResult.Err(Error.TimeZone("list of local time types must not be empty"))
        }

        var iTransition = 0
        while (iTransition < transitions.size) {
            if (transitions[iTransition].localTimeTypeIndex >= localTimeTypesSize) {
                return TzResult.Err(Error.TimeZone("invalid local time type index"))
            }

            if (iTransition + 1 < transitions.size &&
                transitions[iTransition].unixLeapTime >= transitions[iTransition + 1].unixLeapTime
            ) {
                return TzResult.Err(Error.TimeZone("invalid transition"))
            }

            iTransition += 1
        }

        if (!(
                leapSeconds.isEmpty() ||
                    (
                        leapSeconds[0].unixLeapTime >= 0L &&
                            saturatingAbs(leapSeconds[0].correction) == 1
                    )
            )
        ) {
            return TzResult.Err(Error.TimeZone("invalid leap second"))
        }

        val minInterval = SECONDS_PER_28_DAYS - 1L

        var iLeapSecond = 0
        while (iLeapSecond < leapSeconds.size) {
            if (iLeapSecond + 1 < leapSeconds.size) {
                val x0 = leapSeconds[iLeapSecond]
                val x1 = leapSeconds[iLeapSecond + 1]

                val diffUnixLeapTime = saturatingSub(x1.unixLeapTime, x0.unixLeapTime)
                val absDiffCorrection = saturatingAbs(saturatingSub(x1.correction, x0.correction))

                if (!(diffUnixLeapTime >= minInterval && absDiffCorrection == 1)) {
                    return TzResult.Err(Error.TimeZone("invalid leap second"))
                }
            }
            iLeapSecond += 1
        }

        val rule = extraRule ?: return TzResult.Ok(Unit)
        val lastTransition = transitions.lastOrNull() ?: return TzResult.Ok(Unit)

        val lastLocalTimeType = localTimeTypes[lastTransition.localTimeTypeIndex]
        val unixTime =
            when (val res = unixLeapTimeToUnixTime(lastTransition.unixLeapTime)) {
                is TzResult.Ok -> {
                    res.value
                }

                is TzResult.Err -> {
                    return when (val err = res.error) {
                        is Error.OutOfRange -> TzResult.Err(Error.TimeZone(err.description))
                        else -> res
                    }
                }
            }

        val ruleLocalTimeType =
            when (val res = rule.findLocalTimeType(unixTime)) {
                is TzResult.Ok -> {
                    res.value
                }

                is TzResult.Err -> {
                    return when (val err = res.error) {
                        is Error.OutOfRange -> TzResult.Err(Error.TimeZone(err.description))
                        else -> res
                    }
                }
            }

        val namesEqual =
            when {
                lastLocalTimeType.name != null && ruleLocalTimeType.name != null -> {
                    lastLocalTimeType.name.equal(ruleLocalTimeType.name)
                }

                lastLocalTimeType.name == null && ruleLocalTimeType.name == null -> {
                    true
                }

                else -> {
                    false
                }
            }

        val check =
            lastLocalTimeType.utOffset == ruleLocalTimeType.utOffset &&
                lastLocalTimeType.isDst() == ruleLocalTimeType.isDst() &&
                namesEqual

        if (!check) {
            return TzResult.Err(
                Error.TimeZone("extra transition rule is inconsistent with the last transition"),
            )
        }

        return TzResult.Ok(Unit)
    }

    /**
     * Convert Unix time to Unix leap time, from the list of leap seconds in a time zone.
     */
    fun unixTimeToUnixLeapTime(unixTime: Long): TzResult<Long> {
        var unixLeapTime = unixTime

        var i = 0
        while (i < leapSeconds.size) {
            val leapSecond = leapSeconds[i]

            if (unixLeapTime < leapSecond.unixLeapTime) {
                break
            }

            val corr = leapSecond.correction.toLong()
            val sum = unixTime + corr
            if ((unixTime xor corr) >= 0L && (unixTime xor sum) < 0L) {
                return TzResult.Err(Error.OutOfRange("out of range operation"))
            }
            unixLeapTime = sum

            i += 1
        }

        return TzResult.Ok(unixLeapTime)
    }

    /**
     * Convert Unix leap time to Unix time, from the list of leap seconds in a time zone.
     */
    fun unixLeapTimeToUnixTime(unixLeapTime: Long): TzResult<Long> {
        if (unixLeapTime == Long.MIN_VALUE) {
            return TzResult.Err(Error.OutOfRange("out of range operation"))
        }

        val target = unixLeapTime - 1L
        val idx = leapSeconds.binarySearch { it.unixLeapTime.compareTo(target) }
        val index = if (idx >= 0) idx + 1 else -(idx + 1)

        val correction = if (index > 0) leapSeconds[index - 1].correction.toLong() else 0L

        val diff = unixLeapTime - correction
        if ((unixLeapTime xor correction) < 0L && (unixLeapTime xor diff) < 0L) {
            return TzResult.Err(Error.OutOfRange("out of range operation"))
        }
        return TzResult.Ok(diff)
    }

    companion object {
        val UTC: TimeZoneRef =
            TimeZoneRef(
                transitions = emptyList(),
                localTimeTypes = listOf(LocalTimeType.UTC),
                leapSeconds = emptyList(),
                extraRule = null,
            )
    }
}

private fun saturatingAbs(value: Int): Int =
    if (value == Int.MIN_VALUE) Int.MAX_VALUE else abs(value)

private fun saturatingSub(a: Int, b: Int): Int {
    val diff = a.toLong() - b.toLong()
    return when {
        diff > Int.MAX_VALUE.toLong() -> Int.MAX_VALUE
        diff < Int.MIN_VALUE.toLong() -> Int.MIN_VALUE
        else -> diff.toInt()
    }
}

private fun saturatingSub(a: Long, b: Long): Long {
    val diff = a - b
    if ((a xor b) < 0L && (a xor diff) < 0L) {
        return if (a < 0L) Long.MIN_VALUE else Long.MAX_VALUE
    }
    return diff
}

/**
 * Transition of a TZif file
 */
internal data class Transition(
    val unixLeapTime: Long,
    val localTimeTypeIndex: Int,
) {
    companion object {
        fun new(unixLeapTime: Long, localTimeTypeIndex: Int): Transition =
            Transition(unixLeapTime, localTimeTypeIndex)
    }
}

/**
 * Leap second of a TZif file
 */
internal data class LeapSecond(
    val unixLeapTime: Long,
    val correction: Int,
) {
    companion object {
        fun new(unixLeapTime: Long, correction: Int): LeapSecond =
            LeapSecond(unixLeapTime, correction)
    }
}

/**
 * ASCII-encoded fixed-capacity string, used for storing time zone names
 */
internal class TimeZoneName private constructor(
    internal val bytes: ByteArray,
) {
    fun asBytes(): ByteArray {
        val len = bytes[0].toInt() and 0xFF
        return bytes.copyOfRange(1, 1 + len)
    }

    fun equal(other: TimeZoneName): Boolean = bytes.contentEquals(other.bytes)

    fun asStr(): String = asBytes().decodeToString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TimeZoneName) return false
        return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int = bytes.contentHashCode()

    override fun toString(): String = asStr()

    companion object {
        fun new(input: ByteArray): TzResult<TimeZoneName> {
            val len = input.size
            if (len !in 3..7) {
                return TzResult.Err(
                    Error.LocalTimeType("time zone name must have between 3 and 7 characters"),
                )
            }

            val bytes = ByteArray(8)
            bytes[0] = len.toByte()

            var i = 0
            while (i < len) {
                val b = input[i]
                val c = b.toInt() and 0xFF
                val valid =
                    (c in '0'.code..'9'.code) ||
                        (c in 'A'.code..'Z'.code) ||
                        (c in 'a'.code..'z'.code) ||
                        c == '+'.code ||
                        c == '-'.code
                if (!valid) {
                    return TzResult.Err(Error.LocalTimeType("invalid characters in time zone name"))
                }
                bytes[i + 1] = b
                i += 1
            }

            return TzResult.Ok(TimeZoneName(bytes))
        }
    }
}

/**
 * Local time type associated to a time zone
 */
internal data class LocalTimeType(
    val utOffset: Int,
    private val dst: Boolean,
    val name: TimeZoneName?,
) {
    /** Returns offset from UTC in seconds */
    fun offset(): Int = utOffset

    /** Returns daylight saving time indicator */
    fun isDst(): Boolean = dst

    companion object {
        val UTC: LocalTimeType = LocalTimeType(0, false, null)

        fun new(utOffset: Int, isDst: Boolean, name: ByteArray?): TzResult<LocalTimeType> {
            if (utOffset == Int.MIN_VALUE) {
                return TzResult.Err(Error.LocalTimeType("invalid UTC offset"))
            }

            if (name == null) {
                return TzResult.Ok(LocalTimeType(utOffset, isDst, null))
            }

            val tzName =
                when (val res = TimeZoneName.new(name)) {
                    is TzResult.Ok -> res.value
                    is TzResult.Err -> return res
                }

            return TzResult.Ok(LocalTimeType(utOffset, isDst, tzName))
        }

        fun withOffset(utOffset: Int): TzResult<LocalTimeType> {
            if (utOffset == Int.MIN_VALUE) {
                return TzResult.Err(Error.LocalTimeType("invalid UTC offset"))
            }

            return TzResult.Ok(LocalTimeType(utOffset, false, null))
        }
    }
}

private fun findTzFile(path: String): TzResult<ByteArray> {
    if (path.startsWith('/')) {
        val bytes = readTzFileBytes(path)
        if (bytes != null) return TzResult.Ok(bytes)
        return TzResult.Err(Error.Io("file not found"))
    }

    for (folder in ZONE_INFO_DIRECTORIES) {
        val bytes = readTzFileBytes("$folder/$path")
        if (bytes != null) {
            return TzResult.Ok(bytes)
        }
    }

    val directBytes = readTzFileBytes(path)
    if (directBytes != null) {
        return TzResult.Ok(directBytes)
    }

    return TzResult.Err(Error.Io("file not found"))
}

private val ZONE_INFO_DIRECTORIES: Array<String> =
    arrayOf("/usr/share/zoneinfo", "/share/zoneinfo", "/etc/zoneinfo", "/usr/share/lib/zoneinfo")

/** Number of seconds in one week */
internal const val SECONDS_PER_WEEK: Long = SECONDS_PER_DAY * DAYS_PER_WEEK

/** Number of seconds in 28 days */
private const val SECONDS_PER_28_DAYS: Long = SECONDS_PER_DAY * 28L
