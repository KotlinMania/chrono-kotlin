// port-lint: source src/lib.rs
package io.github.kotlinmania.chrono

/** Out of range error type used in various converting APIs */
class OutOfRange internal constructor() : Throwable("out of range") {
    override fun equals(other: Any?): Boolean = other is OutOfRange
    override fun hashCode(): Int = 0
    override fun toString(): String = "out of range"
}
