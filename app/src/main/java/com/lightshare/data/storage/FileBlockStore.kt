package com.lightshare.data.storage

import com.lightshare.domain.decoder.BlockStore
import java.io.File
import java.io.RandomAccessFile

/** Temp-file backed block store: recovered blocks never accumulate in RAM. */
class FileBlockStore(val file: File, private val blockSize: Int) : BlockStore {
    private val raf = RandomAccessFile(file, "rw").also { it.setLength(0) }

    override fun write(index: Int, data: ByteArray) {
        raf.seek(index.toLong() * blockSize)
        raf.write(data, 0, blockSize)
    }

    override fun read(index: Int, into: ByteArray) {
        raf.seek(index.toLong() * blockSize)
        var off = 0
        while (off < blockSize) {
            val n = raf.read(into, off, blockSize - off)
            if (n < 0) break
            off += n
        }
        if (off < blockSize) java.util.Arrays.fill(into, off, blockSize, 0)
    }

    override fun finalizeLength(fileSize: Long): Long {
        raf.setLength(fileSize)
        return raf.length()
    }

    override fun close() { runCatching { raf.close() } }
}
