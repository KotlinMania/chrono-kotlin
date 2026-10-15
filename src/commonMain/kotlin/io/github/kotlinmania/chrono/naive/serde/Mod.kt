// port-lint: source naive/datetime/serde.rs
package io.github.kotlinmania.chrono.naive.serde

import io.github.kotlinmania.chrono.DateTime
import io.github.kotlinmania.chrono.naive.NaiveDateTime
import io.github.kotlinmania.chrono.serde.invalidTs
import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.ser.Serialize
import io.github.kotlinmania.serdecore.ser.Serializer

private class I64Serialize(
    private val value: Long,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serializer.serializeI64(value)
}

internal object SecondsTimestampVisitor : Visitor<NaiveDateTime> {
    override fun expecting(): String = "a unix timestamp"

    override fun visitI64(v: Long): SerdeResult<NaiveDateTime> {
        val dt = DateTime.fromTimestampSecs(v)?.naiveUtc() ?: return SerdeResult.failure(invalidTs(v))
        return SerdeResult.success(dt)
    }

    override fun visitU64(v: ULong): SerdeResult<NaiveDateTime> {
        if (v > Long.MAX_VALUE.toULong()) {
            return SerdeResult.failure(invalidTs(v))
        }
        val dt =
            DateTime.fromTimestampSecs(v.toLong())?.naiveUtc()
                ?: return SerdeResult.failure(invalidTs(v))
        return SerdeResult.success(dt)
    }
}

internal object MilliSecondsTimestampVisitor : Visitor<NaiveDateTime> {
    override fun expecting(): String = "a unix timestamp"

    override fun visitI64(v: Long): SerdeResult<NaiveDateTime> {
        val dt =
            DateTime.fromTimestampMillis(v)?.naiveUtc()
                ?: return SerdeResult.failure(invalidTs(v))
        return SerdeResult.success(dt)
    }

    override fun visitU64(v: ULong): SerdeResult<NaiveDateTime> {
        val secs = (v / 1_000uL).toLong()
        val nsecs = ((v % 1_000uL) * 1_000_000uL).toUInt()
        val dt =
            DateTime.fromTimestamp(secs, nsecs)?.naiveUtc()
                ?: return SerdeResult.failure(invalidTs(v))
        return SerdeResult.success(dt)
    }
}

internal object MicroSecondsTimestampVisitor : Visitor<NaiveDateTime> {
    override fun expecting(): String = "a unix timestamp"

    override fun visitI64(v: Long): SerdeResult<NaiveDateTime> {
        val dt =
            DateTime.fromTimestampMicros(v)?.naiveUtc()
                ?: return SerdeResult.failure(invalidTs(v))
        return SerdeResult.success(dt)
    }

    override fun visitU64(v: ULong): SerdeResult<NaiveDateTime> {
        val secs = (v / 1_000_000uL).toLong()
        val nsecs = ((v % 1_000_000uL) * 1_000uL).toUInt()
        val dt =
            DateTime.fromTimestamp(secs, nsecs)?.naiveUtc()
                ?: return SerdeResult.failure(invalidTs(v))
        return SerdeResult.success(dt)
    }
}

internal object NanoSecondsTimestampVisitor : Visitor<NaiveDateTime> {
    override fun expecting(): String = "a unix timestamp"

    override fun visitI64(v: Long): SerdeResult<NaiveDateTime> {
        val secs = v.floorDiv(1_000_000_000L)
        val nsecs = v.mod(1_000_000_000L).toUInt()
        val dt =
            DateTime.fromTimestamp(secs, nsecs)?.naiveUtc()
                ?: return SerdeResult.failure(invalidTs(v))
        return SerdeResult.success(dt)
    }

    override fun visitU64(v: ULong): SerdeResult<NaiveDateTime> {
        val secs = (v / 1_000_000_000uL).toLong()
        val nsecs = (v % 1_000_000_000uL).toUInt()
        val dt =
            DateTime.fromTimestamp(secs, nsecs)?.naiveUtc()
                ?: return SerdeResult.failure(invalidTs(v))
        return SerdeResult.success(dt)
    }
}

/** Serialize/deserialize [NaiveDateTime] to/from nanosecond-precision timestamps. */
object TsNanoseconds : Deserialize<NaiveDateTime> {
    fun <Ok> serialize(
        dt: NaiveDateTime,
        serializer: Serializer<Ok>,
    ): SerdeResult<Ok> {
        val nanos =
            dt.andUtc().timestampNanosOpt()
                ?: return SerdeResult.failure(
                    SerdeError.custom("value out of range for a timestamp with nanosecond precision"),
                )
        return serializer.serializeI64(nanos)
    }

    override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<NaiveDateTime> =
        deserializer.deserializeI64(NanoSecondsTimestampVisitor)
}

/** Serialize/deserialize optional [NaiveDateTime] to/from nanosecond-precision timestamps. */
object TsNanosecondsOption : Deserialize<NaiveDateTime?> {
    fun <Ok> serialize(
        opt: NaiveDateTime?,
        serializer: Serializer<Ok>,
    ): SerdeResult<Ok> =
        if (opt != null) {
            val nanos =
                opt.andUtc().timestampNanosOpt()
                    ?: return SerdeResult.failure(
                        SerdeError.custom("value out of range for a timestamp with nanosecond precision"),
                    )
            serializer.serializeSome(I64Serialize(nanos))
        } else {
            serializer.serializeNone()
        }

    override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<NaiveDateTime?> =
        deserializer.deserializeOption(OptionNanoSecondsTimestampVisitor)

    private object OptionNanoSecondsTimestampVisitor : Visitor<NaiveDateTime?> {
        override fun expecting(): String = "a unix timestamp in nanoseconds or none"

        override fun <D : Deserializer> visitSome(deserializer: D): SerdeResult<NaiveDateTime?> =
            deserializer.deserializeI64(NanoSecondsTimestampVisitor)

        override fun visitNone(): SerdeResult<NaiveDateTime?> = SerdeResult.success(null)

        override fun visitUnit(): SerdeResult<NaiveDateTime?> = SerdeResult.success(null)
    }
}

/** Serialize/deserialize [NaiveDateTime] to/from microsecond-precision timestamps. */
object TsMicroseconds : Deserialize<NaiveDateTime> {
    fun <Ok> serialize(
        dt: NaiveDateTime,
        serializer: Serializer<Ok>,
    ): SerdeResult<Ok> = serializer.serializeI64(dt.andUtc().timestampMicros())

    override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<NaiveDateTime> =
        deserializer.deserializeI64(MicroSecondsTimestampVisitor)
}

/** Serialize/deserialize optional [NaiveDateTime] to/from microsecond-precision timestamps. */
object TsMicrosecondsOption : Deserialize<NaiveDateTime?> {
    fun <Ok> serialize(
        opt: NaiveDateTime?,
        serializer: Serializer<Ok>,
    ): SerdeResult<Ok> =
        if (opt != null) {
            serializer.serializeSome(I64Serialize(opt.andUtc().timestampMicros()))
        } else {
            serializer.serializeNone()
        }

    override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<NaiveDateTime?> =
        deserializer.deserializeOption(OptionMicroSecondsTimestampVisitor)

    private object OptionMicroSecondsTimestampVisitor : Visitor<NaiveDateTime?> {
        override fun expecting(): String = "a unix timestamp in microseconds or none"

        override fun <D : Deserializer> visitSome(deserializer: D): SerdeResult<NaiveDateTime?> =
            deserializer.deserializeI64(MicroSecondsTimestampVisitor)

        override fun visitNone(): SerdeResult<NaiveDateTime?> = SerdeResult.success(null)

        override fun visitUnit(): SerdeResult<NaiveDateTime?> = SerdeResult.success(null)
    }
}

/** Serialize/deserialize [NaiveDateTime] to/from millisecond-precision timestamps. */
object TsMilliseconds : Deserialize<NaiveDateTime> {
    fun <Ok> serialize(
        dt: NaiveDateTime,
        serializer: Serializer<Ok>,
    ): SerdeResult<Ok> = serializer.serializeI64(dt.andUtc().timestampMillis())

    override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<NaiveDateTime> =
        deserializer.deserializeI64(MilliSecondsTimestampVisitor)
}

/** Serialize/deserialize optional [NaiveDateTime] to/from millisecond-precision timestamps. */
object TsMillisecondsOption : Deserialize<NaiveDateTime?> {
    fun <Ok> serialize(
        opt: NaiveDateTime?,
        serializer: Serializer<Ok>,
    ): SerdeResult<Ok> =
        if (opt != null) {
            serializer.serializeSome(I64Serialize(opt.andUtc().timestampMillis()))
        } else {
            serializer.serializeNone()
        }

    override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<NaiveDateTime?> =
        deserializer.deserializeOption(OptionMilliSecondsTimestampVisitor)

    private object OptionMilliSecondsTimestampVisitor : Visitor<NaiveDateTime?> {
        override fun expecting(): String = "a unix timestamp in milliseconds or none"

        override fun <D : Deserializer> visitSome(deserializer: D): SerdeResult<NaiveDateTime?> =
            deserializer.deserializeI64(MilliSecondsTimestampVisitor)

        override fun visitNone(): SerdeResult<NaiveDateTime?> = SerdeResult.success(null)

        override fun visitUnit(): SerdeResult<NaiveDateTime?> = SerdeResult.success(null)
    }
}

/** Serialize/deserialize [NaiveDateTime] to/from second-precision timestamps. */
object TsSeconds : Deserialize<NaiveDateTime> {
    fun <Ok> serialize(
        dt: NaiveDateTime,
        serializer: Serializer<Ok>,
    ): SerdeResult<Ok> = serializer.serializeI64(dt.andUtc().timestamp())

    override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<NaiveDateTime> =
        deserializer.deserializeI64(SecondsTimestampVisitor)
}

/** Serialize/deserialize optional [NaiveDateTime] to/from second-precision timestamps. */
object TsSecondsOption : Deserialize<NaiveDateTime?> {
    fun <Ok> serialize(
        opt: NaiveDateTime?,
        serializer: Serializer<Ok>,
    ): SerdeResult<Ok> =
        if (opt != null) {
            serializer.serializeSome(I64Serialize(opt.andUtc().timestamp()))
        } else {
            serializer.serializeNone()
        }

    override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<NaiveDateTime?> =
        deserializer.deserializeOption(OptionSecondsTimestampVisitor)

    private object OptionSecondsTimestampVisitor : Visitor<NaiveDateTime?> {
        override fun expecting(): String = "a unix timestamp in seconds or none"

        override fun <D : Deserializer> visitSome(deserializer: D): SerdeResult<NaiveDateTime?> =
            deserializer.deserializeI64(SecondsTimestampVisitor)

        override fun visitNone(): SerdeResult<NaiveDateTime?> = SerdeResult.success(null)

        override fun visitUnit(): SerdeResult<NaiveDateTime?> = SerdeResult.success(null)
    }
}
