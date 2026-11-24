package com.lightshare.domain.protocol

/**
 * QR text = "LS1:" + Base45(packet bytes) + ":"
 * The fixed prefix lets the receiver ignore unrelated QR codes cheaply; the fixed (alphanumeric-legal)
 * suffix guards against scanners trimming trailing spaces, which are part of the Base45 alphabet.
 */
object FrameCodec {
    private const val PREFIX = "LS1:"
    private const val SUFFIX = ":"

    fun toQrText(packetBytes: ByteArray): String = PREFIX + Base45.encode(packetBytes) + SUFFIX

    fun fromQrText(text: String): ByteArray? {
        if (text.length < PREFIX.length + SUFFIX.length) return null
        if (!text.startsWith(PREFIX) || !text.endsWith(SUFFIX)) return null
        return Base45.decode(text.substring(PREFIX.length, text.length - SUFFIX.length))
    }
}
