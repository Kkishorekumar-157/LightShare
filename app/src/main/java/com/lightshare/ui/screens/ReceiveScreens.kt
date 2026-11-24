package com.lightshare.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lightshare.camera.permission.hasCameraPermission
import com.lightshare.camera.scanner.CameraScanner
import com.lightshare.domain.decoder.Verification
import com.lightshare.ui.components.*
import com.lightshare.ui.theme.Gold
import com.lightshare.ui.viewmodel.ReceivePhase
import com.lightshare.ui.viewmodel.ReceiveUiState
import com.lightshare.util.formatBytes
import com.lightshare.util.formatDuration
import java.util.Locale

@Composable
fun ReceiveScreen(onStart: () -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var denied by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) onStart() else denied = true
    }
    LsScaffold("Receive a file", onBack) {
        Text("1. On the other phone, open Send and start a transmission.", color = Color.White)
        Spacer(Modifier.height(6.dp))
        Text("2. Point this phone's camera at the animated QR code.", color = Color.White)
        Spacer(Modifier.height(6.dp))
        Text("3. Keep both phones steady until the progress bar fills and the SHA-256 check passes.", color = Color.White)
        Spacer(Modifier.height(12.dp))
        Text(
            "The camera is used only to read QR codes. Nothing is recorded or sent anywhere.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (denied) {
            Spacer(Modifier.height(12.dp))
            Text("Camera permission is required to scan. You can grant it in system Settings > Apps > LightShare.", color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(24.dp))
        GoldButton("Start scanning", { if (hasCameraPermission(ctx)) onStart() else launcher.launch(android.Manifest.permission.CAMERA) })
    }
}

@Composable
fun ScannerScreen(
    ui: ReceiveUiState,
    onQr: (String) -> Unit, onTorch: (Boolean) -> Unit, onTorchAvailable: (Boolean) -> Unit,
    onDetails: () -> Unit, onCancel: () -> Unit, onRetry: () -> Unit
) {
    BackHandler(onBack = onCancel)
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        CameraScanner(Modifier.fillMaxSize(), ui.torchOn, onQr, onTorchAvailable)
        Canvas(Modifier.fillMaxSize()) {
            val side = size.minDimension * 0.72f
            val l = (size.width - side) / 2; val t = (size.height - side) / 2 - size.height * 0.08f
            val c = side * 0.12f; val sw = 6f
            fun corner(x: Float, y: Float, dx: Float, dy: Float) {
                drawLine(Gold, Offset(x, y), Offset(x + dx * c, y), sw)
                drawLine(Gold, Offset(x, y), Offset(x, y + dy * c), sw)
            }
            corner(l, t, 1f, 1f); corner(l + side, t, -1f, 1f); corner(l, t + side, 1f, -1f); corner(l + side, t + side, -1f, -1f)
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp).navigationBarsPadding()
                .clip(RoundedCornerShape(22.dp)).background(Color(0xE60A0A0B)).padding(16.dp)
        ) {
            when (ui.phase) {
                ReceivePhase.VERIFYING -> { Text("Verifying SHA-256...", color = Gold); LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold) }
                ReceivePhase.FAILED -> {
                    Text(ui.error ?: "Transfer failed", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp)); GoldButton("Try again", onRetry)
                }
                else -> {
                    val status = when {
                        ui.blockCount == 0 -> "Looking for a LightShare QR code..."
                        ui.recoveredBlocks == ui.blockCount && ui.fileName == null -> "All blocks recovered. Waiting for file info frame..."
                        else -> "${ui.fileName ?: "Receiving"} · ${(ui.progress * 100).toInt()}%"
                    }
                    Text(status, color = Color.White, maxLines = 1)
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(progress = { ui.progress }, modifier = Modifier.fillMaxWidth(), color = Gold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Packets ${ui.uniquePackets} · Duplicates ${ui.duplicates} · Invalid ${ui.invalid} · " +
                            String.format(Locale.US, "%.1f pkt/s · %s/s", ui.packetsPerSec, formatBytes(ui.bytesPerSec.toLong())),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (ui.torchAvailable) GhostButton(if (ui.torchOn) "Torch off" else "Torch", { onTorch(!ui.torchOn) }, Modifier.weight(1f))
                GhostButton("Details", onDetails, Modifier.weight(1f))
                GhostButton("Cancel", onCancel, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun TransferProgressScreen(ui: ReceiveUiState, onBack: () -> Unit) {
    LsScaffold("Transfer progress", onBack) {
        Text("${ui.recoveredBlocks} / ${ui.blockCount} blocks recovered", style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(progress = { ui.progress }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), color = Gold)
        val cov = ui.coverage
        val cols = 40
        Canvas(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF161618))) {
            if (cov.isEmpty()) return@Canvas
            val rows = (cov.size + cols - 1) / cols
            val cw = size.width / cols; val ch = size.height / rows
            for (i in cov.indices) {
                drawRect(Gold.copy(alpha = 0.12f + 0.88f * cov[i]), Offset((i % cols) * cw, (i / cols) * ch), Size(cw - 1.5f, ch - 1.5f))
            }
        }
        Spacer(Modifier.height(12.dp))
        StatGrid(
            listOf(
                "Unique packets" to ui.uniquePackets.toString(),
                "Duplicates" to ui.duplicates.toString(),
                "Redundant" to ui.redundant.toString(),
                "Invalid / corrupt" to ui.invalid.toString(),
                "Other transfer" to ui.foreign.toString(),
                "Pending equations" to ui.pendingEquations.toString(),
                "Packets/s (measured)" to String.format(Locale.US, "%.1f", ui.packetsPerSec),
                "Throughput (measured)" to formatBytes(ui.bytesPerSec.toLong()) + "/s",
                "Frames seen" to ui.framesSeen.toString(),
                "Elapsed" to formatDuration(ui.elapsedMs)
            )
        )
        if (ui.blockCount > 0) Text(
            "File ${formatBytes(ui.fileSize)} in ${ui.blockCount} blocks of ${ui.blockSize} B",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CompletionScreen(ui: ReceiveUiState, onSave: (android.net.Uri) -> Unit, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val name = ui.fileName ?: "received_file"
    val mime = ui.mimeType?.takeIf { it.isNotBlank() } ?: "application/octet-stream"
    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(mime)) { uri -> if (uri != null) onSave(uri) }
    BackHandler(onBack = onDone)
    LsScaffold("Transfer complete") {
        val ok = ui.verification == Verification.OK
        LsCard {
            Text(if (ok) "Verified" else "Not verified", style = MaterialTheme.typography.titleLarge, color = if (ok) Gold else MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
            InfoRow("File", name)
            InfoRow("Size", formatBytes(ui.fileSize))
            InfoRow("Duration", formatDuration(ui.elapsedMs))
            InfoRow("SHA-256", if (ok) "Match - file is byte-identical" else ui.verification.toString())
            InfoRow("Packets used", "${ui.uniquePackets} unique, ${ui.duplicates} duplicate")
        }
        ui.error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(20.dp))
        val saved = ui.savedUri
        GoldButton(if (saved == null) "Save file" else "Saved", { saver.launch(name) }, enabled = saved == null && !ui.saving)
        if (ui.saving) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp), color = Gold)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GhostButton("Open", {
                val i = Intent(Intent.ACTION_VIEW).setDataAndType(saved, mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                try { ctx.startActivity(i) } catch (e: ActivityNotFoundException) { /* no viewer installed */ }
            }, Modifier.weight(1f), enabled = saved != null)
            GhostButton("Share", {
                val i = Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, saved)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                ctx.startActivity(Intent.createChooser(i, "Share file"))
            }, Modifier.weight(1f), enabled = saved != null)
        }
        Text("Open and Share become available after saving.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done", color = Gold) }
    }
}
