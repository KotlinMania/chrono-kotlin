// port-lint: source offset/fixed.rs
package io.github.kotlinmania.chrono.offset

import io.github.kotlinmania.chrono.naive.NaiveDate
import io.github.kotlinmania.chrono.naive.NaiveDateTime

/**
 * The time zone with fixed offset, from UTC-23:59:59 to UTC+23:59:59.
 */
class FixedOffset private constructor(
    val localMinusUtc: Int,
) : TimeZone<FixedOffset>, Offset {

    /** Returns the number of seconds to add to convert from the local time to UTC. */
    fun utcMinusLocal(): Int = -localMinusUtc

    override fun fromOffset(state: FixedOffset): FixedOffset = state

    override fun offsetFromLocalDate(local: NaiveDate): MappedLocalTime<FixedOffset> =
        MappedLocalTime.Single(this)

    override fun offsetFromLocalDatetime(local: NaiveDateTime): MappedLocalTime<FixedOffset> =
        MappedLocalTime.Single(this)

    override fun offsetFromUtcDate(utc: NaiveDate): FixedOffset = this

    override fun offsetFromUtcDatetime(utc: NaiveDateTime): FixedOffset = this

    override fun fix(): FixedOffset = this

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FixedOffset) return false
        return localMinusUtc == other.localMinusUtc
    }

    override fun hashCode(): Int = localMinusUtc.hashCode()

    override fun toString(): String {
        val sign = if (localMinusUtc < 0) '-' else '+'
        val totalSecs = if (localMinusUtc < 0) -localMinusUtc else localMinusUtc
        val sec = totalSecs % 60
        val mins = totalSecs / 60
        val min = mins % 60
        val hour = mins / 60
        val hStr = hour.toString().padStart(2, '0')
        val mStr = min.toString().padStart(2, '0')
        return if (sec == 0) {
            "$sign$hStr:$mStr"
        } else {
            val sStr = sec.toString().padStart(2, '0')
            "$sign$hStr:$mStr:$sStr"
        }
    }

    companion object {
        /**
         * Makes a new [FixedOffset] for the Eastern Hemisphere with given seconds difference.
         */
        fun eastOpt(secs: Int): FixedOffset? {
            return if (secs > -86_400 && secs < 86_400) {
                FixedOffset(secs)
            } else {
                null
            }
        }

        /**
         * Makes a new [FixedOffset] for the Eastern Hemisphere with given seconds difference.
         */
        fun east(secs: Int): FixedOffset =
            eastOpt(secs) ?: throw IllegalArgumentException("FixedOffset::east out of bounds")

        /**
         * Makes a new [FixedOffset] for the Western Hemisphere with given seconds difference.
         */
        fun westOpt(secs: Int): FixedOffset? =
            eastOpt(-secs)

        /**
         * Makes a new [FixedOffset] for the Western Hemisphere with given seconds difference.
         */
        fun west(secs: Int): FixedOffset =
            westOpt(secs) ?: throw IllegalArgumentException("FixedOffset::west out of bounds")
    }
}
