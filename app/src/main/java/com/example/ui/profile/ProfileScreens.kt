package com.example.ui.profile

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Key
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.media.MediaUtils
import com.example.data.model.PublicProfile
import com.example.data.model.UiState
import com.example.ui.components.ReportDialog
import com.example.ui.components.WeConnectAvatar
import com.example.ui.components.formatRelativeTime
import com.example.ui.main.WeConnectViewModel
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WhatsAppCallRed
import com.example.ui.theme.WhatsAppEmerald
import com.example.ui.theme.WhatsAppTeal
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    viewModel: WeConnectViewModel,
    myProfile: PublicProfile,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val privateUser by viewModel.privateUserState.collectAsStateWithLifecycle()
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val blockedUsers by viewModel.blockedUsers.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val deviceIdentity by viewModel.deviceIdentity.collectAsStateWithLifecycle()
    val myPosts by remember(myProfile.uid) {
        viewModel.socialRepo.observeUserPosts(myProfile.uid)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    var showEditProfile by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showDevicesDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }

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
                            isOnline = myProfile.online
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
                            StatBox("Followers", myProfile.followersCount)
                            StatBox("Following", myProfile.followingCount)
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
                            title = "Notifications (${notifications.count { !it.read }} unread)",
                            subtitle = "Follows, reactions, comments, and call alerts",
                            onClick = { showNotificationsDialog = true }
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

    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = { Text("Notifications") },
            text = {
                LazyColumn(modifier = Modifier.height(260.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (notifications.isEmpty()) {
                        item { Text("You are all caught up.", style = MaterialTheme.typography.bodyMedium) }
                    } else {
                        items(notifications, key = { it.id }) { notif ->
                            Surface(
                                color = if (notif.read) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch { viewModel.authRepo.markNotificationRead(notif.id) }
                                    }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(notif.summary, style = MaterialTheme.typography.bodyMedium)
                                    Text(formatRelativeTime(notif.createdAt), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { OutlinedButton(onClick = { showNotificationsDialog = false }) { Text("Close") } }
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
}

@Composable
private fun StatBox(label: String, count: Long) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WeConnectAvatar(target.displayName, target.avatarBase64, size = 50.dp, showOnlineDot = target.onlineStatusVisible, isOnline = target.online)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(target.displayName, style = MaterialTheme.typography.titleMedium)
                    Text("@${target.username}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
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
                                    viewModel.handleResult(viewModel.authRepo.toggleFollow(myProfile, target, isFollowing))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFollowing) MaterialTheme.colorScheme.secondaryContainer else WhatsAppEmerald,
                                contentColor = if (isFollowing) MaterialTheme.colorScheme.onSecondaryContainer else PureWhite
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isFollowing) "Following" else "Follow")
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
    val profiles = (viewModel.allProfilesState.collectAsStateWithLifecycle().value as? UiState.Success)?.data ?: emptyList()
    val communities = (viewModel.communitiesState.collectAsStateWithLifecycle().value as? UiState.Success)?.data ?: emptyList()
    val posts = (viewModel.publicPostsState.collectAsStateWithLifecycle().value as? UiState.Success)?.data ?: emptyList()

    val matchedUsers = remember(query, profiles) {
        profiles.filter {
            it.uid != myUid && (query.isBlank() || it.displayName.contains(query, true) || it.username.contains(query, true))
        }.take(10)
    }
    val matchedCommunities = remember(query, communities) {
        communities.filter {
            query.isNotBlank() && (it.name.contains(query, true) || it.description.contains(query, true))
        }.take(6)
    }
    val matchedPosts = remember(query, posts) {
        posts.filter {
            query.isNotBlank() && (it.caption.contains(query, true) || it.hashtags.any { h -> h.contains(query.removePrefix("#"), true) })
        }.take(6)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Search WeConnect") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search users, @handles, #tags, communities…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(modifier = Modifier.height(260.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (matchedUsers.isNotEmpty()) {
                        item { Text("People", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
                        items(matchedUsers, key = { "u_${it.uid}" }) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onDismiss(); onSelectUser(user.uid) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                WeConnectAvatar(user.displayName, user.avatarBase64, size = 34.dp)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(user.displayName, style = MaterialTheme.typography.titleSmall)
                                    Text("@${user.username}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    if (matchedCommunities.isNotEmpty()) {
                        item { Text("Communities & Channels", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
                        items(matchedCommunities, key = { "c_${it.id}" }) { comm ->
                            Text(
                                text = "${comm.name} (${comm.memberCount} members)",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onDismiss(); onSelectCommunity(comm) }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                    if (matchedPosts.isNotEmpty()) {
                        item { Text("Posts", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
                        items(matchedPosts, key = { "p_${it.id}" }) { post ->
                            Text(
                                text = "@${post.authorUsername}: ${post.caption.take(80)}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onDismiss(); onSelectUser(post.authorId) }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Close") } }
    )
}
