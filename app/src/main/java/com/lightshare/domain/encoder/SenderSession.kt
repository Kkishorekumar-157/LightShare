package com.lightshare.domain.encoder

import com.lightshare.domain.model.Manifest
import com.lightshare.domain.model.PacketType
import com.lightshare.domain.model.SenderConfig
import com.lightshare.domain.protocol.FrameCodec
import com.lightshare.domain.protocol.PacketCodec
import java.security.SecureRandom

class Frame(val text: String, val type: PacketType, val seq: Int, val payloadBytes: Int)

/**
 * Produces an endless stream of QR frames:
 *   manifest, s0, s1, ... , manifest, ..., s(k-1), r(k), r(k+1), ...  (manifest every N frames)
 * Source blocks go first (a receiver that sees everything finishes in ~k frames); after that only
 * repair packets are sent, forever, so any receiver that joined late or missed frames keeps converging.
 */
class SenderSession(
    private val source: BlockSource,
    fileName: String,
    mimeType: String,
    sha256: ByteArray,
    val config: SenderConfig = SenderConfig(),
    val transferId: Int = SecureRandom().nextInt()
) {
    val manifest = Manifest(
        Manifest.sanitizeName(fileName),
        mimeType.filter { it.code in 32..126 }.take(Manifest.MAX_MIME_BYTES),
        source.fileSize, sha256, source.blockCount, source.blockSize
    )
    val blockCount get() = source.blockCount
    private val encoder = FountainEncoder(source, transferId)
    private val manifestText = FrameCodec.toQrText(PacketCodec.encode(manifest.toPacket(transferId)))
    private var nextSeq = 0

    var framesEmitted = 0L; private set
    var dataPacketsEmitted = 0L; private set
    val repairPacketsEmitted: Long get() = maxOf(0L, dataPacketsEmitted - blockCount)
    val maxQrChars: Int = minOf(config.maxQrChars, config.ecLevel.maxAlphanumericChars)

    init {
        require(source.blockSize == config.blockSize) { "block size mismatch" }
        val probe = FrameCodec.toQrText(PacketCodec.encode(encoder.packetFor(0)))
        require(probe.length <= maxQrChars) {
            "Packets are ${probe.length} characters but the QR limit is $maxQrChars. Use a smaller block size."
        }
        require(manifestText.length <= maxQrChars) { "Manifest does not fit in one QR code" }
    }

    fun nextFrame(): Frame {
        val f = framesEmitted++
        if (f % config.manifestInterval == 0L) return Frame(manifestText, PacketType.MANIFEST, 0, 0)
        val seq = nextSeq
        nextSeq = if (nextSeq == Int.MAX_VALUE) blockCount else nextSeq + 1
        val p = encoder.packetFor(seq)
        dataPacketsEmitted++
        return Frame(FrameCodec.toQrText(PacketCodec.encode(p)), p.type, seq, p.payload.size)
    }
}
