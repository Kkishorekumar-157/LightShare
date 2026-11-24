package com.lightshare.domain.decoder

import java.io.Closeable

/** Where recovered blocks live. Production: a temp file. Tests: memory. */
interface BlockStore : Closeable {
    fun write(index: Int, data: ByteArray)
    /** Fills [into] with block [index]; bytes past end-of-store are zero. */
    fun read(index: Int, into: ByteArray)
    /** Trims padding so the store is exactly [fileSize] bytes; returns the resulting length. */
    fun finalizeLength(fileSize: Long): Long
}

class MemoryBlockStore(blockCount: Int, private val blockSize: Int) : BlockStore {
    private val blocks = arrayOfNulls<ByteArray>(blockCount)
    override fun write(index: Int, data: ByteArray) { blocks[index] = data.copyOf(blockSize) }
    override fun read(index: Int, into: ByteArray) {
        val b = blocks[index]
        if (b == null) java.util.Arrays.fill(into, 0, blockSize, 0) else System.arraycopy(b, 0, into, 0, blockSize)
    }
    override fun finalizeLength(fileSize: Long): Long = fileSize
    override fun close() {}
    fun toByteArray(fileSize: Long): ByteArray {
        val out = ByteArray(fileSize.toInt())
        for (i in blocks.indices) {
            val b = blocks[i] ?: continue
            val start = i * blockSize
            System.arraycopy(b, 0, out, start, minOf(blockSize, out.size - start))
        }
        return out
    }
}
