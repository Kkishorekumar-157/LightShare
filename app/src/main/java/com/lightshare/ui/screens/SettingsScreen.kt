package com.lightshare.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lightshare.data.model.AppSettings
import com.lightshare.ui.components.LsCard
import com.lightshare.ui.components.LsScaffold
import com.lightshare.ui.theme.Gold
import com.lightshare.qr.generator.QrEcLevel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    s: AppSettings,
    onFps: (Int) -> Unit, onBlock: (Int) -> Unit, onEc: (QrEcLevel) -> Unit, onBack: () -> Unit
) {
    var fps by remember(s.framesPerSecond) { mutableFloatStateOf(s.framesPerSecond.toFloat()) }
    LsScaffold("Settings", onBack) {
        LsCard {
            Text("QR frame rate: ${fps.toInt()} frames/s", style = MaterialTheme.typography.titleMedium)
            Slider(value = fps, onValueChange = { fps = it }, onValueChangeFinished = { onFps(fps.toInt()) },
                valueRange = 1f..15f, steps = 13,
                colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold))
            Text("Cameras rarely decode more than ~10 distinct QR frames/s reliably. Lower it if the receiver misses frames.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))
        LsCard {
            Text("Block size (bytes per QR)", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(128, 192, 256, 384, 512).forEach {
                    FilterChip(selected = s.blockSize == it, onClick = { onBlock(it) }, label = { Text("$it") })
                }
            }
            Text("Bigger blocks = faster, but denser QR codes that are harder to scan.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))
        LsCard {
            Text("QR error correction", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QrEcLevel.entries.forEach {
                    FilterChip(selected = s.ecLevel == it, onClick = { onEc(it) }, label = { Text(it.name) })
                }
            }
            Text("Higher levels survive blur/glare but make denser codes. M is a good default; the fountain code already covers lost frames.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))
        Text("Settings apply to the sender when a transmission starts.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
