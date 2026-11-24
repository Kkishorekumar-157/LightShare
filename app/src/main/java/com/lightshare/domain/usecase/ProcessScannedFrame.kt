package com.lightshare.domain.usecase

import com.lightshare.domain.decoder.AcceptResult
import com.lightshare.domain.decoder.FountainDecoder
import com.lightshare.domain.protocol.FrameCodec
import com.lightshare.domain.protocol.PacketCodec
import com.lightshare.domain.protocol.PacketFormatException

sealed interface FrameResult {
    /** Not a LightShare frame, bad Base45, or failed CRC/validation. */
    data object Malformed : FrameResult
    data class Processed(val result: AcceptResult, val payloadBytes: Int) : FrameResult
}

/** QR text -> bytes -> validated packet -> decoder. Shared by the receiver and the camera-free protocol tests. */
class ProcessScannedFrame(private val decoder: FountainDecoder) {
    fun process(qrText: String): FrameResult {
        val bytes = FrameCodec.fromQrText(qrText) ?: return FrameResult.Malformed
        val packet = try { PacketCodec.decode(bytes) } catch (e: PacketFormatException) { return FrameResult.Malformed }
        return FrameResult.Processed(decoder.accept(packet), packet.payload.size)
    }
}
