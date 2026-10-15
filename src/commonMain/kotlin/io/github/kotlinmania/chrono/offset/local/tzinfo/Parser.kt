// port-lint: source offset/local/tz_info/parser.rs
package io.github.kotlinmania.chrono.offset.local.tzinfo

internal fun parse(bytes: ByteArray): TzResult<TimeZone> {
    val cursor = Cursor.new(bytes)
    val firstState =
        when (val res = State.new(cursor, true)) {
            is TzResult.Ok -> res.value
            is TzResult.Err -> return res
        }
    val (state, footer) =
        when (firstState.header.version) {
            Version.V1 -> {
                if (cursor.isEmpty()) {
                    firstState to null
                } else {
                    return TzResult.Err(Error.InvalidTzFile("remaining data after end of TZif v1 data block"))
                }
            }

            Version.V2, Version.V3 -> {
                val secondState =
                    when (val res = State.new(cursor, false)) {
                        is TzResult.Ok -> res.value
                        is TzResult.Err -> return res
                    }
                secondState to cursor.remaining()
            }
        }

    val transitions = ArrayList<Transition>(state.header.transitionCount)
    var transIdx = 0
    while (transIdx < state.header.transitionCount) {
        val timeStart = transIdx * state.timeSize
        val arrTime = state.transitionTimes.copyOfRange(timeStart, timeStart + state.timeSize)
        val unixLeapTime =
            when (val res = state.parseTime(arrTime, state.header.version)) {
                is TzResult.Ok -> res.value
                is TzResult.Err -> return res
            }
        val localTimeTypeIndex = state.transitionTypes[transIdx].toInt() and 0xFF
        transitions.add(Transition.new(unixLeapTime, localTimeTypeIndex))
        transIdx += 1
    }

    val localTimeTypes = ArrayList<LocalTimeType>(state.header.typeCount)
    var typeIdx = 0
    while (typeIdx < state.header.typeCount) {
        val base = typeIdx * 6
        val utOffset =
            when (val res = readBeI32(state.localTimeTypes.copyOfRange(base, base + 4))) {
                is TzResult.Ok -> res.value
                is TzResult.Err -> return res
            }

        val isDst =
            when (state.localTimeTypes[base + 4].toInt() and 0xFF) {
                0 -> false
                1 -> true
                else -> return TzResult.Err(Error.InvalidTzFile("invalid DST indicator"))
            }

        val charIndex = state.localTimeTypes[base + 5].toInt() and 0xFF
        if (charIndex >= state.header.charCount) {
            return TzResult.Err(Error.InvalidTzFile("invalid time zone name char index"))
        }

        var nullPos = -1
        for (i in charIndex until state.names.size) {
            if (state.names[i] == 0.toByte()) {
                nullPos = i - charIndex
                break
            }
        }
        if (nullPos < 0) {
            return TzResult.Err(Error.InvalidTzFile("invalid time zone name char index"))
        }

        val nameBytes = state.names.copyOfRange(charIndex, charIndex + nullPos)
        val name = if (nameBytes.isNotEmpty()) nameBytes else null
        val ltt =
            when (val res = LocalTimeType.new(utOffset, isDst, name)) {
                is TzResult.Ok -> res.value
                is TzResult.Err -> return res
            }
        localTimeTypes.add(ltt)
        typeIdx += 1
    }

    val leapSeconds = ArrayList<LeapSecond>(state.header.leapCount)
    val leapRecordSize = state.timeSize + 4
    var leapIdx = 0
    while (leapIdx < state.header.leapCount) {
        val base = leapIdx * leapRecordSize
        val unixLeapTime =
            when (
                val res =
                    state.parseTime(
                        state.leapSeconds.copyOfRange(base, base + state.timeSize),
                        state.header.version,
                    )
            ) {
                is TzResult.Ok -> res.value
                is TzResult.Err -> return res
            }
        val correction =
            when (
                val res =
                    readBeI32(
                        state.leapSeconds.copyOfRange(base + state.timeSize, base + leapRecordSize),
                    )
            ) {
                is TzResult.Ok -> res.value
                is TzResult.Err -> return res
            }
        leapSeconds.add(LeapSecond.new(unixLeapTime, correction))
        leapIdx += 1
    }

    for (i in 0 until state.header.typeCount) {
        val stdWall = if (i < state.stdWalls.size) state.stdWalls[i].toInt() and 0xFF else 0
        val utLocal = if (i < state.utLocals.size) state.utLocals[i].toInt() and 0xFF else 0
        if (stdWall == 0 && utLocal == 1) {
            return TzResult.Err(
                Error.InvalidTzFile("invalid couple of standard/wall and UT/local indicators"),
            )
        }
    }

    val extraRule =
        if (footer != null) {
            val footerStr =
                when (val res = decodeUtf8(footer)) {
                    is TzResult.Ok -> res.value
                    is TzResult.Err -> return res
                }
            if (!(footerStr.startsWith('\n') && footerStr.endsWith('\n'))) {
                return TzResult.Err(Error.InvalidTzFile("invalid footer"))
            }

            val tzString = footerStr.trim { c -> c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\u000C' }
            if (tzString.startsWith(':') || tzString.contains('\u0000')) {
                return TzResult.Err(Error.InvalidTzFile("invalid footer"))
            }

            if (tzString.isEmpty()) {
                null
            } else {
                when (
                    val res =
                        TransitionRule.fromTzString(
                            tzString.encodeToByteArray(),
                            state.header.version == Version.V3,
                        )
                ) {
                    is TzResult.Ok -> res.value
                    is TzResult.Err -> return res
                }
            }
        } else {
            null
        }

    return TimeZone.new(transitions, localTimeTypes, leapSeconds, extraRule)
}

/** TZif data blocks */
private class State(
    val header: Header,
    val timeSize: Int,
    val transitionTimes: ByteArray,
    val transitionTypes: ByteArray,
    val localTimeTypes: ByteArray,
    val names: ByteArray,
    val leapSeconds: ByteArray,
    val stdWalls: ByteArray,
    val utLocals: ByteArray,
) {
    fun parseTime(arr: ByteArray, version: Version): TzResult<Long> =
        when (version) {
            Version.V1 -> readBeI32(arr.copyOfRange(0, 4)).map { it.toLong() }
            Version.V2, Version.V3 -> readBeI64(arr)
        }

    companion object {
        fun new(cursor: Cursor, first: Boolean): TzResult<State> {
            val header =
                when (val res = Header.new(cursor)) {
                    is TzResult.Ok -> res.value
                    is TzResult.Err -> return res
                }
            val timeSize = if (first) 4 else 8

            val transitionTimes =
                when (val res = cursor.readExact(header.transitionCount * timeSize)) {
                    is TzResult.Ok -> res.value
                    is TzResult.Err -> return res
                }
            val transitionTypes =
                when (val res = cursor.readExact(header.transitionCount)) {
                    is TzResult.Ok -> res.value
                    is TzResult.Err -> return res
                }
            val localTimeTypes =
                when (val res = cursor.readExact(header.typeCount * 6)) {
                    is TzResult.Ok -> res.value
                    is TzResult.Err -> return res
                }
            val names =
                when (val res = cursor.readExact(header.charCount)) {
                    is TzResult.Ok -> res.value
                    is TzResult.Err -> return res
                }
            val leapSeconds =
                when (val res = cursor.readExact(header.leapCount * (timeSize + 4))) {
                    is TzResult.Ok -> res.value
                    is TzResult.Err -> return res
                }
            val stdWalls =
                when (val res = cursor.readExact(header.stdWallCount)) {
                    is TzResult.Ok -> res.value
                    is TzResult.Err -> return res
                }
            val utLocals =
                when (val res = cursor.readExact(header.utLocalCount)) {
                    is TzResult.Ok -> res.value
                    is TzResult.Err -> return res
                }

            return TzResult.Ok(
                State(
                    header = header,
                    timeSize = timeSize,
                    transitionTimes = transitionTimes,
                    transitionTypes = transitionTypes,
                    localTimeTypes = localTimeTypes,
                    names = names,
                    leapSeconds = leapSeconds,
                    stdWalls = stdWalls,
                    utLocals = utLocals,
                ),
            )
        }
    }
}

/** TZif header */
private class Header(
    val version: Version,
    val utLocalCount: Int,
    val stdWallCount: Int,
    val leapCount: Int,
    val transitionCount: Int,
    val typeCount: Int,
    val charCount: Int,
) {
    companion object {
        fun new(cursor: Cursor): TzResult<Header> {
            val magic =
                when (val res = cursor.readExact(4)) {
                    is TzResult.Ok -> res.value
                    is TzResult.Err -> return res
                }
            if (!magic.contentEquals(byteArrayOf('T'.code.toByte(), 'Z'.code.toByte(), 'i'.code.toByte(), 'f'.code.toByte()))) {
                return TzResult.Err(Error.InvalidTzFile("invalid magic number"))
            }

            val versionByte =
                when (val res = cursor.readExact(1)) {
                    is TzResult.Ok -> res.value
                    is TzResult.Err -> return res
                }
            val version =
                when (versionByte[0].toInt() and 0xFF) {
                    0x00 -> Version.V1
                    0x32 -> Version.V2
                    0x33 -> Version.V3
                    else -> return TzResult.Err(Error.UnsupportedTzFile("unsupported TZif version"))
                }

            when (val res = cursor.readExact(15)) {
                is TzResult.Ok -> Unit
                is TzResult.Err -> return res
            }

            val utLocalCount =
                when (val res = cursor.readBeU32()) {
                    is TzResult.Ok -> res.value
                    is TzResult.Err -> return res
                }
            val stdWallCount =
                when (val res = cursor.readBeU32()) {
                    is TzResult.Ok -> res.value
                    is TzResult.Err -> return res
                }
            val leapCount =
                when (val res = cursor.readBeU32()) {
                    is TzResult.Ok -> res.value
                    is TzResult.Err -> return res
                }
            val transitionCount =
                when (val res = cursor.readBeU32()) {
                    is TzResult.Ok -> res.value
                    is TzResult.Err -> return res
                }
            val typeCount =
                when (val res = cursor.readBeU32()) {
                    is TzResult.Ok -> res.value
                    is TzResult.Err -> return res
                }
            val charCount =
                when (val res = cursor.readBeU32()) {
                    is TzResult.Ok -> res.value
                    is TzResult.Err -> return res
                }

            if (!(
                    typeCount != 0u &&
                        charCount != 0u &&
                        (utLocalCount == 0u || utLocalCount == typeCount) &&
                        (stdWallCount == 0u || stdWallCount == typeCount)
                )
            ) {
                return TzResult.Err(Error.InvalidTzFile("invalid header"))
            }

            return TzResult.Ok(
                Header(
                    version = version,
                    utLocalCount = utLocalCount.toInt(),
                    stdWallCount = stdWallCount.toInt(),
                    leapCount = leapCount.toInt(),
                    transitionCount = transitionCount.toInt(),
                    typeCount = typeCount.toInt(),
                    charCount = charCount.toInt(),
                ),
            )
        }
    }
}

/**
 * A `Cursor` contains a slice of a buffer and a read count.
 */
internal class Cursor private constructor(
    private val buffer: ByteArray,
    private var offset: Int,
    private var readCount: Int,
) {
    fun peek(): Byte? = if (offset < buffer.size) buffer[offset] else null

    fun remaining(): ByteArray = buffer.copyOfRange(offset, buffer.size)

    fun isEmpty(): Boolean = offset >= buffer.size

    fun readBeU32(): TzResult<UInt> {
        val bytes =
            when (val res = readExact(4)) {
                is TzResult.Ok -> res.value
                is TzResult.Err -> return res
            }
        val b0 = (bytes[0].toInt() and 0xFF).toUInt()
        val b1 = (bytes[1].toInt() and 0xFF).toUInt()
        val b2 = (bytes[2].toInt() and 0xFF).toUInt()
        val b3 = (bytes[3].toInt() and 0xFF).toUInt()
        return TzResult.Ok((b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3)
    }

    fun readExact(count: Int): TzResult<ByteArray> {
        if (count < 0 || offset + count > buffer.size) {
            return TzResult.Err(Error.Io("unexpected end of file"))
        }
        val result = buffer.copyOfRange(offset, offset + count)
        offset += count
        readCount += count
        return TzResult.Ok(result)
    }

    fun readTag(tag: ByteArray): TzResult<Unit> {
        val bytes =
            when (val res = readExact(tag.size)) {
                is TzResult.Ok -> res.value
                is TzResult.Err -> return res
            }
        return if (bytes.contentEquals(tag)) {
            TzResult.Ok(Unit)
        } else {
            TzResult.Err(Error.Io("invalid data"))
        }
    }

    fun readOptionalTag(tag: ByteArray): TzResult<Boolean> {
        if (offset + tag.size <= buffer.size) {
            var matches = true
            for (i in tag.indices) {
                if (buffer[offset + i] != tag[i]) {
                    matches = false
                    break
                }
            }
            if (matches) {
                readExact(tag.size)
                return TzResult.Ok(true)
            }
        }
        return TzResult.Ok(false)
    }

    fun readWhile(predicate: (Byte) -> Boolean): TzResult<ByteArray> {
        var pos = offset
        while (pos < buffer.size && predicate(buffer[pos])) {
            pos += 1
        }
        return readExact(pos - offset)
    }

    fun readInt(): TzResult<Int> {
        val bytes =
            when (val res = readWhile { b -> b in '0'.code.toByte()..'9'.code.toByte() }) {
                is TzResult.Ok -> res.value
                is TzResult.Err -> return res
            }
        val str =
            when (val res = decodeUtf8(bytes)) {
                is TzResult.Ok -> res.value
                is TzResult.Err -> return res
            }
        val parsed = str.toIntOrNull() ?: return TzResult.Err(Error.ParseInt("invalid integer"))
        return TzResult.Ok(parsed)
    }

    fun readU16(): TzResult<UShort> {
        val bytes =
            when (val res = readWhile { b -> b in '0'.code.toByte()..'9'.code.toByte() }) {
                is TzResult.Ok -> res.value
                is TzResult.Err -> return res
            }
        val str =
            when (val res = decodeUtf8(bytes)) {
                is TzResult.Ok -> res.value
                is TzResult.Err -> return res
            }
        val parsed = str.toUShortOrNull() ?: return TzResult.Err(Error.ParseInt("invalid integer"))
        return TzResult.Ok(parsed)
    }

    fun readU8(): TzResult<UByte> {
        val bytes =
            when (val res = readWhile { b -> b in '0'.code.toByte()..'9'.code.toByte() }) {
                is TzResult.Ok -> res.value
                is TzResult.Err -> return res
            }
        val str =
            when (val res = decodeUtf8(bytes)) {
                is TzResult.Ok -> res.value
                is TzResult.Err -> return res
            }
        val parsed = str.toUByteOrNull() ?: return TzResult.Err(Error.ParseInt("invalid integer"))
        return TzResult.Ok(parsed)
    }

    fun readUntil(predicate: (Byte) -> Boolean): TzResult<ByteArray> {
        var pos = offset
        while (pos < buffer.size && !predicate(buffer[pos])) {
            pos += 1
        }
        return readExact(pos - offset)
    }

    companion object {
        fun new(remaining: ByteArray): Cursor = Cursor(remaining, 0, 0)
    }
}

internal fun readBeI32(bytes: ByteArray): TzResult<Int> {
    if (bytes.size != 4) {
        return TzResult.Err(Error.InvalidSlice("too short for i32"))
    }
    val b0 = bytes[0].toInt() and 0xFF
    val b1 = bytes[1].toInt() and 0xFF
    val b2 = bytes[2].toInt() and 0xFF
    val b3 = bytes[3].toInt() and 0xFF
    return TzResult.Ok((b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3)
}

internal fun readBeI64(bytes: ByteArray): TzResult<Long> {
    if (bytes.size != 8) {
        return TzResult.Err(Error.InvalidSlice("too short for i64"))
    }
    val b0 = (bytes[0].toInt() and 0xFF).toLong()
    val b1 = (bytes[1].toInt() and 0xFF).toLong()
    val b2 = (bytes[2].toInt() and 0xFF).toLong()
    val b3 = (bytes[3].toInt() and 0xFF).toLong()
    val b4 = (bytes[4].toInt() and 0xFF).toLong()
    val b5 = (bytes[5].toInt() and 0xFF).toLong()
    val b6 = (bytes[6].toInt() and 0xFF).toLong()
    val b7 = (bytes[7].toInt() and 0xFF).toLong()
    return TzResult.Ok(
        (b0 shl 56) or
            (b1 shl 48) or
            (b2 shl 40) or
            (b3 shl 32) or
            (b4 shl 24) or
            (b5 shl 16) or
            (b6 shl 8) or
            b7,
    )
}

/** TZif version */
private enum class Version {
    /** Version 1 */
    V1,

    /** Version 2 */
    V2,

    /** Version 3 */
    V3,
}
