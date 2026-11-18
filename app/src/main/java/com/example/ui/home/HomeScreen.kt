package com.example.ui.home

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.media.MediaUtils
import com.example.data.model.PublicProfile
import com.example.data.model.SocialPost
import com.example.data.model.StoryItem
import com.example.data.model.UiState
import com.example.data.model.isEffectivelyOnline
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ReportDialog
import com.example.ui.components.WeConnectAvatar
import com.example.ui.components.formatRelativeTime
import com.example.ui.main.WeConnectViewModel
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WhatsAppCallRed
import com.example.ui.theme.WhatsAppDarkBg
import com.example.ui.theme.WhatsAppDarkTeal
import com.example.ui.theme.WhatsAppEmerald
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppInkDark
import com.example.ui.theme.WhatsAppOfflineGray
import com.example.ui.theme.WhatsAppTeal
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: WeConnectViewModel,
    myProfile: PublicProfile,
    onOpenUserProfile: (String) -> Unit,
    onMessageUser: (PublicProfile) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val postsState by viewModel.publicPostsState.collectAsStateWithLifecycle()
    val storiesState by viewModel.storiesState.collectAsStateWithLifecycle()
    val allProfilesState by viewModel.allProfilesState.collectAsStateWithLifecycle()
    val followingIds by viewModel.followingIds.collectAsStateWithLifecycle()
    val followerIds by viewModel.followerIds.collectAsStateWithLifecycle()
    val savedIds by viewModel.savedPostIds.collectAsStateWithLifecycle()
    val blockedUsers by viewModel.blockedUsers.collectAsStateWithLifecycle()
    val nowMs by viewModel.nowMsTicker.collectAsStateWithLifecycle()

    val blockedSet = remember(blockedUsers) { blockedUsers.map { it.blockedUid }.toSet() }
    val allUsers = remember(allProfilesState, blockedSet, myProfile.uid) {
        ((allProfilesState as? UiState.Success)?.data ?: emptyList())
            .filter { it.uid != myProfile.uid && it.uid !in blockedSet }
    }
    val profilesMap = remember(allProfilesState) {
        ((allProfilesState as? UiState.Success)?.data ?: emptyList()).associateBy { it.uid }
    }

    var selectedTab by remember { mutableStateOf("ALL") } // ALL, FOLLOWING, PEOPLE, SAVED
    var selectedHashtag by remember { mutableStateOf<String?>(null) }
    var peopleSearch by remember { mutableStateOf("") }

    var showCreatePost by remember { mutableStateOf(false) }
    var editingPost by remember { mutableStateOf<SocialPost?>(null) }
    var showCreateStory by remember { mutableStateOf(false) }
    var viewingStory by remember { mutableStateOf<StoryItem?>(null) }
    var commentsPost by remember { mutableStateOf<SocialPost?>(null) }
    var reportingPost by remember { mutableStateOf<SocialPost?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreatePost = true },
                containerColor = WhatsAppEmerald,
                contentColor = PureWhite,
                modifier = Modifier.testTag("create_post_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Post")
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
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Stories Strip
                item {
                    val stories = (storiesState as? UiState.Success)?.data
                        ?.filter { it.authorId !in blockedSet } ?: emptyList()
                    StoriesStrip(
                        myProfile = myProfile,
                        stories = stories,
                        onCreateStory = { showCreateStory = true },
                        onOpenStory = { viewingStory = it }
                    )
                }

                // Feed Filters + Trending Hashtags
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 0.dp)
                        ) {
                            items(
                                listOf(
                                    "ALL" to "For You",
                                    "FOLLOWING" to "Following",
                                    "PEOPLE" to "Find People (${allUsers.size})",
                                    "SAVED" to "Saved"
                                )
                            ) { (key, label) ->
                                FilterChip(
                                    selected = selectedTab == key,
                                    onClick = { selectedTab = key },
                                    label = { Text(label, maxLines = 1, style = MaterialTheme.typography.labelMedium) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = WhatsAppEmerald,
                                        selectedLabelColor = PureWhite,
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selectedTab == key,
                                        borderColor = if (selectedTab == key) WhatsAppEmerald else MaterialTheme.colorScheme.outline
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                )
                            }
                        }

                        val trending = ((postsState as? UiState.Success)?.data ?: emptyList())
                            .flatMap { it.hashtags }
                            .groupingBy { it }
                            .eachCount()
                            .entries
                            .sortedByDescending { it.value }
                            .take(6)
                            .map { it.key }

                        if (trending.isNotEmpty() || selectedHashtag != null) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (selectedHashtag != null) {
                                    item {
                                        FilterChip(
                                            selected = true,
                                            onClick = { selectedHashtag = null },
                                            label = { Text("#$selectedHashtag ×") },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = WhatsAppTeal,
                                                selectedLabelColor = PureWhite
                                            )
                                        )
                                    }
                                }
                                items(trending.filter { it != selectedHashtag }) { tag ->
                                    FilterChip(
                                        selected = false,
                                        onClick = { selectedHashtag = tag },
                                        label = { Text("#$tag") }
                                    )
                                }
                            }
                        }
                    }
                }

                // People You Know Strip (shown on For You when there are people to connect with)
                if (selectedTab == "ALL" && allUsers.isNotEmpty()) {
                    item {
                        val prioritized = remember(allUsers, followingIds, followerIds) {
                            allUsers.sortedWith(
                                compareByDescending<PublicProfile> { it.uid !in followingIds && it.uid in followerIds }
                                    .thenByDescending { it.uid !in followingIds }
                                    .thenBy { it.displayName.lowercase() }
                            )
                        }
                        PeopleYouMayKnowCarousel(
                            people = prioritized,
                            followingIds = followingIds,
                            followerIds = followerIds,
                            nowMs = nowMs,
                            onSeeAllClick = { selectedTab = "PEOPLE" },
                            onPersonClick = { onOpenUserProfile(it.uid) },
                            onToggleFollow = { person, isFollowing ->
                                scope.launch {
                                    viewModel.handleResult(
                                        viewModel.authRepo.toggleFollow(myProfile, person, isFollowing),
                                        if (isFollowing) "Unfollowed @${person.username}" else "Following @${person.username}"
                                    )
                                }
                            },
                            onChatClick = { person -> onMessageUser(person) }
                        )
                    }
                }

                // Feed or Find People Directory
                if (selectedTab == "PEOPLE") {
                    item {
                        OutlinedTextField(
                            value = peopleSearch,
                            onValueChange = { peopleSearch = it },
                            placeholder = { Text("Search people you know by name or @username…") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .testTag("find_people_search_input")
                        )
                    }

                    val matchedPeople = allUsers.filter { u ->
                        peopleSearch.isBlank() ||
                            u.displayName.contains(peopleSearch, ignoreCase = true) ||
                            u.username.contains(peopleSearch, ignoreCase = true)
                    }.sortedWith(
                        compareByDescending<PublicProfile> { it.uid in followerIds && it.uid !in followingIds }
                            .thenByDescending { it.uid in followerIds && it.uid in followingIds }
                            .thenBy { it.displayName.lowercase() }
                    )

                    if (matchedPeople.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Default.PersonAdd,
                                title = "No people found",
                                subtitle = if (peopleSearch.isBlank()) {
                                    "Invite friends to sign in on WeConnect so you can follow and chat with each other."
                                } else {
                                    "No users matched \"$peopleSearch\"."
                                },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        items(matchedPeople, key = { "person_${it.uid}" }) { person ->
                            val isFollowing = person.uid in followingIds
                            val followsMe = person.uid in followerIds
                            val isOnline = person.isEffectivelyOnline(nowMs)
                            PersonConnectCard(
                                person = person,
                                isOnline = isOnline,
                                isFollowing = isFollowing,
                                followsMe = followsMe,
                                onOpenProfile = { onOpenUserProfile(person.uid) },
                                onToggleFollow = {
                                    scope.launch {
                                        viewModel.handleResult(
                                            viewModel.authRepo.toggleFollow(myProfile, person, isFollowing),
                                            if (isFollowing) "Unfollowed @${person.username}" else "Following @${person.username}"
                                        )
                                    }
                                },
                                onChat = { onMessageUser(person) }
                            )
                        }
                    }
                } else {
                    when (val state = postsState) {
                        is UiState.Loading -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = WhatsAppEmerald)
                                }
                            }
                        }
                        is UiState.Error -> {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Default.Public,
                                    title = "Unable to load feed",
                                    subtitle = state.message,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                        is UiState.Success -> {
                            val filtered = state.data
                                .filter { it.authorId !in blockedSet }
                                .filter { post ->
                                    when (selectedTab) {
                                        "FOLLOWING" -> post.authorId in followingIds || post.authorId == myProfile.uid
                                        "SAVED" -> post.id in savedIds
                                        else -> true
                                    }
                                }
                                .filter { post ->
                                    selectedHashtag == null || selectedHashtag in post.hashtags
                                }

                            if (filtered.isEmpty()) {
                                item {
                                    EmptyStateCard(
                                        icon = Icons.Default.Public,
                                        title = "No posts yet",
                                        subtitle = if (selectedTab == "FOLLOWING") {
                                            "Follow people you know on WeConnect to see their updates here."
                                        } else {
                                            "Be the first to share an update with the WeConnect community."
                                        },
                                        actionLabel = if (selectedTab == "FOLLOWING") "Find People" else "Create Post",
                                        onAction = {
                                            if (selectedTab == "FOLLOWING") selectedTab = "PEOPLE"
                                            else showCreatePost = true
                                        },
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            } else {
                                items(filtered, key = { it.id }) { post ->
                                    val authorProfile = profilesMap[post.authorId]
                                        ?: PublicProfile(
                                            uid = post.authorId,
                                            username = post.authorUsername,
                                            displayName = post.authorName,
                                            avatarBase64 = post.authorAvatarBase64
                                        )
                                    val isFollowingAuthor = post.authorId in followingIds
                                    val authorFollowsMe = post.authorId in followerIds
                                    PostCard(
                                        post = post,
                                        myUid = myProfile.uid,
                                        isSaved = post.id in savedIds,
                                        isFollowingAuthor = isFollowingAuthor,
                                        authorFollowsMe = authorFollowsMe,
                                        authorOnline = authorProfile.isEffectivelyOnline(nowMs),
                                        authorShowDot = authorProfile.uid != myProfile.uid && authorProfile.onlineStatusVisible,
                                        viewModel = viewModel,
                                        onAuthorClick = { onOpenUserProfile(post.authorId) },
                                        onFollowAuthorClick = {
                                            scope.launch {
                                                viewModel.handleResult(
                                                    viewModel.authRepo.toggleFollow(myProfile, authorProfile, isFollowingAuthor),
                                                    if (isFollowingAuthor) "Unfollowed @${authorProfile.username}" else "Following @${authorProfile.username}"
                                                )
                                            }
                                        },
                                        onChatAuthorClick = { onMessageUser(authorProfile) },
                                        onHashtagClick = { selectedHashtag = it },
                                        onLikeClick = {
                                            scope.launch {
                                                viewModel.handleResult(viewModel.socialRepo.togglePostLike(myProfile, post))
                                            }
                                        },
                                        onCommentClick = { commentsPost = post },
                                        onShareClick = {
                                            scope.launch { viewModel.socialRepo.incrementShareCount(post.id) }
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "${post.authorName} (@${post.authorUsername}) on WeConnect:\n${post.caption}"
                                                )
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Post"))
                                        },
                                        onSaveClick = {
                                            scope.launch {
                                                viewModel.handleResult(
                                                    viewModel.socialRepo.toggleSavePost(post.id, post.id in savedIds)
                                                )
                                            }
                                        },
                                        onEditClick = { editingPost = post },
                                        onDeleteClick = {
                                            scope.launch {
                                                viewModel.handleResult(
                                                    viewModel.socialRepo.deletePost(post.id),
                                                    "Post deleted"
                                                )
                                            }
                                        },
                                        onReportClick = { reportingPost = post }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreatePost || editingPost != null) {
        CreateOrEditPostDialog(
            existingPost = editingPost,
            onDismiss = {
                showCreatePost = false
                editingPost = null
            },
            onSubmit = { caption, mediaList, visibility ->
                scope.launch {
                    val editing = editingPost
                    if (editing == null) {
                        viewModel.handleResult(
                            viewModel.socialRepo.createPost(myProfile, caption, mediaList, visibility),
                            "Post published"
                        ) { showCreatePost = false }
                    } else {
                        viewModel.handleResult(
                            viewModel.socialRepo.editPost(editing.id, caption, visibility),
                            "Post updated"
                        ) { editingPost = null }
                    }
                }
            }
        )
    }

    if (showCreateStory) {
        CreateStoryDialog(
            onDismiss = { showCreateStory = false },
            onSubmit = { caption, mediaBase64, bgHex ->
                scope.launch {
                    viewModel.handleResult(
                        viewModel.socialRepo.createStory(myProfile, caption, mediaBase64, bgHex),
                        "Story shared for 24h"
                    ) { showCreateStory = false }
                }
            }
        )
    }

    viewingStory?.let { story ->
        StoryViewerDialog(
            story = story,
            myProfile = myProfile,
            viewModel = viewModel,
            onDismiss = { viewingStory = null },
            onDelete = {
                scope.launch {
                    viewModel.handleResult(viewModel.socialRepo.deleteStory(story.id), "Story deleted") {
                        viewingStory = null
                    }
                }
            }
        )
    }

    commentsPost?.let { post ->
        val commentsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { commentsPost = null },
            sheetState = commentsSheetState
        ) {
            PostCommentsSheet(
                post = post,
                myProfile = myProfile,
                profilesMap = profilesMap,
                nowMs = nowMs,
                viewModel = viewModel,
                onOpenUserProfile = { uid ->
                    commentsPost = null
                    onOpenUserProfile(uid)
                }
            )
        }
    }

    reportingPost?.let { post ->
        ReportDialog(
            targetLabel = "Post",
            onDismiss = { reportingPost = null },
            onSubmit = { reason ->
                scope.launch {
                    viewModel.handleResult(
                        viewModel.socialRepo.submitReport("POST", post.id, reason),
                        "Report submitted"
                    ) { reportingPost = null }
                }
            }
        )
    }
}

@Composable
private fun StoriesStrip(
    myProfile: PublicProfile,
    stories: List<StoryItem>,
    onCreateStory: () -> Unit,
    onOpenStory: (StoryItem) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(72.dp)
                    .clickable(onClick = onCreateStory)
                    .testTag("add_story_button")
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    WeConnectAvatar(
                        displayName = myProfile.displayName,
                        avatarBase64 = myProfile.avatarBase64,
                        size = 58.dp
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(WhatsAppGreen)
                            .border(2.dp, PureWhite, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Story",
                            tint = PureWhite,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "My status",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        items(stories, key = { it.id }) { story ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(72.dp)
                    .clickable { onOpenStory(story) }
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .border(2.5.dp, WhatsAppGreen, CircleShape)
                        .padding(3.dp)
                ) {
                    WeConnectAvatar(
                        displayName = story.authorName,
                        avatarBase64 = story.authorAvatarBase64,
                        size = 56.dp
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = story.authorName.split(" ").firstOrNull() ?: story.authorUsername,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PeopleYouMayKnowCarousel(
    people: List<PublicProfile>,
    followingIds: Set<String>,
    followerIds: Set<String>,
    nowMs: Long,
    onSeeAllClick: () -> Unit,
    onPersonClick: (PublicProfile) -> Unit,
    onToggleFollow: (PublicProfile, Boolean) -> Unit,
    onChatClick: (PublicProfile) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "People You May Know",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "See all",
                style = MaterialTheme.typography.labelMedium,
                color = WhatsAppTeal,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable { onSeeAllClick() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(people.take(15), key = { "suggest_${it.uid}" }) { person ->
                val isFollowing = person.uid in followingIds
                val followsMe = person.uid in followerIds
                val isOnline = person.isEffectivelyOnline(nowMs)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .width(158.dp)
                        .clickable { onPersonClick(person) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WeConnectAvatar(
                            displayName = person.displayName,
                            avatarBase64 = person.avatarBase64,
                            size = 52.dp,
                            showOnlineDot = person.onlineStatusVisible,
                            isOnline = isOnline
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = person.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "@${person.username}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                isFollowing && followsMe -> WhatsAppGreen.copy(alpha = 0.14f)
                                followsMe -> WhatsAppTeal.copy(alpha = 0.14f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            }
                        ) {
                            Text(
                                text = when {
                                    isFollowing && followsMe -> "Mutual"
                                    followsMe -> "Follows you"
                                    isOnline -> "Online"
                                    else -> "Offline"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    isFollowing && followsMe -> WhatsAppEmerald
                                    followsMe -> WhatsAppTeal
                                    isOnline -> WhatsAppEmerald
                                    else -> WhatsAppOfflineGray
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { onToggleFollow(person, isFollowing) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowing) MaterialTheme.colorScheme.secondaryContainer else WhatsAppEmerald,
                                    contentColor = if (isFollowing) MaterialTheme.colorScheme.onSecondaryContainer else PureWhite
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                            ) {
                                Text(
                                    text = when {
                                        isFollowing -> "Following"
                                        followsMe -> "Follow Back"
                                        else -> "Follow"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Surface(
                                onClick = { onChatClick(person) },
                                shape = RoundedCornerShape(10.dp),
                                color = WhatsAppTeal.copy(alpha = 0.14f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = "Chat with ${person.displayName}",
                                        tint = WhatsAppTeal,
                                        modifier = Modifier.size(17.dp)
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

@Composable
private fun PersonConnectCard(
    person: PublicProfile,
    isOnline: Boolean,
    isFollowing: Boolean,
    followsMe: Boolean,
    onOpenProfile: () -> Unit,
    onToggleFollow: () -> Unit,
    onChat: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onOpenProfile() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WeConnectAvatar(
                displayName = person.displayName,
                avatarBase64 = person.avatarBase64,
                size = 48.dp,
                showOnlineDot = person.onlineStatusVisible,
                isOnline = isOnline
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = person.displayName,
                        style = MaterialTheme.typography.titleMedium,
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
                    text = "@${person.username}" + if (person.bio.isNotBlank()) " · ${person.bio}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onToggleFollow,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing) MaterialTheme.colorScheme.secondaryContainer else WhatsAppEmerald,
                    contentColor = if (isFollowing) MaterialTheme.colorScheme.onSecondaryContainer else PureWhite
                ),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = when {
                        isFollowing -> "Following"
                        followsMe -> "Follow Back"
                        else -> "Follow"
                    },
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Spacer(Modifier.width(6.dp))
            IconButton(
                onClick = onChat,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(WhatsAppTeal.copy(alpha = 0.14f))
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Chat,
                    contentDescription = "Chat",
                    tint = WhatsAppTeal,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@Composable
private fun PostCard(
    post: SocialPost,
    myUid: String,
    isSaved: Boolean,
    isFollowingAuthor: Boolean,
    authorFollowsMe: Boolean,
    authorOnline: Boolean,
    authorShowDot: Boolean,
    viewModel: WeConnectViewModel,
    onAuthorClick: () -> Unit,
    onFollowAuthorClick: () -> Unit,
    onChatAuthorClick: () -> Unit,
    onHashtagClick: (String) -> Unit,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onSaveClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onReportClick: () -> Unit
) {
    val myReaction by remember(post.id, myUid) {
        viewModel.socialRepo.observeMyReaction(post.id, myUid)
    }.collectAsStateWithLifecycle(initialValue = null)

    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onAuthorClick),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WeConnectAvatar(
                        displayName = post.authorName,
                        avatarBase64 = post.authorAvatarBase64,
                        size = 42.dp,
                        showOnlineDot = authorShowDot,
                        isOnline = authorOnline
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = post.authorName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "@${post.authorUsername} · ${formatRelativeTime(post.createdAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (post.authorId != myUid) {
                    Surface(
                        onClick = onFollowAuthorClick,
                        shape = RoundedCornerShape(16.dp),
                        color = if (isFollowingAuthor) MaterialTheme.colorScheme.secondaryContainer else WhatsAppEmerald.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, if (isFollowingAuthor) MaterialTheme.colorScheme.outline else WhatsAppEmerald)
                    ) {
                        Text(
                            text = when {
                                isFollowingAuthor -> "Following"
                                authorFollowsMe -> "Follow Back"
                                else -> "+ Follow"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isFollowingAuthor) MaterialTheme.colorScheme.onSecondaryContainer else WhatsAppEmerald,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = onChatAuthorClick, modifier = Modifier.size(34.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Chat with ${post.authorName}",
                            tint = WhatsAppTeal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Post options")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        if (post.authorId == myUid) {
                            DropdownMenuItem(
                                text = { Text("Edit Post") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = { menuExpanded = false; onEditClick() }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Post") },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                onClick = { menuExpanded = false; onDeleteClick() }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Message @${post.authorUsername}") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null) },
                                onClick = { menuExpanded = false; onChatAuthorClick() }
                            )
                            DropdownMenuItem(
                                text = { Text("Report Post") },
                                leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
                                onClick = { menuExpanded = false; onReportClick() }
                            )
                        }
                    }
                }
            }

            if (post.caption.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = post.caption,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            if (post.hashtags.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(post.hashtags) { tag ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable { onHashtagClick(tag) }
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (post.mediaBase64List.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(post.mediaBase64List) { b64 ->
                        val bmp = remember(b64) { MediaUtils.decodeBase64ToImageBitmap(b64) }
                        if (bmp != null) {
                            Image(
                                bitmap = bmp,
                                contentDescription = "Post attachment",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 260.dp, height = 220.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = onLikeClick,
                        shape = RoundedCornerShape(20.dp),
                        color = if (myReaction != null) WhatsAppEmerald.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = if (myReaction != null) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "React",
                                tint = if (myReaction != null) WhatsAppEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            if (post.likesCount > 0) {
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = "${post.likesCount}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (myReaction != null) WhatsAppEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = onCommentClick,
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = "Comments",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            if (post.commentsCount > 0) {
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = "${post.commentsCount}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = onShareClick,
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            if (post.sharesCount > 0) {
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = "${post.sharesCount}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onSaveClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Save Post",
                        tint = if (isSaved) WhatsAppEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateOrEditPostDialog(
    existingPost: SocialPost?,
    onDismiss: () -> Unit,
    onSubmit: (String, List<String>, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var caption by remember { mutableStateOf(existingPost?.caption ?: "") }
    var visibility by remember { mutableStateOf(existingPost?.visibility ?: "PUBLIC") }
    val mediaList = remember { mutableStateListOf<String>() }
    var mediaHint by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 3)
    ) { uris ->
        scope.launch {
            mediaList.clear()
            uris.take(3).forEach { uri ->
                val encoded = MediaUtils.compressUriToBase64(context, uri, maxDimension = 640, maxBase64Chars = 160_000)
                if (encoded != null) mediaList.add(encoded) else mediaHint = "One image exceeded size limit."
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingPost == null) "New Post" else "Edit Post") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it.take(2000) },
                    label = { Text("Share an update or #topic…") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("post_caption_input")
                )
                if (existingPost == null) {
                    OutlinedButton(
                        onClick = {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (mediaList.isEmpty()) "Add Photos (up to 3)" else "${mediaList.size} photo(s) attached")
                    }
                }
                if (mediaHint != null) {
                    Text(mediaHint!!, style = MaterialTheme.typography.bodySmall, color = WhatsAppCallRed)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = visibility == "PUBLIC",
                        onClick = { visibility = "PUBLIC" },
                        label = { Text("Public") }
                    )
                    FilterChip(
                        selected = visibility == "FOLLOWERS",
                        onClick = { visibility = "FOLLOWERS" },
                        label = { Text("Followers Only") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(caption, mediaList.toList(), visibility) },
                enabled = caption.isNotBlank() || mediaList.isNotEmpty(),
                modifier = Modifier.testTag("publish_post_button")
            ) {
                Text(if (existingPost == null) "Publish" else "Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CreateStoryDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String?, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var caption by remember { mutableStateOf("") }
    var bgHex by remember { mutableStateOf("#075E54") }
    var mediaBase64 by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                mediaBase64 = MediaUtils.compressUriToBase64(context, uri, maxDimension = 720, maxBase64Chars = 220_000)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add 24h Status") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it.take(400) },
                    label = { Text("Status caption") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (mediaBase64 == null) "Pick Status Image" else "Image Selected")
                }
                Text("Background Color", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        "#075E54" to WhatsAppTeal,
                        "#25D366" to WhatsAppGreen,
                        "#005C4B" to WhatsAppDarkTeal,
                        "#111B21" to WhatsAppInkDark
                    ).forEach { (hex, color) ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (bgHex == hex) 3.dp else 1.dp,
                                    color = if (bgHex == hex) PureWhite else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable { bgHex = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(caption, mediaBase64, bgHex) },
                enabled = caption.isNotBlank() || mediaBase64 != null
            ) {
                Text("Share Status")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun StoryViewerDialog(
    story: StoryItem,
    myProfile: PublicProfile,
    viewModel: WeConnectViewModel,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val views by remember(story.id) {
        viewModel.socialRepo.observeStoryViews(story.id)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    LaunchedEffect(story.id) {
        if (story.authorId != myProfile.uid) {
            viewModel.socialRepo.recordStoryView(story, myProfile.displayName)
        }
    }

    val bgColor = when (story.bgColorHex) {
        "#25D366", "#16A34A" -> WhatsAppGreen
        "#005C4B", "#075E45" -> WhatsAppDarkTeal
        "#111B21", "#0B1F14" -> WhatsAppDarkBg
        else -> WhatsAppTeal
    }
    val textColor = PureWhite

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = bgColor,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(story.authorName, color = textColor, style = MaterialTheme.typography.titleMedium)
                    Text("@${story.authorUsername} · ${formatRelativeTime(story.createdAt)}", color = textColor.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                }
                Row {
                    if (story.authorId == myProfile.uid) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Story", tint = textColor)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = textColor)
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val bmp = remember(story.mediaBase64) { MediaUtils.decodeBase64ToImageBitmap(story.mediaBase64) }
                if (bmp != null) {
                    Image(
                        bitmap = bmp,
                        contentDescription = "Story image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                }
                if (story.caption.isNotBlank()) {
                    Text(
                        text = story.caption,
                        style = MaterialTheme.typography.titleLarge,
                        color = textColor
                    )
                }
                if (story.authorId == myProfile.uid) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = textColor, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Viewed by ${views.size} user(s)" + if (views.isNotEmpty()) ": " + views.joinToString(", ") { it.viewerName } else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("💚", "✨", "👏").forEach { emoji ->
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        viewModel.socialRepo.recordStoryView(story, myProfile.displayName, emoji)
                                        viewModel.emitNotice("Reacted $emoji to ${story.authorName}'s story")
                                    }
                                },
                                border = BorderStroke(1.dp, textColor)
                            ) {
                                Text(emoji)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}
