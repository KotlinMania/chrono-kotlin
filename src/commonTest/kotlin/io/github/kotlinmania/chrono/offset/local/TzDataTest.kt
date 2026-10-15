// port-lint: tests offset/local/tz_data.rs
package io.github.kotlinmania.chrono.offset.local

import io.github.kotlinmania.chrono.offset.local.tzinfo.Cursor
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TzDataTest {
    private fun loadTestFile(path: String): ByteArray? =
        readTzFileBytes(path) ?: readTzFileBytes("tmp/chrono/$path")

    private fun buildSyntheticTzData(version: String, entryLen: Int): ByteArray {
        val tz1Name = "Asia/Shanghai".encodeToByteArray()
        val tz1Payload = ByteArray(393) { (it % 251).toByte() }
        val tz2Name = "Pacific/Noumea".encodeToByteArray()
        val tz2Payload = ByteArray(180) { (it % 199).toByte() }

        val indexOffset = 24
        val indexSize = 2 * entryLen
        val dataOffset = indexOffset + indexSize
        val zonetabOffset = dataOffset + tz1Payload.size + tz2Payload.size
        val out = ByteArray(zonetabOffset)

        val headerStr = "tzdata$version\u0000".encodeToByteArray()
        headerStr.copyInto(out, 0)
        writeU32Be(out, 12, indexOffset.toUInt())
        writeU32Be(out, 16, dataOffset.toUInt())
        writeU32Be(out, 20, zonetabOffset.toUInt())

        tz1Name.copyInto(out, indexOffset)
        writeU32Be(out, indexOffset + 40, 0u)
        writeU32Be(out, indexOffset + 44, tz1Payload.size.toUInt())

        val secondEntryStart = indexOffset + entryLen
        tz2Name.copyInto(out, secondEntryStart)
        writeU32Be(out, secondEntryStart + 40, tz1Payload.size.toUInt())
        writeU32Be(out, secondEntryStart + 44, tz2Payload.size.toUInt())

        tz1Payload.copyInto(out, dataOffset)
        tz2Payload.copyInto(out, dataOffset + tz1Payload.size)
        return out
    }

    private fun writeU32Be(buf: ByteArray, offset: Int, value: UInt) {
        buf[offset] = ((value shr 24) and 0xFFu).toByte()
        buf[offset + 1] = ((value shr 16) and 0xFFu).toByte()
        buf[offset + 2] = ((value shr 8) and 0xFFu).toByte()
        buf[offset + 3] = (value and 0xFFu).toByte()
    }

    @Test
    fun testOhosTzdataHeaderAndIndex() {
        val fileBytes = loadTestFile("tests/ohos/tzdata")
        if (fileBytes != null) {
            val cursor = Cursor.new(fileBytes)
            val header = TzDataHeader.new(cursor).unwrap()
            assertContentEquals("2024a".encodeToByteArray(), header.version)
            assertEquals(24u, header.indexOffset)
            assertEquals(21240u, header.dataOffset)
            assertEquals(272428u, header.zonetabOffset)

            val iter = TzDataIndexes.new(OHOS_ENTRY_LEN, cursor, header).unwrap()
            assertEquals(442, iter.indexes.size)
            assertNotNull(iter.findTimezone("Asia/Shanghai".encodeToByteArray()))
            assertNotNull(iter.findTimezone("Pacific/Noumea".encodeToByteArray()))
        } else {
            val synthetic = buildSyntheticTzData("2024a", OHOS_ENTRY_LEN)
            val cursor = Cursor.new(synthetic)
            val header = TzDataHeader.new(cursor).unwrap()
            assertContentEquals("2024a".encodeToByteArray(), header.version)
            val iter = TzDataIndexes.new(OHOS_ENTRY_LEN, cursor, header).unwrap()
            assertNotNull(iter.findTimezone("Asia/Shanghai".encodeToByteArray()))
            assertNotNull(iter.findTimezone("Pacific/Noumea".encodeToByteArray()))
        }
    }

    @Test
    fun testOhosTzdataLoading() {
        val fileBytes = loadTestFile("tests/ohos/tzdata") ?: buildSyntheticTzData("2024a", OHOS_ENTRY_LEN)
        val cursor = Cursor.new(fileBytes)
        val header = TzDataHeader.new(cursor).unwrap()
        val iter = TzDataIndexes.new(OHOS_ENTRY_LEN, cursor, header).unwrap()
        val timezone = iter.findTimezone("Asia/Shanghai".encodeToByteArray())!!
        val tzdata = iter.findTzdata(fileBytes, header, timezone).unwrap()
        assertEquals(393, tzdata.size)
    }

    @Test
    fun testInvalidTzdataHeader() {
        val invalid = "tzdaaa2024aaaaaaaaaaaaaaa\u0000".encodeToByteArray()
        assertTrue(TzDataHeader.new(Cursor.new(invalid)).isErr)
    }

    @Test
    fun testAndroidTzdataHeaderAndIndex() {
        val fileBytes = loadTestFile("tests/android/tzdata")
        if (fileBytes != null) {
            val cursor = Cursor.new(fileBytes)
            val header = TzDataHeader.new(cursor).unwrap()
            assertContentEquals("2021a".encodeToByteArray(), header.version)
            assertEquals(24u, header.indexOffset)
            assertEquals(30860u, header.dataOffset)
            assertEquals(491837u, header.zonetabOffset)

            val iter = TzDataIndexes.new(ANDROID_ENTRY_LEN, cursor, header).unwrap()
            assertEquals(593, iter.indexes.size)
            assertNotNull(iter.findTimezone("Asia/Shanghai".encodeToByteArray()))
            assertNotNull(iter.findTimezone("Pacific/Noumea".encodeToByteArray()))
        } else {
            val synthetic = buildSyntheticTzData("2021a", ANDROID_ENTRY_LEN)
            val cursor = Cursor.new(synthetic)
            val header = TzDataHeader.new(cursor).unwrap()
            assertContentEquals("2021a".encodeToByteArray(), header.version)
            val iter = TzDataIndexes.new(ANDROID_ENTRY_LEN, cursor, header).unwrap()
            assertNotNull(iter.findTimezone("Asia/Shanghai".encodeToByteArray()))
            assertNotNull(iter.findTimezone("Pacific/Noumea".encodeToByteArray()))
        }
    }

    @Test
    fun testAndroidTzdataLoading() {
        val fileBytes = loadTestFile("tests/android/tzdata")
        if (fileBytes != null) {
            val cursor = Cursor.new(fileBytes)
            val header = TzDataHeader.new(cursor).unwrap()
            val iter = TzDataIndexes.new(ANDROID_ENTRY_LEN, cursor, header).unwrap()
            val timezone = iter.findTimezone("Asia/Shanghai".encodeToByteArray())!!
            val tzdata = iter.findTzdata(fileBytes, header, timezone).unwrap()
            assertEquals(573, tzdata.size)
        } else {
            val synthetic = buildSyntheticTzData("2021a", ANDROID_ENTRY_LEN)
            val cursor = Cursor.new(synthetic)
            val header = TzDataHeader.new(cursor).unwrap()
            val iter = TzDataIndexes.new(ANDROID_ENTRY_LEN, cursor, header).unwrap()
            val timezone = iter.findTimezone("Asia/Shanghai".encodeToByteArray())!!
            val tzdata = iter.findTzdata(synthetic, header, timezone).unwrap()
            assertEquals(393, tzdata.size)
        }
    }

    @Test
    fun testOhosTzdataFind() {
        val fileBytes = loadTestFile("tests/ohos/tzdata") ?: buildSyntheticTzData("2024a", OHOS_ENTRY_LEN)
        val tzdata = findTzData(OHOS_ENTRY_LEN, fileBytes, "Asia/Shanghai".encodeToByteArray()).unwrap()!!
        assertEquals(393, tzdata.size)
    }

    @Test
    fun testOhosTzdataFindMissing() {
        val fileBytes = loadTestFile("tests/ohos/tzdata") ?: buildSyntheticTzData("2024a", OHOS_ENTRY_LEN)
        assertNull(findTzData(OHOS_ENTRY_LEN, fileBytes, "Asia/Sjasdfai".encodeToByteArray()).unwrap())
    }

    @Test
    fun testAndroidTzdataFind() {
        val fileBytes = loadTestFile("tests/android/tzdata")
        if (fileBytes != null) {
            val tzdata = findTzData(ANDROID_ENTRY_LEN, fileBytes, "Asia/Shanghai".encodeToByteArray()).unwrap()!!
            assertEquals(573, tzdata.size)
        } else {
            val synthetic = buildSyntheticTzData("2021a", ANDROID_ENTRY_LEN)
            val tzdata = findTzData(ANDROID_ENTRY_LEN, synthetic, "Asia/Shanghai".encodeToByteArray()).unwrap()!!
            assertEquals(393, tzdata.size)
        }
    }

    @Test
    fun testAndroidTzdataFindMissing() {
        val fileBytes = loadTestFile("tests/android/tzdata") ?: buildSyntheticTzData("2021a", ANDROID_ENTRY_LEN)
        assertNull(findTzData(ANDROID_ENTRY_LEN, fileBytes, "Asia/S000000i".encodeToByteArray()).unwrap())
    }
}
