package com.example.ui.components

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.media.MediaUtils
import com.example.data.repository.AiChatMessage
import com.example.ui.main.AiAssistUiState
import com.example.ui.main.AiStreamUiState
import com.example.ui.main.AiViewModel
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
import com.example.ui.theme.WhatsAppNavIndicatorLight
import com.example.ui.theme.WhatsAppOutgoingBubbleLight
import com.example.ui.theme.WhatsAppTeal
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GeminiAssistantDialog(
    aiViewModel: AiViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val messages by aiViewModel.messages.collectAsStateWithLifecycle()
    val streamState by aiViewModel.streamState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    var promptText by remember { mutableStateOf("") }
    var pendingBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = MediaUtils.decodeUriToScaledBitmap(context, uri, maxDimension = 1024)
                if (bmp != null) pendingBitmap = bmp
            }
        }
    }

    LaunchedEffect(messages.size, streamState) {
        if (messages.isNotEmpty() || streamState is AiStreamUiState.Streaming) {
            val target = (messages.size + if (streamState is AiStreamUiState.Streaming) 1 else 0).coerceAtLeast(1) - 1
            listState.animateScrollToItem(target)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BackHandler { onDismiss() }
        val isDark = isSystemInDarkTheme()
        val chatBg = if (isDark) WhatsAppDarkBg else WhatsAppChatWallpaperLight

        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = WhatsAppTeal,
                        titleContentColor = PureWhite,
                        navigationIconContentColor = PureWhite,
                        actionIconContentColor = PureWhite
                    ),
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close AI Assistant")
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = WhatsAppGreen.copy(alpha = 0.22f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = WhatsAppGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("WeConnect AI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Powered by Gemini",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PureWhite.copy(alpha = 0.85f)
                                )
                            }
                        }
                    },
                    actions = {
                        if (messages.isNotEmpty()) {
                            IconButton(onClick = { aiViewModel.clearConversation() }) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear AI Chat")
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(chatBg)
                    .padding(innerPadding)
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (messages.isEmpty() && streamState is AiStreamUiState.Idle) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = WhatsAppTeal)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "Ask Gemini anything",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "Draft thoughtful messages, translate conversations, summarize ideas, or share a photo for visual Q&A.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(
                                            "Draft a warm welcome message for a community",
                                            "Suggest 3 engaging social post captions",
                                            "Translate 'Let's meet tomorrow at 10' to Spanish & French"
                                        ).forEach { suggestion ->
                                            AssistChip(
                                                onClick = { aiViewModel.sendPrompt(suggestion) },
                                                label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) },
                                                colors = AssistChipDefaults.assistChipColors(
                                                    containerColor = WhatsAppNavIndicatorLight,
                                                    labelColor = WhatsAppInkDark
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    items(messages, key = { it.id }) { msg ->
                        AiMessageBubble(msg = msg, isDark = isDark)
                    }

                    when (val st = streamState) {
                        is AiStreamUiState.Loading -> {
                            item {
                                Surface(
                                    color = if (isDark) WhatsAppDarkElevated else PureWhite,
                                    shape = RoundedCornerShape(16.dp),
                                    shadowElevation = 1.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = WhatsAppTeal)
                                        Spacer(Modifier.width(10.dp))
                                        Text("Gemini is thinking…", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                        is AiStreamUiState.Streaming -> {
                            item {
                                AiMessageBubble(
                                    msg = AiChatMessage(
                                        id = "streaming_preview",
                                        isUser = false,
                                        text = st.partialText
                                    ),
                                    isDark = isDark
                                )
                            }
                        }
                        is AiStreamUiState.Error -> {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                    border = BorderStroke(1.dp, WhatsAppCallRed.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = st.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                        else -> {}
                    }
                }

                pendingBitmap?.let { bmp ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Attached image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Photo attached for Gemini", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            IconButton(onClick = { pendingBitmap = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove photo")
                            }
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val isBusy = streamState is AiStreamUiState.Loading || streamState is AiStreamUiState.Streaming
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            enabled = !isBusy
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Attach photo for Gemini", tint = WhatsAppTeal)
                        }
                        OutlinedTextField(
                            value = promptText,
                            onValueChange = { promptText = it.take(2000) },
                            placeholder = { Text("Ask Gemini…") },
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 4,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("gemini_prompt_input")
                        )
                        Spacer(Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                val text = promptText
                                val bmp = pendingBitmap
                                promptText = ""
                                pendingBitmap = null
                                aiViewModel.sendPrompt(text, bmp)
                            },
                            enabled = !isBusy && (promptText.isNotBlank() || pendingBitmap != null),
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (!isBusy && (promptText.isNotBlank() || pendingBitmap != null)) WhatsAppEmerald else MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("gemini_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send to Gemini",
                                tint = PureWhite
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiMessageBubble(msg: AiChatMessage, isDark: Boolean) {
    val bubbleColor = when {
        msg.isUser && isDark -> WhatsAppDarkTeal
        msg.isUser && !isDark -> WhatsAppOutgoingBubbleLight
        !msg.isUser && isDark -> WhatsAppDarkElevated
        else -> PureWhite
    }
    val textColor = if (isDark) WhatsAppDarkText else WhatsAppInkDark

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (msg.isUser) 16.dp else 4.dp,
                bottomEnd = if (msg.isUser) 4.dp else 16.dp
            ),
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!msg.isUser) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = WhatsAppTeal,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Gemini",
                            style = MaterialTheme.typography.labelSmall,
                            color = WhatsAppTeal,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
                msg.attachedImage?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Prompt image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(Modifier.height(6.dp))
                }
                Text(
                    text = msg.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GeminiChatAssistSheet(
    aiViewModel: AiViewModel,
    currentDraft: String,
    recentConversationLines: List<String>,
    myDisplayName: String,
    onApplyText: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val assistState by aiViewModel.assistState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        aiViewModel.resetAssistState()
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = WhatsAppTeal)
                Spacer(Modifier.width(8.dp))
                Text("Gemini Writing & Reply Assist", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { aiViewModel.suggestReplies(recentConversationLines, myDisplayName) },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppTeal, contentColor = PureWhite)
                ) {
                    Text("Suggest Replies")
                }
                if (currentDraft.isNotBlank()) {
                    listOf(
                        "Polish" to "Fix grammar and make this concise and clear",
                        "Friendly" to "Rewrite in a warm, friendly tone",
                        "Formal" to "Rewrite in a polite, professional tone",
                        "Spanish" to "Translate accurately into Spanish",
                        "French" to "Translate accurately into French"
                    ).forEach { (label, instruction) ->
                        AssistChip(
                            onClick = { aiViewModel.rewriteDraft(currentDraft, instruction) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            when (val st = assistState) {
                is AiAssistUiState.Loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = WhatsAppTeal)
                        Spacer(Modifier.width(10.dp))
                        Text("Generating with Gemini…", style = MaterialTheme.typography.bodySmall)
                    }
                }
                is AiAssistUiState.SuggestionsReady -> {
                    Text("Tap a reply to use:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    st.suggestions.forEach { reply ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onApplyText(reply)
                                    onDismiss()
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(reply, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
                is AiAssistUiState.DraftReady -> {
                    Text("Suggested rewrite:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onApplyText(st.rewrittenText)
                                onDismiss()
                            },
                        colors = CardDefaults.cardColors(containerColor = WhatsAppNavIndicatorLight)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(st.rewrittenText, style = MaterialTheme.typography.bodyMedium, color = WhatsAppInkDark)
                            Spacer(Modifier.height(4.dp))
                            Text("Tap to insert into message box", style = MaterialTheme.typography.labelSmall, color = WhatsAppTeal)
                        }
                    }
                }
                is AiAssistUiState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(st.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp))
                    }
                }
                is AiAssistUiState.Idle -> {
                    Text(
                        text = if (currentDraft.isBlank()) "Tap 'Suggest Replies' or type a message draft first to polish or translate it." else "Choose an option above to polish, rewrite, or translate your draft.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
