package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppNotification
import com.example.data.model.PublicProfile
import com.example.data.model.UiState
import com.example.data.model.isEffectivelyOnline
import com.example.ui.main.WeConnectViewModel
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WhatsAppCallRed
import com.example.ui.theme.WhatsAppEmerald
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppOfflineGray
import com.example.ui.theme.WhatsAppTeal
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsCenterSheet(
    viewModel: WeConnectViewModel,
    myProfile: PublicProfile,
    onDismiss: () -> Unit,
    onOpenUserProfile: (String) -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenCommunity: (String) -> Unit,
    onOpenHome: () -> Unit,
    onOpenCalls: () -> Unit,
    onMessageUser: (PublicProfile) -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val allProfilesState by viewModel.allProfilesState.collectAsStateWithLifecycle()
    val followingIds by viewModel.followingIds.collectAsStateWithLifecycle()
    val followerIds by viewModel.followerIds.collectAsStateWithLifecycle()
    val recentNewcomers by viewModel.recentNewcomers.collectAsStateWithLifecycle()
    val welcomedNewcomerIds by viewModel.welcomedNewcomerIds.collectAsStateWithLifecycle()
    val unwelcomedNewcomerIds by viewModel.unwelcomedNewcomerIds.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle()
    val conversationsState by viewModel.conversationsState.collectAsStateWithLifecycle()
    val communitiesState by viewModel.communitiesState.collectAsStateWithLifecycle()
    val nowMs by viewModel.nowMsTicker.collectAsStateWithLifecycle()

    val profilesMap = remember(allProfilesState) {
        ((allProfilesState as? UiState.Success)?.data ?: emptyList()).associateBy { it.uid }
    }
    val conversationIds = remember(conversationsState) {
        ((conversationsState as? UiState.Success)?.data ?: emptyList()).map { it.id }.toSet()
    }
    val communityIds = remember(communitiesState) {
        ((communitiesState as? UiState.Success)?.data ?: emptyList()).map { it.id }.toSet()
    }

    var selectedFilter by remember { mutableStateOf("ALL") }

    val followNotifs = remember(notifications) { notifications.filter { it.type == "FOLLOW" } }
    val commNotifs = remember(notifications) {
        notifications.filter { it.type in listOf("MESSAGE_REQUEST", "CALL_MISSED", "COMMUNITY") }
    }
    val socialNotifs = remember(notifications) {
        notifications.filter { it.type in listOf("COMMENT", "LIKE") }
    }

    val filteredNotifs = remember(selectedFilter, notifications, followNotifs, commNotifs, socialNotifs) {
        when (selectedFilter) {
            "FOLLOW" -> followNotifs
            "COMM" -> commNotifs
            "SOCIAL" -> socialNotifs
            "NEWCOMERS" -> emptyList()
            else -> notifications
        }
    }

    val newcomersToShow = remember(selectedFilter, recentNewcomers, unwelcomedNewcomerIds) {
        if (selectedFilter == "NEWCOMERS") recentNewcomers
        else if (selectedFilter == "ALL") recentNewcomers.filter { it.uid in unwelcomedNewcomerIds }.take(3)
        else emptyList()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = WhatsAppEmerald.copy(alpha = 0.14f),
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = WhatsAppEmerald,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Notifications",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (unreadCount > 0) {
                                Surface(
                                    color = WhatsAppCallRed,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "$unreadCount new",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PureWhite,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Followers, chats, calls, activity & newcomers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (unreadCount > 0) {
                        IconButton(
                            onClick = {
                                val unreadIds = notifications.filter { !it.read }.map { it.id }
                                viewModel.markAllNewcomersWelcomed()
                                scope.launch {
                                    viewModel.authRepo.markAllNotificationsRead(unreadIds)
                                }
                            },
                            modifier = Modifier.testTag("mark_all_notifications_read")
                        ) {
                            Icon(
                                Icons.Default.DoneAll,
                                contentDescription = "Mark all as read",
                                tint = WhatsAppEmerald
                            )
                        }
                    }
                    if (notifications.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                val ids = notifications.map { it.id }
                                scope.launch {
                                    viewModel.handleResult(
                                        viewModel.authRepo.clearAllNotifications(ids),
                                        "Notifications cleared"
                                    )
                                }
                            },
                            modifier = Modifier.testTag("clear_all_notifications")
                        ) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Clear all notifications",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close Notifications")
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) {
                items(
                    listOf(
                        "ALL" to "All (${notifications.size + recentNewcomers.count { it.uid in unwelcomedNewcomerIds }})",
                        "FOLLOW" to "Follows (${followNotifs.size})",
                        "COMM" to "Chats & Calls (${commNotifs.size})",
                        "SOCIAL" to "Feed (${socialNotifs.size})",
                        "NEWCOMERS" to "Newcomers (${recentNewcomers.size})"
                    )
                ) { (key, label) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { selectedFilter = key },
                        label = { Text(label, maxLines = 1, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WhatsAppEmerald,
                            selectedLabelColor = PureWhite
                        ),
                        shape = RoundedCornerShape(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(10.dp))

            // Notifications + Newcomers list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp, max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                if (newcomersToShow.isNotEmpty()) {
                    item(key = "newcomers_header") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Celebration,
                                    contentDescription = null,
                                    tint = WhatsAppEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Welcome Newcomers to WeConnect",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = WhatsAppEmerald
                                )
                            }
                            if (selectedFilter == "ALL" && recentNewcomers.size > newcomersToShow.size) {
                                TextButton(
                                    onClick = { selectedFilter = "NEWCOMERS" },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                ) {
                                    Text("See all", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }

                    items(newcomersToShow, key = { "newcomer_${it.uid}" }) { newcomer ->
                        val isWelcomed = newcomer.uid in welcomedNewcomerIds
                        val isFollowing = newcomer.uid in followingIds
                        val followsMe = newcomer.uid in followerIds
                        NewcomerWelcomeCard(
                            newcomer = newcomer,
                            nowMs = nowMs,
                            isWelcomed = isWelcomed,
                            isFollowing = isFollowing,
                            followsMe = followsMe,
                            onOpenProfile = {
                                onDismiss()
                                onOpenUserProfile(newcomer.uid)
                            },
                            onToggleFollow = {
                                viewModel.markNewcomerWelcomed(newcomer.uid)
                                scope.launch {
                                    viewModel.handleResult(
                                        viewModel.authRepo.toggleFollow(myProfile, newcomer, isFollowing),
                                        if (isFollowing) "Unfollowed @${newcomer.username}" else "Following @${newcomer.username}"
                                    )
                                }
                            },
                            onSendWelcome = {
                                viewModel.markNewcomerWelcomed(newcomer.uid)
                                scope.launch {
                                    val chatRes = viewModel.messagingRepo.createOrOpenDirectChat(myProfile, newcomer)
                                    chatRes.onSuccess { chatId ->
                                        val convList = (conversationsState as? UiState.Success)?.data ?: emptyList()
                                        val conv = convList.firstOrNull { it.id == chatId }
                                        if (!isWelcomed && conv != null) {
                                            viewModel.sendMessageOrQueueOffline(
                                                conversation = conv,
                                                senderProfile = myProfile,
                                                plaintext = "👋 Welcome to WeConnect, ${newcomer.displayName}! Glad to connect with you."
                                            )
                                            viewModel.emitNotice("Sent welcome to @${newcomer.username}!")
                                        } else {
                                            onDismiss()
                                            onOpenChat(chatId)
                                        }
                                    }.onFailure {
                                        onDismiss()
                                        onMessageUser(newcomer)
                                    }
                                }
                            }
                        )
                    }
                }

                if (filteredNotifs.isEmpty() && newcomersToShow.isEmpty()) {
                    item(key = "empty_notifications") {
                        EmptyStateCard(
                            icon = Icons.Default.NotificationsNone,
                            title = "You're all caught up",
                            subtitle = when (selectedFilter) {
                                "FOLLOW" -> "New follower alerts will appear here when people follow you."
                                "COMM" -> "Incoming messages, missed calls, and group updates show up here."
                                "SOCIAL" -> "Post likes, comments, and replies will appear here."
                                "NEWCOMERS" -> "When new members join WeConnect, you can welcome them here."
                                else -> "Follows, messages, calls, comments, and newcomer welcomes appear here."
                            }
                        )
                    }
                } else if (filteredNotifs.isNotEmpty()) {
                    if (newcomersToShow.isNotEmpty()) {
                        item(key = "activity_header") {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Recent Activity",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    items(filteredNotifs, key = { it.id }) { notif ->
                        val actorProfile = profilesMap[notif.actorUid]
                        val isFollowingActor = notif.actorUid in followingIds
                        val actorFollowsMe = notif.actorUid in followerIds
                        NotificationActivityCard(
                            notification = notif,
                            actorProfile = actorProfile,
                            isFollowingActor = isFollowingActor,
                            actorFollowsMe = actorFollowsMe,
                            nowMs = nowMs,
                            onCardClick = {
                                scope.launch { viewModel.authRepo.markNotificationRead(notif.id) }
                                when {
                                    notif.id.startsWith("welcome_") -> {
                                        onDismiss()
                                        onOpenHome()
                                    }
                                    notif.type == "FOLLOW" -> {
                                        onDismiss()
                                        onOpenUserProfile(notif.actorUid)
                                    }
                                    notif.type == "MESSAGE_REQUEST" -> {
                                        if (notif.targetId in conversationIds) {
                                            onDismiss()
                                            onOpenChat(notif.targetId)
                                        } else if (actorProfile != null) {
                                            onDismiss()
                                            onMessageUser(actorProfile)
                                        }
                                    }
                                    notif.type == "CALL_MISSED" -> {
                                        onDismiss()
                                        onOpenCalls()
                                    }
                                    notif.type == "COMMUNITY" -> {
                                        when {
                                            notif.targetId in conversationIds -> {
                                                onDismiss()
                                                onOpenChat(notif.targetId)
                                            }
                                            notif.targetId in communityIds -> {
                                                onDismiss()
                                                onOpenCommunity(notif.targetId)
                                            }
                                            else -> {
                                                onDismiss()
                                                onOpenHome()
                                            }
                                        }
                                    }
                                    notif.type == "COMMENT" || notif.type == "LIKE" -> {
                                        onDismiss()
                                        onOpenHome()
                                    }
                                }
                            },
                            onFollowBackClick = if (notif.type == "FOLLOW" && actorProfile != null) {
                                {
                                    scope.launch {
                                        viewModel.authRepo.markNotificationRead(notif.id)
                                        viewModel.handleResult(
                                            viewModel.authRepo.toggleFollow(myProfile, actorProfile, isFollowingActor),
                                            if (isFollowingActor) "Unfollowed @${actorProfile.username}" else "Following @${actorProfile.username}"
                                        )
                                    }
                                }
                            } else null,
                            onChatActorClick = if (notif.actorUid != myProfile.uid && actorProfile != null &&
                                (notif.type == "FOLLOW" || notif.type == "MESSAGE_REQUEST")
                            ) {
                                {
                                    scope.launch { viewModel.authRepo.markNotificationRead(notif.id) }
                                    if (notif.type == "MESSAGE_REQUEST" && notif.targetId in conversationIds) {
                                        onDismiss()
                                        onOpenChat(notif.targetId)
                                    } else {
                                        onDismiss()
                                        onMessageUser(actorProfile)
                                    }
                                }
                            } else null,
                            onDeleteClick = {
                                scope.launch { viewModel.authRepo.deleteNotification(notif.id) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NewcomerWelcomeCard(
    newcomer: PublicProfile,
    nowMs: Long,
    isWelcomed: Boolean,
    isFollowing: Boolean,
    followsMe: Boolean,
    onOpenProfile: () -> Unit,
    onToggleFollow: () -> Unit,
    onSendWelcome: () -> Unit
) {
    val isOnline = newcomer.isEffectivelyOnline(nowMs)
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isWelcomed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            else WhatsAppEmerald.copy(alpha = 0.08f)
        ),
        border = BorderStroke(
            1.dp,
            if (isWelcomed) MaterialTheme.colorScheme.outline else WhatsAppEmerald.copy(alpha = 0.45f)
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenProfile)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WeConnectAvatar(
                    displayName = newcomer.displayName,
                    avatarBase64 = newcomer.avatarBase64,
                    size = 42.dp,
                    showOnlineDot = newcomer.onlineStatusVisible,
                    isOnline = isOnline
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = newcomer.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Surface(
                            color = WhatsAppGreen.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "New Member",
                                style = MaterialTheme.typography.labelSmall,
                                color = WhatsAppEmerald,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "@${newcomer.username} · Joined ${formatRelativeTime(newcomer.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSendWelcome,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isWelcomed) MaterialTheme.colorScheme.secondaryContainer else WhatsAppEmerald,
                        contentColor = if (isWelcomed) MaterialTheme.colorScheme.onSecondaryContainer else PureWhite
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("welcome_newcomer_${newcomer.uid}")
                ) {
                    Icon(
                        if (isWelcomed) Icons.AutoMirrored.Filled.Chat else Icons.Default.WavingHand,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = if (isWelcomed) "Welcomed · Chat" else "Say Welcome",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = onToggleFollow,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("follow_newcomer_${newcomer.uid}")
                ) {
                    Text(
                        text = when {
                            isFollowing -> "Following"
                            followsMe -> "Follow Back"
                            else -> "Follow"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isFollowing) MaterialTheme.colorScheme.onSurface else WhatsAppEmerald
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationActivityCard(
    notification: AppNotification,
    actorProfile: PublicProfile?,
    isFollowingActor: Boolean,
    actorFollowsMe: Boolean,
    nowMs: Long,
    onCardClick: () -> Unit,
    onFollowBackClick: (() -> Unit)?,
    onChatActorClick: (() -> Unit)?,
    onDeleteClick: () -> Unit
) {
    val isWelcome = notification.id.startsWith("welcome_")
    val (icon, accentColor, categoryLabel) = when {
        isWelcome -> Triple(Icons.Default.Celebration, WhatsAppEmerald, "Welcome")
        notification.type == "FOLLOW" -> Triple(Icons.Default.PersonAdd, WhatsAppEmerald, "New Follower")
        notification.type == "MESSAGE_REQUEST" -> Triple(Icons.AutoMirrored.Filled.Chat, WhatsAppTeal, "Message")
        notification.type == "CALL_MISSED" -> Triple(Icons.Default.Call, WhatsAppCallRed, "Call")
        notification.type == "COMMENT" -> Triple(Icons.Default.ChatBubbleOutline, WhatsAppTeal, "Comment")
        notification.type == "LIKE" -> Triple(Icons.Default.Favorite, WhatsAppCallRed, "Reaction")
        else -> Triple(Icons.Default.Campaign, WhatsAppEmerald, "Community")
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (notification.read) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.36f)
        ),
        border = BorderStroke(
            1.dp,
            if (notification.read) MaterialTheme.colorScheme.outlineVariant else accentColor.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                if (actorProfile != null && !isWelcome) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        WeConnectAvatar(
                            displayName = actorProfile.displayName,
                            avatarBase64 = actorProfile.avatarBase64,
                            size = 40.dp,
                            showOnlineDot = actorProfile.onlineStatusVisible,
                            isOnline = actorProfile.isEffectivelyOnline(nowMs)
                        )
                        Surface(
                            color = accentColor,
                            shape = CircleShape,
                            modifier = Modifier.size(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(icon, contentDescription = null, tint = PureWhite, modifier = Modifier.size(11.dp))
                            }
                        }
                    }
                } else {
                    Surface(
                        color = accentColor.copy(alpha = 0.14f),
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = accentColor.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = categoryLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                        Text(
                            text = formatRelativeTime(notification.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!notification.read) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(WhatsAppGreen)
                            )
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = notification.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss notification",
                        tint = WhatsAppOfflineGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (onFollowBackClick != null || onChatActorClick != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 50.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onFollowBackClick != null) {
                        Button(
                            onClick = onFollowBackClick,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFollowingActor) MaterialTheme.colorScheme.secondaryContainer else WhatsAppEmerald,
                                contentColor = if (isFollowingActor) MaterialTheme.colorScheme.onSecondaryContainer else PureWhite
                            ),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = when {
                                    isFollowingActor -> "Following"
                                    actorFollowsMe -> "Follow Back"
                                    else -> "Follow"
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    if (onChatActorClick != null) {
                        OutlinedButton(
                            onClick = onChatActorClick,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (notification.type == "MESSAGE_REQUEST") "Reply" else "Message",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}
