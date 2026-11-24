package com.lightshare.data.storage

import android.content.Context
import android.net.Uri
import java.io.File

class ReceiveStorage(private val context: Context) {
    private val dir: File get() = File(context.cacheDir, "lightshare").apply { mkdirs() }

    fun openStore(transferId: Int, blockSize: Int) =
        FileBlockStore(File(dir, "recv_${transferId.toUInt()}.part"), blockSize)

    fun purge() { dir.listFiles()?.forEach { it.delete() } }

    /** Streams the finished temp file to a Storage Access Framework URI. */
    fun copyTo(file: File, target: Uri) {
        val out = context.contentResolver.openOutputStream(target, "w") ?: error("Cannot open destination")
        out.use { o -> file.inputStream().use { it.copyTo(o, 64 * 1024) } }
    }
}
