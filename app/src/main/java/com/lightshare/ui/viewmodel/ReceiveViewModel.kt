package com.lightshare.ui.viewmodel

import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightshare.data.model.HistoryEntry
import com.lightshare.data.repository.HistoryRepository
import com.lightshare.data.storage.FileBlockStore
import com.lightshare.data.storage.ReceiveStorage
import com.lightshare.domain.decoder.AcceptResult
import com.lightshare.domain.decoder.FountainDecoder
import com.lightshare.domain.decoder.Verification
import com.lightshare.domain.usecase.FrameResult
import com.lightshare.domain.usecase.ProcessScannedFrame
import com.lightshare.util.RateMeter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class ReceivePhase { IDLE, SCANNING, VERIFYING, COMPLETE, FAILED }

data class ReceiveUiState(
    val phase: ReceivePhase = ReceivePhase.IDLE,
    val blockCount: Int = 0,
    val blockSize: Int = 0,
    val fileSize: Long = 0,
    val recoveredBlocks: Int = 0,
    val pendingEquations: Int = 0,
    val framesSeen: Int = 0,
    val uniquePackets: Int = 0,
    val duplicates: Int = 0,
    val redundant: Int = 0,
    val invalid: Int = 0,
    val foreign: Int = 0,
    val dropped: Int = 0,
    val packetsPerSec: Double = 0.0,
    val bytesPerSec: Double = 0.0,
    val elapsedMs: Long = 0,
    val fileName: String? = null,
    val mimeType: String? = null,
    val coverage: FloatArray = FloatArray(0),
    val torchOn: Boolean = false,
    val torchAvailable: Boolean = false,
    val verification: Verification? = null,
    val error: String? = null,
    val saving: Boolean = false,
    val savedUri: Uri? = null
) {
    val progress: Float get() = if (blockCount == 0) 0f else recoveredBlocks.toFloat() / blockCount
}

class ReceiveViewModel(
    private val storage: ReceiveStorage,
    private val history: HistoryRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(ReceiveUiState())
    val ui: StateFlow<ReceiveUiState> = _ui.asStateFlow()

    private val inbox = Channel<String>(64, BufferOverflow.DROP_OLDEST)
    private var consumer: Job? = null
    private var decoder: FountainDecoder? = null
    private var tempFile: File? = null

    fun onQrText(text: String) { inbox.trySend(text) }

    fun setTorch(on: Boolean) = _ui.update { it.copy(torchOn = on) }
    fun setTorchAvailable(a: Boolean) = _ui.update { it.copy(torchAvailable = a) }

    fun startReceiving() {
        cleanup()
        val dec = FountainDecoder({ _, bs, tid -> storage.openStore(tid, bs).also { tempFile = it.file } })
        decoder = dec
        _ui.value = ReceiveUiState(phase = ReceivePhase.SCANNING)
        consumer = viewModelScope.launch(Dispatchers.Default) { Session(dec).run() }
    }

    private inner class Session(val dec: FountainDecoder) {
        val processor = ProcessScannedFrame(dec)
        var seen = 0; var unique = 0; var dup = 0; var redundant = 0; var invalid = 0; var foreign = 0; var dropped = 0
        var lastText: String? = null
        val meter = RateMeter()
        var firstPacketAt = 0L
        var lastCoverageAt = 0L
        var coverage = FloatArray(0)
        var done = false

        suspend fun run() {
            for (text in inbox) {
                handle(text)
                if (done) break
            }
        }

        private suspend fun handle(text: String) {
            val now = SystemClock.elapsedRealtime()
            seen++
            if (text == lastText) { dup++; publish(now); return }
            lastText = text
            when (val r = processor.process(text)) {
                FrameResult.Malformed -> invalid++
                is FrameResult.Processed -> when (r.result) {
                    AcceptResult.ACCEPTED, AcceptResult.MANIFEST -> {
                        if (firstPacketAt == 0L) firstPacketAt = now
                        unique++; meter.record(now, r.payloadBytes)
                    }
                    AcceptResult.DUPLICATE, AcceptResult.ALREADY_COMPLETE -> dup++
                    AcceptResult.REDUNDANT -> { redundant++; if (firstPacketAt == 0L) firstPacketAt = now }
                    AcceptResult.WRONG_TRANSFER -> foreign++
                    AcceptResult.INVALID -> invalid++
                    AcceptResult.DROPPED -> dropped++
                }
            }
            publish(now)
            if (dec.isComplete && dec.manifest != null) finish(now)
        }

        private fun publish(now: Long) {
            if (now - lastCoverageAt > 500 && dec.isInitialized) {
                coverage = dec.coverage(800); lastCoverageAt = now
            }
            val m = dec.manifest
            _ui.update {
                it.copy(
                    blockCount = dec.blockCount, blockSize = dec.blockSize, fileSize = dec.fileSize,
                    recoveredBlocks = dec.recoveredCount, pendingEquations = dec.pendingEquations,
                    framesSeen = seen, uniquePackets = unique, duplicates = dup, redundant = redundant,
                    invalid = invalid, foreign = foreign, dropped = dropped,
                    packetsPerSec = meter.eventsPerSecond(now), bytesPerSec = meter.bytesPerSecond(now),
                    elapsedMs = if (firstPacketAt == 0L) 0 else now - firstPacketAt,
                    fileName = m?.fileName, mimeType = m?.mimeType, coverage = coverage
                )
            }
        }

        private suspend fun finish(now: Long) {
            done = true
            _ui.update { it.copy(phase = ReceivePhase.VERIFYING) }
            val v = withContext(Dispatchers.IO) { dec.verify() }
            val m = dec.manifest!!
            val duration = now - firstPacketAt
            if (v == Verification.OK) {
                _ui.update { it.copy(phase = ReceivePhase.COMPLETE, verification = v, elapsedMs = duration, coverage = dec.coverage(800)) }
                history.add(HistoryEntry(System.currentTimeMillis(), m.fileName, m.fileSize, "RECEIVED", "VERIFIED", duration))
            } else {
                _ui.update { it.copy(phase = ReceivePhase.FAILED, verification = v, error = "Integrity check failed ($v). The file was discarded.") }
                history.add(HistoryEntry(System.currentTimeMillis(), m.fileName, m.fileSize, "RECEIVED", "FAILED", duration))
                tempFile?.delete()
            }
        }
    }

    fun saveTo(uri: Uri) {
        val f = tempFile ?: return
        viewModelScope.launch {
            _ui.update { it.copy(saving = true, error = null) }
            runCatching { withContext(Dispatchers.IO) { storage.copyTo(f, uri) } }
                .onSuccess { _ui.update { s -> s.copy(saving = false, savedUri = uri) } }
                .onFailure { e -> _ui.update { s -> s.copy(saving = false, error = "Save failed: ${e.message}") } }
        }
    }

    fun cancel() {
        cleanup()
        _ui.value = ReceiveUiState()
    }

    private fun cleanup() {
        consumer?.cancel(); consumer = null
        decoder?.close(); decoder = null
        tempFile?.delete(); tempFile = null
        while (inbox.tryReceive().isSuccess) { /* drain */ }
    }

    override fun onCleared() = cleanup()
}
