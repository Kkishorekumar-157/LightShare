package com.lightshare.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.lightshare.data.model.AppSettings
import com.lightshare.ui.components.*
import com.lightshare.ui.theme.Gold
import com.lightshare.ui.viewmodel.SendUiState
import com.lightshare.util.formatBytes
import com.lightshare.util.formatDuration
import java.util.Locale

@Composable
fun SendScreen(ui: SendUiState, onPick: (android.net.Uri?) -> Unit, onContinue: () -> Unit, onBack: () -> Unit) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { onPick(it) }
    LsScaffold("Send a file", onBack) {
        Text(
            "Any file type works: PDF, TXT, JPG, PNG, MP4, ZIP... Nothing leaves the phone except what the screen shows.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        GoldButton(if (ui.file == null) "Choose file" else "Choose a different file", { picker.launch(arrayOf("*/*")) }, enabled = !ui.preparing)
        if (ui.preparing) {
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold)
            Text("Reading and hashing file...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ui.error?.let { Spacer(Modifier.height(12.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
        ui.file?.let { f ->
            Spacer(Modifier.height(20.dp))
            LsCard {
                Text(f.name, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                InfoRow("Type", f.mimeType)
                InfoRow("Size", formatBytes(f.size))
            }
            Spacer(Modifier.height(16.dp))
            GoldButton("Continue", onContinue)
        }
    }
}

@Composable
fun FileInfoScreen(ui: SendUiState, settings: AppSettings, onStart: () -> Unit, onBack: () -> Unit) {
    val f = ui.file
    LsScaffold("File information", onBack) {
        if (f == null) { Text("No file selected."); return@LsScaffold }
        val blocks = ((f.size + settings.blockSize - 1) / settings.blockSize)
        val minSeconds = blocks.toDouble() / settings.framesPerSecond * 1.1
        LsCard {
            Text(f.name, style = MaterialTheme.typography.titleMedium, maxLines = 3)
            Spacer(Modifier.height(8.dp))
            InfoRow("MIME type", f.mimeType)
            InfoRow("Size", formatBytes(f.size) + " (${f.size} bytes)")
            InfoRow("SHA-256", f.sha256Hex.take(16) + "...")
            InfoRow("Source blocks", "$blocks x ${settings.blockSize} B")
            InfoRow("QR rate", "${settings.framesPerSecond} frames/s, EC ${settings.ecLevel}")
            InfoRow("Best-case time", formatDuration((minSeconds * 1000).toLong()))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Best case assumes the receiver sees every frame. Missed frames are covered by repair packets that keep streaming until you stop.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ui.error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(20.dp))
        GoldButton("Start transmission", onStart)
    }
}

@Composable
fun TransmissionScreen(ui: SendUiState, onTogglePause: () -> Unit, onCancel: () -> Unit) {
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    BackHandler(onBack = onCancel)
    LsScaffold("Transmitting") {
        ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(12.dp)) }
        CenterColumn {
            Box(
                Modifier.widthIn(max = 480.dp).fillMaxWidth().aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp)).background(Color.White).padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                ui.frame?.let {
                    Image(it, "QR frame", Modifier.fillMaxSize(), contentScale = ContentScale.Fit, filterQuality = FilterQuality.None)
                } ?: CircularProgressIndicator(color = Gold)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(ui.file?.name ?: "", maxLines = 1, color = Color.White)
        Text(
            "Hold the receiver steady about 20-30 cm away. Raise screen brightness.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        StatGrid(
            listOf(
                "Frame" to ui.frameNumber.toString(),
                "Packets generated" to "${ui.dataPackets} (${ui.repairPackets} repair)",
                "Frames/s (measured)" to String.format(Locale.US, "%.1f of %d", ui.measuredFps, ui.configuredFps),
                "Link rate (max)" to formatBytes(ui.linkBytesPerSec.toLong()) + "/s",
                "File size" to formatBytes(ui.file?.size ?: 0),
                "Elapsed" to formatDuration(ui.elapsedMs),
                "Source blocks" to ui.sourceBlocks.toString(),
                "QR characters" to ui.qrChars.toString()
            )
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GoldButton(if (ui.paused) "Resume" else "Pause", onTogglePause, Modifier.weight(1f))
            GhostButton("Cancel", onCancel, Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))
    }
}
