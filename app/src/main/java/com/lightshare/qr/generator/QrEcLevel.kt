package com.lightshare.qr.generator

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** maxAlphanumericChars = capacity of a version-40 QR in alphanumeric mode at that level. */
enum class QrEcLevel(val zxing: ErrorCorrectionLevel, val maxAlphanumericChars: Int) {
    L(ErrorCorrectionLevel.L, 4296),
    M(ErrorCorrectionLevel.M, 3391),
    Q(ErrorCorrectionLevel.Q, 2420),
    H(ErrorCorrectionLevel.H, 1852)
}
