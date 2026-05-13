// port-lint: source src/weekday_set.rs
package io.github.kotlinmania.chrono

/**
 * A collection of [Weekday]s stored as a single byte.
 *
 * This type provides efficient set-like and slice-like operations.
 *
 * Implemented as a bitmask where bits 1-7 correspond to Monday-Sunday.
 */
class WeekdaySet internal constructor(internal var bits: Int) : Comparable<WeekdaySet> {
    // Invariant: only the low 7 bits are used; the 8-th bit is always 0.

    /**
     * Returns the day if this collection contains exactly one day.
     *
     * Returns `null` otherwise.
     *
     * # Example
     * ```
     * check(WeekdaySet.single(Weekday.Mon).singleDay() == Weekday.Mon)
     * check(WeekdaySet.fromArray(Weekday.Mon, Weekday.Tue).singleDay() == null)
     * check(WeekdaySet.EMPTY.singleDay() == null)
     * check(WeekdaySet.ALL.singleDay() == null)
     * ```
     */
    fun singleDay(): Weekday? = when (bits) {
        0b000_0001 -> Weekday.Mon
        0b000_0010 -> Weekday.Tue
        0b000_0100 -> Weekday.Wed
        0b000_1000 -> Weekday.Thu
        0b001_0000 -> Weekday.Fri
        0b010_0000 -> Weekday.Sat
        0b100_0000 -> Weekday.Sun
        else -> null
    }

    /**
     * Adds a day to the collection.
     *
     * Returns `true` if the day was new to the collection.
     *
     * # Example
     * ```
     * val weekdays = WeekdaySet.single(Weekday.Mon)
     * check(weekdays.insert(Weekday.Tue))
     * check(!weekdays.insert(Weekday.Tue))
     * ```
     */
    fun insert(day: Weekday): Boolean {
        if (contains(day)) {
            return false
        }

        bits = bits or single(day).bits
        return true
    }

    /**
     * Removes a day from the collection.
     *
     * Returns `true` if the collection did contain the day.
     *
     * # Example
     * ```
     * val weekdays = WeekdaySet.single(Weekday.Mon)
     * check(weekdays.remove(Weekday.Mon))
     * check(!weekdays.remove(Weekday.Mon))
     * ```
     */
    fun remove(day: Weekday): Boolean {
        if (contains(day)) {
            bits = bits and single(day).bits.inv() and 0xFF
            return true
        }

        return false
    }

    /**
     * Returns `true` if `other` contains all days in this set.
     *
     * # Example
     * ```
     * check(WeekdaySet.single(Weekday.Mon).isSubset(WeekdaySet.ALL))
     * check(!WeekdaySet.single(Weekday.Mon).isSubset(WeekdaySet.EMPTY))
     * check(WeekdaySet.EMPTY.isSubset(WeekdaySet.single(Weekday.Mon)))
     * ```
     */
    fun isSubset(other: WeekdaySet): Boolean = intersection(other).bits == bits

    /**
     * Returns days that are in both this set and `other`.
     *
     * # Example
     * ```
     * check(WeekdaySet.single(Weekday.Mon).intersection(WeekdaySet.single(Weekday.Mon)) == WeekdaySet.single(Weekday.Mon))
     * check(WeekdaySet.single(Weekday.Mon).intersection(WeekdaySet.single(Weekday.Tue)) == WeekdaySet.EMPTY)
     * check(WeekdaySet.ALL.intersection(WeekdaySet.single(Weekday.Mon)) == WeekdaySet.single(Weekday.Mon))
     * check(WeekdaySet.ALL.intersection(WeekdaySet.EMPTY) == WeekdaySet.EMPTY)
     * ```
     */
    fun intersection(other: WeekdaySet): WeekdaySet = WeekdaySet(bits and other.bits)

    /**
     * Returns days that are in either this set or `other`.
     *
     * # Example
     * ```
     * check(WeekdaySet.single(Weekday.Mon).union(WeekdaySet.single(Weekday.Mon)) == WeekdaySet.single(Weekday.Mon))
     * check(WeekdaySet.single(Weekday.Mon).union(WeekdaySet.single(Weekday.Tue)) == WeekdaySet.fromArray(Weekday.Mon, Weekday.Tue))
     * check(WeekdaySet.ALL.union(WeekdaySet.single(Weekday.Mon)) == WeekdaySet.ALL)
     * check(WeekdaySet.ALL.union(WeekdaySet.EMPTY) == WeekdaySet.ALL)
     * ```
     */
    fun union(other: WeekdaySet): WeekdaySet = WeekdaySet(bits or other.bits)

    /**
     * Returns days that are in this set or `other` but not in both.
     *
     * # Example
     * ```
     * check(WeekdaySet.single(Weekday.Mon).symmetricDifference(WeekdaySet.single(Weekday.Mon)) == WeekdaySet.EMPTY)
     * check(
     *     WeekdaySet.single(Weekday.Mon).symmetricDifference(WeekdaySet.single(Weekday.Tue))
     *         == WeekdaySet.fromArray(Weekday.Mon, Weekday.Tue)
     * )
     * check(
     *     WeekdaySet.ALL.symmetricDifference(WeekdaySet.single(Weekday.Mon))
     *         == WeekdaySet.fromArray(Weekday.Tue, Weekday.Wed, Weekday.Thu, Weekday.Fri, Weekday.Sat, Weekday.Sun)
     * )
     * check(WeekdaySet.ALL.symmetricDifference(WeekdaySet.EMPTY) == WeekdaySet.ALL)
     * ```
     */
    fun symmetricDifference(other: WeekdaySet): WeekdaySet = WeekdaySet(bits xor other.bits)

    /**
     * Returns days that are in this set but not in `other`.
     *
     * # Example
     * ```
     * check(WeekdaySet.single(Weekday.Mon).difference(WeekdaySet.single(Weekday.Mon)) == WeekdaySet.EMPTY)
     * check(WeekdaySet.single(Weekday.Mon).difference(WeekdaySet.single(Weekday.Tue)) == WeekdaySet.single(Weekday.Mon))
     * check(WeekdaySet.EMPTY.difference(WeekdaySet.single(Weekday.Mon)) == WeekdaySet.EMPTY)
     * ```
     */
    fun difference(other: WeekdaySet): WeekdaySet = WeekdaySet(bits and other.bits.inv() and 0xFF)

    /**
     * Get the first day in the collection, starting from Monday.
     *
     * Returns `null` if the collection is empty.
     *
     * # Example
     * ```
     * check(WeekdaySet.single(Weekday.Mon).first() == Weekday.Mon)
     * check(WeekdaySet.single(Weekday.Tue).first() == Weekday.Tue)
     * check(WeekdaySet.ALL.first() == Weekday.Mon)
     * check(WeekdaySet.EMPTY.first() == null)
     * ```
     */
    fun first(): Weekday? {
        if (isEmpty()) {
            return null
        }

        // Find the first non-zero bit.
        val bit = 1 shl bits.countTrailingZeroBits()

        return WeekdaySet(bit).singleDay()
    }

    /**
     * Get the last day in the collection, starting from Sunday.
     *
     * Returns `null` if the collection is empty.
     *
     * # Example
     * ```
     * check(WeekdaySet.single(Weekday.Mon).last() == Weekday.Mon)
     * check(WeekdaySet.single(Weekday.Sun).last() == Weekday.Sun)
     * check(WeekdaySet.fromArray(Weekday.Mon, Weekday.Tue).last() == Weekday.Tue)
     * check(WeekdaySet.EMPTY.last() == null)
     * ```
     */
    fun last(): Weekday? {
        if (isEmpty()) {
            return null
        }

        // Find the last non-zero bit (in the low 7 bits).
        // `bits` is in 0..127, so it is safe to compute the leading-zero count
        // against an 8-bit window: 7 - (leading zeros in an 8-bit value).
        val leadingZerosU8 = (bits and 0xFF).countLeadingZeroBits() - 24
        val bit = 1 shl (7 - leadingZerosU8)

        return WeekdaySet(bit).singleDay()
    }

    /**
     * Split the collection in two at the given day.
     *
     * Returns a pair `(before, after)`. `before` contains all days starting from Monday
     * up to but __not__ including `weekday`. `after` contains all days starting from `weekday`
     * up to and including Sunday.
     */
    internal fun splitAt(weekday: Weekday): Pair<WeekdaySet, WeekdaySet> {
        val daysAfter = 0b1000_0000 - single(weekday).bits
        val daysBefore = daysAfter xor 0b0111_1111
        return Pair(WeekdaySet(bits and daysBefore), WeekdaySet(bits and daysAfter))
    }

    /**
     * Iterate over the [Weekday]s in the collection starting from a given day.
     *
     * Wraps around from Sunday to Monday if necessary.
     *
     * # Example
     * ```
     * val weekdays = WeekdaySet.fromArray(Weekday.Mon, Weekday.Wed, Weekday.Fri)
     * val iter = weekdays.iter(Weekday.Wed)
     * check(iter.next() == Weekday.Wed)
     * check(iter.next() == Weekday.Fri)
     * check(iter.next() == Weekday.Mon)
     * check(!iter.hasNext())
     * ```
     */
    fun iter(start: Weekday): WeekdaySetIter = WeekdaySetIter(WeekdaySet(bits), start)

    /**
     * Returns `true` if the collection contains the given day.
     *
     * # Example
     * ```
     * check(WeekdaySet.single(Weekday.Mon).contains(Weekday.Mon))
     * check(WeekdaySet.fromArray(Weekday.Mon, Weekday.Tue).contains(Weekday.Tue))
     * check(!WeekdaySet.single(Weekday.Mon).contains(Weekday.Tue))
     * ```
     */
    fun contains(day: Weekday): Boolean = (bits and single(day).bits) != 0

    /**
     * Returns `true` if the collection is empty.
     *
     * # Example
     * ```
     * check(WeekdaySet.EMPTY.isEmpty())
     * check(!WeekdaySet.single(Weekday.Mon).isEmpty())
     * ```
     */
    fun isEmpty(): Boolean = len() == 0

    /**
     * Returns the number of days in the collection.
     *
     * # Example
     * ```
     * check(WeekdaySet.single(Weekday.Mon).len() == 1)
     * check(WeekdaySet.fromArray(Weekday.Mon, Weekday.Wed, Weekday.Fri).len() == 3)
     * check(WeekdaySet.ALL.len() == 7)
     * ```
     */
    fun len(): Int = bits.countOneBits()

    /**
     * Print the collection as a slice-like list of weekdays.
     *
     * # Example
     * ```
     * check("[]" == WeekdaySet.EMPTY.toString())
     * check("[Mon]" == WeekdaySet.single(Weekday.Mon).toString())
     * check("[Mon, Fri, Sun]" == WeekdaySet.fromArray(Weekday.Mon, Weekday.Fri, Weekday.Sun).toString())
     * ```
     */
    override fun toString(): String {
        val sb = StringBuilder()
        sb.append('[')
        val iter = iter(Weekday.Mon)
        if (iter.hasNext()) {
            sb.append(iter.next().toString())
        }
        while (iter.hasNext()) {
            sb.append(", ")
            sb.append(iter.next().toString())
        }
        sb.append(']')
        return sb.toString()
    }

    override fun equals(other: Any?): Boolean = other is WeekdaySet && other.bits == bits

    override fun hashCode(): Int = bits

    override fun compareTo(other: WeekdaySet): Int = bits.compareTo(other.bits)

    companion object {
        /** An empty `WeekdaySet`. */
        val EMPTY: WeekdaySet get() = WeekdaySet(0b000_0000)

        /** A `WeekdaySet` containing all seven [Weekday]s. */
        val ALL: WeekdaySet get() = WeekdaySet(0b111_1111)

        /**
         * Create a `WeekdaySet` from an array of [Weekday]s.
         *
         * # Example
         * ```
         * check(WeekdaySet.EMPTY == WeekdaySet.fromArray())
         * check(WeekdaySet.single(Weekday.Mon) == WeekdaySet.fromArray(Weekday.Mon))
         * check(
         *     WeekdaySet.ALL == WeekdaySet.fromArray(
         *         Weekday.Mon, Weekday.Tue, Weekday.Wed, Weekday.Thu, Weekday.Fri, Weekday.Sat, Weekday.Sun,
         *     )
         * )
         * ```
         */
        fun fromArray(vararg days: Weekday): WeekdaySet {
            var acc = EMPTY
            var idx = 0
            while (idx < days.size) {
                acc.bits = acc.bits or single(days[idx]).bits
                idx += 1
            }
            return acc
        }

        /** Create a `WeekdaySet` from a single [Weekday]. */
        fun single(weekday: Weekday): WeekdaySet = when (weekday) {
            Weekday.Mon -> WeekdaySet(0b000_0001)
            Weekday.Tue -> WeekdaySet(0b000_0010)
            Weekday.Wed -> WeekdaySet(0b000_0100)
            Weekday.Thu -> WeekdaySet(0b000_1000)
            Weekday.Fri -> WeekdaySet(0b001_0000)
            Weekday.Sat -> WeekdaySet(0b010_0000)
            Weekday.Sun -> WeekdaySet(0b100_0000)
        }

        /**
         * Build a `WeekdaySet` by folding the [Weekday]s of [iterable] together.
         */
        fun fromIterable(iterable: Iterable<Weekday>): WeekdaySet {
            var acc = EMPTY
            for (day in iterable) {
                acc = acc.union(single(day))
            }
            return acc
        }
    }
}

/**
 * An iterator over a collection of weekdays, starting from a given day.
 *
 * See [WeekdaySet.iter].
 */
class WeekdaySetIter internal constructor(
    private val days: WeekdaySet,
    private val start: Weekday,
) : Iterator<Weekday> {

    override fun hasNext(): Boolean = !days.isEmpty()

    override fun next(): Weekday {
        if (days.isEmpty()) {
            throw NoSuchElementException()
        }

        // Split the collection in two at `start`.
        // Look for the first day among the days after `start` first, including `start` itself.
        // If there are no days after `start`, look for the first day among the days before `start`.
        val (before, after) = days.splitAt(start)
        val pool = if (after.isEmpty()) before else after

        val next = pool.first() ?: error("the collection is not empty")
        days.remove(next)
        return next
    }

    /**
     * The last remaining day in the collection, viewed from the back of the iterator.
     *
     * Returns `null` once the iterator is exhausted. The companion of [next] for
     * double-ended iteration.
     */
    fun nextBack(): Weekday? {
        if (days.isEmpty()) {
            return null
        }

        // Split the collection in two at `start`.
        // Look for the last day among the days before `start` first, NOT including `start` itself.
        // If there are no days before `start`, look for the last day among the days after `start`.
        val (before, after) = days.splitAt(start)
        val pool = if (before.isEmpty()) after else before

        val nextBack = pool.last() ?: error("the collection is not empty")
        days.remove(nextBack)
        return nextBack
    }

    /** The number of days remaining in the iterator. */
    val size: Int get() = days.len()
}
