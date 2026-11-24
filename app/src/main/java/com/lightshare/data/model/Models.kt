package com.lightshare.data.model

import com.lightshare.qr.generator.QrEcLevel
import kotlinx.serialization.Serializable

data class AppSettings(
    val framesPerSecond: Int = 6,
    val blockSize: Int = 256,
    val ecLevel: QrEcLevel = QrEcLevel.M
)

@Serializable
data class HistoryEntry(
    val timestampMs: Long,
    val fileName: String,
    val sizeBytes: Long,
    /** "SENT" or "RECEIVED" */
    val direction: String,
    /** "TRANSMITTED" (sender cannot know delivery), "VERIFIED", or "FAILED" */
    val status: String,
    val durationMs: Long
)

/** What the UI needs to know about the picked file. */
data class FileInfo(val name: String, val mimeType: String, val size: Long, val sha256Hex: String)
