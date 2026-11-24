package com.lightshare.ui.viewmodel

import android.net.Uri
import android.os.SystemClock
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightshare.data.model.FileInfo
import com.lightshare.data.model.HistoryEntry
import com.lightshare.data.repository.FileRepository
import com.lightshare.data.repository.HistoryRepository
import com.lightshare.data.repository.SettingsRepository
import com.lightshare.data.storage.PreparedFile
import com.lightshare.domain.encoder.SenderSession
import com.lightshare.domain.model.PacketType
import com.lightshare.domain.model.SenderConfig
import com.lightshare.qr.generator.QrMatrixGenerator
import com.lightshare.qr.renderer.QrBitmapRenderer
import com.lightshare.util.RateMeter
import com.lightshare.util.toHex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SendUiState(
    val file: FileInfo? = null,
    val preparing: Boolean = false,
    val error: String? = null,
    val transmitting: Boolean = false,
    val paused: Boolean = false,
    val frame: ImageBitmap? = null,
    val frameNumber: Long = 0,
    val sourceBlocks: Int = 0,
    val dataPackets: Long = 0,
    val repairPackets: Long = 0,
    val qrChars: Int = 0,
    val measuredFps: Double = 0.0,
    val linkBytesPerSec: Double = 0.0,
    val elapsedMs: Long = 0,
    val configuredFps: Int = 0,
    val blockSize: Int = 0
)

class SendViewModel(
    private val files: FileRepository,
    private val settings: SettingsRepository,
    private val history: HistoryRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(SendUiState())
    val ui: StateFlow<SendUiState> = _ui.asStateFlow()

    private var prepared: PreparedFile? = null
    private var job: Job? = null
    private val paused = MutableStateFlow(false)

    fun onFilePicked(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            _ui.update { it.copy(preparing = true, error = null) }
            runCatching { files.prepare(uri) }
                .onSuccess { pf ->
                    prepared?.release()
                    prepared = pf
                    _ui.update {
                        it.copy(preparing = false, file = FileInfo(pf.name, pf.mimeType, pf.size, pf.sha256.toHex()))
                    }
                }
                .onFailure { e -> _ui.update { it.copy(preparing = false, error = e.message ?: "Could not read file") } }
        }
    }

    fun startTransmission() {
        val file = prepared ?: return
        job?.cancel()
        paused.value = false
        job = viewModelScope.launch(Dispatchers.Default) {
            val s = settings.settings.first()
            val source = try { file.openSource(s.blockSize) } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Cannot open file", transmitting = false) }; return@launch
            }
            val session = try {
                SenderSession(source, file.name, file.mimeType, file.sha256,
                    SenderConfig(blockSize = s.blockSize, framesPerSecond = s.framesPerSecond, ecLevel = s.ecLevel))
            } catch (e: IllegalArgumentException) {
                source.close()
                _ui.update { it.copy(error = e.message, transmitting = false) }; return@launch
            }
            val generator = QrMatrixGenerator()
            val frameMeter = RateMeter(4_000)
            val intervalMs = 1000L / s.framesPerSecond
            var activeMs = 0L
            var lastTick = SystemClock.elapsedRealtime()
            _ui.update {
                it.copy(transmitting = true, paused = false, error = null, frameNumber = 0, sourceBlocks = session.blockCount,
                    configuredFps = s.framesPerSecond, blockSize = s.blockSize)
            }
            try {
                while (isActive) {
                    if (paused.value) {
                        paused.first { !it }
                        frameMeter.clear()
                        lastTick = SystemClock.elapsedRealtime()
                    }
                    val t0 = SystemClock.elapsedRealtime()
                    val frame = session.nextFrame()
                    val qr = generator.generate(frame.text, s.ecLevel)
                    val bmp = QrBitmapRenderer.toImageBitmap(qr)
                    val now = SystemClock.elapsedRealtime()
                    activeMs += now - lastTick; lastTick = now
                    frameMeter.record(now, if (frame.type == PacketType.MANIFEST) 0 else s.blockSize)
                    val dataFps = frameMeter.eventsPerSecond(now)
                    _ui.update {
                        it.copy(
                            frame = bmp, frameNumber = session.framesEmitted,
                            dataPackets = session.dataPacketsEmitted, repairPackets = session.repairPacketsEmitted,
                            qrChars = frame.text.length, measuredFps = dataFps,
                            linkBytesPerSec = frameMeter.bytesPerSecond(now), elapsedMs = activeMs
                        )
                    }
                    delay((intervalMs - (SystemClock.elapsedRealtime() - t0)).coerceAtLeast(1L))
                }
            } finally {
                source.close()
            }
        }
    }

    fun togglePause() {
        paused.value = !paused.value
        _ui.update { it.copy(paused = paused.value) }
    }

    fun stop() {
        job?.cancel(); job = null
        val st = _ui.value
        val f = st.file
        if (f != null && st.frameNumber > 0) {
            viewModelScope.launch {
                history.add(HistoryEntry(System.currentTimeMillis(), f.name, f.size, "SENT", "TRANSMITTED", st.elapsedMs))
            }
        }
        prepared?.release(); prepared = null
        paused.value = false
        _ui.value = SendUiState()
    }

    fun clearError() = _ui.update { it.copy(error = null) }

    override fun onCleared() {
        job?.cancel(); prepared?.release()
    }
}
