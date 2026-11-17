package com.example.ui.chats

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Conversation
import com.example.data.model.PublicProfile
import com.example.data.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.WeConnectAvatar
import com.example.ui.components.formatRelativeTime
import com.example.ui.main.WeConnectViewModel
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WhatsAppEmerald
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppTeal
import kotlinx.coroutines.launch

@Composable
fun ChatsListScreen(
    viewModel: WeConnectViewModel,
    myProfile: PublicProfile,
    onOpenChat: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val conversationsState by viewModel.conversationsState.collectAsStateWithLifecycle()
    val allProfilesState by viewModel.allProfilesState.collectAsStateWithLifecycle()
    val blockedUsers by viewModel.blockedUsers.collectAsStateWithLifecycle()

    val blockedSet = remember(blockedUsers) { blockedUsers.map { it.blockedUid }.toSet() }
    val profilesMap = remember(allProfilesState) {
        ((allProfilesState as? UiState.Success)?.data ?: emptyList()).associateBy { it.uid }
    }

    var searchQuery by remember { mutableStateOf("") }
    var showArchived by remember { mutableStateOf(false) }
    var showNewChatDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewChatDialog = true },
                containerColor = WhatsAppEmerald,
                contentColor = PureWhite,
                modifier = Modifier.testTag("new_chat_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Chat or Group")
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
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search chats or people…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("chats_search_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !showArchived,
                        onClick = { showArchived = false },
                        label = { Text("All") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    FilterChip(
                        selected = showArchived,
                        onClick = { showArchived = true },
                        leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Archived") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                Spacer(Modifier.height(6.dp))

                when (val state = conversationsState) {
                    is UiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = WhatsAppEmerald)
                        }
                    }
                    is UiState.Error -> {
                        Box(Modifier.padding(16.dp)) {
                            EmptyStateCard(
                                icon = Icons.AutoMirrored.Filled.Chat,
                                title = "Unable to load chats",
                                subtitle = state.message
                            )
                        }
                    }
                    is UiState.Success -> {
                        val list = state.data
                            .filter { conv ->
                                val isArchived = myProfile.uid in conv.archivedBy
                                isArchived == showArchived
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

                        if (list.isEmpty()) {
                            Box(Modifier.padding(16.dp)) {
                                EmptyStateCard(
                                    icon = Icons.AutoMirrored.Filled.Chat,
                                    title = if (showArchived) "No archived chats" else "Start a real conversation",
                                    subtitle = "Search any registered WeConnect user to start an end-to-end encrypted 1-to-1 chat or create a group.",
                                    actionLabel = "New Chat",
                                    onAction = { showNewChatDialog = true }
                                )
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(list, key = { it.id }) { conv ->
                                    val peerUid = conv.memberUids.firstOrNull { it != myProfile.uid }
                                    val peerProfile = peerUid?.let { profilesMap[it] }
                                    val title = conversationDisplayName(conv, myProfile.uid, profilesMap)
                                    val isMuted = myProfile.uid in conv.mutedBy

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenChat(conv.id) },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (conv.type == "GROUP") {
                                                Surface(
                                                    color = WhatsAppTeal,
                                                    shape = RoundedCornerShape(24.dp),
                                                    modifier = Modifier.size(48.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(Icons.Default.Group, contentDescription = "Group", tint = PureWhite)
                                                    }
                                                }
                                            } else {
                                                WeConnectAvatar(
                                                    displayName = title,
                                                    avatarBase64 = peerProfile?.avatarBase64,
                                                    size = 48.dp,
                                                    showOnlineDot = peerProfile?.onlineStatusVisible == true,
                                                    isOnline = peerProfile?.online == true
                                                )
                                            }

                                            Spacer(Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = title,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    if (conv.type == "DIRECT") {
                                                        Icon(
                                                            Icons.Default.Lock,
                                                            contentDescription = "End-to-end encrypted",
                                                            tint = WhatsAppGreen,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                    if (isMuted) {
                                                        Icon(
                                                            Icons.Default.NotificationsOff,
                                                            contentDescription = "Muted",
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = conv.lastMessageText ?: "Tap to start messaging",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = formatRelativeTime(conv.lastMessageAt ?: conv.updatedAt),
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
            }
        }
    }

    if (showNewChatDialog) {
        val users = ((allProfilesState as? UiState.Success)?.data ?: emptyList())
            .filter { it.uid != myProfile.uid && it.uid !in blockedSet }
        NewChatOrGroupDialog(
            availableUsers = users,
            onDismiss = { showNewChatDialog = false },
            onStartDirectChat = { peer ->
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

fun conversationDisplayName(
    conv: Conversation,
    myUid: String,
    profilesMap: Map<String, PublicProfile>
): String {
    if (conv.type == "GROUP") return conv.title.ifBlank { "Group Chat" }
    val peerUid = conv.memberUids.firstOrNull { it != myUid }
    return peerUid?.let { profilesMap[it]?.displayName ?: conv.memberNames[it] } ?: "Direct Chat"
}

@Composable
private fun NewChatOrGroupDialog(
    availableUsers: List<PublicProfile>,
    onDismiss: () -> Unit,
    onStartDirectChat: (PublicProfile) -> Unit,
    onCreateGroup: (String, String, List<PublicProfile>) -> Unit
) {
    var modeTab by remember { mutableStateOf(0) } // 0 = Direct, 1 = Group
    var search by remember { mutableStateOf("") }
    var groupTitle by remember { mutableStateOf("") }
    var groupDesc by remember { mutableStateOf("") }
    val selectedMembers = remember { mutableStateListOf<PublicProfile>() }

    val filtered = remember(availableUsers, search) {
        availableUsers.filter {
            search.isBlank() ||
                it.displayName.contains(search, ignoreCase = true) ||
                it.username.contains(search, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Conversation") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(selectedTabIndex = modeTab) {
                    Tab(selected = modeTab == 0, onClick = { modeTab = 0 }, text = { Text("Direct Chat") })
                    Tab(selected = modeTab == 1, onClick = { modeTab = 1 }, text = { Text("New Group") })
                }
                Spacer(Modifier.height(10.dp))

                if (modeTab == 1) {
                    OutlinedTextField(
                        value = groupTitle,
                        onValueChange = { groupTitle = it.take(80) },
                        label = { Text("Group Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = groupDesc,
                        onValueChange = { groupDesc = it.take(250) },
                        label = { Text("Description (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search by @username or name…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (filtered.isEmpty()) {
                        item {
                            Text(
                                text = "No other registered users found yet. Invite another person to sign in with their account!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    } else {
                        items(filtered, key = { it.uid }) { user ->
                            val isSelected = selectedMembers.any { it.uid == user.uid }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (modeTab == 0) {
                                            onStartDirectChat(user)
                                        } else {
                                            if (isSelected) selectedMembers.removeAll { it.uid == user.uid }
                                            else selectedMembers.add(user)
                                        }
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                WeConnectAvatar(
                                    displayName = user.displayName,
                                    avatarBase64 = user.avatarBase64,
                                    size = 38.dp,
                                    showOnlineDot = true,
                                    isOnline = user.online
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(user.displayName, style = MaterialTheme.typography.titleSmall)
                                    Text("@${user.username}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (modeTab == 1) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = {
                                            if (it) selectedMembers.add(user)
                                            else selectedMembers.removeAll { m -> m.uid == user.uid }
                                        }
                                    )
                                } else {
                                    Icon(Icons.Default.Check, contentDescription = "Open chat", tint = WhatsAppTeal)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (modeTab == 1) {
                Button(
                    onClick = { onCreateGroup(groupTitle, groupDesc, selectedMembers.toList()) },
                    enabled = groupTitle.isNotBlank() && selectedMembers.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppEmerald, contentColor = PureWhite)
                ) {
                    Text("Create Group (${selectedMembers.size})")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
