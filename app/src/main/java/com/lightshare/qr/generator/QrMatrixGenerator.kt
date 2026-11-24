package com.lightshare.qr.generator

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

/** Native-resolution QR (1 pixel per module incl. quiet zone). Scaling is done at draw time with no filtering. */
class QrPixels(val size: Int, val argb: IntArray)

class QrMatrixGenerator(private val quietZoneModules: Int = 4) {
    private val writer = QRCodeWriter()

    fun generate(text: String, level: QrEcLevel): QrPixels {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to level.zxing,
            EncodeHintType.MARGIN to quietZoneModules
        )
        // width/height 0 => ZXing returns the minimum size: modules + quiet zone, 1 px per module.
        val m = writer.encode(text, BarcodeFormat.QR_CODE, 0, 0, hints)
        val w = m.width
        val h = m.height
        val px = IntArray(w * h)
        for (y in 0 until h) for (x in 0 until w) px[y * w + x] = if (m.get(x, y)) BLACK else WHITE
        return QrPixels(w, px)
    }

    private companion object {
        const val BLACK = 0xFF000000.toInt()
        const val WHITE = 0xFFFFFFFF.toInt()
    }
}
