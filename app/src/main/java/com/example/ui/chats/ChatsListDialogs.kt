package com.example.ui.chats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.MarkChatRead
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Conversation
import com.example.data.model.PublicProfile
import com.example.data.model.isEffectivelyOnline
import com.example.ui.components.WeConnectAvatar
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WhatsAppEmerald
import com.example.ui.theme.WhatsAppTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatQuickActionsSheet(
    conversation: Conversation,
    title: String,
    isPinned: Boolean,
    isMuted: Boolean,
    isArchived: Boolean,
    hasNewMessage: Boolean,
    peerUid: String?,
    onDismiss: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleArchive: () -> Unit,
    onMarkAsRead: () -> Unit,
    onOpenPeerProfile: ((String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(4.dp))

            QuickActionRow(
                icon = Icons.Default.PushPin,
                label = if (isPinned) "Unpin chat" else "Pin chat to top",
                tag = "quick_action_pin",
                onClick = { onTogglePin(); onDismiss() }
            )
            QuickActionRow(
                icon = if (isMuted) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                label = if (isMuted) "Unmute notifications" else "Mute notifications",
                tag = "quick_action_mute",
                onClick = { onToggleMute(); onDismiss() }
            )
            QuickActionRow(
                icon = if (isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                label = if (isArchived) "Unarchive chat" else "Archive chat",
                tag = "quick_action_archive",
                onClick = { onToggleArchive(); onDismiss() }
            )
            if (hasNewMessage) {
                QuickActionRow(
                    icon = Icons.Default.MarkChatRead,
                    label = "Mark as read",
                    tag = "quick_action_mark_read",
                    onClick = { onMarkAsRead(); onDismiss() }
                )
            }
            if (conversation.type == "DIRECT" && peerUid != null && onOpenPeerProfile != null) {
                QuickActionRow(
                    icon = Icons.Default.Person,
                    label = "View contact profile",
                    tag = "quick_action_profile",
                    onClick = { onDismiss(); onOpenPeerProfile(peerUid) }
                )
            }
        }
    }
}

@Composable
private fun QuickActionRow(
    icon: ImageVector,
    label: String,
    tag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = WhatsAppTeal, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun NewChatOrGroupDialog(
    availableUsers: List<PublicProfile>,
    followingIds: Set<String>,
    followerIds: Set<String>,
    nowMs: Long,
    onDismiss: () -> Unit,
    onToggleFollow: (PublicProfile, Boolean) -> Unit,
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
                            val isFollowing = user.uid in followingIds
                            val followsMe = user.uid in followerIds
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
                                    showOnlineDot = user.onlineStatusVisible,
                                    isOnline = user.isEffectivelyOnline(nowMs)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(user.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        text = "@${user.username}" + if (followsMe) " · Follows you" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
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
                                    Surface(
                                        onClick = { onToggleFollow(user, isFollowing) },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isFollowing) MaterialTheme.colorScheme.secondaryContainer else WhatsAppEmerald.copy(alpha = 0.14f)
                                    ) {
                                        Text(
                                            text = if (isFollowing) "Following" else if (followsMe) "Follow Back" else "Follow",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isFollowing) MaterialTheme.colorScheme.onSecondaryContainer else WhatsAppEmerald,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Open chat", tint = WhatsAppTeal, modifier = Modifier.size(18.dp))
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
