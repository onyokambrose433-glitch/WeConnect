package com.example.ui.communities

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.Community
import com.example.data.model.CommunityMember
import com.example.data.model.PublicProfile
import com.example.data.model.UiState
import com.example.data.model.isEffectivelyOnline
import com.example.ui.components.EmptyStateCard
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
fun CommunitiesScreen(
    viewModel: WeConnectViewModel,
    myProfile: PublicProfile,
    onOpenCommunity: (Community) -> Unit
) {
    val scope = rememberCoroutineScope()
    val communitiesState by viewModel.communitiesState.collectAsStateWithLifecycle()
    var kindFilter by remember { mutableStateOf("ALL") } // ALL, COMMUNITY, CHANNEL
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = WhatsAppEmerald,
                contentColor = PureWhite,
                modifier = Modifier.testTag("create_community_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Community or Channel")
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
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listOf("ALL" to "All", "COMMUNITY" to "Communities", "CHANNEL" to "Channels")) { (key, label) ->
                        FilterChip(
                            selected = kindFilter == key,
                            onClick = { kindFilter = key },
                            label = { Text(label, maxLines = 1, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WhatsAppEmerald,
                                selectedLabelColor = PureWhite,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = kindFilter == key,
                                borderColor = if (kindFilter == key) WhatsAppEmerald else MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }

                when (val state = communitiesState) {
                    is UiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = WhatsAppEmerald)
                        }
                    }
                    is UiState.Error -> {
                        Box(Modifier.padding(16.dp)) {
                            EmptyStateCard(
                                icon = Icons.Default.Groups,
                                title = "Unable to load communities",
                                subtitle = state.message
                            )
                        }
                    }
                    is UiState.Success -> {
                        val list = state.data.filter { kindFilter == "ALL" || it.kind == kindFilter }
                        if (list.isEmpty()) {
                            Box(Modifier.padding(16.dp)) {
                                EmptyStateCard(
                                    icon = Icons.Default.Groups,
                                    title = "No communities or channels yet",
                                    subtitle = "Create a public community space or announcement channel around shared interests.",
                                    actionLabel = "Create Community",
                                    onAction = { showCreateDialog = true }
                                )
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(list, key = { it.id }) { community ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenCommunity(community) },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = WhatsAppTeal,
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = if (community.kind == "CHANNEL") Icons.Default.Campaign else Icons.Default.Groups,
                                                        contentDescription = null,
                                                        tint = PureWhite
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(community.name, style = MaterialTheme.typography.titleMedium)
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text(
                                                            text = if (community.kind == "CHANNEL") "Channel" else "Community",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                if (community.description.isNotBlank()) {
                                                    Spacer(Modifier.height(4.dp))
                                                    Text(
                                                        text = community.description,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 2
                                                    )
                                                }
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    text = "${community.memberCount} member(s)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
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
    }

    if (showCreateDialog) {
        CreateCommunityDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, desc, kind, visibility, rules ->
                scope.launch {
                    viewModel.handleResult(
                        viewModel.communityCallRepo.createCommunity(myProfile, name, desc, kind, visibility, rules),
                        "${if (kind == "CHANNEL") "Channel" else "Community"} created"
                    ) { showCreateDialog = false }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityDetailScreen(
    community: Community,
    viewModel: WeConnectViewModel,
    myProfile: PublicProfile,
    onBack: () -> Unit,
    onOpenUserProfile: (String) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val scope = rememberCoroutineScope()
    val members by remember(community.id) {
        viewModel.communityCallRepo.observeCommunityMembers(community.id)
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val posts by remember(community.id) {
        viewModel.communityCallRepo.observeCommunityPosts(community.id)
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val allProfilesState by viewModel.allProfilesState.collectAsStateWithLifecycle()
    val blockedUsers by viewModel.blockedUsers.collectAsStateWithLifecycle()
    val nowMs by viewModel.nowMsTicker.collectAsStateWithLifecycle()

    val profilesMap = remember(allProfilesState) {
        ((allProfilesState as? UiState.Success)?.data ?: emptyList()).associateBy { it.uid }
    }
    val allDiscoverableProfiles = remember(allProfilesState) {
        (allProfilesState as? UiState.Success)?.data ?: emptyList()
    }
    val blockedUidSet = remember(blockedUsers) {
        blockedUsers.map { it.blockedUid }.toSet()
    }
    val sortedMembers = remember(members, community.ownerId) {
        members.sortedWith(
            compareBy<CommunityMember> {
                when {
                    it.uid == community.ownerId -> 0
                    it.role == "ADMIN" -> 1
                    it.role == "MODERATOR" -> 2
                    else -> 3
                }
            }.thenBy { it.displayName.lowercase() }
        )
    }

    val isMember = members.any { it.uid == myProfile.uid }
    val isAdmin = myProfile.uid in community.adminUids || community.ownerId == myProfile.uid
    val canPost = isMember && (community.kind == "COMMUNITY" || isAdmin)

    var showNewPost by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }
    var showMembersDialog by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var memberPendingRemoval by remember { mutableStateOf<CommunityMember?>(null) }
    var busyMemberUids by remember { mutableStateOf<Set<String>>(emptySet()) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text(community.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = "${members.size} member(s) · ${if (community.kind == "CHANNEL") "Channel" else "Community"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (isAdmin) {
                        IconButton(
                            onClick = { showAddMemberDialog = true },
                            modifier = Modifier.testTag("topbar_add_community_member")
                        ) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = "Add people",
                                tint = WhatsAppEmerald
                            )
                        }
                    }
                    IconButton(onClick = { showMembersDialog = true }) {
                        Icon(Icons.Default.People, contentDescription = "Community Members")
                    }
                    IconButton(onClick = { showReport = true }) {
                        Icon(Icons.Default.Flag, contentDescription = "Report Community")
                    }
                }
            )
        },
        floatingActionButton = {
            if (canPost) {
                FloatingActionButton(
                    onClick = { showNewPost = true },
                    containerColor = WhatsAppEmerald,
                    contentColor = PureWhite
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Community Post")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(community.description.ifBlank { "Welcome to ${community.name}" }, style = MaterialTheme.typography.bodyLarge)
                        Text("Rules: ${community.rulesText}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        if (sortedMembers.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                items(sortedMembers.take(8), key = { it.uid }) { member ->
                                    val memberProfile = profilesMap[member.uid]
                                    val isOnline = memberProfile?.isEffectivelyOnline(nowMs) == true
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .width(58.dp)
                                            .clickable { showMembersDialog = true }
                                    ) {
                                        WeConnectAvatar(
                                            displayName = member.displayName,
                                            avatarBase64 = memberProfile?.avatarBase64,
                                            size = 38.dp,
                                            showOnlineDot = memberProfile?.onlineStatusVisible != false,
                                            isOnline = isOnline
                                        )
                                        Spacer(Modifier.height(3.dp))
                                        Text(
                                            text = member.displayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (sortedMembers.size > 8) {
                                    item {
                                        TextButton(onClick = { showMembersDialog = true }) {
                                            Text("+${sortedMembers.size - 8} more")
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { showMembersDialog = true },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("view_community_members_button")
                            ) {
                                Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Members (${members.size})")
                            }

                            if (isAdmin) {
                                Button(
                                    onClick = { showAddMemberDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = WhatsAppEmerald,
                                        contentColor = PureWhite
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("add_community_member_button")
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Add People")
                                }
                            }

                            Spacer(Modifier.weight(1f))

                            if (community.ownerId != myProfile.uid) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            viewModel.handleResult(
                                                viewModel.communityCallRepo.joinOrLeaveCommunity(community, myProfile, isMember),
                                                if (isMember) "Left ${community.name}" else "Joined ${community.name}"
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isMember) MaterialTheme.colorScheme.secondaryContainer else WhatsAppEmerald,
                                        contentColor = if (isMember) MaterialTheme.colorScheme.onSecondaryContainer else PureWhite
                                    )
                                ) {
                                    Text(if (isMember) "Leave" else "Join")
                                }
                            }
                        }
                    }
                }
            }

            if (posts.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.Campaign,
                        title = "No announcements or discussions yet",
                        subtitle = if (canPost) "Tap + to post an update." else "Join this space to participate."
                    )
                }
            } else {
                items(posts, key = { it.id }) { post ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, if (post.pinned) WhatsAppTeal else MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (post.pinned) {
                                        Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = WhatsAppTeal, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                    }
                                    Text(post.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                }
                                if (isAdmin) {
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                viewModel.handleResult(
                                                    viewModel.communityCallRepo.togglePinCommunityPost(community.id, post.id, post.pinned)
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.PushPin, contentDescription = "Pin toggle", tint = if (post.pinned) WhatsAppTeal else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            Text(
                                text = "${post.authorName} (@${post.authorUsername}) · ${formatRelativeTime(post.createdAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(post.body, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    if (showMembersDialog) {
        CommunityMembersDialog(
            community = community,
            members = sortedMembers,
            profilesMap = profilesMap,
            myUid = myProfile.uid,
            isAdmin = isAdmin,
            nowMs = nowMs,
            busyMemberUids = busyMemberUids,
            onDismiss = { showMembersDialog = false },
            onOpenAddPeople = {
                showMembersDialog = false
                showAddMemberDialog = true
            },
            onOpenProfile = { uid ->
                showMembersDialog = false
                onOpenUserProfile(uid)
            },
            onRequestRemove = { member ->
                memberPendingRemoval = member
            }
        )
    }

    if (showAddMemberDialog) {
        AddPeopleToCommunityDialog(
            community = community,
            allProfiles = allDiscoverableProfiles,
            existingMemberUids = members.map { it.uid }.toSet(),
            blockedUids = blockedUidSet,
            myUid = myProfile.uid,
            nowMs = nowMs,
            busyMemberUids = busyMemberUids,
            onDismiss = { showAddMemberDialog = false },
            onAddUser = { targetUser, role ->
                if (targetUser.uid in busyMemberUids) return@AddPeopleToCommunityDialog
                busyMemberUids = busyMemberUids + targetUser.uid
                scope.launch {
                    try {
                        viewModel.handleResult(
                            viewModel.communityCallRepo.addMemberToCommunity(
                                community = community,
                                actor = myProfile,
                                target = targetUser,
                                role = role
                            ),
                            "Added ${targetUser.displayName} (@${targetUser.username}) to ${community.name}"
                        )
                    } finally {
                        busyMemberUids = busyMemberUids - targetUser.uid
                    }
                }
            }
        )
    }

    memberPendingRemoval?.let { targetMember ->
        AlertDialog(
            onDismissRequest = { memberPendingRemoval = null },
            title = { Text("Remove Member") },
            text = {
                Text(
                    "Remove ${targetMember.displayName} (@${targetMember.username}) from ${community.name}? They will no longer be a member of this space."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        memberPendingRemoval = null
                        busyMemberUids = busyMemberUids + targetMember.uid
                        scope.launch {
                            try {
                                viewModel.handleResult(
                                    viewModel.communityCallRepo.removeMemberFromCommunity(
                                        community = community,
                                        actor = myProfile,
                                        targetUid = targetMember.uid
                                    ),
                                    "Removed ${targetMember.displayName} from ${community.name}"
                                )
                            } finally {
                                busyMemberUids = busyMemberUids - targetMember.uid
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WhatsAppCallRed,
                        contentColor = PureWhite
                    ),
                    modifier = Modifier.testTag("confirm_remove_member_button")
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { memberPendingRemoval = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showNewPost) {
        var title by remember { mutableStateOf("") }
        var body by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewPost = false },
            title = { Text("New ${if (community.kind == "CHANNEL") "Announcement" else "Discussion"}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it.take(120) },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it.take(2000) },
                        label = { Text("Message") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.handleResult(
                                viewModel.communityCallRepo.publishCommunityPost(community.id, myProfile, title, body),
                                "Published"
                            ) { showNewPost = false }
                        }
                    },
                    enabled = title.isNotBlank() && body.isNotBlank()
                ) { Text("Post") }
            },
            dismissButton = { OutlinedButton(onClick = { showNewPost = false }) { Text("Cancel") } }
        )
    }

    if (showReport) {
        ReportDialog(
            targetLabel = "Community",
            onDismiss = { showReport = false },
            onSubmit = { reason ->
                scope.launch {
                    viewModel.handleResult(
                        viewModel.socialRepo.submitReport("COMMUNITY", community.id, reason),
                        "Report submitted"
                    ) { showReport = false }
                }
            }
        )
    }
}

@Composable
private fun CommunityMembersDialog(
    community: Community,
    members: List<CommunityMember>,
    profilesMap: Map<String, PublicProfile>,
    myUid: String,
    isAdmin: Boolean,
    nowMs: Long,
    busyMemberUids: Set<String>,
    onDismiss: () -> Unit,
    onOpenAddPeople: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onRequestRemove: (CommunityMember) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filteredMembers = remember(members, query) {
        val clean = query.trim().lowercase()
        if (clean.isEmpty()) members
        else members.filter {
            it.displayName.lowercase().contains(clean) || it.username.lowercase().contains(clean)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Members (${members.size})", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = community.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isAdmin) {
                    TextButton(
                        onClick = onOpenAddPeople,
                        modifier = Modifier.testTag("members_dialog_add_button")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add")
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (members.size > 4) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it.take(40) },
                        placeholder = { Text("Search members") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("community_members_search")
                    )
                }
                if (filteredMembers.isEmpty()) {
                    Text(
                        text = "No matching members found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 340.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredMembers, key = { it.uid }) { member ->
                            val profile = profilesMap[member.uid]
                            val isOnline = profile?.isEffectivelyOnline(nowMs) == true
                            val isOwnerMember = member.uid == community.ownerId
                            val roleLabel = when {
                                isOwnerMember -> "Owner"
                                member.role == "ADMIN" -> "Admin"
                                member.role == "MODERATOR" -> "Moderator"
                                else -> "Member"
                            }
                            val canRemove = isAdmin && !isOwnerMember && member.uid != myUid
                            val isBusy = member.uid in busyMemberUids

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = member.uid != myUid) { onOpenProfile(member.uid) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                WeConnectAvatar(
                                    displayName = member.displayName,
                                    avatarBase64 = profile?.avatarBase64,
                                    size = 40.dp,
                                    showOnlineDot = profile?.onlineStatusVisible != false,
                                    isOnline = isOnline
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = if (member.uid == myUid) "${member.displayName} (You)" else member.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Surface(
                                            color = if (isOwnerMember || member.role == "ADMIN") {
                                                WhatsAppEmerald.copy(alpha = 0.14f)
                                            } else {
                                                MaterialTheme.colorScheme.secondaryContainer
                                            },
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = roleLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isOwnerMember || member.role == "ADMIN") {
                                                    WhatsAppEmerald
                                                } else {
                                                    MaterialTheme.colorScheme.onSecondaryContainer
                                                },
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "@${member.username}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (canRemove) {
                                    if (isBusy) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = WhatsAppCallRed
                                        )
                                    } else {
                                        IconButton(
                                            onClick = { onRequestRemove(member) },
                                            modifier = Modifier.testTag("remove_member_${member.uid}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PersonRemove,
                                                contentDescription = "Remove ${member.displayName}",
                                                tint = WhatsAppCallRed
                                            )
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun AddPeopleToCommunityDialog(
    community: Community,
    allProfiles: List<PublicProfile>,
    existingMemberUids: Set<String>,
    blockedUids: Set<String>,
    myUid: String,
    nowMs: Long,
    busyMemberUids: Set<String>,
    onDismiss: () -> Unit,
    onAddUser: (PublicProfile, String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("MEMBER") }

    val candidates = remember(allProfiles, existingMemberUids, blockedUids, myUid, query) {
        val clean = query.trim().lowercase()
        allProfiles
            .filter { it.uid != myUid && it.uid !in blockedUids }
            .filter {
                clean.isEmpty() ||
                    it.displayName.lowercase().contains(clean) ||
                    it.username.lowercase().contains(clean)
            }
            .sortedWith(
                compareBy<PublicProfile> { it.uid in existingMemberUids }
                    .thenBy { it.displayName.lowercase() }
            )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Add People", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Invite or add people directly to ${community.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(50) },
                    placeholder = { Text("Search name or @username") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("community_add_member_search")
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Role:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FilterChip(
                        selected = selectedRole == "MEMBER",
                        onClick = { selectedRole = "MEMBER" },
                        label = { Text("Member") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WhatsAppEmerald,
                            selectedLabelColor = PureWhite
                        )
                    )
                    FilterChip(
                        selected = selectedRole == "MODERATOR",
                        onClick = { selectedRole = "MODERATOR" },
                        label = { Text("Moderator") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WhatsAppTeal,
                            selectedLabelColor = PureWhite
                        )
                    )
                }

                if (candidates.isEmpty()) {
                    Text(
                        text = if (query.isBlank()) "No other discoverable WeConnect users available." else "No users match \"$query\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(candidates, key = { it.uid }) { user ->
                            val alreadyMember = user.uid in existingMemberUids
                            val isBusy = user.uid in busyMemberUids
                            val isOnline = user.isEffectivelyOnline(nowMs)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                WeConnectAvatar(
                                    displayName = user.displayName,
                                    avatarBase64 = user.avatarBase64,
                                    size = 40.dp,
                                    showOnlineDot = user.onlineStatusVisible,
                                    isOnline = isOnline
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "@${user.username}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (alreadyMember) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = "Member",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = { onAddUser(user, selectedRole) },
                                        enabled = !isBusy,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = WhatsAppEmerald,
                                            contentColor = PureWhite
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                        modifier = Modifier
                                            .height(34.dp)
                                            .testTag("add_member_${user.uid}")
                                    ) {
                                        if (isBusy) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                strokeWidth = 2.dp,
                                                color = PureWhite
                                            )
                                        } else {
                                            Text("Add", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun CreateCommunityDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var rules by remember { mutableStateOf("Be respectful, verify sources, and protect privacy.") }
    var kind by remember { mutableStateOf("COMMUNITY") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Community or Channel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = kind == "COMMUNITY",
                        onClick = { kind = "COMMUNITY" },
                        label = { Text("Community") }
                    )
                    FilterChip(
                        selected = kind == "CHANNEL",
                        onClick = { kind = "CHANNEL" },
                        label = { Text("Channel") }
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(80) },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it.take(400) },
                    label = { Text("Description") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rules,
                    onValueChange = { rules = it.take(600) },
                    label = { Text("Community Rules") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, desc, kind, "PUBLIC", rules) },
                enabled = name.trim().length >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppEmerald, contentColor = PureWhite)
            ) { Text("Create") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
