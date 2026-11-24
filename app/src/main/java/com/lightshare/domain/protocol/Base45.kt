package com.lightshare.domain.protocol

/**
 * Base45 (RFC 9285). Its alphabet is exactly the QR "alphanumeric" set, so ZXing encodes it in
 * alphanumeric mode (5.5 bits/char) which is ~35% denser than byte mode for binary data.
 */
object Base45 {
    private const val ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ \$%*+-./:"
    private val REVERSE = IntArray(128) { -1 }.also { r ->
        for (i in ALPHABET.indices) r[ALPHABET[i].code] = i
    }

    fun encodedLength(byteCount: Int): Int = (byteCount / 2) * 3 + (byteCount % 2) * 2

    fun encode(data: ByteArray): String {
        val sb = StringBuilder(encodedLength(data.size))
        var i = 0
        while (i + 1 < data.size) {
            val v = ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            sb.append(ALPHABET[v % 45]).append(ALPHABET[(v / 45) % 45]).append(ALPHABET[v / 2025])
            i += 2
        }
        if (i < data.size) {
            val v = data[i].toInt() and 0xFF
            sb.append(ALPHABET[v % 45]).append(ALPHABET[v / 45])
        }
        return sb.toString()
    }

    /** Returns null for any malformed input. */
    fun decode(text: String): ByteArray? {
        val n = text.length
        if (n % 3 == 1) return null
        val out = ByteArray((n / 3) * 2 + if (n % 3 == 2) 1 else 0)
        var o = 0
        var i = 0
        while (n - i >= 3) {
            val a = value(text[i]); val b = value(text[i + 1]); val c = value(text[i + 2])
            if (a < 0 || b < 0 || c < 0) return null
            val v = a + b * 45 + c * 2025
            if (v > 0xFFFF) return null
            out[o++] = (v shr 8).toByte()
            out[o++] = (v and 0xFF).toByte()
            i += 3
        }
        if (n - i == 2) {
            val a = value(text[i]); val b = value(text[i + 1])
            if (a < 0 || b < 0) return null
            val v = a + b * 45
            if (v > 0xFF) return null
            out[o] = v.toByte()
        }
        return out
    }

    private fun value(ch: Char): Int = if (ch.code < 128) REVERSE[ch.code] else -1
}
