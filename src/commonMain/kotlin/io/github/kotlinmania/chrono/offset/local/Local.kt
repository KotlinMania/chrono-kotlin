// port-lint: source offset/local/mod.rs
package io.github.kotlinmania.chrono.offset.local

import io.github.kotlinmania.chrono.Date
import io.github.kotlinmania.chrono.DateTime
import io.github.kotlinmania.chrono.naive.NaiveDate
import io.github.kotlinmania.chrono.naive.NaiveDateTime
import io.github.kotlinmania.chrono.naive.NaiveTime
import io.github.kotlinmania.chrono.offset.FixedOffset
import io.github.kotlinmania.chrono.offset.MappedLocalTime
import io.github.kotlinmania.chrono.offset.Offset
import io.github.kotlinmania.chrono.offset.TimeZone
import io.github.kotlinmania.chrono.offset.Utc

/**
 * The local timescale.
 *
 * Using the [TimeZone] methods on [Local] is the preferred way to construct `DateTime<Local>`
 * instances.
 */
open class Local internal constructor(
    internal val fixedOffset: FixedOffset = FixedOffset.east(0),
) : TimeZone,
    Offset {
    /**
     * Returns the number of seconds to add to convert from UTC to the local time.
     */
    fun localMinusUtc(): Int = fixedOffset.localMinusUtc

    /**
     * Returns the number of seconds to add to convert from the local time to UTC.
     */
    fun utcMinusLocal(): Int = fixedOffset.utcMinusLocal()

    /**
     * Returns a `Date` which corresponds to the current date.
     */
    @Deprecated("Use Local.now() instead")
    fun today(): Date<Local> = now().let { Date(it.dateNaive(), it.offset()) }

    /**
     * Returns a `DateTime<Local>` which corresponds to the current date, time and offset from
     * UTC.
     */
    fun now(): DateTime<Local> = Utc.now().withTimezone(Local)

    override fun fromOffset(state: TimeZone): Local =
        when (state) {
            is Local -> state
            is FixedOffset -> Local(state)
            is Offset -> Local(state.fix())
            else -> Local
        }

    fun fromOffset(offset: FixedOffset): Local = Local(offset)

    override fun offsetFromLocalDate(local: NaiveDate): MappedLocalTime<Local> =
        offsetFromLocalDatetime(local.andTime(NaiveTime.MIN))

    override fun offsetFromLocalDatetime(local: NaiveDateTime): MappedLocalTime<Local> =
        LocalPlatform.offsetFromLocalDatetime(local).map { Local(it) }

    override fun offsetFromUtcDate(utc: NaiveDate): Local =
        offsetFromUtcDatetime(utc.andTime(NaiveTime.MIN))

    override fun offsetFromUtcDatetime(utc: NaiveDateTime): Local =
        Local(LocalPlatform.offsetFromUtcDatetime(utc).unwrap())

    override fun fromLocalDatetime(local: NaiveDateTime): MappedLocalTime<DateTime<Local>> =
        LocalPlatform.offsetFromLocalDatetime(local).andThen { offset ->
            val utc = local.checkedSubOffset(offset)
            if (utc != null) {
                MappedLocalTime.Single(DateTime.fromNaiveUtcAndOffset(utc, Local(offset)))
            } else {
                MappedLocalTime.None
            }
        }

    override fun fromUtcDatetime(utc: NaiveDateTime): DateTime<Local> {
        val offset = LocalPlatform.offsetFromUtcDatetime(utc).unwrap()
        return DateTime.fromNaiveUtcAndOffset(utc, Local(offset))
    }

    override fun withYmdAndHms(
        year: Int,
        month: UInt,
        day: UInt,
        hour: UInt,
        min: UInt,
        sec: UInt,
    ): MappedLocalTime<DateTime<Local>> {
        val d = NaiveDate.fromYmdOpt(year, month, day) ?: return MappedLocalTime.None
        val dt = d.andHmsOpt(hour, min, sec) ?: return MappedLocalTime.None
        return fromLocalDatetime(dt)
    }

    override fun fix(): FixedOffset = fixedOffset

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return when (other) {
            is Local -> fixedOffset == other.fixedOffset
            is FixedOffset -> fixedOffset == other
            else -> false
        }
    }

    override fun hashCode(): Int = fixedOffset.hashCode()

    override fun toString(): String = fixedOffset.toString()

    companion object : Local(FixedOffset.east(0))
}

internal data class Transition(
    val transitionUtc: NaiveDateTime,
    val offsetBefore: FixedOffset,
    val offsetAfter: FixedOffset,
) : Comparable<Transition> {
    override fun compareTo(other: Transition): Int =
        transitionUtc.compareTo(other.transitionUtc)

    companion object {
        fun new(
            transitionLocal: NaiveDateTime,
            offsetBefore: FixedOffset,
            offsetAfter: FixedOffset,
        ): Transition {
            // It is no problem if the transition time in UTC falls a couple of hours inside the buffer
            // space around the `NaiveDateTime` range (although it is very theoretical to have a
            // transition at midnight around `NaiveDate.MIN` or `NaiveDate.MAX`).
            val transitionUtc = transitionLocal.overflowingSubOffset(offsetBefore)
            return Transition(transitionUtc, offsetBefore, offsetAfter)
        }
    }
}

// Calculate the time in UTC given a local time and transitions.
// `transitions` must be sorted.
internal fun lookupWithDstTransitions(
    transitions: Array<Transition>,
    dt: NaiveDateTime,
): MappedLocalTime<FixedOffset> {
    for (t in transitions) {
        // A transition can result in the wall clock time going forward (creating a gap) or going
        // backward (creating a fold). We are interested in the earliest and latest wall time of the
        // transition, as these are the times between which `dt` may not exist or is ambiguous.
        //
        // It is no problem if the transition times fall a couple of hours inside the buffer
        // space around the `NaiveDateTime` range (although it is very theoretical to have a
        // transition at midnight around `NaiveDate.MIN` or `NaiveDate.MAX`).
        val (offsetMin, offsetMax) =
            if (t.offsetAfter.localMinusUtc > t.offsetBefore.localMinusUtc) {
                t.offsetBefore to t.offsetAfter
            } else {
                t.offsetAfter to t.offsetBefore
            }
        val wallEarliest = t.transitionUtc.overflowingAddOffset(offsetMin)
        val wallLatest = t.transitionUtc.overflowingAddOffset(offsetMax)

        if (dt < wallEarliest) {
            return MappedLocalTime.Single(t.offsetBefore)
        } else if (dt <= wallLatest) {
            val cmp = t.offsetAfter.localMinusUtc.compareTo(t.offsetBefore.localMinusUtc)
            return when {
                cmp == 0 -> {
                    MappedLocalTime.Single(t.offsetBefore)
                }

                cmp < 0 -> {
                    MappedLocalTime.Ambiguous(t.offsetBefore, t.offsetAfter)
                }

                else -> {
                    when (dt) {
                        wallEarliest -> MappedLocalTime.Single(t.offsetBefore)
                        wallLatest -> MappedLocalTime.Single(t.offsetAfter)
                        else -> MappedLocalTime.None
                    }
                }
            }
        }
    }
    return MappedLocalTime.Single(transitions.last().offsetAfter)
}

internal fun readTzFileBytes(path: String): ByteArray? = LocalPlatform.readTzFileBytes(path)

internal fun getEnvVar(name: String): String? = LocalPlatform.getEnvVar(name)

internal expect object LocalPlatform {
    fun offsetFromUtcDatetime(utc: NaiveDateTime): MappedLocalTime<FixedOffset>

    fun offsetFromLocalDatetime(local: NaiveDateTime): MappedLocalTime<FixedOffset>

    fun readTzFileBytes(path: String): ByteArray?

    fun getEnvVar(name: String): String?

    fun getLocalTimeMtimeSeconds(): Long?

    fun getTimeZoneInformationForYear(
        wyear: UShort,
        pdtzi: DynamicTimeZoneInformation?,
        ptzi: TimeZoneInformation,
    ): Bool

    fun systemTimeToFileTime(
        lpsystemtime: Systemtime,
        lpfiletime: Filetime,
    ): Bool

    fun systemTimeToTzSpecificLocalTime(
        lptimezoneinformation: TimeZoneInformation?,
        lpuniversaltime: Systemtime,
        lplocaltime: Systemtime,
    ): Bool

    fun tzSpecificLocalTimeToSystemTime(
        lptimezoneinformation: TimeZoneInformation?,
        lplocaltime: Systemtime,
        lpuniversaltime: Systemtime,
    ): Bool
}
