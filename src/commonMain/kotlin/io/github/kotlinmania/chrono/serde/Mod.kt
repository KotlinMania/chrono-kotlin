// port-lint: source datetime/serde.rs
package io.github.kotlinmania.chrono.serde

import io.github.kotlinmania.chrono.DateTime
import io.github.kotlinmania.chrono.offset.Utc
import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.ser.Serialize
import io.github.kotlinmania.serdecore.ser.Serializer

internal fun invalidTs(value: Any): SerdeError =
    SerdeError.custom("value is not a legal timestamp: $value")

private class I64Serialize(
    private val value: Long,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serializer.serializeI64(value)
}

/** Visitor for deserializing a unix timestamp in seconds into [DateTime]<[Utc]>. */
object SecondsTimestampVisitor : Visitor<DateTime<Utc>> {
    override fun expecting(): String = "a unix timestamp in seconds"

    override fun visitI64(v: Long): SerdeResult<DateTime<Utc>> {
        val dt = DateTime.fromTimestampSecs(v) ?: return SerdeResult.failure(invalidTs(v))
        return SerdeResult.success(dt)
    }

    override fun visitU64(v: ULong): SerdeResult<DateTime<Utc>> {
        if (v > Long.MAX_VALUE.toULong()) {
            return SerdeResult.failure(invalidTs(v))
        }
        val dt = DateTime.fromTimestampSecs(v.toLong()) ?: return SerdeResult.failure(invalidTs(v))
        return SerdeResult.success(dt)
    }
}

/** Visitor for deserializing a unix timestamp in milliseconds into [DateTime]<[Utc]>. */
object MilliSecondsTimestampVisitor : Visitor<DateTime<Utc>> {
    override fun expecting(): String = "a unix timestamp in milliseconds"

    override fun visitI64(v: Long): SerdeResult<DateTime<Utc>> {
        val dt = DateTime.fromTimestampMillis(v) ?: return SerdeResult.failure(invalidTs(v))
        return SerdeResult.success(dt)
    }

    override fun visitU64(v: ULong): SerdeResult<DateTime<Utc>> {
        val secs = (v / 1_000uL).toLong()
        val nsecs = ((v % 1_000uL) * 1_000_000uL).toUInt()
        val dt = DateTime.fromTimestamp(secs, nsecs) ?: return SerdeResult.failure(invalidTs(v))
        return SerdeResult.success(dt)
    }
}

/** Visitor for deserializing a unix timestamp in microseconds into [DateTime]<[Utc]>. */
object MicroSecondsTimestampVisitor : Visitor<DateTime<Utc>> {
    override fun expecting(): String = "a unix timestamp in microseconds"

    override fun visitI64(v: Long): SerdeResult<DateTime<Utc>> {
        val dt = DateTime.fromTimestampMicros(v) ?: return SerdeResult.failure(invalidTs(v))
        return SerdeResult.success(dt)
    }

    override fun visitU64(v: ULong): SerdeResult<DateTime<Utc>> {
        val secs = (v / 1_000_000uL).toLong()
        val nsecs = ((v % 1_000_000uL) * 1_000uL).toUInt()
        val dt = DateTime.fromTimestamp(secs, nsecs) ?: return SerdeResult.failure(invalidTs(v))
        return SerdeResult.success(dt)
    }
}

/** Visitor for deserializing a unix timestamp in nanoseconds into [DateTime]<[Utc]>. */
object NanoSecondsTimestampVisitor : Visitor<DateTime<Utc>> {
    override fun expecting(): String = "a unix timestamp in nanoseconds"

    override fun visitI64(v: Long): SerdeResult<DateTime<Utc>> =
        SerdeResult.success(DateTime.fromTimestampNanos(v))

    override fun visitU64(v: ULong): SerdeResult<DateTime<Utc>> {
        val secs = (v / 1_000_000_000uL).toLong()
        val nsecs = (v % 1_000_000_000uL).toUInt()
        val dt = DateTime.fromTimestamp(secs, nsecs) ?: return SerdeResult.failure(invalidTs(v))
        return SerdeResult.success(dt)
    }
}

/** Serialize/deserialize [DateTime]<[Utc]> to/from timestamps in nanoseconds. */
object TsNanoseconds : Deserialize<DateTime<Utc>> {
    fun <Ok> serialize(
        dt: DateTime<Utc>,
        serializer: Serializer<Ok>,
    ): SerdeResult<Ok> {
        val nanos =
            dt.timestampNanosOpt()
                ?: return SerdeResult.failure(
                    SerdeError.custom("value out of range for a timestamp with nanosecond precision"),
                )
        return serializer.serializeI64(nanos)
    }

    override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<DateTime<Utc>> =
        deserializer.deserializeI64(NanoSecondsTimestampVisitor)
}

/** Serialize/deserialize optional [DateTime]<[Utc]> to/from timestamps in nanoseconds. */
object TsNanosecondsOption : Deserialize<DateTime<Utc>?> {
    fun <Ok> serialize(
        opt: DateTime<Utc>?,
        serializer: Serializer<Ok>,
    ): SerdeResult<Ok> =
        if (opt != null) {
            val nanos =
                opt.timestampNanosOpt()
                    ?: return SerdeResult.failure(
                        SerdeError.custom("value out of range for a timestamp with nanosecond precision"),
                    )
            serializer.serializeSome(I64Serialize(nanos))
        } else {
            serializer.serializeNone()
        }

    override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<DateTime<Utc>?> =
        deserializer.deserializeOption(OptionNanoSecondsTimestampVisitor)

    private object OptionNanoSecondsTimestampVisitor : Visitor<DateTime<Utc>?> {
        override fun expecting(): String = "a unix timestamp in nanoseconds or none"

        override fun <D : Deserializer> visitSome(deserializer: D): SerdeResult<DateTime<Utc>?> =
            deserializer.deserializeI64(NanoSecondsTimestampVisitor)

        override fun visitNone(): SerdeResult<DateTime<Utc>?> = SerdeResult.success(null)

        override fun visitUnit(): SerdeResult<DateTime<Utc>?> = SerdeResult.success(null)
    }
}

/** Serialize/deserialize [DateTime]<[Utc]> to/from timestamps in microseconds. */
object TsMicroseconds : Deserialize<DateTime<Utc>> {
    fun <Ok> serialize(
        dt: DateTime<Utc>,
        serializer: Serializer<Ok>,
    ): SerdeResult<Ok> = serializer.serializeI64(dt.timestampMicros())

    override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<DateTime<Utc>> =
        deserializer.deserializeI64(MicroSecondsTimestampVisitor)
}

/** Serialize/deserialize optional [DateTime]<[Utc]> to/from timestamps in microseconds. */
object TsMicrosecondsOption : Deserialize<DateTime<Utc>?> {
    fun <Ok> serialize(
        opt: DateTime<Utc>?,
        serializer: Serializer<Ok>,
    ): SerdeResult<Ok> =
        if (opt != null) {
            serializer.serializeSome(I64Serialize(opt.timestampMicros()))
        } else {
            serializer.serializeNone()
        }

    override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<DateTime<Utc>?> =
        deserializer.deserializeOption(OptionMicroSecondsTimestampVisitor)

    private object OptionMicroSecondsTimestampVisitor : Visitor<DateTime<Utc>?> {
        override fun expecting(): String = "a unix timestamp in microseconds or none"

        override fun <D : Deserializer> visitSome(deserializer: D): SerdeResult<DateTime<Utc>?> =
            deserializer.deserializeI64(MicroSecondsTimestampVisitor)

        override fun visitNone(): SerdeResult<DateTime<Utc>?> = SerdeResult.success(null)

        override fun visitUnit(): SerdeResult<DateTime<Utc>?> = SerdeResult.success(null)
    }
}

/** Serialize/deserialize [DateTime]<[Utc]> to/from timestamps in milliseconds. */
object TsMilliseconds : Deserialize<DateTime<Utc>> {
    fun <Ok> serialize(
        dt: DateTime<Utc>,
        serializer: Serializer<Ok>,
    ): SerdeResult<Ok> = serializer.serializeI64(dt.timestampMillis())

    override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<DateTime<Utc>> =
        deserializer.deserializeI64(MilliSecondsTimestampVisitor)
}

/** Serialize/deserialize optional [DateTime]<[Utc]> to/from timestamps in milliseconds. */
object TsMillisecondsOption : Deserialize<DateTime<Utc>?> {
    fun <Ok> serialize(
        opt: DateTime<Utc>?,
        serializer: Serializer<Ok>,
    ): SerdeResult<Ok> =
        if (opt != null) {
            serializer.serializeSome(I64Serialize(opt.timestampMillis()))
        } else {
            serializer.serializeNone()
        }

    override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<DateTime<Utc>?> =
        deserializer.deserializeOption(OptionMilliSecondsTimestampVisitor)

    private object OptionMilliSecondsTimestampVisitor : Visitor<DateTime<Utc>?> {
        override fun expecting(): String = "a unix timestamp in milliseconds or none"

        override fun <D : Deserializer> visitSome(deserializer: D): SerdeResult<DateTime<Utc>?> =
            deserializer.deserializeI64(MilliSecondsTimestampVisitor)

        override fun visitNone(): SerdeResult<DateTime<Utc>?> = SerdeResult.success(null)

        override fun visitUnit(): SerdeResult<DateTime<Utc>?> = SerdeResult.success(null)
    }
}

/** Serialize/deserialize [DateTime]<[Utc]> to/from timestamps in seconds. */
object TsSeconds : Deserialize<DateTime<Utc>> {
    fun <Ok> serialize(
        dt: DateTime<Utc>,
        serializer: Serializer<Ok>,
    ): SerdeResult<Ok> = serializer.serializeI64(dt.timestamp())

    override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<DateTime<Utc>> =
        deserializer.deserializeI64(SecondsTimestampVisitor)
}

/** Serialize/deserialize optional [DateTime]<[Utc]> to/from timestamps in seconds. */
object TsSecondsOption : Deserialize<DateTime<Utc>?> {
    fun <Ok> serialize(
        opt: DateTime<Utc>?,
        serializer: Serializer<Ok>,
    ): SerdeResult<Ok> =
        if (opt != null) {
            serializer.serializeSome(I64Serialize(opt.timestamp()))
        } else {
            serializer.serializeNone()
        }

    override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<DateTime<Utc>?> =
        deserializer.deserializeOption(OptionSecondsTimestampVisitor)

    private object OptionSecondsTimestampVisitor : Visitor<DateTime<Utc>?> {
        override fun expecting(): String = "a unix timestamp in seconds or none"

        override fun <D : Deserializer> visitSome(deserializer: D): SerdeResult<DateTime<Utc>?> =
            deserializer.deserializeI64(SecondsTimestampVisitor)

        override fun visitNone(): SerdeResult<DateTime<Utc>?> = SerdeResult.success(null)

        override fun visitUnit(): SerdeResult<DateTime<Utc>?> = SerdeResult.success(null)
    }
}
