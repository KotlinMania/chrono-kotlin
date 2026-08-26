// port-lint: source offset/mod.rs
package io.github.kotlinmania.chrono.offset

/**
 * The result of mapping a local time to a concrete instant in a given time zone.
 */
sealed class MappedLocalTime<out T> {
    /** The local time maps to a single unique result. */
    data class Single<out T>(val value: T) : MappedLocalTime<T>()

    /** The local time is ambiguous because of a fold (e.g. daylight saving transition). */
    data class Ambiguous<out T>(val earliest: T, val latest: T) : MappedLocalTime<T>()

    /** The local time does not exist (e.g. gap during daylight saving transition). */
    data object None : MappedLocalTime<Nothing>()

    /** Returns the single result if unique, or `null`. */
    fun single(): T? = when (this) {
        is Single -> value
        else -> null
    }

    /** Returns the earliest possible result, or `null`. */
    fun earliest(): T? = when (this) {
        is Single -> value
        is Ambiguous -> earliest
        None -> null
    }

    /** Returns the latest possible result, or `null`. */
    fun latest(): T? = when (this) {
        is Single -> value
        is Ambiguous -> latest
        None -> null
    }

    /** Maps a [MappedLocalTime] of `T` to a [MappedLocalTime] of `U`. */
    fun <U> map(transform: (T) -> U): MappedLocalTime<U> = when (this) {
        None -> None
        is Single -> Single(transform(value))
        is Ambiguous -> Ambiguous(transform(earliest), transform(latest))
    }

    /** Chains a transformation that returns another [MappedLocalTime]. */
    fun <U> andThen(transform: (T) -> MappedLocalTime<U>): MappedLocalTime<U> = when (this) {
        None -> None
        is Single -> transform(value)
        is Ambiguous -> {
            val min = transform(earliest).single()
            val max = transform(latest).single()
            if (min != null && max != null) Ambiguous(min, max) else None
        }
    }

    /** Returns the unwrapped value, or throws an exception if [None] or [Ambiguous]. */
    fun unwrap(): T = when (this) {
        None -> throw NoSuchElementException("No such local time")
        is Single -> value
        is Ambiguous -> throw IllegalStateException("Ambiguous local time: $earliest to $latest")
    }
}
