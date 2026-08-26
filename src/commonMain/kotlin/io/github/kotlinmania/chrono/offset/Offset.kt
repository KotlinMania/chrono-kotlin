// port-lint: source offset/mod.rs
package io.github.kotlinmania.chrono.offset

/**
 * The offset from the local time to UTC.
 */
interface Offset {
    /** Returns the fixed offset from UTC to the local time stored. */
    fun fix(): FixedOffset
}
