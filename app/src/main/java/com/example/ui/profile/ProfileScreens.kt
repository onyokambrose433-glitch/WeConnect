package com.example.ui.profile

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.media.MediaUtils
import com.example.data.model.PublicProfile
import com.example.data.model.UiState
import com.example.data.model.isEffectivelyOnline
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.example.ui.components.ReportDialog
import com.example.ui.components.WeConnectAvatar
import com.example.ui.components.formatLastSeenTime
import com.example.ui.components.formatRelativeTime
import com.example.ui.main.SearchHistoryKind
import com.example.ui.main.WeConnectViewModel
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WhatsAppCallRed
import com.example.ui.theme.WhatsAppEmerald
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppOfflineGray
import com.example.ui.theme.WhatsAppTeal
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    viewModel: WeConnectViewModel,
    myProfile: PublicProfile,
    onSignOut: () -> Unit,
    onMessageUser: (PublicProfile) -> Unit = {},
    onOpenNotifications: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val privateUser by viewModel.privateUserState.collectAsStateWithLifecycle()
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val blockedUsers by viewModel.blockedUsers.collectAsStateWithLifecycle()
    val unreadNotifCount by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle()
    val deviceIdentity by viewModel.deviceIdentity.collectAsStateWithLifecycle()
    val isOnlineNetwork by viewModel.isOnlineNetwork.collectAsStateWithLifecycle()
    val allProfilesState by viewModel.allProfilesState.collectAsStateWithLifecycle()
    val followingIds by viewModel.followingIds.collectAsStateWithLifecycle()
    val followerIds by viewModel.followerIds.collectAsStateWithLifecycle()
    val nowMs by viewModel.nowMsTicker.collectAsStateWithLifecycle()
    val myPosts by remember(myProfile.uid) {
        viewModel.socialRepo.observeUserPosts(myProfile.uid)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    var showEditProfile by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showDevicesDialog by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var connectionsInitialTab by remember { mutableStateOf<Int?>(null) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Profile Header Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WeConnectAvatar(
                            displayName = myProfile.displayName,
                            avatarBase64 = myProfile.avatarBase64,
                            size = 82.dp,
                            showOnlineDot = true,
                            isOnline = isOnlineNetwork && myProfile.online
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(myProfile.displayName, style = MaterialTheme.typography.headlineSmall)
                        Text("@${myProfile.username}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        if (myProfile.bio.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(myProfile.bio, style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatBox("Posts", myProfile.postsCount)
                            StatBox(
                                label = "Followers",
                                count = followerIds.size.toLong().coerceAtLeast(myProfile.followersCount),
                                onClick = { connectionsInitialTab = 0 }
                            )
                            StatBox(
                                label = "Following",
                                count = followingIds.size.toLong().coerceAtLeast(myProfile.followingCount),
                                onClick = { connectionsInitialTab = 1 }
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = { showEditProfile = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_profile_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Edit Profile")
                        }
                    }
                }
            }

            // Account, Security & Devices Menu
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        ProfileActionRow(
                            icon = Icons.Default.Notifications,
                            title = "Notifications ($unreadNotifCount unread)",
                            subtitle = "Follows, messages, reactions, calls & newcomers",
                            onClick = onOpenNotifications
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ProfileActionRow(
                            icon = Icons.Default.Devices,
                            title = "Linked Devices & Encryption Keys (${devices.size})",
                            subtitle = "Fingerprint: ${deviceIdentity?.fingerprint?.take(23) ?: "Ready"}…",
                            onClick = { showDevicesDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ProfileActionRow(
                            icon = Icons.Default.Security,
                            title = "Privacy, Blocked Accounts & Security",
                            subtitle = "Profile: ${myProfile.accountVisibility} · Messages: ${privateUser?.messagePermission ?: "EVERYONE"}",
                            onClick = { showPrivacyDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ProfileActionRow(
                            icon = Icons.Default.Download,
                            title = "Export Account Data",
                            subtitle = "Share your profile & activity summary",
                            onClick = {
                                val summary = """
                                    WeConnect Account Export
                                    Display Name: ${myProfile.displayName}
                                    Username: @${myProfile.username}
                                    Email: ${privateUser?.email ?: "Private"}
                                    Bio: ${myProfile.bio}
                                    Posts: ${myPosts.size}, Followers: ${myProfile.followersCount}, Following: ${myProfile.followingCount}
                                    Devices: ${devices.size}
                                """.trimIndent()
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, summary)
                                }
                                context.startActivity(Intent.createChooser(intent, "Export WeConnect Data"))
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ProfileActionRow(
                            icon = Icons.AutoMirrored.Filled.Logout,
                            title = "Sign Out",
                            subtitle = "Sign out safely and clear session credentials",
                            onClick = { showSignOutConfirm = true }
                        )
                    }
                }
            }

            // My Posts Section
            item {
                Text("My Posts (${myPosts.size})", style = MaterialTheme.typography.titleMedium)
            }

            if (myPosts.isEmpty()) {
                item {
                    Text(
                        "You haven't published any posts yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(myPosts, key = { it.id }) { post ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(post.caption.ifBlank { "(Photo post)" }, style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${post.likesCount} likes · ${post.commentsCount} comments · ${formatRelativeTime(post.createdAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditProfile) {
        EditProfileDialog(
            profile = myProfile,
            onDismiss = { showEditProfile = false },
            onSave = { name, bio, avatar, vis, showOnline ->
                scope.launch {
                    viewModel.handleResult(
                        viewModel.authRepo.updateProfile(name, bio, avatar, vis, showOnline),
                        "Profile updated"
                    ) { showEditProfile = false }
                }
            }
        )
    }

    if (showPrivacyDialog) {
        PrivacySettingsDialog(
            privateUser = privateUser,
            blockedUsers = blockedUsers,
            onDismiss = { showPrivacyDialog = false },
            onSave = { phone, msgPerm, lastSeen, receipts, storyVis ->
                scope.launch {
                    viewModel.handleResult(
                        viewModel.authRepo.updatePrivacySettings(phone, msgPerm, lastSeen, receipts, storyVis),
                        "Privacy settings saved"
                    ) { showPrivacyDialog = false }
                }
            },
            onUnblock = { uid ->
                scope.launch { viewModel.handleResult(viewModel.authRepo.unblockUser(uid), "User unblocked") }
            }
        )
    }

    if (showDevicesDialog) {
        AlertDialog(
            onDismissRequest = { showDevicesDialog = false },
            title = { Text("Linked Devices & E2EE Keys") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = WhatsAppTeal)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "This device fingerprint:\n${deviceIdentity?.fingerprint ?: "Loading…"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    HorizontalDivider()
                    LazyColumn(modifier = Modifier.height(200.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(devices, key = { it.deviceId }) { dev ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dev.deviceName + if (dev.deviceId == deviceIdentity?.deviceId) " (This device)" else "",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text("Active ${formatRelativeTime(dev.lastActiveAt)}", style = MaterialTheme.typography.bodySmall)
                                }
                                if (dev.deviceId != deviceIdentity?.deviceId) {
                                    IconButton(onClick = {
                                        scope.launch { viewModel.handleResult(viewModel.authRepo.revokeDevice(dev.deviceId), "Device revoked") }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Revoke device")
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { OutlinedButton(onClick = { showDevicesDialog = false }) { Text("Close") } }
        )
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("Sign out of WeConnect?") },
            text = { Text("Your encrypted device keys remain protected locally. You can sign back in with your Google account anytime.") },
            confirmButton = {
                Button(
                    onClick = { showSignOutConfirm = false; onSignOut() },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppCallRed, contentColor = PureWhite)
                ) { Text("Sign Out") }
            },
            dismissButton = { OutlinedButton(onClick = { showSignOutConfirm = false }) { Text("Cancel") } }
        )
    }

    connectionsInitialTab?.let { startTab ->
        val profiles = (allProfilesState as? UiState.Success)?.data ?: emptyList()
        ConnectionsDialog(
            initialTab = startTab,
            allProfiles = profiles.filter { it.uid != myProfile.uid },
            followingIds = followingIds,
            followerIds = followerIds,
            nowMs = nowMs,
            onDismiss = { connectionsInitialTab = null },
            onToggleFollow = { user, isFollowing ->
                scope.launch {
                    viewModel.handleResult(
                        viewModel.authRepo.toggleFollow(myProfile, user, isFollowing),
                        if (isFollowing) "Unfollowed @${user.username}" else "Following @${user.username}"
                    )
                }
            },
            onChatWithUser = { user ->
                connectionsInitialTab = null
                onMessageUser(user)
            }
        )
    }
}

@Composable
private fun ConnectionsDialog(
    initialTab: Int,
    allProfiles: List<PublicProfile>,
    followingIds: Set<String>,
    followerIds: Set<String>,
    nowMs: Long,
    onDismiss: () -> Unit,
    onToggleFollow: (PublicProfile, Boolean) -> Unit,
    onChatWithUser: (PublicProfile) -> Unit
) {
    var tab by remember { mutableIntStateOf(initialTab) }
    val followersList = remember(allProfiles, followerIds) { allProfiles.filter { it.uid in followerIds } }
    val followingList = remember(allProfiles, followingIds) { allProfiles.filter { it.uid in followingIds } }
    val displayList = if (tab == 0) followersList else followingList

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connections") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Followers (${followersList.size})") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Following (${followingList.size})") })
                }
                Spacer(Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (displayList.isEmpty()) {
                        item {
                            Text(
                                text = if (tab == 0) "No followers yet." else "You aren't following anyone yet. Browse Find People on Home to connect!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    } else {
                        items(displayList, key = { "conn_${tab}_${it.uid}" }) { user ->
                            val isFollowing = user.uid in followingIds
                            val followsMe = user.uid in followerIds
                            Row(
                                modifier = Modifier.fillMaxWidth(),
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
                                    Text(user.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                    Text(
                                        text = "@${user.username}" + if (isFollowing && followsMe) " · Mutual" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isFollowing && followsMe) WhatsAppEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    onClick = { onToggleFollow(user, isFollowing) },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isFollowing) MaterialTheme.colorScheme.secondaryContainer else WhatsAppEmerald
                                ) {
                                    Text(
                                        text = if (isFollowing) "Following" else if (followsMe) "Follow Back" else "Follow",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isFollowing) MaterialTheme.colorScheme.onSecondaryContainer else PureWhite,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                                IconButton(onClick = { onChatWithUser(user) }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat", tint = WhatsAppTeal, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun StatBox(label: String, count: Long, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onClick != null) {
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { onClick() }
                .padding(horizontal = 10.dp, vertical = 4.dp)
        } else Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text("$count", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProfileActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EditProfileDialog(
    profile: PublicProfile,
    onDismiss: () -> Unit,
    onSave: (String, String, String?, String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(profile.displayName) }
    var bio by remember { mutableStateOf(profile.bio) }
    var avatar by remember { mutableStateOf<String?>(null) }
    var visibility by remember { mutableStateOf(profile.accountVisibility) }
    var showOnline by remember { mutableStateOf(profile.onlineStatusVisible) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                avatar = MediaUtils.compressUriToBase64(context, uri, maxDimension = 480, maxBase64Chars = 220_000)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (avatar == null) "Update Photo" else "New Photo Selected")
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(50) },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it.take(200) },
                    label = { Text("Bio") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = visibility == "PUBLIC", onClick = { visibility = "PUBLIC" }, label = { Text("Public") })
                    FilterChip(selected = visibility == "PRIVATE", onClick = { visibility = "PRIVATE" }, label = { Text("Private") })
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Show Online Status", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = showOnline, onCheckedChange = { showOnline = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, bio, avatar, visibility, showOnline) },
                enabled = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PrivacySettingsDialog(
    privateUser: com.example.data.model.PrivateUser?,
    blockedUsers: List<com.example.data.model.BlockedUser>,
    onDismiss: () -> Unit,
    onSave: (String, String, Boolean, Boolean, String) -> Unit,
    onUnblock: (String) -> Unit
) {
    var phone by remember { mutableStateOf(privateUser?.phone ?: "") }
    var msgPerm by remember { mutableStateOf(privateUser?.messagePermission ?: "EVERYONE") }
    var lastSeen by remember { mutableStateOf(privateUser?.lastSeenVisible != false) }
    var readReceipts by remember { mutableStateOf(privateUser?.readReceiptsEnabled != false) }
    var storyVis by remember { mutableStateOf(privateUser?.storyVisibility ?: "PUBLIC") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Privacy & Security") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.take(30) },
                    label = { Text("Private Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Who can message you", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = msgPerm == "EVERYONE", onClick = { msgPerm = "EVERYONE" }, label = { Text("Everyone") })
                    FilterChip(selected = msgPerm == "FOLLOWERS", onClick = { msgPerm = "FOLLOWERS" }, label = { Text("Followers") })
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Share Last Seen", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = lastSeen, onCheckedChange = { lastSeen = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Read Receipts", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = readReceipts, onCheckedChange = { readReceipts = it })
                }
                if (blockedUsers.isNotEmpty()) {
                    Text("Blocked Users (${blockedUsers.size})", style = MaterialTheme.typography.labelMedium)
                    blockedUsers.take(4).forEach { b ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("@${b.blockedUsername}", style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(onClick = { onUnblock(b.blockedUid) }) { Text("Unblock") }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(phone, msgPerm, lastSeen, readReceipts, storyVis) }) { Text("Save") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun UserPublicProfileDialog(
    targetUid: String,
    myProfile: PublicProfile,
    viewModel: WeConnectViewModel,
    onDismiss: () -> Unit,
    onMessageUser: (PublicProfile) -> Unit,
    onCallUser: (PublicProfile, String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val allProfilesState by viewModel.allProfilesState.collectAsStateWithLifecycle()
    val followingIds by viewModel.followingIds.collectAsStateWithLifecycle()
    val followerIds by viewModel.followerIds.collectAsStateWithLifecycle()
    val nowMs by viewModel.nowMsTicker.collectAsStateWithLifecycle()
    val target = remember(allProfilesState, targetUid) {
        ((allProfilesState as? UiState.Success)?.data ?: emptyList()).firstOrNull { it.uid == targetUid }
    }
    var showReport by remember { mutableStateOf(false) }

    if (target == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("User Profile") },
            text = { Text("Profile unavailable.") },
            confirmButton = { OutlinedButton(onClick = onDismiss) { Text("Close") } }
        )
        return
    }

    val isFollowing = target.uid in followingIds
    val followsMe = target.uid in followerIds
    val targetOnline = target.isEffectivelyOnline(nowMs)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WeConnectAvatar(
                    displayName = target.displayName,
                    avatarBase64 = target.avatarBase64,
                    size = 52.dp,
                    showOnlineDot = target.onlineStatusVisible,
                    isOnline = targetOnline
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(target.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("@${target.username}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
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
                    if (target.onlineStatusVisible) {
                        Spacer(Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (targetOnline) WhatsAppGreen else WhatsAppOfflineGray)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = if (targetOnline) "Online" else "Last seen ${formatLastSeenTime(target.lastSeenAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (targetOnline) WhatsAppEmerald else WhatsAppOfflineGray
                            )
                        }
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (target.bio.isNotBlank()) Text(target.bio, style = MaterialTheme.typography.bodyMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatBox("Posts", target.postsCount)
                    StatBox("Followers", target.followersCount)
                    StatBox("Following", target.followingCount)
                }
                if (target.uid != myProfile.uid) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.handleResult(
                                        viewModel.authRepo.toggleFollow(myProfile, target, isFollowing),
                                        if (isFollowing) "Unfollowed @${target.username}" else "Following @${target.username}"
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFollowing) MaterialTheme.colorScheme.secondaryContainer else WhatsAppEmerald,
                                contentColor = if (isFollowing) MaterialTheme.colorScheme.onSecondaryContainer else PureWhite
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isFollowing) "Following" else if (followsMe) "Follow Back" else "Follow")
                        }
                        OutlinedButton(
                            onClick = { onDismiss(); onMessageUser(target) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Message")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { onDismiss(); onCallUser(target, "VOICE") }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Voice")
                        }
                        OutlinedButton(onClick = { onDismiss(); onCallUser(target, "VIDEO") }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Video")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    viewModel.handleResult(viewModel.authRepo.blockUser(target.uid, target.username), "Blocked @${target.username}") { onDismiss() }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Block")
                        }
                        OutlinedButton(onClick = { showReport = true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Report")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Close") } }
    )

    if (showReport) {
        ReportDialog(
            targetLabel = "@${target.username}",
            onDismiss = { showReport = false },
            onSubmit = { reason ->
                scope.launch {
                    viewModel.handleResult(viewModel.socialRepo.submitReport("USER", target.uid, reason), "Report submitted") {
                        showReport = false
                    }
                }
            }
        )
    }
}

@Composable
fun GlobalSearchDialog(
    viewModel: WeConnectViewModel,
    myUid: String,
    onDismiss: () -> Unit,
    onSelectUser: (String) -> Unit,
    onSelectCommunity: (com.example.data.model.Community) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val allProfilesState by viewModel.allProfilesState.collectAsStateWithLifecycle()
    val communitiesState by viewModel.communitiesState.collectAsStateWithLifecycle()
    val postsState by viewModel.publicPostsState.collectAsStateWithLifecycle()
    val myProfileState by viewModel.myProfileState.collectAsStateWithLifecycle()
    val blockedUsers by viewModel.blockedUsers.collectAsStateWithLifecycle()
    val followingIds by viewModel.followingIds.collectAsStateWithLifecycle()
    val followerIds by viewModel.followerIds.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val recentInteractedUsers by viewModel.recentInteractedUsers.collectAsStateWithLifecycle()
    val recentInteractedCommunities by viewModel.recentInteractedCommunities.collectAsStateWithLifecycle()
    val nowMs by viewModel.nowMsTicker.collectAsStateWithLifecycle()

    val blockedSet = remember(blockedUsers) { blockedUsers.map { it.blockedUid }.toSet() }
    val profiles = remember(allProfilesState, blockedSet) {
        ((allProfilesState as? UiState.Success)?.data ?: emptyList())
            .filter { it.uid != myUid && it.uid !in blockedSet }
    }
    val profileMap = remember(profiles) { profiles.associateBy { it.uid } }
    val communities = (communitiesState as? UiState.Success)?.data ?: emptyList()
    val communityMap = remember(communities) { communities.associateBy { it.id } }
    val posts = (postsState as? UiState.Success)?.data ?: emptyList()
    val myProfile = (myProfileState as? UiState.Success)?.data

    val matchingHistory = remember(query, searchHistory) {
        if (query.isBlank()) searchHistory
        else searchHistory.filter {
            it.title.contains(query, true) || it.subtitle.contains(query, true)
        }
    }

    val matchedUsers = remember(query, profiles) {
        if (query.isBlank()) emptyList()
        else profiles.filter {
            it.displayName.contains(query, true) || it.username.contains(query, true)
        }.take(12)
    }
    val matchedCommunities = remember(query, communities) {
        if (query.isBlank()) emptyList()
        else communities.filter {
            it.name.contains(query, true) || it.description.contains(query, true)
        }.take(8)
    }
    val matchedPosts = remember(query, posts) {
        if (query.isBlank()) emptyList()
        else posts.filter {
            it.caption.contains(query, true) ||
                it.authorName.contains(query, true) ||
                it.hashtags.any { h -> h.contains(query.removePrefix("#"), true) }
        }.take(6)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .widthIn(max = 620.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search")
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it.take(80) },
                        placeholder = { Text("Search people, @handles, communities, #tags…") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = WhatsAppTeal) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(
                                    onClick = { query = "" },
                                    modifier = Modifier.testTag("global_search_clear")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (query.isNotBlank()) viewModel.recordQuerySearch(query)
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("global_search_input")
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 260.dp, max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 6.dp)
                ) {
                    if (query.isBlank()) {
                        if (searchHistory.isNotEmpty()) {
                            item(key = "history_header") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Recent searches",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    TextButton(
                                        onClick = { viewModel.clearSearchHistory() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        modifier = Modifier.testTag("clear_search_history_button")
                                    ) {
                                        Text("Clear all", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }

                            items(searchHistory.take(8), key = { "hist_${it.id}" }) { entry ->
                                val linkedProfile = if (entry.kind == SearchHistoryKind.USER) profileMap[entry.targetId] else null
                                val linkedCommunity = if (entry.kind == SearchHistoryKind.COMMUNITY) communityMap[entry.targetId] else null
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            when (entry.kind) {
                                                SearchHistoryKind.QUERY -> {
                                                    query = entry.targetId
                                                    viewModel.recordQuerySearch(entry.targetId)
                                                }
                                                SearchHistoryKind.USER -> {
                                                    linkedProfile?.let { viewModel.recordUserInteractionSearch(it) }
                                                    onDismiss()
                                                    onSelectUser(entry.targetId)
                                                }
                                                SearchHistoryKind.COMMUNITY -> {
                                                    if (linkedCommunity != null) {
                                                        viewModel.recordCommunityInteractionSearch(linkedCommunity)
                                                        onDismiss()
                                                        onSelectCommunity(linkedCommunity)
                                                    } else {
                                                        query = entry.title
                                                    }
                                                }
                                            }
                                        }
                                        .padding(horizontal = 6.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    when (entry.kind) {
                                        SearchHistoryKind.USER -> {
                                            WeConnectAvatar(
                                                displayName = linkedProfile?.displayName ?: entry.title,
                                                avatarBase64 = linkedProfile?.avatarBase64,
                                                size = 34.dp,
                                                showOnlineDot = linkedProfile?.onlineStatusVisible == true,
                                                isOnline = linkedProfile?.isEffectivelyOnline(nowMs) == true
                                            )
                                        }
                                        SearchHistoryKind.COMMUNITY -> {
                                            Surface(
                                                color = WhatsAppTeal.copy(alpha = 0.14f),
                                                shape = CircleShape,
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = if (linkedCommunity?.kind == "CHANNEL") Icons.Default.Campaign else Icons.Default.Groups,
                                                        contentDescription = null,
                                                        tint = WhatsAppTeal,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                        SearchHistoryKind.QUERY -> {
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                shape = CircleShape,
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Default.History,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = entry.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (entry.subtitle.isNotBlank()) {
                                            Text(
                                                text = entry.subtitle,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    if (entry.kind == SearchHistoryKind.QUERY) {
                                        Icon(
                                            Icons.Default.NorthWest,
                                            contentDescription = "Use search query",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.removeSearchHistoryItem(entry.id) },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("remove_search_history_${entry.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove from history",
                                            tint = WhatsAppOfflineGray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (recentInteractedUsers.isNotEmpty()) {
                            item(key = "recent_users_header") {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Recent people you interacted with",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            item(key = "recent_users_carousel") {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    items(recentInteractedUsers, key = { "rec_u_${it.uid}" }) { user ->
                                        val isOnline = user.isEffectivelyOnline(nowMs)
                                        Surface(
                                            onClick = {
                                                viewModel.recordUserInteractionSearch(user)
                                                onDismiss()
                                                onSelectUser(user.uid)
                                            },
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier
                                                    .width(86.dp)
                                                    .padding(horizontal = 8.dp, vertical = 10.dp)
                                            ) {
                                                WeConnectAvatar(
                                                    displayName = user.displayName,
                                                    avatarBase64 = user.avatarBase64,
                                                    size = 44.dp,
                                                    showOnlineDot = user.onlineStatusVisible,
                                                    isOnline = isOnline
                                                )
                                                Spacer(Modifier.height(6.dp))
                                                Text(
                                                    text = user.displayName,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "@${user.username}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (recentInteractedCommunities.isNotEmpty()) {
                            item(key = "recent_comms_header") {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Recent & suggested communities",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            items(recentInteractedCommunities, key = { "rec_c_${it.id}" }) { comm ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            viewModel.recordCommunityInteractionSearch(comm)
                                            onDismiss()
                                            onSelectCommunity(comm)
                                        }
                                        .padding(horizontal = 6.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = WhatsAppTeal,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (comm.kind == "CHANNEL") Icons.Default.Campaign else Icons.Default.Groups,
                                                contentDescription = null,
                                                tint = PureWhite,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = comm.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${if (comm.kind == "CHANNEL") "Channel" else "Community"} · ${comm.memberCount} member(s)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        if (searchHistory.isEmpty() && recentInteractedUsers.isEmpty() && recentInteractedCommunities.isEmpty()) {
                            item(key = "empty_history") {
                                Text(
                                    text = "Search for people, communities, or hashtags — your recent searches and interactions will appear here.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 24.dp, horizontal = 8.dp)
                                )
                            }
                        }
                    } else {
                        if (matchingHistory.isNotEmpty()) {
                            item(key = "matching_hist_header") {
                                Text(
                                    text = "From search history",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            items(matchingHistory.take(3), key = { "m_hist_${it.id}" }) { entry ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            when (entry.kind) {
                                                SearchHistoryKind.QUERY -> query = entry.targetId
                                                SearchHistoryKind.USER -> {
                                                    profileMap[entry.targetId]?.let { viewModel.recordUserInteractionSearch(it) }
                                                    onDismiss()
                                                    onSelectUser(entry.targetId)
                                                }
                                                SearchHistoryKind.COMMUNITY -> {
                                                    communityMap[entry.targetId]?.let {
                                                        viewModel.recordCommunityInteractionSearch(it)
                                                        onDismiss()
                                                        onSelectCommunity(it)
                                                    } ?: run { query = entry.title }
                                                }
                                            }
                                        }
                                        .padding(horizontal = 6.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = null,
                                        tint = WhatsAppTeal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(entry.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    if (entry.subtitle.isNotBlank()) {
                                        Text(
                                            entry.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        if (matchedUsers.isNotEmpty()) {
                            item(key = "people_results_header") {
                                Text("People", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            }
                            items(matchedUsers, key = { "u_${it.uid}" }) { user ->
                                val isFollowing = user.uid in followingIds
                                val followsMe = user.uid in followerIds
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            viewModel.recordQuerySearch(query)
                                            viewModel.recordUserInteractionSearch(user)
                                            onDismiss()
                                            onSelectUser(user.uid)
                                        }
                                        .padding(horizontal = 4.dp, vertical = 6.dp),
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
                                    if (myProfile != null) {
                                        Surface(
                                            onClick = {
                                                scope.launch {
                                                    viewModel.handleResult(
                                                        viewModel.authRepo.toggleFollow(myProfile, user, isFollowing),
                                                        if (isFollowing) "Unfollowed @${user.username}" else "Following @${user.username}"
                                                    )
                                                }
                                            },
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isFollowing) MaterialTheme.colorScheme.secondaryContainer else WhatsAppEmerald.copy(alpha = 0.14f)
                                        ) {
                                            Text(
                                                text = if (isFollowing) "Following" else if (followsMe) "Follow Back" else "Follow",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isFollowing) MaterialTheme.colorScheme.onSecondaryContainer else WhatsAppEmerald,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (matchedCommunities.isNotEmpty()) {
                            item(key = "comm_results_header") {
                                Text("Communities & Channels", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            }
                            items(matchedCommunities, key = { "c_${it.id}" }) { comm ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            viewModel.recordQuerySearch(query)
                                            viewModel.recordCommunityInteractionSearch(comm)
                                            onDismiss()
                                            onSelectCommunity(comm)
                                        }
                                        .padding(horizontal = 4.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = WhatsAppTeal,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (comm.kind == "CHANNEL") Icons.Default.Campaign else Icons.Default.Groups,
                                                contentDescription = null,
                                                tint = PureWhite,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(comm.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            text = "${comm.memberCount} member(s)" + if (comm.description.isNotBlank()) " · ${comm.description}" else "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        if (matchedPosts.isNotEmpty()) {
                            item(key = "post_results_header") {
                                Text("Posts", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            }
                            items(matchedPosts, key = { "p_${it.id}" }) { post ->
                                Surface(
                                    onClick = {
                                        viewModel.recordQuerySearch(query)
                                        profileMap[post.authorId]?.let { viewModel.recordUserInteractionSearch(it) }
                                        onDismiss()
                                        onSelectUser(post.authorId)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "@${post.authorUsername}: ${post.caption.take(100)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(10.dp),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        if (matchedUsers.isEmpty() && matchedCommunities.isEmpty() && matchedPosts.isEmpty() && matchingHistory.isEmpty()) {
                            item(key = "no_search_results") {
                                Text(
                                    text = "No results found for \"$query\".",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 24.dp, horizontal = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

