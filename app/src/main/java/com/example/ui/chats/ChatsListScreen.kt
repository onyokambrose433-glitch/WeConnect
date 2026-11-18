package com.example.ui.chats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Conversation
import com.example.data.model.PublicProfile
import com.example.data.model.UiState
import com.example.data.model.isEffectivelyOnline
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.WeConnectAvatar
import com.example.ui.components.formatInboxTimestamp
import com.example.ui.main.SearchHistoryKind
import com.example.ui.main.WeConnectViewModel
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WhatsAppEmerald
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppOfflineGray
import com.example.ui.theme.WhatsAppReadBlue
import com.example.ui.theme.WhatsAppTeal
import kotlinx.coroutines.launch

private enum class InboxFilter(val label: String) {
    ALL("All"),
    UNREAD("Unread"),
    GROUPS("Groups"),
    FOLLOWING("Following"),
    ARCHIVED("Archived")
}

@Composable
fun ChatsListScreen(
    viewModel: WeConnectViewModel,
    myProfile: PublicProfile,
    onOpenChat: (String) -> Unit,
    onOpenGemini: () -> Unit = {},
    onOpenUserProfile: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()

    val conversationsState by viewModel.conversationsState.collectAsStateWithLifecycle()
    val allProfilesState by viewModel.allProfilesState.collectAsStateWithLifecycle()
    val blockedUsers by viewModel.blockedUsers.collectAsStateWithLifecycle()
    val readTimestamps by viewModel.readChatTimestamps.collectAsStateWithLifecycle()
    val isOnlineNetwork by viewModel.isOnlineNetwork.collectAsStateWithLifecycle()
    val nowMs by viewModel.nowMsTicker.collectAsStateWithLifecycle()
    val pendingMessages by viewModel.pendingMessages.collectAsStateWithLifecycle()
    val followingIds by viewModel.followingIds.collectAsStateWithLifecycle()
    val followerIds by viewModel.followerIds.collectAsStateWithLifecycle()
    val pinnedChatIds by viewModel.pinnedChatIds.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val recentInteractedUsers by viewModel.recentInteractedUsers.collectAsStateWithLifecycle()

    val blockedSet = remember(blockedUsers) { blockedUsers.map { it.blockedUid }.toSet() }
    val allDiscoverableUsers = remember(allProfilesState, blockedSet, myProfile.uid) {
        ((allProfilesState as? UiState.Success)?.data ?: emptyList())
            .filter { it.uid != myProfile.uid && it.uid !in blockedSet }
    }
    val profilesMap = remember(allProfilesState) {
        ((allProfilesState as? UiState.Success)?.data ?: emptyList()).associateBy { it.uid }
    }
    val pendingByChatId = remember(pendingMessages) {
        pendingMessages.groupBy { it.chatId }
    }

    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(InboxFilter.ALL) }
    var showNewChatDialog by remember { mutableStateOf(false) }
    var quickActionConv by remember { mutableStateOf<Conversation?>(null) }

    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 3 }
    }

    val allConversations = (conversationsState as? UiState.Success)?.data ?: emptyList()

    val archivedCount = remember(allConversations, myProfile.uid) {
        allConversations.count { myProfile.uid in it.archivedBy }
    }

    val unreadInboxCount = remember(allConversations, readTimestamps, myProfile.uid) {
        allConversations.count { conv ->
            val archived = myProfile.uid in conv.archivedBy
            val lastMsgTs = (conv.lastMessageAt?.seconds ?: 0L) * 1000L
            val lastRead = readTimestamps[conv.id] ?: 0L
            !archived && conv.lastMessageSenderId != null &&
                conv.lastMessageSenderId != myProfile.uid &&
                lastMsgTs > lastRead
        }
    }

    val filteredConversations = remember(
        allConversations,
        myProfile.uid,
        activeFilter,
        blockedSet,
        searchQuery,
        profilesMap,
        readTimestamps,
        followingIds,
        pinnedChatIds
    ) {
        allConversations
            .filter { conv ->
                val isArchived = myProfile.uid in conv.archivedBy
                when (activeFilter) {
                    InboxFilter.ARCHIVED -> isArchived
                    InboxFilter.ALL -> !isArchived
                    InboxFilter.UNREAD -> {
                        val lastMsgTs = (conv.lastMessageAt?.seconds ?: 0L) * 1000L
                        val lastRead = readTimestamps[conv.id] ?: 0L
                        !isArchived && conv.lastMessageSenderId != null &&
                            conv.lastMessageSenderId != myProfile.uid &&
                            lastMsgTs > lastRead
                    }
                    InboxFilter.GROUPS -> !isArchived && conv.type == "GROUP"
                    InboxFilter.FOLLOWING -> {
                        val peerUid = conv.memberUids.firstOrNull { it != myProfile.uid }
                        !isArchived && conv.type == "DIRECT" && peerUid != null && peerUid in followingIds
                    }
                }
            }
            .filter { conv ->
                if (conv.type == "DIRECT") {
                    val peer = conv.memberUids.firstOrNull { it != myProfile.uid }
                    peer !in blockedSet
                } else true
            }
            .filter { conv ->
                if (searchQuery.isBlank()) true
                else {
                    val label = conversationDisplayName(conv, myProfile.uid, profilesMap)
                    label.contains(searchQuery, ignoreCase = true) ||
                        (conv.lastMessageText ?: "").contains(searchQuery, ignoreCase = true)
                }
            }
            .sortedWith(
                compareByDescending<Conversation> { it.id in pinnedChatIds }
                    .thenByDescending { (it.lastMessageAt ?: it.updatedAt ?: it.createdAt)?.seconds ?: 0L }
            )
    }

    val matchingPeople = remember(allDiscoverableUsers, searchQuery) {
        if (searchQuery.isBlank()) {
            allDiscoverableUsers.take(10)
        } else {
            allDiscoverableUsers.filter { user ->
                user.displayName.contains(searchQuery, ignoreCase = true) ||
                    user.username.contains(searchQuery, ignoreCase = true)
            }.take(15)
        }
    }

    val recentQueryChips = remember(searchHistory) {
        searchHistory.filter { it.kind == SearchHistoryKind.QUERY }.take(5)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnimatedVisibility(
                    visible = showScrollToTop,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    SmallFloatingActionButton(
                        onClick = { scope.launch { listState.animateScrollToItem(0) } },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = WhatsAppTeal,
                        modifier = Modifier.testTag("chats_scroll_to_top")
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Scroll to top")
                    }
                }
                FloatingActionButton(
                    onClick = { showNewChatDialog = true },
                    containerColor = WhatsAppEmerald,
                    contentColor = PureWhite,
                    modifier = Modifier.testTag("new_chat_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Chat or Group")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                if (!isOnlineNetwork) {
                    item(key = "offline_banner", contentType = "offline_banner") {
                        Surface(
                            color = WhatsAppOfflineGray.copy(alpha = 0.16f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                                    text = "Waiting for network · Messages won't send until you're online",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // WhatsApp-style Search Pill inside smooth scrolling list
                item(key = "inbox_search_bar", contentType = "search_bar") {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it.take(80) },
                        placeholder = { Text("Ask WeConnect AI or search chats…") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.testTag("chats_search_clear")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            focusedBorderColor = WhatsAppEmerald.copy(alpha = 0.7f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (searchQuery.isNotBlank()) viewModel.recordQuerySearch(searchQuery)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("chats_search_input")
                    )
                }

                // Search History + Recent Interacted People Quick Strip
                if (searchQuery.isBlank() && (recentQueryChips.isNotEmpty() || recentInteractedUsers.isNotEmpty())) {
                    item(key = "inbox_recent_strip", contentType = "recent_strip") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(recentQueryChips, key = { "rq_${it.id}" }, contentType = { "query_chip" }) { entry ->
                                Surface(
                                    onClick = { searchQuery = entry.targetId },
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.History,
                                            contentDescription = null,
                                            tint = WhatsAppTeal,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = entry.title,
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove recent search",
                                            tint = WhatsAppOfflineGray,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { viewModel.removeSearchHistoryItem(entry.id) }
                                        )
                                    }
                                }
                            }
                            items(recentInteractedUsers.take(6), key = { "rec_peer_${it.uid}" }, contentType = { "recent_user_chip" }) { peer ->
                                Surface(
                                    onClick = {
                                        viewModel.recordUserInteractionSearch(peer)
                                        scope.launch {
                                            viewModel.handleResult(
                                                viewModel.messagingRepo.createOrOpenDirectChat(myProfile, peer)
                                            ) { chatId -> onOpenChat(chatId) }
                                        }
                                    },
                                    shape = RoundedCornerShape(18.dp),
                                    color = WhatsAppEmerald.copy(alpha = 0.10f),
                                    border = BorderStroke(0.5.dp, WhatsAppEmerald.copy(alpha = 0.35f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        WeConnectAvatar(
                                            displayName = peer.displayName,
                                            avatarBase64 = peer.avatarBase64,
                                            size = 20.dp,
                                            showOnlineDot = peer.onlineStatusVisible,
                                            isOnline = peer.isEffectivelyOnline(nowMs)
                                        )
                                        Text(
                                            text = peer.displayName.substringBefore(" "),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = WhatsAppTeal,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // WhatsApp-style Horizontal Filter Pills
                item(key = "inbox_filter_pills", contentType = "filter_pills") {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(InboxFilter.entries, key = { it.name }) { filter ->
                            val selected = activeFilter == filter
                            val label = when (filter) {
                                InboxFilter.UNREAD -> if (unreadInboxCount > 0) "Unread ($unreadInboxCount)" else "Unread"
                                InboxFilter.ARCHIVED -> if (archivedCount > 0) "Archived ($archivedCount)" else "Archived"
                                else -> filter.label
                            }
                            FilterChip(
                                selected = selected,
                                onClick = { activeFilter = filter },
                                leadingIcon = if (filter == InboxFilter.ARCHIVED) {
                                    { Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(15.dp)) }
                                } else null,
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WhatsAppGreen.copy(alpha = 0.20f),
                                    selectedLabelColor = WhatsAppTeal,
                                    selectedLeadingIconColor = WhatsAppTeal,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = null,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.testTag("inbox_filter_${filter.name.lowercase()}")
                            )
                        }
                    }
                }

                // Compact WeConnect AI Row (WhatsApp-style streamlined assistant row)
                item(key = "ask_gemini_row", contentType = "ai_row") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenGemini() }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag("chats_ask_gemini_card"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = WhatsAppTeal,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PureWhite, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ask WeConnect AI",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Chat with Gemini, analyze photos, or draft replies",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 78.dp, end = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                    )
                }

                // Pinned Archived Row (WhatsApp style when viewing All and archived chats exist)
                if (activeFilter == InboxFilter.ALL && archivedCount > 0 && searchQuery.isBlank()) {
                    item(key = "archived_banner_row", contentType = "archived_row") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { activeFilter = InboxFilter.ARCHIVED }
                                .padding(horizontal = 22.dp, vertical = 12.dp)
                                .testTag("archived_chats_row"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Archive,
                                contentDescription = "Archived chats",
                                tint = WhatsAppTeal,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(24.dp))
                            Text(
                                text = "Archived",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "$archivedCount",
                                style = MaterialTheme.typography.labelMedium,
                                color = WhatsAppEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 78.dp, end = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                        )
                    }
                }

                when (val state = conversationsState) {
                    is UiState.Loading -> {
                        item(key = "chats_loading", contentType = "loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = WhatsAppEmerald)
                            }
                        }
                    }
                    is UiState.Error -> {
                        item(key = "chats_error", contentType = "error") {
                            Box(Modifier.padding(16.dp)) {
                                EmptyStateCard(
                                    icon = Icons.AutoMirrored.Filled.Chat,
                                    title = "Unable to load chats",
                                    subtitle = state.message
                                )
                            }
                        }
                    }
                    is UiState.Success -> {
                        if (filteredConversations.isEmpty() && (searchQuery.isBlank() || matchingPeople.isEmpty())) {
                            item(key = "chats_empty", contentType = "empty") {
                                Box(Modifier.padding(16.dp)) {
                                    EmptyStateCard(
                                        icon = Icons.AutoMirrored.Filled.Chat,
                                        title = when (activeFilter) {
                                            InboxFilter.ARCHIVED -> "No archived chats"
                                            InboxFilter.UNREAD -> "All caught up!"
                                            InboxFilter.GROUPS -> "No group chats yet"
                                            InboxFilter.FOLLOWING -> "No chats with people you follow yet"
                                            InboxFilter.ALL -> "Start a real conversation"
                                        },
                                        subtitle = "Search any registered WeConnect user to follow them and start an end-to-end encrypted 1-to-1 chat or group.",
                                        actionLabel = if (activeFilter == InboxFilter.ALL) "Find People & Chat" else "Show All Chats",
                                        onAction = {
                                            if (activeFilter == InboxFilter.ALL) showNewChatDialog = true
                                            else activeFilter = InboxFilter.ALL
                                        }
                                    )
                                }
                            }
                        } else {
                            items(
                                items = filteredConversations,
                                key = { it.id },
                                contentType = { "chat_row" }
                            ) { conv ->
                                val peerUid = conv.memberUids.firstOrNull { it != myProfile.uid }
                                val peerProfile = peerUid?.let { profilesMap[it] }
                                val peerIsOnline = peerProfile?.isEffectivelyOnline(nowMs) == true
                                val title = conversationDisplayName(conv, myProfile.uid, profilesMap)
                                val isMuted = myProfile.uid in conv.mutedBy
                                val isPinned = conv.id in pinnedChatIds
                                val pendingForChat = pendingByChatId[conv.id].orEmpty()

                                val lastMsgTs = (conv.lastMessageAt?.seconds ?: 0L) * 1000L
                                val lastRead = readTimestamps[conv.id] ?: 0L
                                val hasNewMessage = conv.lastMessageSenderId != null &&
                                    conv.lastMessageSenderId != myProfile.uid &&
                                    lastMsgTs > lastRead

                                WhatsAppChatInboxRow(
                                    modifier = Modifier.animateItem(),
                                    conversation = conv,
                                    title = title,
                                    myUid = myProfile.uid,
                                    peerProfile = peerProfile,
                                    peerIsOnline = peerIsOnline,
                                    isMuted = isMuted,
                                    isPinned = isPinned,
                                    hasNewMessage = hasNewMessage,
                                    pendingPreview = pendingForChat.lastOrNull()?.plaintext,
                                    nowMs = nowMs,
                                    onClick = {
                                        if (searchQuery.isNotBlank()) viewModel.recordQuerySearch(searchQuery)
                                        peerProfile?.let { viewModel.recordUserInteractionSearch(it) }
                                        viewModel.markChatAsRead(conv.id)
                                        onOpenChat(conv.id)
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        quickActionConv = conv
                                    }
                                )
                            }
                        }

                        // People Directory / Search Discovery below chats
                        if (matchingPeople.isNotEmpty() && activeFilter != InboxFilter.ARCHIVED) {
                            item(key = "people_section_header", contentType = "section_header") {
                                Text(
                                    text = if (searchQuery.isBlank()) "People on WeConnect · Follow & Chat"
                                    else "People matching \"$searchQuery\"",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = WhatsAppTeal,
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 6.dp)
                                )
                            }
                            items(
                                items = matchingPeople,
                                key = { "people_chat_${it.uid}" },
                                contentType = { "person_row" }
                            ) { person ->
                                val isFollowing = person.uid in followingIds
                                val followsMe = person.uid in followerIds
                                val isOnline = person.isEffectivelyOnline(nowMs)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem()
                                        .clickable {
                                            if (searchQuery.isNotBlank()) viewModel.recordQuerySearch(searchQuery)
                                            viewModel.recordUserInteractionSearch(person)
                                            scope.launch {
                                                viewModel.handleResult(
                                                    viewModel.messagingRepo.createOrOpenDirectChat(myProfile, person)
                                                ) { chatId -> onOpenChat(chatId) }
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    WeConnectAvatar(
                                        displayName = person.displayName,
                                        avatarBase64 = person.avatarBase64,
                                        size = 46.dp,
                                        showOnlineDot = person.onlineStatusVisible,
                                        isOnline = isOnline
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = person.displayName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (followsMe) {
                                                Surface(
                                                    color = if (isFollowing) WhatsAppGreen.copy(alpha = 0.14f) else WhatsAppTeal.copy(alpha = 0.14f),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        text = if (isFollowing) "Mutual" else "Follows you",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (isFollowing) WhatsAppEmerald else WhatsAppTeal,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "@${person.username} · " + if (isOnline) "online" else "offline",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isOnline) WhatsAppEmerald else WhatsAppOfflineGray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                viewModel.handleResult(
                                                    viewModel.authRepo.toggleFollow(myProfile, person, isFollowing),
                                                    if (isFollowing) "Unfollowed @${person.username}" else "Following @${person.username}"
                                                )
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isFollowing) MaterialTheme.colorScheme.secondaryContainer else WhatsAppEmerald,
                                            contentColor = if (isFollowing) MaterialTheme.colorScheme.onSecondaryContainer else PureWhite
                                        ),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                isFollowing -> "Following"
                                                followsMe -> "Follow Back"
                                                else -> "Follow"
                                            },
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 74.dp, end = 16.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                )
                            }
                        }

                        // WhatsApp Signature End-to-End Encrypted Footer
                        item(key = "e2ee_footer", contentType = "footer") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 22.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Your personal messages are end-to-end encrypted",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    quickActionConv?.let { conv ->
        val peerUid = conv.memberUids.firstOrNull { it != myProfile.uid }
        val title = conversationDisplayName(conv, myProfile.uid, profilesMap)
        val lastMsgTs = (conv.lastMessageAt?.seconds ?: 0L) * 1000L
        val lastRead = readTimestamps[conv.id] ?: 0L
        val hasNewMessage = conv.lastMessageSenderId != null &&
            conv.lastMessageSenderId != myProfile.uid &&
            lastMsgTs > lastRead

        ChatQuickActionsSheet(
            conversation = conv,
            title = title,
            isPinned = conv.id in pinnedChatIds,
            isMuted = myProfile.uid in conv.mutedBy,
            isArchived = myProfile.uid in conv.archivedBy,
            hasNewMessage = hasNewMessage,
            peerUid = peerUid,
            onDismiss = { quickActionConv = null },
            onTogglePin = {
                val nowPinned = viewModel.togglePinChat(conv.id)
                viewModel.emitNotice(if (nowPinned) "Pinned \"$title\"" else "Unpinned \"$title\"")
            },
            onToggleMute = {
                scope.launch {
                    viewModel.handleResult(
                        viewModel.messagingRepo.toggleMute(conv),
                        if (myProfile.uid in conv.mutedBy) "Unmuted \"$title\"" else "Muted \"$title\""
                    )
                }
            },
            onToggleArchive = {
                scope.launch {
                    viewModel.handleResult(
                        viewModel.messagingRepo.toggleArchive(conv),
                        if (myProfile.uid in conv.archivedBy) "Unarchived \"$title\"" else "Archived \"$title\""
                    )
                }
            },
            onMarkAsRead = { viewModel.markChatAsRead(conv.id) },
            onOpenPeerProfile = onOpenUserProfile
        )
    }

    if (showNewChatDialog) {
        NewChatOrGroupDialog(
            availableUsers = allDiscoverableUsers,
            followingIds = followingIds,
            followerIds = followerIds,
            nowMs = nowMs,
            onDismiss = { showNewChatDialog = false },
            onToggleFollow = { user, isFollowing ->
                scope.launch {
                    viewModel.handleResult(
                        viewModel.authRepo.toggleFollow(myProfile, user, isFollowing),
                        if (isFollowing) "Unfollowed @${user.username}" else "Following @${user.username}"
                    )
                }
            },
            onStartDirectChat = { peer ->
                viewModel.recordUserInteractionSearch(peer)
                scope.launch {
                    viewModel.handleResult(
                        viewModel.messagingRepo.createOrOpenDirectChat(myProfile, peer)
                    ) { chatId ->
                        showNewChatDialog = false
                        onOpenChat(chatId)
                    }
                }
            },
            onCreateGroup = { title, desc, selected ->
                scope.launch {
                    viewModel.handleResult(
                        viewModel.messagingRepo.createGroupChat(myProfile, title, desc, selected)
                    ) { chatId ->
                        showNewChatDialog = false
                        onOpenChat(chatId)
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WhatsAppChatInboxRow(
    modifier: Modifier = Modifier,
    conversation: Conversation,
    title: String,
    myUid: String,
    peerProfile: PublicProfile?,
    peerIsOnline: Boolean,
    isMuted: Boolean,
    isPinned: Boolean,
    hasNewMessage: Boolean,
    pendingPreview: String?,
    nowMs: Long,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val snippetText = pendingPreview ?: (conversation.lastMessageText ?: "Tap to start messaging")
    val sentByMe = conversation.lastMessageSenderId == myUid
    val mediaIcon = when {
        pendingPreview != null -> Icons.Default.Schedule
        snippetText.startsWith("Photo attachment", ignoreCase = true) -> Icons.Default.PhotoCamera
        snippetText.startsWith("Voice note", ignoreCase = true) -> Icons.Default.Mic
        snippetText.startsWith("File:", ignoreCase = true) -> Icons.Default.InsertDriveFile
        else -> null
    }
    val timeLabel = formatInboxTimestamp(conversation.lastMessageAt ?: conversation.updatedAt, nowMs)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (conversation.type == "GROUP") {
                Surface(
                    color = WhatsAppTeal,
                    shape = CircleShape,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Group, contentDescription = "Group", tint = PureWhite, modifier = Modifier.size(26.dp))
                    }
                }
            } else {
                WeConnectAvatar(
                    displayName = title,
                    avatarBase64 = peerProfile?.avatarBase64,
                    size = 52.dp,
                    showOnlineDot = peerProfile == null || peerProfile.onlineStatusVisible,
                    isOnline = peerIsOnline
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (hasNewMessage) FontWeight.Bold else FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (conversation.type == "DIRECT") {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "End-to-end encrypted",
                                tint = WhatsAppGreen.copy(alpha = 0.85f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasNewMessage) WhatsAppGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (hasNewMessage) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Spacer(Modifier.height(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (pendingPreview != null) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = "Waiting for network",
                                tint = WhatsAppOfflineGray,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                        } else if (sentByMe && conversation.lastMessageText != null) {
                            Icon(
                                Icons.Default.DoneAll,
                                contentDescription = "Delivered",
                                tint = WhatsAppReadBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                        } else if (mediaIcon != null) {
                            Icon(
                                mediaIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                        }

                        Text(
                            text = if (pendingPreview != null) "Waiting for network: $pendingPreview" else snippetText,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = if (hasNewMessage) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (hasNewMessage) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isMuted) {
                            Icon(
                                Icons.Default.NotificationsOff,
                                contentDescription = "Muted",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (isPinned) {
                            Icon(
                                Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        if (hasNewMessage) {
                            Surface(
                                color = WhatsAppGreen,
                                shape = CircleShape,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "•",
                                        color = PureWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(start = 82.dp, end = 16.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    }
}

fun conversationDisplayName(
    conv: Conversation,
    myUid: String,
    profilesMap: Map<String, PublicProfile>
): String {
    if (conv.type == "GROUP") return conv.title.ifBlank { "Group Chat" }
    val peerUid = conv.memberUids.firstOrNull { it != myUid }
    return peerUid?.let { profilesMap[it]?.displayName ?: conv.memberNames[it] } ?: "Direct Chat"
}
