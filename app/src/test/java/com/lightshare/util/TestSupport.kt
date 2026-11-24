package com.lightshare.util

import com.lightshare.domain.decoder.FountainDecoder
import com.lightshare.domain.decoder.MemoryBlockStore
import com.lightshare.domain.decoder.Verification
import com.lightshare.domain.encoder.ByteArrayBlockSource
import com.lightshare.domain.encoder.Frame
import com.lightshare.domain.encoder.SenderSession
import com.lightshare.domain.model.SenderConfig
import com.lightshare.domain.usecase.ProcessScannedFrame
import java.security.MessageDigest
import kotlin.random.Random

object TestSupport {
    fun randomBytes(n: Int, seed: Long = 1): ByteArray = ByteArray(n).also { Random(seed).nextBytes(it) }
    fun sha256(d: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(d)

    fun session(data: ByteArray, bs: Int = 256, name: String = "test.bin"): SenderSession =
        SenderSession(ByteArrayBlockSource(data, bs), name, "application/octet-stream", sha256(data),
            SenderConfig(blockSize = bs), transferId = 0x1234ABCD)

    fun decoder() = FountainDecoder({ k, bs, _ -> MemoryBlockStore(k, bs) })

    class Outcome(val framesGenerated: Int, val framesDelivered: Int, val verification: Verification, val data: ByteArray)

    /** Full camera-free pipeline: encoder -> QR text -> (loss/shuffle) -> parse -> decoder -> verify. */
    fun transfer(data: ByteArray, bs: Int = 256, dropRate: Double = 0.0, shuffleWindow: Int = 1,
                 seed: Long = 7, maxFrames: Int = 0): Outcome {
        val s = session(data, bs)
        val dec = decoder()
        val proc = ProcessScannedFrame(dec)
        val rnd = Random(seed)
        val limit = if (maxFrames > 0) maxFrames else s.blockCount * 8 + 100
        var generated = 0; var delivered = 0
        val window = ArrayList<Frame>()
        fun flush() {
            window.shuffle(rnd)
            for (f in window) { proc.process(f.text); delivered++; if (dec.isComplete && dec.manifest != null) break }
            window.clear()
        }
        while (!(dec.isComplete && dec.manifest != null)) {
            check(generated < limit) { "did not converge within $limit frames (recovered ${dec.recoveredCount}/${dec.blockCount})" }
            val f = s.nextFrame(); generated++
            if (rnd.nextDouble() < dropRate) continue
            window.add(f)
            if (window.size >= shuffleWindow) flush()
        }
        val v = dec.verify()
        val out = (dec.blockStore as MemoryBlockStore).toByteArray(dec.fileSize)
        return Outcome(generated, delivered, v, out)
    }
}
