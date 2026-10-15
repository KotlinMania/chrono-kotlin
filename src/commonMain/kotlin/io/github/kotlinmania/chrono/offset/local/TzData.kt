// port-lint: source offset/local/tz_data.rs
package io.github.kotlinmania.chrono.offset.local

import io.github.kotlinmania.chrono.offset.local.tzinfo.Cursor
import io.github.kotlinmania.chrono.offset.local.tzinfo.Error
import io.github.kotlinmania.chrono.offset.local.tzinfo.TzResult

/**
 * Get timezone data from the `tzdata` file of Android or HarmonyOS NEXT.
 */
internal fun forZone(tzString: String): TzResult<ByteArray?> {
    val androidFile = openAndroidTzDataFile()
    if (androidFile != null) {
        return findTzData(ANDROID_ENTRY_LEN, androidFile, tzString.encodeToByteArray())
    }
    val ohosFile = readTzFileBytes("/system/etc/zoneinfo/tzdata")
    if (ohosFile != null) {
        return findTzData(OHOS_ENTRY_LEN, ohosFile, tzString.encodeToByteArray())
    }
    return TzResult.Ok(null)
}

/**
 * Open the `tzdata` file of Android from the environment variables.
 */
private fun openAndroidTzDataFile(): ByteArray? {
    val locations =
        arrayOf(
            "ANDROID_DATA" to "/misc/zoneinfo",
            "ANDROID_ROOT" to "/usr/share/zoneinfo",
        )
    for ((envVar, path) in locations) {
        val envValue = LocalPlatform.getEnvVar(envVar)
        if (envValue != null) {
            val bytes = readTzFileBytes("$envValue$path/tzdata")
            if (bytes != null) {
                return bytes
            }
        }
    }
    return null
}

/**
 * Get timezone data from the `tzdata` file bytes.
 */
internal fun findTzData(
    entryLen: Int,
    fileBytes: ByteArray,
    tzName: ByteArray,
): TzResult<ByteArray?> {
    val cursor = Cursor.new(fileBytes)
    val header =
        when (val res = TzDataHeader.new(cursor)) {
            is TzResult.Ok -> res.value
            is TzResult.Err -> return res
        }
    val index =
        when (val res = TzDataIndexes.new(entryLen, cursor, header)) {
            is TzResult.Ok -> res.value
            is TzResult.Err -> return res
        }
    val entry = index.findTimezone(tzName) ?: return TzResult.Ok(null)
    return index.findTzdata(fileBytes, header, entry).map { it }
}

/**
 * Header of the `tzdata` file.
 */
internal class TzDataHeader(
    val version: ByteArray,
    val indexOffset: UInt,
    val dataOffset: UInt,
    val zonetabOffset: UInt,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TzDataHeader) return false
        return version.contentEquals(other.version) &&
            indexOffset == other.indexOffset &&
            dataOffset == other.dataOffset &&
            zonetabOffset == other.zonetabOffset
    }

    override fun hashCode(): Int {
        var result = version.contentHashCode()
        result = 31 * result + indexOffset.hashCode()
        result = 31 * result + dataOffset.hashCode()
        result = 31 * result + zonetabOffset.hashCode()
        return result
    }

    companion object {
        fun new(cursor: Cursor): TzResult<TzDataHeader> {
            val magic =
                when (val res = cursor.readExact(TZDATA_VERSION_LEN)) {
                    is TzResult.Ok -> res.value
                    is TzResult.Err -> return res
                }
            val prefix = "tzdata".encodeToByteArray()
            var startsWithPrefix = true
            for (i in prefix.indices) {
                if (magic[i] != prefix[i]) {
                    startsWithPrefix = false
                    break
                }
            }
            if (!startsWithPrefix || magic[TZDATA_VERSION_LEN - 1] != 0.toByte()) {
                return TzResult.Err(Error.Io("invalid tzdata header magic"))
            }
            val version = magic.copyOfRange(6, 11)
            val indexOffset =
                when (val res = cursor.readBeU32()) {
                    is TzResult.Ok -> res.value
                    is TzResult.Err -> return res
                }
            val dataOffset =
                when (val res = cursor.readBeU32()) {
                    is TzResult.Ok -> res.value
                    is TzResult.Err -> return res
                }
            val zonetabOffset =
                when (val res = cursor.readBeU32()) {
                    is TzResult.Ok -> res.value
                    is TzResult.Err -> return res
                }
            return TzResult.Ok(TzDataHeader(version, indexOffset, dataOffset, zonetabOffset))
        }
    }
}

/**
 * Indexes of the `tzdata` file.
 */
internal class TzDataIndexes(
    val indexes: List<TzDataIndex>,
) {
    /** Find a timezone by name. */
    fun findTimezone(timezone: ByteArray): TzDataIndex? {
        val idx = indexes.binarySearch { compareByteArrays(it.name, timezone) }
        return if (idx >= 0) indexes[idx] else null
    }

    /** Retrieve a chunk of timezone data by the index. */
    fun findTzdata(
        fileBytes: ByteArray,
        header: TzDataHeader,
        index: TzDataIndex,
    ): TzResult<ByteArray> {
        val startLong = index.offset.toLong() + header.dataOffset.toLong()
        val endLong = startLong + index.length.toLong()
        if (startLong < 0L || endLong > fileBytes.size.toLong()) {
            return TzResult.Err(Error.Io("unexpected end of file"))
        }
        return TzResult.Ok(fileBytes.copyOfRange(startLong.toInt(), endLong.toInt()))
    }

    companion object {
        fun new(entryLen: Int, cursor: Cursor, header: TzDataHeader): TzResult<TzDataIndexes> {
            val size =
                if (header.dataOffset >= header.indexOffset) {
                    (header.dataOffset - header.indexOffset).toInt()
                } else {
                    0
                }
            val buf =
                when (val res = cursor.readExact(size)) {
                    is TzResult.Ok -> res.value
                    is TzResult.Err -> return res
                }
            val list = ArrayList<TzDataIndex>()
            var pos = 0
            while (pos < buf.size) {
                val end = minOf(pos + entryLen, buf.size)
                val chunk = buf.copyOfRange(pos, end)
                if (chunk.size >= TZ_NAME_LEN + 8) {
                    val nameBytes = fromBytesUntilNul(chunk.copyOfRange(0, TZ_NAME_LEN))
                    if (nameBytes != null) {
                        val offset = readU32Be(chunk, TZ_NAME_LEN)
                        val length = readU32Be(chunk, TZ_NAME_LEN + 4)
                        list.add(TzDataIndex(nameBytes, offset, length))
                    }
                }
                pos += entryLen
            }
            return TzResult.Ok(TzDataIndexes(list))
        }
    }
}

/**
 * Index entry of the `tzdata` file.
 */
internal class TzDataIndex(
    val name: ByteArray,
    val offset: UInt,
    val length: UInt,
)

private fun fromBytesUntilNul(bytes: ByteArray): ByteArray? {
    var nulPos = -1
    for (i in bytes.indices) {
        if (bytes[i] == 0.toByte()) {
            nulPos = i
            break
        }
    }
    if (nulPos < 0) return null
    return bytes.copyOfRange(0, nulPos)
}

private fun readU32Be(bytes: ByteArray, offset: Int): UInt {
    val b0 = (bytes[offset].toInt() and 0xFF).toUInt()
    val b1 = (bytes[offset + 1].toInt() and 0xFF).toUInt()
    val b2 = (bytes[offset + 2].toInt() and 0xFF).toUInt()
    val b3 = (bytes[offset + 3].toInt() and 0xFF).toUInt()
    return (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
}

private fun compareByteArrays(a: ByteArray, b: ByteArray): Int {
    val minLen = minOf(a.size, b.size)
    for (i in 0 until minLen) {
        val cmp = (a[i].toInt() and 0xFF).compareTo(b[i].toInt() and 0xFF)
        if (cmp != 0) return cmp
    }
    return a.size.compareTo(b.size)
}

/** Ohos tzdata index entry size: `name + offset + length` */
internal const val OHOS_ENTRY_LEN: Int = 40 + 2 * 4

/** Android tzdata index entry size: `name + offset + length + raw_utc_offset(legacy)` */
internal const val ANDROID_ENTRY_LEN: Int = 40 + 3 * 4

/** The database reserves 40 bytes for each id. */
private const val TZ_NAME_LEN: Int = 40

/** Size of the version string in the header of `tzdata` file. */
private const val TZDATA_VERSION_LEN: Int = 12
