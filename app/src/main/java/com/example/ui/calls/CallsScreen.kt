package com.example.ui.calls

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.SmartToy
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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

                // Fast test header card
                Card(
                    colors = CardDefaults.cardColors(containerColor = WhatsAppTeal.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, WhatsAppTeal.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(WhatsAppTeal.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, tint = WhatsAppTeal)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "WeConnect Call Test Bot",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Instantly test microphone, speaker & CameraX video stream",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            val testBot = PublicProfile(
                                uid = "${myProfile.uid}_bot",
                                displayName = "WeConnect Test Bot",
                                username = "test_bot"
                            )
                            onStartCall(testBot, "VOICE")
                        }) {
                            Icon(Icons.Default.Call, contentDescription = "Voice Test", tint = WhatsAppTeal)
                        }
                        IconButton(onClick = {
                            val testBot = PublicProfile(
                                uid = "${myProfile.uid}_bot",
                                displayName = "WeConnect Test Bot",
                                username = "test_bot"
                            )
                            onStartCall(testBot, "VIDEO")
                        }) {
                            Icon(Icons.Default.Videocam, contentDescription = "Video Test", tint = WhatsAppTeal)
                        }
                    }
                }

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
                                subtitle = "Place real-time encrypted voice or video calls with any WeConnect user.",
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
                                    val peerName = if (outgoing) call.calleeName.ifBlank { "User" } else call.callerName.ifBlank { "User" }
                                    val peerProfile = profilesMap[peerId] ?: PublicProfile(uid = peerId, displayName = peerName)

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
                                                avatarBase64 = peerProfile.avatarBase64,
                                                size = 46.dp
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(peerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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

    if (showStartCallPicker) {
        val users = ((allProfilesState as? UiState.Success)?.data ?: emptyList())
            .filter { it.uid != myProfile.uid }

        AlertDialog(
            onDismissRequest = { showStartCallPicker = false },
            title = { Text("Start Voice or Video Call") },
            text = {
                LazyColumn(modifier = Modifier.height(290.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Quick Echo & Hardware Test row
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = WhatsAppTeal.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, WhatsAppTeal.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showStartCallPicker = false
                                    val testBot = PublicProfile(
                                        uid = "${myProfile.uid}_bot",
                                        displayName = "WeConnect Test Bot",
                                        username = "test_bot"
                                    )
                                    onStartCall(testBot, "VOICE")
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(WhatsAppTeal.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.SmartToy, contentDescription = null, tint = WhatsAppTeal)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("WeConnect Hardware Test", style = MaterialTheme.typography.titleSmall)
                                    Text("Test voice, video & camera now", style = MaterialTheme.typography.bodySmall, color = WhatsAppTeal)
                                }
                                IconButton(onClick = {
                                    showStartCallPicker = false
                                    val testBot = PublicProfile(
                                        uid = "${myProfile.uid}_bot",
                                        displayName = "WeConnect Test Bot",
                                        username = "test_bot"
                                    )
                                    onStartCall(testBot, "VOICE")
                                }) {
                                    Icon(Icons.Default.Call, contentDescription = "Voice", tint = WhatsAppTeal)
                                }
                                IconButton(onClick = {
                                    showStartCallPicker = false
                                    val testBot = PublicProfile(
                                        uid = "${myProfile.uid}_bot",
                                        displayName = "WeConnect Test Bot",
                                        username = "test_bot"
                                    )
                                    onStartCall(testBot, "VIDEO")
                                }) {
                                    Icon(Icons.Default.Videocam, contentDescription = "Video", tint = WhatsAppTeal)
                                }
                            }
                        }
                    }

                    if (users.isEmpty()) {
                        item {
                            Text(
                                "No other users registered yet. Use the Test Bot above to test voice and video calling features.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
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

/**
 * CameraX live preview composable with Front/Back lens switching.
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    lensFacing: Int = CameraSelector.LENS_FACING_FRONT
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraAvailable by remember { mutableStateOf(true) }

    if (!cameraAvailable) {
        Box(
            modifier = modifier.background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("Camera unavailable in emulator environment", color = WhatsAppDarkText, style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val selector = try {
                        CameraSelector.Builder().requireLensFacing(lensFacing).build()
                    } catch (_: Exception) {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
                } catch (_: Exception) {
                    cameraAvailable = false
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(previewView.context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val selector = try {
                        CameraSelector.Builder().requireLensFacing(lensFacing).build()
                    } catch (_: Exception) {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
                } catch (_: Exception) {
                    cameraAvailable = false
                }
            }, ContextCompat.getMainExecutor(previewView.context))
        },
        modifier = modifier
    )
}

/**
 * Safe audio helper for phone dial tones, incoming ringtones, speakerphone, and haptics.
 */
class CallAudioHelper(private val context: Context) {
    private var toneGenerator: ToneGenerator? = null
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var ringJob: Job? = null

    fun startOutgoingRinging(scope: CoroutineScope) {
        stop()
        ringJob = scope.launch(Dispatchers.IO) {
            try {
                val tg = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 75)
                toneGenerator = tg
                while (isActive) {
                    tg.startTone(ToneGenerator.TONE_SUP_RINGTONE, 1400)
                    delay(3500)
                }
            } catch (_: Exception) {}
        }
    }

    fun startIncomingRinging() {
        stop()
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(context, uri).apply { play() }
            val vib = ContextCompat.getSystemService(context, Vibrator::class.java)
            vibrator = vib
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 1000, 800), 0))
            } else {
                @Suppress("DEPRECATION")
                vib?.vibrate(longArrayOf(0, 1000, 800), 0)
            }
        } catch (_: Exception) {}
    }

    fun playConnectedTone() {
        stop()
        try {
            val tg = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80)
            tg.startTone(ToneGenerator.TONE_PROP_BEEP, 350)
            tg.release()
        } catch (_: Exception) {}
    }

    fun playEndedTone() {
        stop()
        try {
            val tg = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80)
            tg.startTone(ToneGenerator.TONE_SUP_BUSY, 800)
            tg.release()
        } catch (_: Exception) {}
    }

    fun stop() {
        ringJob?.cancel()
        ringJob = null
        try {
            toneGenerator?.stopTone()
            toneGenerator?.release()
        } catch (_: Exception) {}
        toneGenerator = null

        try {
            ringtone?.stop()
        } catch (_: Exception) {}
        ringtone = null

        try {
            vibrator?.cancel()
        } catch (_: Exception) {}
        vibrator = null
    }

    fun setSpeakerphone(speakerOn: Boolean) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager?.isSpeakerphoneOn = speakerOn
        } catch (_: Exception) {}
    }
}

/**
 * Full-screen WhatsApp-style Call Overlay supporting live CameraX preview,
 * tone generation, speakerphone, mute, simulated peer connection for testing, and Firestore signaling.
 */
@Composable
fun ActiveCallDialog(
    call: CallRecord,
    myUid: String,
    viewModel: WeConnectViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val audioHelper = remember(context) { CallAudioHelper(context) }

    val isIncoming = call.calleeId == myUid && call.status == "RINGING"
    val peerName = if (call.callerId == myUid) {
        call.calleeName.ifBlank { "WeConnect User" }
    } else {
        call.callerName.ifBlank { "WeConnect User" }
    }

    var micMuted by remember { mutableStateOf(false) }
    var speakerOn by remember { mutableStateOf(call.callType == "VIDEO") }
    var cameraEnabled by remember { mutableStateOf(call.callType == "VIDEO") }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_FRONT) }
    var connectedSeconds by remember { mutableLongStateOf(0L) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Request permissions
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

    // Audio & Ringtone management
    LaunchedEffect(call.status, isIncoming) {
        when {
            call.status == "RINGING" && isIncoming -> audioHelper.startIncomingRinging()
            call.status == "RINGING" && !isIncoming -> audioHelper.startOutgoingRinging(scope)
            call.status == "CONNECTED" -> {
                audioHelper.playConnectedTone()
                while (true) {
                    delay(1000L)
                    connectedSeconds += 1L
                }
            }
            call.status in listOf("ENDED", "DECLINED", "MISSED") -> {
                audioHelper.playEndedTone()
                delay(800L)
                onDismiss()
            }
        }
    }

    LaunchedEffect(speakerOn) {
        audioHelper.setSpeakerphone(speakerOn)
    }

    DisposableEffect(Unit) {
        onDispose {
            audioHelper.stop()
            audioHelper.setSpeakerphone(false)
        }
    }

    // Back button safely ends or declines call
    BackHandler {
        scope.launch {
            audioHelper.stop()
            val next = if (call.status == "RINGING") (if (isIncoming) "DECLINED" else "MISSED") else "ENDED"
            viewModel.communityCallRepo.updateCallStatus(call.id, next)
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = { /* Prevent accidental background touch dismissal during active call */ },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WhatsAppDarkBg)
        ) {
            // Video background (if video call and camera enabled)
            if (call.callType == "VIDEO" && cameraEnabled) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    lensFacing = lensFacing
                )
                // Gradient scrim on top and bottom for readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.7f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )
            }

            // Floating PiP card for video call showing peer avatar
            if (call.callType == "VIDEO" && call.status == "CONNECTED") {
                Box(
                    modifier = Modifier
                        .padding(top = 70.dp, end = 20.dp)
                        .align(Alignment.TopEnd)
                        .size(110.dp, 150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(WhatsAppDarkElevated)
                        .border(1.5.dp, WhatsAppEmerald.copy(alpha = 0.8f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        WeConnectAvatar(displayName = peerName, avatarBase64 = null, size = 48.dp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = peerName.take(10),
                            style = MaterialTheme.typography.labelSmall,
                            color = PureWhite
                        )
                    }
                }
            }

            // Content container
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top section: Encryption badge, peer name, status
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = WhatsAppGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (call.callType == "VIDEO") "End-to-end Encrypted Video" else "End-to-end Encrypted Voice",
                            style = MaterialTheme.typography.labelSmall,
                            color = WhatsAppGreen
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = peerName,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = PureWhite
                    )

                    Spacer(Modifier.height(6.dp))

                    val statusLabel = when (call.status) {
                        "RINGING" -> if (isIncoming) "Incoming WeConnect Call…" else "Ringing…"
                        "CONNECTED" -> "Connected · %02d:%02d".format(connectedSeconds / 60, connectedSeconds % 60)
                        else -> call.status
                    }
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (call.status == "CONNECTED") WhatsAppGreen else WhatsAppDarkText
                    )
                }

                // Center section: Avatar with pulsating rings (for voice or camera-off)
                if (call.callType == "VOICE" || !cameraEnabled) {
                    Box(
                        modifier = Modifier.size(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (call.status == "RINGING" || (call.status == "CONNECTED" && !micMuted)) {
                            Box(
                                modifier = Modifier
                                    .size(180.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(WhatsAppEmerald.copy(alpha = 0.18f))
                            )
                        }

                        WeConnectAvatar(
                            displayName = peerName,
                            avatarBase64 = null,
                            size = 110.dp
                        )
                    }

                    // Simulated live equalizer waveform bars during active call
                    if (call.status == "CONNECTED") {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(30.dp)
                        ) {
                            val heights = listOf(14.dp, 24.dp, 10.dp, 28.dp, 18.dp, 22.dp, 12.dp)
                            heights.forEachIndexed { idx, h ->
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(if (micMuted) 4.dp else h)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (micMuted) WhatsAppDarkText else WhatsAppEmerald)
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.height(30.dp))
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }

                // Bottom section: Controls & actions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Quick Peer Answer button for easy test in emulator/single-device mode
                    if (call.status == "RINGING" && !isIncoming) {
                        Surface(
                            onClick = {
                                scope.launch {
                                    audioHelper.stop()
                                    viewModel.handleResult(
                                        viewModel.communityCallRepo.updateCallStatus(call.id, "CONNECTED")
                                    )
                                }
                            },
                            shape = RoundedCornerShape(24.dp),
                            color = WhatsAppEmerald.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, WhatsAppEmerald)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(16.dp))
                                Text("⚡ Simulate Peer Answer (Test Mode)", style = MaterialTheme.typography.labelMedium, color = WhatsAppGreen)
                            }
                        }
                    }

                    // Main action buttons
                    if (isIncoming) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Decline button
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            audioHelper.stop()
                                            viewModel.communityCallRepo.updateCallStatus(call.id, "DECLINED")
                                            onDismiss()
                                        }
                                    },
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(WhatsAppCallRed)
                                ) {
                                    Icon(Icons.Default.CallEnd, contentDescription = "Decline", tint = PureWhite, modifier = Modifier.size(32.dp))
                                }
                                Spacer(Modifier.height(8.dp))
                                Text("Decline", color = PureWhite, style = MaterialTheme.typography.labelMedium)
                            }

                            // Answer button
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            audioHelper.stop()
                                            viewModel.communityCallRepo.updateCallStatus(call.id, "CONNECTED")
                                        }
                                    },
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(WhatsAppGreen)
                                ) {
                                    Icon(
                                        if (call.callType == "VIDEO") Icons.Default.Videocam else Icons.Default.Call,
                                        contentDescription = "Answer",
                                        tint = PureWhite,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text("Answer", color = PureWhite, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    } else {
                        // In-Call or Outgoing Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mic Mute
                            IconButton(
                                onClick = { micMuted = !micMuted },
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(if (micMuted) PureWhite else WhatsAppDarkElevated)
                            ) {
                                Icon(
                                    if (micMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = "Mic",
                                    tint = if (micMuted) WhatsAppInkDark else PureWhite
                                )
                            }

                            // Speaker Toggle
                            IconButton(
                                onClick = { speakerOn = !speakerOn },
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(if (speakerOn) PureWhite else WhatsAppDarkElevated)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Speaker",
                                    tint = if (speakerOn) WhatsAppInkDark else PureWhite
                                )
                            }

                            // Video toggles
                            if (call.callType == "VIDEO") {
                                // Camera toggle
                                IconButton(
                                    onClick = { cameraEnabled = !cameraEnabled },
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(if (cameraEnabled) WhatsAppDarkElevated else PureWhite)
                                ) {
                                    Icon(
                                        if (cameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                        contentDescription = "Camera",
                                        tint = if (cameraEnabled) PureWhite else WhatsAppInkDark
                                    )
                                }

                                // Switch Camera (Front/Back)
                                IconButton(
                                    onClick = {
                                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                                            CameraSelector.LENS_FACING_BACK
                                        } else {
                                            CameraSelector.LENS_FACING_FRONT
                                        }
                                    },
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(WhatsAppDarkElevated)
                                ) {
                                    Icon(
                                        Icons.Default.Cameraswitch,
                                        contentDescription = "Switch Camera",
                                        tint = PureWhite
                                    )
                                }
                            }

                            // End Call button
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        audioHelper.stop()
                                        val next = if (call.status == "RINGING") "MISSED" else "ENDED"
                                        viewModel.communityCallRepo.updateCallStatus(call.id, next)
                                        onDismiss()
                                    }
                                },
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .background(WhatsAppCallRed)
                            ) {
                                Icon(
                                    Icons.Default.CallEnd,
                                    contentDescription = "End Call",
                                    tint = PureWhite,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
