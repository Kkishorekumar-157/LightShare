package com.lightshare.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lightshare.ui.theme.Gold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LsScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    scroll: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White)
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).padding(horizontal = 20.dp).fillMaxSize()
                .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            content = content
        )
    }
}

@Composable
fun GoldButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick, enabled = enabled, shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
        modifier = modifier.fillMaxWidth().height(54.dp)
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick, enabled = enabled, shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth().height(54.dp)
    ) { Text(text, color = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
fun LsCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = modifier.fillMaxWidth().clip(shape).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape, color = MaterialTheme.colorScheme.surface
    ) { Column(Modifier.padding(18.dp), content = content) }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).padding(12.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}

@Composable
fun StatGrid(items: List<Pair<String, String>>, columns: Int = 2) {
    items.chunked(columns).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { (l, v) -> StatTile(l, v, Modifier.weight(1f)) }
            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Text(value, color = Color.White, modifier = Modifier.weight(1f, fill = false))
    }
}

@Composable
fun LogoMark(size: Dp) {
    Canvas(Modifier.size(size)) {
        val u = this.size.minDimension / 7f
        fun finder(x: Float, y: Float) {
            drawRect(Gold, Offset(x * u, y * u), Size(3 * u, 3 * u), style = Stroke(u * 0.5f))
            drawRect(Gold, Offset((x + 1) * u, (y + 1) * u), Size(u, u))
        }
        finder(0f, 0f); finder(4f, 0f); finder(0f, 4f)
        drawRect(Gold, Offset(4 * u, 4 * u), Size(u, u))
        drawRect(Gold, Offset(6 * u, 5 * u), Size(u, u))
        drawRect(Gold, Offset(5 * u, 6 * u), Size(u, u))
    }
}

@Composable
fun CenterColumn(content: @Composable ColumnScope.() -> Unit) =
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, content = content)
