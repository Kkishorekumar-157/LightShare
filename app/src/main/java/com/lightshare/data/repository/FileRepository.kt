package com.lightshare.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.lightshare.data.storage.FileHandle
import com.lightshare.data.storage.PreparedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.security.MessageDigest

class FileRepository(private val context: Context) {

    /**
     * Streams the file once to compute SHA-256 and exact size (64 KB buffer, constant memory).
     * Sources that support random access are read in place; others (e.g. some cloud providers)
     * are streamed once into a cache file so blocks can be read in any order.
     */
    suspend fun prepare(uri: Uri): PreparedFile = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var name = "file"
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0)?.let { name = it }
        }
        val mime = resolver.getType(uri) ?: "application/octet-stream"

        val md = MessageDigest.getInstance("SHA-256")
        var total = 0L
        val input = resolver.openInputStream(uri) ?: error("Cannot open file")
        input.use { ins ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = ins.read(buf)
                if (n < 0) break
                md.update(buf, 0, n); total += n
            }
        }
        require(total > 0) { "The selected file is empty" }
        val sha = md.digest()

        val seekable = runCatching {
            resolver.openFileDescriptor(uri, "r")?.use { pfd ->
                pfd.statSize == total &&
                    FileInputStream(pfd.fileDescriptor).channel.read(ByteBuffer.allocate(1), 0) >= 0
            } ?: false
        }.getOrDefault(false)

        if (seekable) {
            PreparedFile(name, mime, total, sha, {
                val pfd = resolver.openFileDescriptor(uri, "r") ?: error("Cannot reopen file")
                val fis = FileInputStream(pfd.fileDescriptor)
                FileHandle(fis.channel) { runCatching { fis.close() }; runCatching { pfd.close() } }
            }, null)
        } else {
            val copy = File(context.cacheDir, "send_${System.nanoTime()}.bin")
            resolver.openInputStream(uri)!!.use { ins -> copy.outputStream().use { ins.copyTo(it, 64 * 1024) } }
            require(copy.length() == total) { "File changed while reading" }
            PreparedFile(name, mime, total, sha, {
                val raf = RandomAccessFile(copy, "r")
                FileHandle(raf.channel) { runCatching { raf.close() } }
            }, copy)
        }
    }
}
