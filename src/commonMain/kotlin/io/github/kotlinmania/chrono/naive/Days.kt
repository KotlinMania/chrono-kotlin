// port-lint: source naive/mod.rs
package io.github.kotlinmania.chrono.naive

import kotlin.jvm.JvmInline

/**
 * A duration in calendar days.
 *
 * This is useful because when using [io.github.kotlinmania.chrono.TimeDelta] it is possible that
 * adding `TimeDelta.days(1)` doesn't increment the day value as expected due to it being a fixed
 * number of seconds.
 */
@JvmInline
value class Days(val num: ULong) : Comparable<Days> {
    override fun compareTo(other: Days): Int = num.compareTo(other.num)

    companion object {
        /** Construct a new [Days] from a number of days. */
        fun new(num: ULong): Days = Days(num)
    }
}
