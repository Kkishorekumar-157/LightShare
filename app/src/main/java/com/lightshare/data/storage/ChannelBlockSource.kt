package com.lightshare.data.storage

import com.lightshare.domain.encoder.BlockSource
import com.lightshare.domain.encoder.blockCountFor
import java.io.Closeable
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

class FileHandle(val channel: FileChannel, private val onClose: () -> Unit) : Closeable {
    override fun close() { runCatching { channel.close() }; runCatching(onClose) }
}

/** Positional reads from a FileChannel: constant memory, any block order. */
class ChannelBlockSource(
    private val handle: FileHandle,
    override val fileSize: Long,
    override val blockSize: Int
) : BlockSource {
    override val blockCount: Int = blockCountFor(fileSize, blockSize)

    override fun readBlock(index: Int, into: ByteArray) {
        require(index in 0 until blockCount)
        val pos = index.toLong() * blockSize
        val want = minOf(blockSize.toLong(), fileSize - pos).toInt()
        val bb = ByteBuffer.wrap(into, 0, want)
        var p = pos
        while (bb.hasRemaining()) {
            val n = handle.channel.read(bb, p)
            if (n < 0) throw IOException("Unexpected end of file at $p")
            p += n
        }
        if (want < blockSize) java.util.Arrays.fill(into, want, blockSize, 0)
    }

    override fun close() = handle.close()
}
