package com.lightshare.data.storage

import com.lightshare.domain.encoder.BlockSource
import java.io.File

/** A file that has been hashed and is ready to be turned into a BlockSource for any block size. */
class PreparedFile(
    val name: String,
    val mimeType: String,
    val size: Long,
    val sha256: ByteArray,
    private val opener: () -> FileHandle,
    private val cacheCopy: File?
) {
    fun openSource(blockSize: Int): BlockSource = ChannelBlockSource(opener(), size, blockSize)
    fun release() { cacheCopy?.delete() }
}
