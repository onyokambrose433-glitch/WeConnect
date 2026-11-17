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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PushPin
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Community
import com.example.data.model.PublicProfile
import com.example.data.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ReportDialog
import com.example.ui.components.formatRelativeTime
import com.example.ui.main.WeConnectViewModel
import com.example.ui.theme.PureWhite
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ALL" to "All", "COMMUNITY" to "Communities", "CHANNEL" to "Channels").forEach { (key, label) ->
                        FilterChip(
                            selected = kindFilter == key,
                            onClick = { kindFilter = key },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
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
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val members by remember(community.id) {
        viewModel.communityCallRepo.observeCommunityMembers(community.id)
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val posts by remember(community.id) {
        viewModel.communityCallRepo.observeCommunityPosts(community.id)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val isMember = members.any { it.uid == myProfile.uid }
    val isAdmin = myProfile.uid in community.adminUids || community.ownerId == myProfile.uid
    val canPost = isMember && (community.kind == "COMMUNITY" || isAdmin)

    var showNewPost by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text(community.name) },
                actions = {
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
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(community.description.ifBlank { "Welcome to ${community.name}" }, style = MaterialTheme.typography.bodyLarge)
                        Text("Rules: ${community.rulesText}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${members.size} member(s)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
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
