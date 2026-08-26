// port-lint: source naive/isoweek.rs
package io.github.kotlinmania.chrono.naive

/**
 * ISO 8601 week.
 *
 * This type, combined with `Weekday`, constitutes the ISO 8601 week date.
 * Existing [io.github.kotlinmania.chrono.Datelike] types can retrieve this
 * type through [io.github.kotlinmania.chrono.Datelike.isoWeek].
 */
@ConsistentCopyVisibility
data class IsoWeek internal constructor(
    private val ywf: Int,
) : Comparable<IsoWeek> {
    /**
     * Returns the year number for this ISO week.
     *
     * This year number might not match the calendar year number.
     */
    fun year(): Int = ywf shr 10

    /**
     * Returns the ISO week number starting from 1.
     *
     * The return value ranges from 1 to 53. The last week of year differs by
     * year.
     */
    fun week(): UInt = ((ywf shr 4) and 0x3f).toUInt()

    /**
     * Returns the ISO week number starting from 0.
     *
     * The return value ranges from 0 to 52. The last week of year differs by
     * year.
     */
    fun week0(): UInt = week() - 1u

    /**
     * The string output of an ISO week is the same shape as formatting any
     * date in that week with the ISO week-year and two-digit week number.
     *
     * ISO 8601 requires an explicit sign for years before year 0 or after
     * year 9999.
     */
    override fun toString(): String {
        val year = year()
        val week = week().toInt()
        val formattedYear =
            if (year in 0..9999) {
                year.toString().padStart(4, '0')
            } else {
                formatSignedYear(year)
            }
        return "$formattedYear-W${week.toString().padStart(2, '0')}"
    }

    override fun compareTo(other: IsoWeek): Int = ywf.compareTo(other.ywf)

    companion object {
        /**
         * Returns the corresponding [IsoWeek] from the year, ordinal, and
         * internal year flags value.
         */
        internal fun fromYof(year: Int, ordinal: UInt, yearFlags: YearFlags): IsoWeek {
            val rawWeek = (ordinal + yearFlags.isoweekDelta()) / 7u
            val (weekYear, week) =
                if (rawWeek < 1u) {
                    val previousLastWeek = YearFlags.fromYear(year - 1).nisoweeks()
                    (year - 1) to previousLastWeek
                } else {
                    val lastWeek = yearFlags.nisoweeks()
                    if (rawWeek > lastWeek) {
                        (year + 1) to 1u
                    } else {
                        year to rawWeek
                    }
                }
            val flags = YearFlags.fromYear(weekYear)
            return IsoWeek((weekYear shl 10) or (week.toInt() shl 4) or flags.value.toInt())
        }

        private fun formatSignedYear(year: Int): String =
            if (year >= 0) {
                "+" + year.toString().padStart(4, '0')
            } else {
                "-" + (-year).toString().padStart(4, '0')
            }
    }
}
