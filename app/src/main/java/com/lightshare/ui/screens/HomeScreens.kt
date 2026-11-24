package com.lightshare.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lightshare.data.model.HistoryEntry
import com.lightshare.ui.components.*
import com.lightshare.ui.theme.Gold
import com.lightshare.util.formatBytes
import com.lightshare.util.formatDuration
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { a.animateTo(1f, tween(700)); delay(500); onFinished() }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(Modifier.alpha(a.value), horizontalAlignment = Alignment.CenterHorizontally) {
            LogoMark(96.dp)
            Spacer(Modifier.height(18.dp))
            Text("LightShare", style = MaterialTheme.typography.headlineLarge)
            Text("Offline optical file transfer", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun HomeScreen(
    history: List<HistoryEntry>,
    onSend: () -> Unit, onReceive: () -> Unit, onHistory: () -> Unit, onSettings: () -> Unit
) {
    LsScaffold(
        title = "",
        actions = { IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, contentDescription = "Settings") } }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LogoMark(44.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text("LightShare", style = MaterialTheme.typography.headlineMedium)
                Text("Offline optical file transfer", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(28.dp))
        LsCard(onClick = onSend) {
            Text("Send files", style = MaterialTheme.typography.titleLarge, color = Gold)
            Spacer(Modifier.height(4.dp))
            Text("Pick a file and show it as animated QR codes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))
        LsCard(onClick = onReceive) {
            Text("Receive files", style = MaterialTheme.typography.titleLarge, color = Gold)
            Spacer(Modifier.height(4.dp))
            Text("Point the camera at another phone's QR codes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(28.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Recent transfers", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onHistory) { Text("See all", color = Gold) }
        }
        if (history.isEmpty()) Text("No transfers yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        else history.take(3).forEach { HistoryRow(it) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun HistoryRow(e: HistoryEntry) {
    LsCard(Modifier.padding(bottom = 8.dp)) {
        Text(e.fileName, color = Color.White, maxLines = 1)
        Text(
            "${e.direction.lowercase().replaceFirstChar { it.uppercase() }} · ${e.status.lowercase()} · ${formatBytes(e.sizeBytes)} · ${formatDuration(e.durationMs)}",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(e.timestampMs)),
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun HistoryScreen(history: List<HistoryEntry>, onBack: () -> Unit, onClear: () -> Unit) {
    LsScaffold("History", onBack) {
        if (history.isEmpty()) Text("No transfers yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        else {
            history.forEach { HistoryRow(it) }
            Spacer(Modifier.height(12.dp))
            GhostButton("Clear history", onClear)
        }
        Spacer(Modifier.height(24.dp))
    }
}
