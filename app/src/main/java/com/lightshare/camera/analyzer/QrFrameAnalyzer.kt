package com.lightshare.camera.analyzer

import android.os.SystemClock
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * One frame at a time: the ImageProxy is closed only when ML Kit finishes, and ImageAnalysis uses
 * STRATEGY_KEEP_ONLY_LATEST, so analyses never overlap and stale frames are dropped.
 * Every path (skipped frame, success, failure, cancel) closes the proxy.
 */
class QrFrameAnalyzer(
    private val minIntervalMs: Long = 30L,
    private val onQr: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
    )
    private var lastAnalysisMs = 0L

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val now = SystemClock.elapsedRealtime()
        val media = imageProxy.image
        if (media == null || now - lastAnalysisMs < minIntervalMs) {
            imageProxy.close()
            return
        }
        lastAnalysisMs = now
        // rotationDegrees makes ML Kit interpret the sensor image in the display's upright orientation.
        val input = InputImage.fromMediaImage(media, imageProxy.imageInfo.rotationDegrees)
        scanner.process(input)
            .addOnSuccessListener { codes -> for (c in codes) c.rawValue?.let(onQr) }
            .addOnCompleteListener { imageProxy.close() }
    }

    fun close() { scanner.close() }
}
