package com.example.ui.chats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.media.MediaUtils
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.PublicProfile
import com.example.ui.components.formatTimeClock
import com.example.ui.main.WeConnectViewModel
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WhatsAppDarkBg
import com.example.ui.theme.WhatsAppEmerald
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppTeal
import kotlinx.coroutines.launch

@Composable
fun PhotoLightboxDialog(
    message: ChatMessage,
    captionText: String,
    onDismiss: () -> Unit,
    onDownload: (String) -> Unit,
    onReply: () -> Unit,
    onForward: () -> Unit
) {
    val bmp = remember(message.mediaBase64) { MediaUtils.decodeBase64ToImageBitmap(message.mediaBase64) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (bmp != null) {
                Image(
                    bitmap = bmp,
                    contentDescription = "Full screen photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1.2f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        scale = 2.4f
                                    }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val nextScale = (scale * zoom).coerceIn(1f, 4f)
                                scale = nextScale
                                offset = if (nextScale > 1.05f) offset + pan else Offset.Zero
                            }
                        }
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                )
            }

            // Top Header
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = PureWhite)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = message.senderName,
                            style = MaterialTheme.typography.titleMedium,
                            color = PureWhite
                        )
                        Text(
                            text = formatTimeClock(message.createdAt).ifBlank { "Just now" },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                    if (!message.mediaBase64.isNullOrBlank()) {
                        IconButton(onClick = { onDownload(message.mediaBase64) }) {
                            Icon(Icons.Default.Download, contentDescription = "Save to Gallery", tint = PureWhite)
                        }
                    }
                }
            }

            // Bottom Caption + Quick Actions
            Surface(
                color = Color.Black.copy(alpha = 0.68f),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (captionText.isNotBlank() && captionText != "Photo") {
                        Text(
                            text = captionText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PureWhite
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(
                            onClick = { onDismiss(); onReply() },
                            border = BorderStroke(1.dp, PureWhite.copy(alpha = 0.45f))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = PureWhite, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Reply", color = PureWhite)
                        }
                        if (!message.mediaBase64.isNullOrBlank()) {
                            Button(
                                onClick = { onDownload(message.mediaBase64) },
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppEmerald, contentColor = PureWhite)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Save")
                            }
                        }
                        OutlinedButton(
                            onClick = { onDismiss(); onForward() },
                            border = BorderStroke(1.dp, PureWhite.copy(alpha = 0.45f))
                        ) {
                            Icon(Icons.Default.Forward, contentDescription = null, tint = PureWhite, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Forward", color = PureWhite)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StarredMessagesDialog(
    starredMessages: List<Pair<ChatMessage, String>>,
    onDismiss: () -> Unit,
    onJumpToMessage: (String) -> Unit,
    onUnstar: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300))
                Text("Starred Messages (${starredMessages.size})")
            }
        },
        text = {
            if (starredMessages.isEmpty()) {
                Text(
                    text = "Tap any message in this chat and choose Star to keep important messages handy here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(starredMessages, key = { it.first.id }) { (msg, decrypted) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismiss()
                                    onJumpToMessage(msg.id)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = msg.senderName,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = WhatsAppTeal
                                        )
                                        Text(
                                            text = formatTimeClock(msg.createdAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = decrypted,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { onUnstar(msg.id) }) {
                                    Icon(Icons.Default.Star, contentDescription = "Unstar", tint = Color(0xFFFFB300))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppTeal, contentColor = PureWhite)
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ChatInfoDialog(
    conversation: Conversation,
    myProfile: PublicProfile,
    peerProfile: PublicProfile?,
    viewModel: WeConnectViewModel,
    onOpenWallpaperStudio: () -> Unit,
    onDismiss: () -> Unit,
    onBackAfterLeave: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var groupTitle by remember { mutableStateOf(conversation.title) }
    var groupDesc by remember { mutableStateOf(conversation.description) }
    var disappearingHours by remember { mutableStateOf(conversation.disappearingHours) }
    val isAdmin = myProfile.uid in conversation.adminUids

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (conversation.type == "GROUP") "Group Details" else "Chat Settings & Security") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (conversation.type == "DIRECT" && peerProfile != null) {
                    val fingerprint = remember(peerProfile.publicKeyBase64) {
                        peerProfile.publicKeyBase64?.let { viewModel.crypto.computeFingerprint(it) } ?: "Key pending"
                    }
                    Text("End-to-End Encryption (ECDH P-256 + AES-256-GCM)", style = MaterialTheme.typography.titleSmall, color = WhatsAppTeal)
                    Text("Peer Fingerprint:\n$fingerprint", style = MaterialTheme.typography.bodySmall)
                }

                if (conversation.type == "GROUP") {
                    Text("Invite Code: ${conversation.inviteCode ?: "N/A"}", style = MaterialTheme.typography.labelLarge, color = WhatsAppTeal)
                    if (isAdmin) {
                        OutlinedTextField(
                            value = groupTitle,
                            onValueChange = { groupTitle = it.take(80) },
                            label = { Text("Group Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = groupDesc,
                            onValueChange = { groupDesc = it.take(250) },
                            label = { Text("Description") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenWallpaperStudio()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Wallpaper, contentDescription = null, tint = WhatsAppTeal)
                    Spacer(Modifier.width(8.dp))
                    Text("Change Chat Wallpaper & Theme")
                }

                Text("Disappearing Messages", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0L to "Off", 24L to "24h", 168L to "7d").forEach { (hrs, lbl) ->
                        FilterChip(
                            selected = disappearingHours == hrs,
                            onClick = { disappearingHours = hrs },
                            label = { Text(lbl) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                viewModel.handleResult(viewModel.messagingRepo.toggleMute(conversation), "Updated mute setting")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (myProfile.uid in conversation.mutedBy) "Unmute" else "Mute")
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                viewModel.handleResult(viewModel.messagingRepo.toggleArchive(conversation), "Updated archive")
                                onDismiss()
                                onBackAfterLeave()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (myProfile.uid in conversation.archivedBy) "Unarchive" else "Archive")
                    }
                }

                if (conversation.type == "DIRECT" && peerProfile != null) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                viewModel.handleResult(
                                    viewModel.authRepo.blockUser(peerProfile.uid, peerProfile.username),
                                    "Blocked @${peerProfile.username}"
                                )
                                onDismiss()
                                onBackAfterLeave()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Block @${peerProfile.username}")
                    }
                }
            }
        },
        confirmButton = {
            if (isAdmin) {
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.handleResult(
                                viewModel.messagingRepo.updateGroupSettings(
                                    conversation, groupTitle, groupDesc, disappearingHours
                                ),
                                "Settings updated"
                            ) { onDismiss() }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppEmerald, contentColor = PureWhite)
                ) {
                    Text("Save")
                }
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Close") } }
    )
}
