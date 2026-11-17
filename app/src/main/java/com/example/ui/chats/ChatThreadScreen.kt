package com.example.ui.chats

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.media.MediaUtils
import com.example.data.media.VoiceNotePlayer
import com.example.data.media.VoiceNoteRecorder
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.PublicProfile
import com.example.data.model.UiState
import com.example.ui.components.WeConnectAvatar
import com.example.ui.components.formatTimeClock
import com.example.ui.main.WeConnectViewModel
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WhatsAppCallRed
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadScreen(
    chatId: String,
    viewModel: WeConnectViewModel,
    myProfile: PublicProfile,
    onBack: () -> Unit,
    onStartCall: (PublicProfile, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val conversation by remember(chatId) {
        viewModel.messagingRepo.observeConversation(chatId)
    }.collectAsStateWithLifecycle(initialValue = null)

    val messages by remember(chatId, myProfile.uid) {
        viewModel.messagingRepo.observeMessages(chatId, myProfile.uid)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val typingList by remember(chatId, myProfile.uid) {
        viewModel.messagingRepo.observeTyping(chatId, myProfile.uid)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val privateUser by viewModel.privateUserState.collectAsStateWithLifecycle()
    val allProfilesState by viewModel.allProfilesState.collectAsStateWithLifecycle()
    val profilesMap = remember(allProfilesState) {
        ((allProfilesState as? UiState.Success)?.data ?: emptyList()).associateBy { it.uid }
    }

    val conv = conversation
    val peerUid = conv?.memberUids?.firstOrNull { it != myProfile.uid }
    val peerProfile = peerUid?.let { profilesMap[it] }

    var inputText by remember { mutableStateOf("") }
    var replyTarget by remember { mutableStateOf<Pair<ChatMessage, String>?>(null) }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var forwardingMessage by remember { mutableStateOf<Pair<ChatMessage, String>?>(null) }
    var searchInChat by remember { mutableStateOf(false) }
    var searchTerm by remember { mutableStateOf("") }
    var showChatInfo by remember { mutableStateOf(false) }
    var playingVoiceId by remember { mutableStateOf<String?>(null) }

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
            scope.launch {
                val b64 = MediaUtils.compressUriToBase64(context, uri, maxDimension = 720, maxBase64Chars = 250_000)
                if (b64 != null) {
                    viewModel.handleResult(
                        viewModel.messagingRepo.sendMessage(
                            conversation = c,
                            senderProfile = myProfile,
                            plaintext = inputText.ifBlank { "Photo" },
                            messageType = "IMAGE",
                            mediaBase64 = b64,
                            mediaMimeType = "image/jpeg"
                        )
                    )
                    inputText = ""
                } else {
                    viewModel.emitNotice("Photo exceeded max size")
                }
            }
        }
    }

    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val c = conv ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            scope.launch {
                val fileName = MediaUtils.getFileDisplayName(context, uri)
                val b64 = MediaUtils.readSmallFileBase64(context, uri)
                viewModel.handleResult(
                    viewModel.messagingRepo.sendMessage(
                        conversation = c,
                        senderProfile = myProfile,
                        plaintext = "Shared file: $fileName",
                        messageType = "FILE",
                        mediaBase64 = b64,
                        fileName = fileName
                    )
                )
            }
        }
    }

    // Mark messages read/delivered
    LaunchedEffect(messages.size, conv?.id) {
        if (conv != null && messages.isNotEmpty()) {
            viewModel.messagingRepo.markMessagesRead(
                chatId = conv.id,
                messages = messages,
                readReceiptsEnabled = privateUser?.readReceiptsEnabled != false
            )
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    // Typing indicator debounced
    LaunchedEffect(inputText, conv?.id) {
        val c = conv ?: return@LaunchedEffect
        if (inputText.isNotBlank()) {
            viewModel.messagingRepo.setTyping(c, myProfile.displayName, true)
            delay(2500)
            viewModel.messagingRepo.setTyping(c, myProfile.displayName, false)
        } else {
            viewModel.messagingRepo.setTyping(c, myProfile.displayName, false)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showChatInfo = true }
                    ) {
                        WeConnectAvatar(
                            displayName = title,
                            avatarBase64 = peerProfile?.avatarBase64,
                            size = 36.dp,
                            showOnlineDot = peerProfile?.onlineStatusVisible == true,
                            isOnline = peerProfile?.online == true
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (conv?.type == "DIRECT") {
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Default.Lock, contentDescription = "Encrypted", tint = WhatsAppGreen, modifier = Modifier.size(14.dp))
                                }
                            }
                            val subtitle = when {
                                typingList.isNotEmpty() -> "${typingList.first().displayName} is typing…"
                                conv?.type == "GROUP" -> "${conv.memberUids.size} members"
                                peerProfile?.online == true && peerProfile.onlineStatusVisible -> "online"
                                else -> "end-to-end encrypted"
                            }
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (typingList.isNotEmpty() || (peerProfile?.online == true && peerProfile.onlineStatusVisible)) WhatsAppEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                    IconButton(onClick = { searchInChat = !searchInChat }) {
                        Icon(Icons.Default.Search, contentDescription = "Search Messages")
                    }
                    IconButton(onClick = { showChatInfo = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Chat Info")
                    }
                }
            )
        }
    ) { innerPadding ->
        val isDark = isSystemInDarkTheme()
        val chatBg = if (isDark) WhatsAppDarkBg else WhatsAppChatWallpaperLight
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(chatBg)
                .padding(innerPadding)
                .imePadding()
                .navigationBarsPadding()
        ) {
            if (searchInChat) {
                OutlinedTextField(
                    value = searchTerm,
                    onValueChange = { searchTerm = it },
                    placeholder = { Text("Search decrypted messages…") },
                    trailingIcon = {
                        IconButton(onClick = { searchInChat = false; searchTerm = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Search")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            if (!conv?.pinnedMessagePreview.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PushPin, contentDescription = null, tint = WhatsAppTeal, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Pinned: ${conv?.pinnedMessagePreview}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Messages Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    var decryptedText by remember(msg.id, msg.ciphertext, msg.edited, msg.deletedForAll) {
                        mutableStateOf(if (msg.encrypted) "Decrypting…" else msg.ciphertext)
                    }
                    LaunchedEffect(msg.id, msg.ciphertext, msg.edited, msg.deletedForAll, conv?.memberKeys) {
                        val c = conv ?: return@LaunchedEffect
                        decryptedText = viewModel.messagingRepo.decryptMessageText(c, msg, myProfile.uid)
                    }

                    if (searchTerm.isNotBlank() && !decryptedText.contains(searchTerm, ignoreCase = true)) {
                        return@items
                    }

                    MessageBubble(
                        message = msg,
                        decryptedText = decryptedText,
                        isMine = msg.senderId == myProfile.uid,
                        isGroup = conv?.type == "GROUP",
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
                        }
                    )
                }
            }

            // Reply / Edit Banner
            if (replyTarget != null || editingMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (editingMessage != null) Icons.Default.Edit else Icons.AutoMirrored.Filled.Reply,
                            contentDescription = null,
                            tint = WhatsAppTeal,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (editingMessage != null) "Editing message" else "Replying to: ${replyTarget?.second?.take(60)}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(
                            onClick = {
                                replyTarget = null
                                editingMessage = null
                                inputText = ""
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Message Composer
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
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
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it.take(1800) },
                        placeholder = { Text(if (isRecording) "Recording voice note…" else "Message") },
                        shape = RoundedCornerShape(24.dp),
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
                                        scope.launch {
                                            viewModel.handleResult(
                                                viewModel.messagingRepo.sendMessage(
                                                    conversation = c,
                                                    senderProfile = myProfile,
                                                    plaintext = "Voice note",
                                                    messageType = "VOICE",
                                                    mediaBase64 = audioB64,
                                                    mediaMimeType = "audio/mp4"
                                                )
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
                                .background(if (isRecording) WhatsAppCallRed else WhatsAppEmerald)
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
                                inputText = ""
                                editingMessage = null
                                replyTarget = null
                                scope.launch {
                                    if (editing != null) {
                                        viewModel.handleResult(viewModel.messagingRepo.editMessage(c, editing, txt))
                                    } else {
                                        viewModel.handleResult(
                                            viewModel.messagingRepo.sendMessage(
                                                conversation = c,
                                                senderProfile = myProfile,
                                                plaintext = txt,
                                                replyToMessageId = reply?.first?.id,
                                                replyToPreview = reply?.second
                                            )
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(WhatsAppEmerald)
                                .testTag("send_message_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = PureWhite)
                        }
                    }
                }
            }
        }
    }

    // Forward Message Dialog
    forwardingMessage?.let { (msg, text) ->
        val convs = (viewModel.conversationsState.collectAsStateWithLifecycle().value as? UiState.Success)?.data ?: emptyList()
        AlertDialog(
            onDismissRequest = { forwardingMessage = null },
            title = { Text("Forward Message") },
            text = {
                LazyColumn(modifier = Modifier.height(220.dp)) {
                    items(convs.filter { it.id != chatId }, key = { it.id }) { targetConv ->
                        val name = conversationDisplayName(targetConv, myProfile.uid, profilesMap)
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        viewModel.handleResult(
                                            viewModel.messagingRepo.sendMessage(
                                                conversation = targetConv,
                                                senderProfile = myProfile,
                                                plaintext = text,
                                                messageType = msg.messageType,
                                                mediaBase64 = msg.mediaBase64,
                                                fileName = msg.fileName,
                                                forwarded = true
                                            ),
                                            "Forwarded to $name"
                                        )
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

    // Chat Info / Group Management Dialog
    if (showChatInfo && conv != null) {
        ChatInfoDialog(
            conversation = conv,
            myProfile = myProfile,
            peerProfile = peerProfile,
            allProfiles = profilesMap.values.filter { it.uid != myProfile.uid },
            viewModel = viewModel,
            onDismiss = { showChatInfo = false },
            onBackAfterLeave = onBack
        )
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    decryptedText: String,
    isMine: Boolean,
    isGroup: Boolean,
    isPlayingVoice: Boolean,
    onPlayVoice: (String) -> Unit,
    onReply: () -> Unit,
    onReact: (String) -> Unit,
    onPin: () -> Unit,
    onForward: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()
    val bubbleColor = when {
        isMine && isDark -> WhatsAppDarkTeal
        isMine -> WhatsAppOutgoingBubbleLight
        isDark -> WhatsAppDarkElevated
        else -> PureWhite
    }
    val textColor = if (isDark) WhatsAppDarkText else WhatsAppInkDark
    val metaColor = textColor.copy(alpha = 0.62f)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Box {
            Surface(
                color = bubbleColor,
                shadowElevation = 0.5.dp,
                shape = RoundedCornerShape(
                    topStart = 14.dp,
                    topEnd = 14.dp,
                    bottomStart = if (isMine) 14.dp else 4.dp,
                    bottomEnd = if (isMine) 4.dp else 14.dp
                ),
                modifier = Modifier
                    .widthIn(max = 310.dp)
                    .clickable { menuOpen = true }
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (isGroup && !isMine) {
                        Text(
                            text = message.senderName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (message.forwarded) {
                        Text(
                            text = "Forwarded",
                            style = MaterialTheme.typography.labelSmall,
                            color = metaColor
                        )
                    }
                    if (!message.replyToPreview.isNullOrBlank()) {
                        Surface(
                            color = textColor.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = message.replyToPreview,
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor.copy(alpha = 0.85f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }

                    if (!message.deletedForAll && message.messageType == "IMAGE" && !message.mediaBase64.isNullOrBlank()) {
                        val bmp = remember(message.mediaBase64) { MediaUtils.decodeBase64ToImageBitmap(message.mediaBase64) }
                        if (bmp != null) {
                            Image(
                                bitmap = bmp,
                                contentDescription = "Shared photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                    }

                    if (!message.deletedForAll && message.messageType == "VOICE" && !message.mediaBase64.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(textColor.copy(alpha = 0.08f))
                                .clickable { onPlayVoice(message.mediaBase64) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlayingVoice) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = "Play voice note",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isPlayingVoice) "Playing Voice Note…" else "Play Voice Note",
                                style = MaterialTheme.typography.labelLarge,
                                color = textColor
                            )
                        }
                    } else {
                        Text(
                            text = decryptedText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                    }

                    Spacer(Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (message.encrypted) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Encrypted",
                                tint = metaColor,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                        Text(
                            text = (if (message.edited) "edited · " else "") + formatTimeClock(message.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = metaColor
                        )
                        if (isMine) {
                            val isRead = message.readBy.size > 1
                            val isDelivered = message.deliveredTo.size > 1
                            Icon(
                                imageVector = if (isRead || isDelivered) Icons.Default.DoneAll else Icons.Default.Done,
                                contentDescription = "Delivery status",
                                tint = if (isRead) WhatsAppReadBlue else metaColor,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                    listOf("💚", "👍", "✨", "👏").forEach { emoji ->
                        Text(
                            text = emoji,
                            modifier = Modifier
                                .clickable { menuOpen = false; onReact(emoji) }
                                .padding(8.dp)
                        )
                    }
                }
                DropdownMenuItem(text = { Text("Reply") }, onClick = { menuOpen = false; onReply() })
                DropdownMenuItem(text = { Text("Pin") }, onClick = { menuOpen = false; onPin() })
                DropdownMenuItem(text = { Text("Forward") }, onClick = { menuOpen = false; onForward() })
                if (isMine && !message.deletedForAll) {
                    DropdownMenuItem(text = { Text("Edit") }, onClick = { menuOpen = false; onEdit() })
                    DropdownMenuItem(text = { Text("Delete for all") }, onClick = { menuOpen = false; onDelete() })
                }
            }
        }

        if (message.reactions.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = message.reactions.values.groupingBy { it }.eachCount()
                        .entries.joinToString(" ") { "${it.key} ${it.value}" },
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatInfoDialog(
    conversation: Conversation,
    myProfile: PublicProfile,
    peerProfile: PublicProfile?,
    allProfiles: List<PublicProfile>,
    viewModel: WeConnectViewModel,
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
        title = { Text(if (conversation.type == "GROUP") "Group Details" else "Conversation Security") },
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
                        }
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
                        }
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
