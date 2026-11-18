package com.example.ui.chats

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.media.ChatPreferencesStore
import com.example.data.media.ChatWallpaperConfig
import com.example.data.media.MediaUtils
import com.example.data.media.WallpaperPreset
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WhatsAppChatWallpaperLight
import com.example.ui.theme.WhatsAppDarkBg
import com.example.ui.theme.WhatsAppDarkElevated
import com.example.ui.theme.WhatsAppDarkTeal
import com.example.ui.theme.WhatsAppDarkText
import com.example.ui.theme.WhatsAppEmerald
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppInkDark
import com.example.ui.theme.WhatsAppOutgoingBubbleLight
import com.example.ui.theme.WhatsAppReadBlue
import com.example.ui.theme.WhatsAppTeal
import kotlinx.coroutines.launch

@Composable
fun ChatWallpaperBackdrop(
    config: ChatWallpaperConfig,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val customBitmap = remember(config.preset, config.customPhotoPath) {
        if (config.preset == WallpaperPreset.CUSTOM_PHOTO) {
            MediaUtils.decodeFileToImageBitmap(config.customPhotoPath)
        } else null
    }

    Box(modifier = modifier) {
        if (customBitmap != null) {
            Image(
                bitmap = customBitmap,
                contentDescription = "Custom chat wallpaper",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val effectivePreset = if (config.preset == WallpaperPreset.CUSTOM_PHOTO) {
                WallpaperPreset.CLASSIC_DOODLE
            } else config.preset

            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                when (effectivePreset) {
                    WallpaperPreset.CLASSIC_DOODLE -> {
                        val base = if (isDark) WhatsAppDarkBg else WhatsAppChatWallpaperLight
                        val ink = if (isDark) Color(0xFF23323B).copy(alpha = 0.55f) else Color(0xFFD6CEC2).copy(alpha = 0.85f)
                        drawRect(color = base)
                        val step = 110f
                        val cols = (w / step).toInt() + 2
                        val rows = (h / step).toInt() + 2
                        for (r in 0..rows) {
                            for (c in 0..cols) {
                                val cx = c * step + if (r % 2 == 1) step * 0.5f else 0f
                                val cy = r * step
                                when ((r * 3 + c) % 4) {
                                    0 -> {
                                        // Mini rounded speech bubble
                                        drawRoundRect(
                                            color = ink,
                                            topLeft = Offset(cx - 16f, cy - 12f),
                                            size = Size(32f, 22f),
                                            cornerRadius = CornerRadius(8f, 8f),
                                            style = Stroke(width = 2.4f)
                                        )
                                    }
                                    1 -> {
                                        // Subtle star / sparkle
                                        drawCircle(color = ink, radius = 9f, center = Offset(cx, cy), style = Stroke(width = 2.2f))
                                        drawCircle(color = ink, radius = 2.5f, center = Offset(cx, cy))
                                    }
                                    2 -> {
                                        // Wave arc
                                        val p = Path().apply {
                                            moveTo(cx - 14f, cy + 4f)
                                            quadraticTo(cx, cy - 12f, cx + 14f, cy + 4f)
                                        }
                                        drawPath(p, color = ink, style = Stroke(width = 2.4f))
                                    }
                                    else -> {
                                        // Tilted diamond / gem
                                        val p = Path().apply {
                                            moveTo(cx, cy - 11f)
                                            lineTo(cx + 10f, cy)
                                            lineTo(cx, cy + 11f)
                                            lineTo(cx - 10f, cy)
                                            close()
                                        }
                                        drawPath(p, color = ink, style = Stroke(width = 2.1f))
                                    }
                                }
                            }
                        }
                    }

                    WallpaperPreset.EMERALD_AURORA -> {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF062C27), Color(0xFF0A4D43), Color(0xFF0B1F1E))
                            )
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF25D366).copy(alpha = 0.28f), Color.Transparent),
                                center = Offset(w * 0.2f, h * 0.25f),
                                radius = w * 0.75f
                            ),
                            center = Offset(w * 0.2f, h * 0.25f),
                            radius = w * 0.75f
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF128C7E).copy(alpha = 0.34f), Color.Transparent),
                                center = Offset(w * 0.85f, h * 0.65f),
                                radius = w * 0.7f
                            ),
                            center = Offset(w * 0.85f, h * 0.65f),
                            radius = w * 0.7f
                        )
                    }

                    WallpaperPreset.MIDNIGHT_COSMOS -> {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF0B1021), Color(0xFF151C36), Color(0xFF090D18))
                            )
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF3F51B5).copy(alpha = 0.24f), Color.Transparent),
                                center = Offset(w * 0.75f, h * 0.2f),
                                radius = w * 0.65f
                            ),
                            center = Offset(w * 0.75f, h * 0.2f),
                            radius = w * 0.65f
                        )
                        // Deterministic starfield
                        for (i in 0 until 45) {
                            val sx = ((i * 197 + 43) % 100) / 100f * w
                            val sy = ((i * 313 + 71) % 100) / 100f * h
                            val r = if (i % 5 == 0) 3.2f else 1.7f
                            drawCircle(
                                color = Color.White.copy(alpha = if (i % 3 == 0) 0.55f else 0.25f),
                                radius = r,
                                center = Offset(sx, sy)
                            )
                        }
                    }

                    WallpaperPreset.SUNSET_TERRACOTTA -> {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF3A1C2A), Color(0xFF7D3B3A), Color(0xFF2D1B22))
                            )
                        )
                        val wave1 = Path().apply {
                            moveTo(0f, h * 0.55f)
                            quadraticTo(w * 0.35f, h * 0.44f, w * 0.7f, h * 0.58f)
                            quadraticTo(w * 0.88f, h * 0.64f, w, h * 0.55f)
                            lineTo(w, h)
                            lineTo(0f, h)
                            close()
                        }
                        drawPath(wave1, color = Color(0xFFD97757).copy(alpha = 0.22f))
                        val wave2 = Path().apply {
                            moveTo(0f, h * 0.72f)
                            quadraticTo(w * 0.45f, h * 0.82f, w, h * 0.68f)
                            lineTo(w, h)
                            lineTo(0f, h)
                            close()
                        }
                        drawPath(wave2, color = Color(0xFFF4A261).copy(alpha = 0.16f))
                    }

                    WallpaperPreset.BOTANICAL_MINT -> {
                        val c1 = if (isDark) Color(0xFF10241D) else Color(0xFFE3F5EC)
                        val c2 = if (isDark) Color(0xFF18362B) else Color(0xFFCDEBD9)
                        drawRect(brush = Brush.linearGradient(colors = listOf(c1, c2)))
                        val arcColor = if (isDark) Color(0xFF25D366).copy(alpha = 0.12f) else Color(0xFF075E54).copy(alpha = 0.08f)
                        for (i in 1..4) {
                            drawCircle(
                                color = arcColor,
                                radius = w * 0.28f * i,
                                center = Offset(w * 0.9f, h * 0.15f),
                                style = Stroke(width = 3f)
                            )
                            drawCircle(
                                color = arcColor,
                                radius = w * 0.25f * i,
                                center = Offset(w * 0.1f, h * 0.88f),
                                style = Stroke(width = 3f)
                            )
                        }
                    }

                    WallpaperPreset.ROSE_QUARTZ -> {
                        val c1 = if (isDark) Color(0xFF271724) else Color(0xFFFCE8EE)
                        val c2 = if (isDark) Color(0xFF392036) else Color(0xFFF3D9EA)
                        drawRect(brush = Brush.verticalGradient(colors = listOf(c1, c2)))
                        val orbColor = if (isDark) Color(0xFFE91E63).copy(alpha = 0.16f) else Color(0xFFD81B60).copy(alpha = 0.08f)
                        drawCircle(color = orbColor, radius = w * 0.42f, center = Offset(w * 0.2f, h * 0.3f))
                        drawCircle(color = orbColor, radius = w * 0.35f, center = Offset(w * 0.85f, h * 0.72f))
                    }

                    WallpaperPreset.AMOLED_CYBER -> {
                        drawRect(color = Color(0xFF05080A))
                        val grid = Color(0xFF25D366).copy(alpha = 0.08f)
                        val spacing = 72f
                        var x = 0f
                        while (x < w) {
                            drawLine(grid, Offset(x, 0f), Offset(x, h), strokeWidth = 1.2f)
                            x += spacing
                        }
                        var y = 0f
                        while (y < h) {
                            drawLine(grid, Offset(0f, y), Offset(w, y), strokeWidth = 1.2f)
                            y += spacing
                        }
                    }

                    WallpaperPreset.CUSTOM_PHOTO -> {
                        drawRect(color = if (isDark) WhatsAppDarkBg else WhatsAppChatWallpaperLight)
                    }
                }
            }
        }

        // Contrast / dimming overlay scrim
        if (config.dimAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = config.dimAlpha))
            )
        }
    }
}

@Composable
fun ChatWallpaperStudioDialog(
    chatId: String,
    currentConfig: ChatWallpaperConfig,
    onDismiss: () -> Unit,
    onNotice: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var applyToAll by remember { mutableStateOf(!currentConfig.isPerChatOverride) }

    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = ChatPreferencesStore.setCustomPhotoWallpaper(context, chatId, uri, applyToAll)
                onNotice(if (ok) "Custom wallpaper applied!" else "Unable to load selected photo")
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Wallpaper, contentDescription = null, tint = WhatsAppTeal)
                Text("Chat Wallpaper Studio", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Live Mini Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                ) {
                    ChatWallpaperBackdrop(config = currentConfig, modifier = Modifier.fillMaxSize())
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Incoming bubble preview
                        Surface(
                            color = if (isSystemInDarkTheme()) WhatsAppDarkElevated else PureWhite,
                            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 4.dp),
                            shadowElevation = 1.dp
                        ) {
                            Text(
                                text = "Love this chat theme!",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSystemInDarkTheme()) WhatsAppDarkText else WhatsAppInkDark,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        // Outgoing reply bubble preview
                        Surface(
                            color = if (isSystemInDarkTheme()) WhatsAppDarkTeal else WhatsAppOutgoingBubbleLight,
                            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 4.dp),
                            shadowElevation = 1.dp,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(14.dp)
                                            .background(WhatsAppEmerald, RoundedCornerShape(2.dp))
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text("Replying to message", style = MaterialTheme.typography.labelSmall, color = WhatsAppTeal)
                                }
                                Spacer(Modifier.height(3.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Looks clean & unique ✨",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSystemInDarkTheme()) WhatsAppDarkText else WhatsAppInkDark
                                    )
                                    Icon(Icons.Default.DoneAll, contentDescription = null, tint = WhatsAppReadBlue, modifier = Modifier.size(13.dp))
                                }
                            }
                        }
                    }
                }

                // Scope chips (This Chat vs All Chats)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = !applyToAll,
                        onClick = { applyToAll = false },
                        label = { Text("This Chat Only") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WhatsAppEmerald.copy(alpha = 0.2f),
                            selectedLabelColor = WhatsAppTeal
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = applyToAll,
                        onClick = { applyToAll = true },
                        label = { Text("All Chats") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WhatsAppEmerald.copy(alpha = 0.2f),
                            selectedLabelColor = WhatsAppTeal
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Pick Custom Photo from Gallery
                Button(
                    onClick = {
                        galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppEmerald, contentColor = PureWhite),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Set Custom Photo from Gallery")
                }

                Text("Artistic Themes", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    val presets = WallpaperPreset.entries.filter { it != WallpaperPreset.CUSTOM_PHOTO }
                    items(presets, key = { it.id }) { preset ->
                        val selected = currentConfig.preset == preset
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(84.dp)
                                .clickable {
                                    scope.launch {
                                        ChatPreferencesStore.setPresetWallpaper(context, chatId, preset, applyToAll)
                                    }
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 80.dp, height = 96.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (selected) 2.5.dp else 1.dp,
                                        color = if (selected) WhatsAppEmerald else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            ) {
                                ChatWallpaperBackdrop(
                                    config = ChatWallpaperConfig(preset = preset, dimAlpha = 0f),
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (selected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(5.dp)
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(WhatsAppEmerald),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = PureWhite, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = preset.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Dimming slider for contrast
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Wallpaper Dimming", style = MaterialTheme.typography.labelMedium)
                        Text("${(currentConfig.dimAlpha * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = WhatsAppTeal)
                    }
                    Slider(
                        value = currentConfig.dimAlpha,
                        onValueChange = { newDim ->
                            scope.launch {
                                ChatPreferencesStore.setWallpaperDim(context, chatId, newDim, applyToAll)
                            }
                        },
                        valueRange = 0f..0.75f,
                        colors = SliderDefaults.colors(
                            thumbColor = WhatsAppEmerald,
                            activeTrackColor = WhatsAppEmerald
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppTeal, contentColor = PureWhite)
            ) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        ChatPreferencesStore.resetChatWallpaper(context, chatId)
                        onNotice("Wallpaper reset to default")
                    }
                }
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Reset")
            }
        }
    )
}
