package com.lightshare.domain.model

import com.lightshare.domain.protocol.PacketCodec

class Manifest(
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val sha256: ByteArray,
    val blockCount: Int,
    val blockSize: Int,
    val version: Int = PacketCodec.VERSION
) {
    init {
        require(sha256.size == 32) { "SHA-256 must be 32 bytes" }
        require(fileName.toByteArray(Charsets.UTF_8).size <= MAX_NAME_BYTES) { "file name too long" }
        require(mimeType.toByteArray(Charsets.UTF_8).size <= MAX_MIME_BYTES) { "mime type too long" }
    }

    fun toPacket(transferId: Int): Packet {
        val name = fileName.toByteArray(Charsets.UTF_8)
        val mime = mimeType.toByteArray(Charsets.UTF_8)
        val payload = ByteArray(1 + name.size + 1 + mime.size + 32)
        var p = 0
        payload[p++] = name.size.toByte()
        System.arraycopy(name, 0, payload, p, name.size); p += name.size
        payload[p++] = mime.size.toByte()
        System.arraycopy(mime, 0, payload, p, mime.size); p += mime.size
        System.arraycopy(sha256, 0, payload, p, 32)
        return Packet(version, PacketType.MANIFEST, transferId, 0, blockCount, blockSize, fileSize, payload)
    }

    companion object {
        const val MAX_NAME_BYTES = 100
        const val MAX_MIME_BYTES = 64

        /** Removes path separators/control chars and truncates to MAX_NAME_BYTES of UTF-8. */
        fun sanitizeName(raw: String): String {
            val sb = StringBuilder()
            var bytes = 0
            var i = 0
            while (i < raw.length) {
                val cp = raw.codePointAt(i)
                i += Character.charCount(cp)
                val bad = cp < 32 || cp == 127 || (cp < 128 && cp.toChar() in "/\\:*?\"<>|")
                val s = if (bad) "_" else String(Character.toChars(cp))
                val n = s.toByteArray(Charsets.UTF_8).size
                if (bytes + n > MAX_NAME_BYTES) break
                sb.append(s); bytes += n
            }
            val out = sb.toString().trim()
            return if (out.isEmpty()) "received_file" else out
        }

        fun fromPacket(p: Packet): Manifest? {
            if (p.type != PacketType.MANIFEST) return null
            val d = p.payload
            try {
                var pos = 0
                val nl = d[pos++].toInt() and 0xFF
                if (nl > MAX_NAME_BYTES || pos + nl >= d.size) return null
                val name = String(d, pos, nl, Charsets.UTF_8); pos += nl
                val ml = d[pos++].toInt() and 0xFF
                if (ml > MAX_MIME_BYTES || pos + ml + 32 != d.size) return null
                val mime = String(d, pos, ml, Charsets.UTF_8); pos += ml
                val sha = d.copyOfRange(pos, pos + 32)
                return Manifest(name, mime, p.fileSize, sha, p.blockCount, p.blockSize, p.version)
            } catch (e: Exception) {
                return null
            }
        }
    }
}
