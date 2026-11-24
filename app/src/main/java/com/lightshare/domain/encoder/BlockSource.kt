package com.lightshare.domain.encoder

import java.io.Closeable

/** Random-access, fixed-size view of the file. The last block is zero-padded. Never holds the whole file. */
interface BlockSource : Closeable {
    val fileSize: Long
    val blockSize: Int
    val blockCount: Int
    /** Fills [into] (size >= blockSize) with block [index], zero-padded. */
    fun readBlock(index: Int, into: ByteArray)
}

fun blockCountFor(fileSize: Long, blockSize: Int): Int {
    require(fileSize > 0) { "file is empty" }
    val k = (fileSize + blockSize - 1) / blockSize
    require(k <= com.lightshare.domain.protocol.PacketCodec.MAX_BLOCK_COUNT) { "file too large for this block size" }
    return k.toInt()
}

/** In-memory source, used by tests. */
class ByteArrayBlockSource(private val data: ByteArray, override val blockSize: Int) : BlockSource {
    override val fileSize: Long = data.size.toLong()
    override val blockCount: Int = blockCountFor(fileSize, blockSize)
    override fun readBlock(index: Int, into: ByteArray) {
        require(index in 0 until blockCount)
        val start = index * blockSize
        val n = minOf(blockSize, data.size - start)
        System.arraycopy(data, start, into, 0, n)
        if (n < blockSize) java.util.Arrays.fill(into, n, blockSize, 0)
    }
    override fun close() {}
}
