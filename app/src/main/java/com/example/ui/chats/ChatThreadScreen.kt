package com.example.ui.chats

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.media.ChatPreferencesStore
import com.example.data.media.ChatWallpaperConfig
import com.example.data.media.MediaUtils
import com.example.data.media.VoiceNotePlayer
import com.example.data.media.VoiceNoteRecorder
import com.example.data.model.ChatMessage
import com.example.data.model.PublicProfile
import com.example.data.model.UiState
import com.example.data.model.isEffectivelyOnline
import com.example.ui.components.GeminiChatAssistSheet
import com.example.ui.components.WeConnectAvatar
import com.example.ui.components.formatLastSeenTime
import com.example.ui.components.formatTimeClock
import com.example.ui.main.AiViewModel
import com.example.ui.main.PendingOutgoingMessage
import com.example.ui.main.WeConnectViewModel
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WhatsAppCallRed
import com.example.ui.theme.WhatsAppDarkElevated
import com.example.ui.theme.WhatsAppDarkTeal
import com.example.ui.theme.WhatsAppDarkText
import com.example.ui.theme.WhatsAppEmerald
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppInkDark
import com.example.ui.theme.WhatsAppOfflineGray
import com.example.ui.theme.WhatsAppOutgoingBubbleLight
import com.example.ui.theme.WhatsAppReadBlue
import com.example.ui.theme.WhatsAppTeal
import com.google.firebase.Timestamp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadScreen(
    chatId: String,
    viewModel: WeConnectViewModel,
    aiViewModel: AiViewModel,
    myProfile: PublicProfile,
    onBack: () -> Unit,
    onStartCall: (PublicProfile, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    val conversation by remember(chatId) {
        viewModel.messagingRepo.observeConversation(chatId)
    }.collectAsStateWithLifecycle(initialValue = null)

    val messages by remember(chatId, myProfile.uid) {
        viewModel.messagingRepo.observeMessages(chatId, myProfile.uid)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val typingList by remember(chatId, myProfile.uid) {
        viewModel.messagingRepo.observeTyping(chatId, myProfile.uid)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val wallpaperConfig by remember(chatId) {
        ChatPreferencesStore.observeChatWallpaper(context, chatId)
    }.collectAsStateWithLifecycle(initialValue = ChatWallpaperConfig())

    val starredMessageIds by remember(chatId) {
        ChatPreferencesStore.observeStarredMessageIds(context, chatId)
    }.collectAsStateWithLifecycle(initialValue = emptySet())

    val privateUser by viewModel.privateUserState.collectAsStateWithLifecycle()
    val allProfilesState by viewModel.allProfilesState.collectAsStateWithLifecycle()
    val isOnlineNetwork by viewModel.isOnlineNetwork.collectAsStateWithLifecycle()
    val nowMs by viewModel.nowMsTicker.collectAsStateWithLifecycle()
    val pendingMessages by viewModel.pendingMessages.collectAsStateWithLifecycle()
    val chatPendingMessages = remember(pendingMessages, chatId) {
        pendingMessages.filter { it.chatId == chatId }
    }
    val profilesMap = remember(allProfilesState) {
        ((allProfilesState as? UiState.Success)?.data ?: emptyList()).associateBy { it.uid }
    }

    val conv = conversation
    val peerUid = conv?.memberUids?.firstOrNull { it != myProfile.uid }
    val peerProfile = remember(peerUid, profilesMap, conv) {
        val found = peerUid?.let { profilesMap[it] }
        if (found != null) found
        else if (peerUid != null) {
            val name = conv?.memberNames?.get(peerUid)?.ifBlank { "User" } ?: "WeConnect User"
            PublicProfile(
                uid = peerUid,
                displayName = name,
                username = "user_${peerUid.take(6).lowercase()}"
            )
        } else null
    }

    var inputText by remember { mutableStateOf("") }
    var replyTarget by remember { mutableStateOf<Pair<ChatMessage, String>?>(null) }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var forwardingMessage by remember { mutableStateOf<Pair<ChatMessage, String>?>(null) }
    var lightboxMessage by remember { mutableStateOf<Pair<ChatMessage, String>?>(null) }
    var searchInChat by remember { mutableStateOf(false) }
    var searchTerm by remember { mutableStateOf("") }
    var showChatInfo by remember { mutableStateOf(false) }
    var showWallpaperStudio by remember { mutableStateOf(false) }
    var showStarredDialog by remember { mutableStateOf(false) }
    var showTopOverflowMenu by remember { mutableStateOf(false) }
    var showGeminiAssist by remember { mutableStateOf(false) }
    var playingVoiceId by remember { mutableStateOf<String?>(null) }
    var highlightedMessageId by remember { mutableStateOf<String?>(null) }
    val decryptedCache = remember { mutableStateMapOf<String, String>() }

    val jumpToMessage: (String) -> Unit = { targetId ->
        val idx = messages.indexOfFirst { it.id == targetId }
        if (idx >= 0) {
            scope.launch {
                listState.animateScrollToItem(idx)
                highlightedMessageId = targetId
                delay(1500L)
                if (highlightedMessageId == targetId) highlightedMessageId = null
            }
        } else {
            viewModel.emitNotice("Original message not in current view")
        }
    }

    // Voice note recorder
    val recorder = remember(context) { VoiceNoteRecorder(context) }
    var isRecording by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            recorder.release()
            VoiceNotePlayer.stop()
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (recorder.startRecording()) isRecording = true
            else viewModel.emitNotice("Unable to access microphone")
        } else {
            viewModel.emitNotice("Microphone permission is needed for voice notes")
        }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val c = conv ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            val reply = replyTarget
            val caption = inputText.ifBlank { "Photo" }
            replyTarget = null
            inputText = ""
            scope.launch {
                val b64 = MediaUtils.compressUriToBase64(context, uri, maxDimension = 720, maxBase64Chars = 250_000)
                if (b64 != null) {
                    viewModel.sendMessageOrQueueOffline(
                        conversation = c,
                        senderProfile = myProfile,
                        plaintext = caption,
                        messageType = "IMAGE",
                        mediaBase64 = b64,
                        mediaMimeType = "image/jpeg",
                        replyToMessageId = reply?.first?.id,
                        replyToPreview = reply?.let { formatStructuredReply(it.first, it.second) }
                    )
                } else {
                    viewModel.emitNotice("Photo exceeded max size")
                }
            }
        }
    }

    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val c = conv ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            val reply = replyTarget
            replyTarget = null
            scope.launch {
                val fileName = MediaUtils.getFileDisplayName(context, uri)
                val b64 = MediaUtils.readSmallFileBase64(context, uri)
                if (b64 != null) {
                    viewModel.sendMessageOrQueueOffline(
                        conversation = c,
                        senderProfile = myProfile,
                        plaintext = "Shared file: $fileName",
                        messageType = "FILE",
                        mediaBase64 = b64,
                        fileName = fileName,
                        replyToMessageId = reply?.first?.id,
                        replyToPreview = reply?.let { formatStructuredReply(it.first, it.second) }
                    )
                } else {
                    viewModel.emitNotice("Document exceeds 220 KB limit")
                }
            }
        }
    }

    // Mark messages read/delivered
    LaunchedEffect(messages.size, chatPendingMessages.size, conv?.id) {
        if (conv != null && messages.isNotEmpty()) {
            viewModel.messagingRepo.markMessagesRead(
                chatId = conv.id,
                messages = messages,
                readReceiptsEnabled = privateUser?.readReceiptsEnabled != false
            )
        }
        val totalItems = messages.size + chatPendingMessages.size
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    // Typing indicator debounced
    LaunchedEffect(inputText, conv?.id, isOnlineNetwork) {
        val c = conv ?: return@LaunchedEffect
        if (!isOnlineNetwork) return@LaunchedEffect
        if (inputText.isNotBlank()) {
            viewModel.messagingRepo.setTyping(c, myProfile.displayName, true)
            delay(2500)
            viewModel.messagingRepo.setTyping(c, myProfile.displayName, false)
        } else {
            viewModel.messagingRepo.setTyping(c, myProfile.displayName, false)
        }
    }

    val showScrollToBottom by remember {
        derivedStateOf {
            messages.isNotEmpty() && listState.firstVisibleItemIndex < (messages.lastIndex - 2).coerceAtLeast(0)
        }
    }

    val title = conv?.let { conversationDisplayName(it, myProfile.uid, profilesMap) } ?: "Chat"

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    val peerIsOnline = peerProfile?.isEffectivelyOnline(nowMs) == true
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showChatInfo = true }
                    ) {
                        WeConnectAvatar(
                            displayName = title,
                            avatarBase64 = peerProfile?.avatarBase64,
                            size = 36.dp,
                            showOnlineDot = conv?.type == "DIRECT" && peerProfile != null && peerProfile.onlineStatusVisible,
                            isOnline = peerIsOnline
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (conv?.type == "DIRECT") {
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Default.Lock, contentDescription = "Encrypted", tint = WhatsAppGreen, modifier = Modifier.size(13.dp))
                                }
                                if (conv != null && myProfile.uid in conv.mutedBy) {
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Default.NotificationsOff, contentDescription = "Muted", tint = WhatsAppOfflineGray, modifier = Modifier.size(13.dp))
                                }
                            }
                            val subtitle = when {
                                !isOnlineNetwork -> "Waiting for network…"
                                typingList.isNotEmpty() -> "${typingList.first().displayName} is typing…"
                                conv?.type == "GROUP" -> "${conv.memberUids.size} members"
                                peerIsOnline -> "online"
                                peerProfile != null && peerProfile.onlineStatusVisible ->
                                    if (peerProfile.lastSeenAt != null) "last seen ${formatLastSeenTime(peerProfile.lastSeenAt)}" else "offline"
                                else -> "end-to-end encrypted"
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (conv?.type == "DIRECT" && peerProfile != null && peerProfile.onlineStatusVisible && typingList.isEmpty() && isOnlineNetwork) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (peerIsOnline) WhatsAppGreen else WhatsAppOfflineGray)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                }
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isOnlineNetwork && (typingList.isNotEmpty() || peerIsOnline)) WhatsAppEmerald else WhatsAppOfflineGray
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (peerProfile != null) {
                        IconButton(onClick = { onStartCall(peerProfile, "VOICE") }) {
                            Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = { onStartCall(peerProfile, "VIDEO") }) {
                            Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Box {
                        IconButton(onClick = { showTopOverflowMenu = true }, modifier = Modifier.testTag("chat_more_menu")) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Chat Options")
                        }
                        DropdownMenu(expanded = showTopOverflowMenu, onDismissRequest = { showTopOverflowMenu = false }) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Wallpaper, contentDescription = null, tint = WhatsAppTeal) },
                                text = { Text("Chat Wallpaper") },
                                onClick = { showTopOverflowMenu = false; showWallpaperStudio = true }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300)) },
                                text = { Text("Starred Messages (${starredMessageIds.size})") },
                                onClick = { showTopOverflowMenu = false; showStarredDialog = true }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                text = { Text(if (searchInChat) "Hide Search" else "Search Chat") },
                                onClick = { showTopOverflowMenu = false; searchInChat = !searchInChat }
                            )
                            if (conv != null) {
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.NotificationsOff, contentDescription = null) },
                                    text = { Text(if (myProfile.uid in conv.mutedBy) "Unmute Chat" else "Mute Notifications") },
                                    onClick = {
                                        showTopOverflowMenu = false
                                        scope.launch {
                                            viewModel.handleResult(viewModel.messagingRepo.toggleMute(conv), "Mute preference updated")
                                        }
                                    }
                                )
                            }
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                text = { Text("Security & Settings") },
                                onClick = { showTopOverflowMenu = false; showChatInfo = true }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .navigationBarsPadding()
        ) {
            // Dynamic Chat Background Wallpaper
            ChatWallpaperBackdrop(
                config = wallpaperConfig,
                modifier = Modifier.fillMaxSize()
            )

            Column(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(visible = !isOnlineNetwork) {
                    Surface(
                        color = WhatsAppOfflineGray.copy(alpha = 0.22f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.WifiOff,
                                contentDescription = "Offline",
                                tint = WhatsAppOfflineGray,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "You are offline · Messages wait in queue until connected",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                AnimatedVisibility(visible = searchInChat) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        tonalElevation = 2.dp
                    ) {
                        OutlinedTextField(
                            value = searchTerm,
                            onValueChange = { searchTerm = it },
                            placeholder = { Text("Search messages in chat…") },
                            trailingIcon = {
                                IconButton(onClick = { searchInChat = false; searchTerm = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Search")
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                if (!conv?.pinnedMessagePreview.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                conv?.pinnedMessageId?.let { jumpToMessage(it) }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PushPin, contentDescription = null, tint = WhatsAppTeal, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Pinned Message", style = MaterialTheme.typography.labelSmall, color = WhatsAppTeal, fontWeight = FontWeight.Bold)
                                Text(
                                    text = conv?.pinnedMessagePreview ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Messages Stream
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (messages.isEmpty() && chatPendingMessages.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = WhatsAppTeal, modifier = Modifier.size(28.dp))
                                    Text(
                                        text = "End-to-End Encrypted Chat",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Swipe right on any message to reply, set custom wallpapers, or use Gemini AI to craft replies.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        AssistChip(
                                            onClick = {
                                                val c = conv ?: return@AssistChip
                                                scope.launch {
                                                    viewModel.sendMessageOrQueueOffline(
                                                        conversation = c,
                                                        senderProfile = myProfile,
                                                        plaintext = "Hey there! 👋"
                                                    )
                                                }
                                            },
                                            label = { Text("Say Hi 👋") },
                                            colors = AssistChipDefaults.assistChipColors(containerColor = WhatsAppEmerald.copy(alpha = 0.14f))
                                        )
                                        AssistChip(
                                            onClick = { showWallpaperStudio = true },
                                            label = { Text("Wallpaper 🎨") }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            itemsIndexed(messages, key = { _, m -> m.id }) { index, msg ->
                                var decryptedText by remember(msg.id, msg.ciphertext, msg.edited, msg.deletedForAll) {
                                    mutableStateOf(if (msg.encrypted) "Decrypting…" else msg.ciphertext)
                                }
                                LaunchedEffect(msg.id, msg.ciphertext, msg.edited, msg.deletedForAll, conv?.memberKeys) {
                                    val c = conv ?: return@LaunchedEffect
                                    val text = viewModel.messagingRepo.decryptMessageText(c, msg, myProfile.uid)
                                    decryptedText = text
                                    if (!msg.deletedForAll && text.isNotBlank()) {
                                        decryptedCache[msg.id] = "${msg.senderName}: $text"
                                    }
                                }

                                if (searchTerm.isNotBlank() && !decryptedText.contains(searchTerm, ignoreCase = true)) {
                                    return@itemsIndexed
                                }

                                // Day separator pill when date changes
                                val prevMsg = if (index > 0) messages[index - 1] else null
                                val showDaySeparator = prevMsg == null || !isSameDay(prevMsg.createdAt, msg.createdAt)
                                if (showDaySeparator) {
                                    DateSeparatorPill(ts = msg.createdAt)
                                }

                                SwipeableMessageItem(
                                    enabled = !msg.deletedForAll,
                                    onSwipeToReply = {
                                        replyTarget = msg to decryptedText
                                    }
                                ) {
                                    MessageBubble(
                                        message = msg,
                                        decryptedText = decryptedText,
                                        isMine = msg.senderId == myProfile.uid,
                                        isGroup = conv?.type == "GROUP",
                                        isStarred = msg.id in starredMessageIds,
                                        isHighlighted = highlightedMessageId == msg.id,
                                        isPlayingVoice = playingVoiceId == msg.id,
                                        onPlayVoice = { b64 ->
                                            if (playingVoiceId == msg.id) {
                                                VoiceNotePlayer.stop()
                                                playingVoiceId = null
                                            } else {
                                                playingVoiceId = msg.id
                                                VoiceNotePlayer.playBase64(context, b64) { playingVoiceId = null }
                                            }
                                        },
                                        onReply = { replyTarget = msg to decryptedText },
                                        onReact = { emoji ->
                                            scope.launch { viewModel.messagingRepo.toggleReaction(chatId, msg, emoji) }
                                        },
                                        onCopy = {
                                            clipboard.setText(AnnotatedString(decryptedText))
                                            viewModel.emitNotice("Message copied")
                                        },
                                        onToggleStar = {
                                            scope.launch {
                                                val nowStarred = ChatPreferencesStore.toggleStarMessage(context, chatId, msg.id)
                                                viewModel.emitNotice(if (nowStarred) "Message starred ⭐" else "Removed from starred")
                                            }
                                        },
                                        onPin = {
                                            scope.launch {
                                                viewModel.handleResult(
                                                    viewModel.messagingRepo.pinMessage(chatId, msg.id, decryptedText),
                                                    "Message pinned"
                                                )
                                            }
                                        },
                                        onForward = { forwardingMessage = msg to decryptedText },
                                        onEdit = {
                                            editingMessage = msg
                                            inputText = decryptedText
                                        },
                                        onDelete = {
                                            scope.launch {
                                                viewModel.handleResult(viewModel.messagingRepo.deleteMessageForAll(chatId, msg.id))
                                            }
                                        },
                                        onDownloadImage = { b64 ->
                                            scope.launch {
                                                val success = MediaUtils.saveBase64ImageToGallery(context, b64)
                                                viewModel.emitNotice(if (success) "Photo saved to Gallery!" else "Failed to save photo")
                                            }
                                        },
                                        onOpenPhotoLightbox = {
                                            lightboxMessage = msg to decryptedText
                                        },
                                        onDownloadFile = { b64, fileName, mime ->
                                            scope.launch {
                                                val success = MediaUtils.saveBase64FileToDownloads(context, b64, fileName, mime)
                                                viewModel.emitNotice(if (success) "Saved $fileName to Downloads/WeConnect" else "Failed to save file")
                                            }
                                        },
                                        onJumpToReply = { targetId ->
                                            jumpToMessage(targetId)
                                        }
                                    )
                                }
                            }
                            items(chatPendingMessages, key = { it.id }) { pending ->
                                PendingMessageBubble(
                                    pending = pending,
                                    onCancel = { viewModel.cancelPendingMessage(pending.id) }
                                )
                            }
                        }
                    }

                    // Scroll-to-bottom FAB
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showScrollToBottom,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(14.dp)
                    ) {
                        SmallFloatingActionButton(
                            onClick = {
                                scope.launch {
                                    if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            contentColor = WhatsAppTeal,
                            elevation = FloatingActionButtonDefaults.elevation(4.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Scroll to bottom")
                        }
                    }
                }

                // Reply / Edit Banner (Animated WhatsApp style with colored left accent stripe)
                AnimatedVisibility(visible = replyTarget != null || editingMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(38.dp)
                                    .background(
                                        if (editingMessage != null) WhatsAppTeal else WhatsAppEmerald,
                                        RoundedCornerShape(4.dp)
                                    )
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (editingMessage != null) {
                                        "Editing message"
                                    } else {
                                        "Replying to ${replyTarget?.first?.senderName ?: "message"}"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (editingMessage != null) WhatsAppTeal else WhatsAppEmerald
                                )
                                Text(
                                    text = if (editingMessage != null) {
                                        inputText.take(80)
                                    } else {
                                        replyTarget?.second?.take(90) ?: ""
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = {
                                    replyTarget = null
                                    editingMessage = null
                                    if (editingMessage != null) inputText = ""
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel Reply/Edit", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Message Composer (sleek floating pill input bar)
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Attach Photo", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(
                            onClick = { docPicker.launch(arrayOf("*/*")) }
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = "Attach File", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(
                            onClick = { showGeminiAssist = true },
                            modifier = Modifier.testTag("chat_gemini_assist_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Gemini Reply & Rewrite Assist", tint = WhatsAppTeal)
                        }
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it.take(1800) },
                            placeholder = {
                                Text(
                                    text = when {
                                        isRecording -> "Recording voice note…"
                                        !isOnlineNetwork -> "Offline · Waiting for network…"
                                        replyTarget != null -> "Write a reply…"
                                        else -> "Message"
                                    }
                                )
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                focusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_message_input"),
                            maxLines = 4
                        )
                        Spacer(Modifier.width(6.dp))
                        if (inputText.isBlank() && editingMessage == null) {
                            IconButton(
                                onClick = {
                                    val c = conv ?: return@IconButton
                                    if (isRecording) {
                                        isRecording = false
                                        val audioB64 = recorder.stopAndEncodeBase64()
                                        if (audioB64 != null) {
                                            val reply = replyTarget
                                            replyTarget = null
                                            scope.launch {
                                                viewModel.sendMessageOrQueueOffline(
                                                    conversation = c,
                                                    senderProfile = myProfile,
                                                    plaintext = "Voice note",
                                                    messageType = "VOICE",
                                                    mediaBase64 = audioB64,
                                                    mediaMimeType = "audio/mp4",
                                                    replyToMessageId = reply?.first?.id,
                                                    replyToPreview = reply?.let { formatStructuredReply(it.first, it.second) }
                                                )
                                            }
                                        } else {
                                            viewModel.emitNotice("Voice note too short or too large")
                                        }
                                    } else {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context, Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (granted) {
                                            if (recorder.startRecording()) isRecording = true
                                        } else {
                                            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (isRecording) WhatsAppCallRed else if (isOnlineNetwork) WhatsAppEmerald else WhatsAppOfflineGray)
                                    .testTag("voice_record_button")
                            ) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = if (isRecording) "Stop and Send Voice" else "Record Voice Note",
                                    tint = PureWhite
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    val c = conv ?: return@IconButton
                                    val txt = inputText
                                    val editing = editingMessage
                                    val reply = replyTarget
                                    if (editing != null) {
                                        if (!isOnlineNetwork) {
                                            viewModel.emitNotice("You are offline · Connect to the internet to edit messages")
                                            return@IconButton
                                        }
                                        inputText = ""
                                        editingMessage = null
                                        replyTarget = null
                                        scope.launch {
                                            viewModel.handleResult(viewModel.messagingRepo.editMessage(c, editing, txt))
                                        }
                                    } else {
                                        inputText = ""
                                        replyTarget = null
                                        scope.launch {
                                            viewModel.sendMessageOrQueueOffline(
                                                conversation = c,
                                                senderProfile = myProfile,
                                                plaintext = txt,
                                                replyToMessageId = reply?.first?.id,
                                                replyToPreview = reply?.let { formatStructuredReply(it.first, it.second) }
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (isOnlineNetwork) WhatsAppEmerald else WhatsAppOfflineGray)
                                    .testTag("send_message_button")
                            ) {
                                Icon(
                                    imageVector = if (isOnlineNetwork) Icons.AutoMirrored.Filled.Send else Icons.Default.Schedule,
                                    contentDescription = if (isOnlineNetwork) "Send" else "Waiting for network",
                                    tint = PureWhite
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showGeminiAssist) {
        val recentLines = remember(messages, decryptedCache.size) {
            messages.takeLast(6).mapNotNull { decryptedCache[it.id] }
        }
        GeminiChatAssistSheet(
            aiViewModel = aiViewModel,
            currentDraft = inputText,
            recentConversationLines = recentLines,
            myDisplayName = myProfile.displayName,
            onApplyText = { inputText = it.take(1800) },
            onDismiss = { showGeminiAssist = false }
        )
    }

    forwardingMessage?.let { (msg, text) ->
        val convs = (viewModel.conversationsState.collectAsStateWithLifecycle().value as? UiState.Success)?.data ?: emptyList()
        AlertDialog(
            onDismissRequest = { forwardingMessage = null },
            title = { Text("Forward Message") },
            text = {
                androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.height(220.dp)) {
                    items(convs.filter { it.id != chatId }.size) { idx ->
                        val targetConv = convs.filter { it.id != chatId }[idx]
                        val name = conversationDisplayName(targetConv, myProfile.uid, profilesMap)
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        viewModel.sendMessageOrQueueOffline(
                                            conversation = targetConv,
                                            senderProfile = myProfile,
                                            plaintext = text,
                                            messageType = msg.messageType,
                                            mediaBase64 = msg.mediaBase64,
                                            fileName = msg.fileName,
                                            forwarded = true
                                        )
                                        if (isOnlineNetwork) viewModel.emitNotice("Forwarded to $name")
                                        forwardingMessage = null
                                    }
                                }
                                .padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { OutlinedButton(onClick = { forwardingMessage = null }) { Text("Cancel") } }
        )
    }

    if (showWallpaperStudio) {
        ChatWallpaperStudioDialog(
            chatId = chatId,
            currentConfig = wallpaperConfig,
            onDismiss = { showWallpaperStudio = false },
            onNotice = { viewModel.emitNotice(it) }
        )
    }

    if (showStarredDialog) {
        val starredList = remember(messages, starredMessageIds, decryptedCache.size) {
            messages.filter { it.id in starredMessageIds && !it.deletedForAll }
                .map { msg -> msg to (decryptedCache[msg.id]?.substringAfter(": ") ?: msg.ciphertext) }
        }
        StarredMessagesDialog(
            starredMessages = starredList,
            onDismiss = { showStarredDialog = false },
            onJumpToMessage = jumpToMessage,
            onUnstar = { id ->
                scope.launch {
                    ChatPreferencesStore.toggleStarMessage(context, chatId, id)
                }
            }
        )
    }

    lightboxMessage?.let { (msg, caption) ->
        PhotoLightboxDialog(
            message = msg,
            captionText = caption,
            onDismiss = { lightboxMessage = null },
            onDownload = { b64 ->
                scope.launch {
                    val ok = MediaUtils.saveBase64ImageToGallery(context, b64)
                    viewModel.emitNotice(if (ok) "Photo saved to Gallery!" else "Failed to save photo")
                }
            },
            onReply = { replyTarget = msg to caption },
            onForward = { forwardingMessage = msg to caption }
        )
    }

    if (showChatInfo && conv != null) {
        ChatInfoDialog(
            conversation = conv,
            myProfile = myProfile,
            peerProfile = peerProfile,
            viewModel = viewModel,
            onOpenWallpaperStudio = { showWallpaperStudio = true },
            onDismiss = { showChatInfo = false },
            onBackAfterLeave = onBack
        )
    }
}

private fun formatStructuredReply(msg: ChatMessage, decryptedText: String): String {
    val sender = msg.senderName.trim().ifBlank { "User" }.take(30)
    val body = when {
        msg.messageType == "IMAGE" -> "📷 Photo"
        msg.messageType == "VOICE" -> "🎤 Voice note"
        msg.messageType == "FILE" -> "📎 ${msg.fileName ?: "Document"}"
        else -> decryptedText.trim().ifBlank { "Message" }
    }
    return "$sender::$body".take(160)
}

@Composable
private fun SwipeableMessageItem(
    enabled: Boolean,
    onSwipeToReply: () -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val thresholdPx = with(density) { 48.dp.toPx() }
    val maxDragPx = with(density) { 78.dp.toPx() }
    val offsetX = remember { Animatable(0f) }
    var triggeredHaptic by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        if (offsetX.value > 8f) {
            val progress = (offsetX.value / thresholdPx).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp)
                    .size(34.dp)
                    .scale(0.6f + 0.4f * progress)
                    .alpha(progress)
                    .clip(CircleShape)
                    .background(WhatsAppEmerald.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Reply,
                    contentDescription = "Swipe to Reply",
                    tint = WhatsAppEmerald,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .then(
                    if (enabled) {
                        Modifier.pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (offsetX.value >= thresholdPx) {
                                        onSwipeToReply()
                                    }
                                    triggeredHaptic = false
                                    scope.launch {
                                        offsetX.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 450f))
                                    }
                                },
                                onDragCancel = {
                                    triggeredHaptic = false
                                    scope.launch { offsetX.animateTo(0f, spring()) }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    val next = (offsetX.value + dragAmount).coerceIn(0f, maxDragPx)
                                    if (!triggeredHaptic && next >= thresholdPx) {
                                        triggeredHaptic = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    scope.launch { offsetX.snapTo(next) }
                                }
                            )
                        }
                    } else Modifier
                )
        ) {
            content()
        }
    }
}

@Composable
private fun DateSeparatorPill(ts: Timestamp?) {
    val label = remember(ts?.seconds) {
        if (ts == null) "Today"
        else {
            val time = ts.seconds * 1000L
            val calNow = Calendar.getInstance()
            val calMsg = Calendar.getInstance().apply { timeInMillis = time }
            when {
                calNow.get(Calendar.YEAR) == calMsg.get(Calendar.YEAR) &&
                    calNow.get(Calendar.DAY_OF_YEAR) == calMsg.get(Calendar.DAY_OF_YEAR) -> "Today"
                calNow.get(Calendar.YEAR) == calMsg.get(Calendar.YEAR) &&
                    calNow.get(Calendar.DAY_OF_YEAR) - calMsg.get(Calendar.DAY_OF_YEAR) == 1 -> "Yesterday"
                else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(time))
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 0.5.dp
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

private fun isSameDay(a: Timestamp?, b: Timestamp?): Boolean {
    if (a == null || b == null) return true
    val calA = Calendar.getInstance().apply { timeInMillis = a.seconds * 1000L }
    val calB = Calendar.getInstance().apply { timeInMillis = b.seconds * 1000L }
    return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
        calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    decryptedText: String,
    isMine: Boolean,
    isGroup: Boolean,
    isStarred: Boolean,
    isHighlighted: Boolean,
    isPlayingVoice: Boolean,
    onPlayVoice: (String) -> Unit,
    onReply: () -> Unit,
    onReact: (String) -> Unit,
    onCopy: () -> Unit,
    onToggleStar: () -> Unit,
    onPin: () -> Unit,
    onForward: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDownloadImage: (String) -> Unit,
    onOpenPhotoLightbox: () -> Unit,
    onDownloadFile: (String, String, String?) -> Unit,
    onJumpToReply: (String) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()
    val bubbleColor = when {
        isMine && isDark -> WhatsAppDarkTeal
        isMine -> WhatsAppOutgoingBubbleLight
        isDark -> WhatsAppDarkElevated
        else -> PureWhite
    }
    val highlightBorderColor by animateColorAsState(
        targetValue = if (isHighlighted) WhatsAppEmerald else Color.Transparent,
        animationSpec = tween(300),
        label = "jump_highlight"
    )
    val textColor = if (isDark) WhatsAppDarkText else WhatsAppInkDark
    val metaColor = textColor.copy(alpha = 0.62f)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Box {
            Surface(
                color = bubbleColor,
                shadowElevation = 0.8.dp,
                border = if (isHighlighted) BorderStroke(2.dp, highlightBorderColor) else null,
                shape = RoundedCornerShape(
                    topStart = 15.dp,
                    topEnd = 15.dp,
                    bottomStart = if (isMine) 15.dp else 4.dp,
                    bottomEnd = if (isMine) 4.dp else 15.dp
                ),
                modifier = Modifier
                    .widthIn(max = 315.dp)
                    .clickable { menuOpen = true }
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    if (isGroup && !isMine && !message.deletedForAll) {
                        Text(
                            text = message.senderName,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(2.dp))
                    }

                    if (message.forwarded && !message.deletedForAll) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(bottom = 3.dp)
                        ) {
                            Icon(Icons.Default.Forward, contentDescription = null, tint = metaColor, modifier = Modifier.size(12.dp))
                            Text(
                                text = "Forwarded",
                                style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                                color = metaColor
                            )
                        }
                    }

                    // Rich Quoted Reply Card (Tap to jump to original message)
                    if (!message.replyToPreview.isNullOrBlank() && !message.deletedForAll) {
                        val rawReply = message.replyToPreview
                        val hasStruct = rawReply.contains("::")
                        val replySender = if (hasStruct) rawReply.substringBefore("::") else "Quoted Message"
                        val replySnippet = if (hasStruct) rawReply.substringAfter("::") else rawReply

                        Surface(
                            color = textColor.copy(alpha = 0.07f),
                            shape = RoundedCornerShape(9.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 5.dp)
                                .clickable {
                                    message.replyToMessageId?.let { onJumpToReply(it) }
                                }
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .width(3.5.dp)
                                        .height(30.dp)
                                        .background(if (isMine) WhatsAppTeal else WhatsAppEmerald, RoundedCornerShape(4.dp))
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = replySender,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isMine) WhatsAppTeal else WhatsAppEmerald,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = replySnippet,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = textColor.copy(alpha = 0.8f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Photo Message with Full-Screen Viewer + Save button
                    if (!message.deletedForAll && message.messageType == "IMAGE" && !message.mediaBase64.isNullOrBlank()) {
                        val bmp = remember(message.mediaBase64) { MediaUtils.decodeBase64ToImageBitmap(message.mediaBase64) }
                        if (bmp != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(195.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onOpenPhotoLightbox() }
                            ) {
                                Image(
                                    bitmap = bmp,
                                    contentDescription = "Shared photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Surface(
                                    onClick = { onDownloadImage(message.mediaBase64) },
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Download,
                                            contentDescription = "Download Photo",
                                            tint = PureWhite,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            if (decryptedText.isNotBlank() && decryptedText != "Photo") {
                                Spacer(Modifier.height(6.dp))
                                Text(text = decryptedText, style = MaterialTheme.typography.bodyMedium, color = textColor)
                            }
                        }
                    } else if (!message.deletedForAll && message.messageType == "FILE") {
                        // Document / File Card with Download action
                        val fName = message.fileName ?: "Document"
                        Surface(
                            color = textColor.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!message.mediaBase64.isNullOrBlank()) {
                                        onDownloadFile(message.mediaBase64, fName, message.mediaMimeType)
                                    }
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
                                        .background(WhatsAppTeal.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = WhatsAppTeal, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(fName, style = MaterialTheme.typography.labelLarge, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Tap to save to Downloads", style = MaterialTheme.typography.labelSmall, color = metaColor)
                                }
                                if (!message.mediaBase64.isNullOrBlank()) {
                                    Icon(Icons.Default.Download, contentDescription = "Save File", tint = WhatsAppTeal, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    } else if (!message.deletedForAll && message.messageType == "VOICE" && !message.mediaBase64.isNullOrBlank()) {
                        // Voice Note Player with animated equalizer waveform
                        val infinite = rememberInfiniteTransition(label = "voice_wave")
                        val wavePhase by infinite.animateFloat(
                            initialValue = 0.35f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(animation = tween(450, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
                            label = "wave_phase"
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(textColor.copy(alpha = 0.07f))
                                .clickable { onPlayVoice(message.mediaBase64) }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(WhatsAppEmerald),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlayingVoice) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = "Play voice note",
                                    tint = PureWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.height(22.dp)
                            ) {
                                val bars = listOf(0.45f, 0.85f, 0.6f, 1f, 0.7f, 0.9f, 0.5f, 0.75f, 0.4f)
                                bars.forEachIndexed { idx, baseFrac ->
                                    val factor = if (isPlayingVoice) {
                                        if (idx % 2 == 0) wavePhase else (1.35f - wavePhase).coerceIn(0.3f, 1f)
                                    } else baseFrac
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height((20.dp * factor).coerceAtLeast(5.dp))
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(if (isPlayingVoice) WhatsAppEmerald else metaColor)
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isPlayingVoice) "Playing" else "Voice",
                                style = MaterialTheme.typography.labelMedium,
                                color = textColor
                            )
                        }
                    } else {
                        Text(
                            text = decryptedText,
                            style = if (message.deletedForAll) MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic) else MaterialTheme.typography.bodyMedium,
                            color = if (message.deletedForAll) metaColor else textColor
                        )
                    }

                    Spacer(Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        if (isStarred && !message.deletedForAll) {
                            Icon(Icons.Default.Star, contentDescription = "Starred", tint = Color(0xFFFFB300), modifier = Modifier.size(12.dp))
                        }
                        if (message.encrypted) {
                            Icon(Icons.Default.Lock, contentDescription = "Encrypted", tint = metaColor, modifier = Modifier.size(11.dp))
                        }
                        Text(
                            text = (if (message.edited) "edited · " else "") + formatTimeClock(message.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = metaColor
                        )
                        if (isMine) {
                            val isRead = message.readBy.size > 1
                            val isDelivered = message.deliveredTo.size > 1
                            val tickIcon = if (isRead || isDelivered) Icons.Default.DoneAll else Icons.Default.Done
                            val tickTint = if (isRead) WhatsAppReadBlue else metaColor
                            Icon(
                                imageVector = tickIcon,
                                contentDescription = if (isRead) "Read (blue ticks)" else if (isDelivered) "Delivered (double ticks)" else "Sent (single tick)",
                                tint = tickTint,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            // WhatsApp-style Rich Action & Reaction Menu
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                if (!message.deletedForAll) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        listOf("❤️", "😂", "😮", "👍", "🙏", "💚", "🔥").forEach { emoji ->
                            Text(
                                text = emoji,
                                modifier = Modifier
                                    .clickable { menuOpen = false; onReact(emoji) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null) },
                        text = { Text("Reply") },
                        onClick = { menuOpen = false; onReply() }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                        text = { Text("Copy") },
                        onClick = { menuOpen = false; onCopy() }
                    )
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                if (isStarred) Icons.Default.Star else Icons.Default.StarOutline,
                                contentDescription = null,
                                tint = if (isStarred) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        text = { Text(if (isStarred) "Unstar" else "Star") },
                        onClick = { menuOpen = false; onToggleStar() }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) },
                        text = { Text("Pin") },
                        onClick = { menuOpen = false; onPin() }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.Forward, contentDescription = null) },
                        text = { Text("Forward") },
                        onClick = { menuOpen = false; onForward() }
                    )
                }
                if (isMine && !message.deletedForAll) {
                    if (message.messageType == "TEXT") {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            text = { Text("Edit") },
                            onClick = { menuOpen = false; onEdit() }
                        )
                    }
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = WhatsAppCallRed) },
                        text = { Text("Delete for all", color = WhatsAppCallRed) },
                        onClick = { menuOpen = false; onDelete() }
                    )
                }
            }
        }

        if (message.reactions.isNotEmpty() && !message.deletedForAll) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 0.5.dp,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = message.reactions.values.groupingBy { it }.eachCount()
                        .entries.joinToString(" ") { "${it.key} ${it.value}" },
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun PendingMessageBubble(
    pending: PendingOutgoingMessage,
    onCancel: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bubbleColor = if (isDark) WhatsAppDarkTeal.copy(alpha = 0.72f) else WhatsAppOutgoingBubbleLight.copy(alpha = 0.85f)
    val textColor = if (isDark) WhatsAppDarkText else WhatsAppInkDark

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Surface(
            color = bubbleColor,
            shadowElevation = 0.5.dp,
            border = BorderStroke(1.dp, WhatsAppOfflineGray.copy(alpha = 0.45f)),
            shape = RoundedCornerShape(topStart = 15.dp, topEnd = 15.dp, bottomStart = 15.dp, bottomEnd = 4.dp),
            modifier = Modifier.widthIn(max = 315.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                if (!pending.replyToPreview.isNullOrBlank()) {
                    val hasStruct = pending.replyToPreview.contains("::")
                    val replySender = if (hasStruct) pending.replyToPreview.substringBefore("::") else "Reply"
                    val replySnippet = if (hasStruct) pending.replyToPreview.substringAfter("::") else pending.replyToPreview
                    Surface(
                        color = textColor.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(24.dp)
                                    .background(WhatsAppOfflineGray, RoundedCornerShape(4.dp))
                            )
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text(replySender, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = WhatsAppOfflineGray)
                                Text(replySnippet, style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                Text(
                    text = when (pending.messageType) {
                        "IMAGE" -> "📷 Photo · ${pending.plaintext}"
                        "VOICE" -> "🎤 Voice note"
                        "FILE" -> "📎 ${pending.fileName ?: pending.plaintext}"
                        else -> pending.plaintext
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )

                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "Waiting for network",
                        style = MaterialTheme.typography.labelSmall,
                        color = WhatsAppOfflineGray
                    )
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Not sent yet (waiting for network)",
                        tint = WhatsAppOfflineGray,
                        modifier = Modifier.size(14.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel waiting message",
                        tint = WhatsAppCallRed,
                        modifier = Modifier
                            .size(15.dp)
                            .clickable { onCancel() }
                    )
                }
            }
        }
    }
}
