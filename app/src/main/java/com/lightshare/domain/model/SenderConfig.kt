package com.lightshare.domain.model

import com.lightshare.qr.generator.QrEcLevel

data class SenderConfig(
    val blockSize: Int = 256,
    val framesPerSecond: Int = 6,
    val ecLevel: QrEcLevel = QrEcLevel.M,
    /** Practical cap on QR text length. Longer payloads make denser QR codes that cameras struggle with. */
    val maxQrChars: Int = 1100,
    /** A manifest frame is interleaved every N frames so late joiners learn the file name/hash. */
    val manifestInterval: Int = 10
) {
    init {
        require(framesPerSecond in 1..30) { "framesPerSecond must be 1..30" }
        require(manifestInterval >= 2) { "manifestInterval must be >= 2" }
    }
}
