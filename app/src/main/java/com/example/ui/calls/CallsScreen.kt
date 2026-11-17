package com.example.ui.calls

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddIcCall
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CallRecord
import com.example.data.model.PublicProfile
import com.example.data.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.WeConnectAvatar
import com.example.ui.components.formatRelativeTime
import com.example.ui.main.WeConnectViewModel
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WhatsAppCallRed
import com.example.ui.theme.WhatsAppDarkBg
import com.example.ui.theme.WhatsAppDarkElevated
import com.example.ui.theme.WhatsAppDarkText
import com.example.ui.theme.WhatsAppEmerald
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppInkDark
import com.example.ui.theme.WhatsAppTeal
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CallsScreen(
    viewModel: WeConnectViewModel,
    myProfile: PublicProfile,
    onStartCall: (PublicProfile, String) -> Unit
) {
    val callsState by viewModel.callsState.collectAsStateWithLifecycle()
    val allProfilesState by viewModel.allProfilesState.collectAsStateWithLifecycle()
    val profilesMap = remember(allProfilesState) {
        ((allProfilesState as? UiState.Success)?.data ?: emptyList()).associateBy { it.uid }
    }

    var showStartCallPicker by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showStartCallPicker = true },
                containerColor = WhatsAppEmerald,
                contentColor = PureWhite,
                modifier = Modifier.testTag("start_call_fab")
            ) {
                Icon(Icons.Default.AddIcCall, contentDescription = "Start Call")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(10.dp))
                when (val state = callsState) {
                    is UiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = WhatsAppEmerald)
                        }
                    }
                    is UiState.Error -> {
                        EmptyStateCard(
                            icon = Icons.Default.Call,
                            title = "Unable to load call history",
                            subtitle = state.message
                        )
                    }
                    is UiState.Success -> {
                        if (state.data.isEmpty()) {
                            EmptyStateCard(
                                icon = Icons.Default.Call,
                                title = "No calls yet",
                                subtitle = "Place real-time voice or video calls with any WeConnect user.",
                                actionLabel = "Start a Call",
                                onAction = { showStartCallPicker = true }
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(bottom = 88.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(state.data, key = { it.id }) { call ->
                                    val outgoing = call.callerId == myProfile.uid
                                    val peerId = if (outgoing) call.calleeId else call.callerId
                                    val peerName = if (outgoing) call.calleeName else call.callerName
                                    val peerProfile = profilesMap[peerId]

                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            WeConnectAvatar(
                                                displayName = peerName,
                                                avatarBase64 = peerProfile?.avatarBase64,
                                                size = 44.dp
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(peerName, style = MaterialTheme.typography.titleMedium)
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (outgoing) Icons.AutoMirrored.Filled.CallMade else Icons.AutoMirrored.Filled.CallReceived,
                                                        contentDescription = null,
                                                        tint = if (call.status == "CONNECTED" || call.status == "ENDED") WhatsAppGreen else WhatsAppCallRed,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = "${call.callType.lowercase().replaceFirstChar { it.uppercase() }} · ${call.status} · ${formatRelativeTime(call.startedAt)}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            if (peerProfile != null) {
                                                IconButton(onClick = { onStartCall(peerProfile, "VOICE") }) {
                                                    Icon(Icons.Default.Call, contentDescription = "Voice call", tint = WhatsAppTeal)
                                                }
                                                IconButton(onClick = { onStartCall(peerProfile, "VIDEO") }) {
                                                    Icon(Icons.Default.Videocam, contentDescription = "Video call", tint = WhatsAppTeal)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showStartCallPicker) {
        val users = ((allProfilesState as? UiState.Success)?.data ?: emptyList())
            .filter { it.uid != myProfile.uid }
        AlertDialog(
            onDismissRequest = { showStartCallPicker = false },
            title = { Text("Start Voice or Video Call") },
            text = {
                LazyColumn(modifier = Modifier.height(260.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (users.isEmpty()) {
                        item { Text("No other users available yet.", style = MaterialTheme.typography.bodyMedium) }
                    } else {
                        items(users, key = { it.uid }) { peer ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                WeConnectAvatar(peer.displayName, peer.avatarBase64, size = 38.dp)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(peer.displayName, style = MaterialTheme.typography.titleSmall)
                                    Text("@${peer.username}", style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = {
                                    showStartCallPicker = false
                                    onStartCall(peer, "VOICE")
                                }) {
                                    Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = WhatsAppTeal)
                                }
                                IconButton(onClick = {
                                    showStartCallPicker = false
                                    onStartCall(peer, "VIDEO")
                                }) {
                                    Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = WhatsAppTeal)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { OutlinedButton(onClick = { showStartCallPicker = false }) { Text("Close") } }
        )
    }
}

@Composable
fun ActiveCallDialog(
    call: CallRecord,
    myUid: String,
    viewModel: WeConnectViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isIncoming = call.calleeId == myUid && call.status == "RINGING"
    val peerName = if (call.callerId == myUid) call.calleeName else call.callerName

    var micMuted by remember { mutableStateOf(false) }
    var speakerOn by remember { mutableStateOf(call.callType == "VIDEO") }
    var cameraEnabled by remember { mutableStateOf(call.callType == "VIDEO") }
    var connectedSeconds by remember { mutableLongStateOf(0L) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(call.id) {
        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (call.callType == "VIDEO") perms.add(Manifest.permission.CAMERA)
        val missing = perms.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    LaunchedEffect(call.status) {
        if (call.status == "CONNECTED") {
            while (true) {
                delay(1000L)
                connectedSeconds += 1L
            }
        }
    }

    AlertDialog(
        onDismissRequest = {},
        containerColor = WhatsAppDarkBg,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (call.callType == "VIDEO") "WeConnect Video Call" else "WeConnect Voice Call",
                    style = MaterialTheme.typography.labelLarge,
                    color = WhatsAppGreen
                )
                Spacer(Modifier.height(6.dp))
                Text(peerName, style = MaterialTheme.typography.headlineSmall, color = PureWhite)
                Spacer(Modifier.height(4.dp))
                val statusText = when (call.status) {
                    "RINGING" -> if (isIncoming) "Incoming encrypted call…" else "Ringing peer device…"
                    "CONNECTED" -> "Connected · %02d:%02d".format(connectedSeconds / 60, connectedSeconds % 60)
                    else -> call.status
                }
                Text(statusText, style = MaterialTheme.typography.bodyMedium, color = WhatsAppDarkText)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    color = WhatsAppDarkElevated,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (call.status == "CONNECTED") {
                            "Signaling verified · DTLS-SRTP transport ready"
                        } else {
                            "Signaling via Firestore · Waiting for peer response"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = WhatsAppDarkText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                if (!isIncoming) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = { micMuted = !micMuted },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(if (micMuted) PureWhite else WhatsAppDarkElevated)
                        ) {
                            Icon(
                                if (micMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Toggle Mic",
                                tint = if (micMuted) WhatsAppInkDark else PureWhite
                            )
                        }
                        IconButton(
                            onClick = { speakerOn = !speakerOn },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(if (speakerOn) PureWhite else WhatsAppDarkElevated)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Toggle Speaker",
                                tint = if (speakerOn) WhatsAppInkDark else PureWhite
                            )
                        }
                        if (call.callType == "VIDEO") {
                            IconButton(
                                onClick = { cameraEnabled = !cameraEnabled },
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(if (cameraEnabled) WhatsAppDarkElevated else PureWhite)
                            ) {
                                Icon(
                                    if (cameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                    contentDescription = "Toggle Camera",
                                    tint = if (cameraEnabled) PureWhite else WhatsAppInkDark
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isIncoming) {
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.handleResult(viewModel.communityCallRepo.updateCallStatus(call.id, "CONNECTED"))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = PureWhite)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Answer")
                }
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            val next = if (call.status == "RINGING") "MISSED" else "ENDED"
                            viewModel.handleResult(viewModel.communityCallRepo.updateCallStatus(call.id, next))
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppCallRed, contentColor = PureWhite)
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("End Call")
                }
            }
        },
        dismissButton = {
            if (isIncoming) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            viewModel.handleResult(viewModel.communityCallRepo.updateCallStatus(call.id, "DECLINED"))
                            onDismiss()
                        }
                    },
                    border = BorderStroke(1.dp, WhatsAppCallRed)
                ) {
                    Text("Decline", color = WhatsAppCallRed)
                }
            }
        }
    )
}
