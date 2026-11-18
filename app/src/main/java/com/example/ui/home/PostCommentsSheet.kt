package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PostComment
import com.example.data.model.PublicProfile
import com.example.data.model.SocialPost
import com.example.data.model.isEffectivelyOnline
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.WeConnectAvatar
import com.example.ui.components.formatRelativeTime
import com.example.ui.main.WeConnectViewModel
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WhatsAppCallRed
import com.example.ui.theme.WhatsAppEmerald
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppTeal
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class CommentReplyItem(
    val comment: PostComment,
    val directParent: PostComment?
)

data class CommentThread(
    val root: PostComment,
    val replies: List<CommentReplyItem>
)

internal fun findRootCommentId(
    commentId: String?,
    byId: Map<String, PostComment>
): String? {
    if (commentId.isNullOrBlank()) return null
    var current = byId[commentId] ?: return null
    val visited = mutableSetOf<String>()
    while (!current.replyToCommentId.isNullOrBlank()) {
        if (!visited.add(current.id)) break
        val parent = byId[current.replyToCommentId] ?: break
        current = parent
    }
    return current.id
}

internal fun buildCommentThreads(comments: List<PostComment>): List<CommentThread> {
    if (comments.isEmpty()) return emptyList()
    val byId = comments.associateBy { it.id }

    val roots = mutableListOf<PostComment>()
    val repliesByRoot = LinkedHashMap<String, MutableList<CommentReplyItem>>()

    comments.forEach { comment ->
        val parentId = comment.replyToCommentId
        val directParent = if (!parentId.isNullOrBlank()) byId[parentId] else null
        if (directParent == null || parentId == comment.id) {
            roots.add(comment)
            repliesByRoot.getOrPut(comment.id) { mutableListOf() }
        } else {
            val rootId = findRootCommentId(comment.id, byId)
            if (rootId == null || rootId == comment.id) {
                roots.add(comment)
                repliesByRoot.getOrPut(comment.id) { mutableListOf() }
            } else {
                repliesByRoot.getOrPut(rootId) { mutableListOf() }
                    .add(CommentReplyItem(comment = comment, directParent = directParent))
            }
        }
    }

    return roots.map { root ->
        CommentThread(
            root = root,
            replies = repliesByRoot[root.id].orEmpty()
        )
    }
}

@Composable
fun PostCommentsSheet(
    post: SocialPost,
    myProfile: PublicProfile,
    profilesMap: Map<String, PublicProfile>,
    nowMs: Long,
    viewModel: WeConnectViewModel,
    onOpenUserProfile: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val comments by remember(post.id) {
        viewModel.socialRepo.observePostComments(post.id)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val threads = remember(comments) { buildCommentThreads(comments) }
    val commentsById = remember(comments) { comments.associateBy { it.id } }

    var commentText by remember { mutableStateOf("") }
    var replyTarget by remember { mutableStateOf<PostComment?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var highlightedCommentId by remember { mutableStateOf<String?>(null) }
    val expandedOverrides = remember { mutableStateMapOf<String, Boolean>() }

    val listState = rememberLazyListState()
    val inputFocusRequester = remember { FocusRequester() }

    LaunchedEffect(highlightedCommentId) {
        if (highlightedCommentId != null) {
            delay(1600L)
            highlightedCommentId = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Comments (${comments.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap Reply on any comment to join its thread",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (replyTarget != null) {
                Surface(
                    color = WhatsAppEmerald.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Replying",
                        style = MaterialTheme.typography.labelSmall,
                        color = WhatsAppEmerald,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp, max = 380.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (threads.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.ChatBubbleOutline,
                        title = "No comments yet",
                        subtitle = "Start the conversation or reply to others once comments arrive."
                    )
                }
            } else {
                itemsIndexed(threads, key = { _, thread -> thread.root.id }) { threadIdx, thread ->
                    val defaultExpanded = thread.replies.size <= 3
                    val isExpanded = expandedOverrides[thread.root.id] ?: defaultExpanded

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CommentItemCard(
                            comment = thread.root,
                            isPostAuthor = thread.root.authorId == post.authorId,
                            isMine = thread.root.authorId == myProfile.uid,
                            isHighlighted = highlightedCommentId == thread.root.id,
                            authorProfile = profilesMap[thread.root.authorId],
                            nowMs = nowMs,
                            isReply = false,
                            replyParent = null,
                            onAuthorClick = { onOpenUserProfile(thread.root.authorId) },
                            onReplyClick = {
                                replyTarget = thread.root
                                expandedOverrides[thread.root.id] = true
                                scope.launch {
                                    delay(50L)
                                    runCatching { inputFocusRequester.requestFocus() }
                                }
                            },
                            onDeleteClick = {
                                scope.launch {
                                    if (replyTarget?.id == thread.root.id) replyTarget = null
                                    viewModel.handleResult(
                                        viewModel.socialRepo.deleteComment(post.id, thread.root.id),
                                        "Comment deleted"
                                    )
                                }
                            },
                            onJumpToParent = {}
                        )

                        if (thread.replies.isNotEmpty()) {
                            if (!isExpanded) {
                                TextButton(
                                    onClick = { expandedOverrides[thread.root.id] = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier
                                        .padding(start = 18.dp)
                                        .height(30.dp)
                                ) {
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = WhatsAppEmerald,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "View ${thread.replies.size} ${if (thread.replies.size == 1) "reply" else "replies"}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = WhatsAppEmerald,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min)
                                        .padding(start = 16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.5.dp)
                                            .fillMaxHeight()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(WhatsAppEmerald.copy(alpha = 0.45f))
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        thread.replies.forEach { replyItem ->
                                            val reply = replyItem.comment
                                            CommentItemCard(
                                                comment = reply,
                                                isPostAuthor = reply.authorId == post.authorId,
                                                isMine = reply.authorId == myProfile.uid,
                                                isHighlighted = highlightedCommentId == reply.id,
                                                authorProfile = profilesMap[reply.authorId],
                                                nowMs = nowMs,
                                                isReply = true,
                                                replyParent = replyItem.directParent,
                                                onAuthorClick = { onOpenUserProfile(reply.authorId) },
                                                onReplyClick = {
                                                    replyTarget = reply
                                                    expandedOverrides[thread.root.id] = true
                                                    scope.launch {
                                                        delay(50L)
                                                        runCatching { inputFocusRequester.requestFocus() }
                                                    }
                                                },
                                                onDeleteClick = {
                                                    scope.launch {
                                                        if (replyTarget?.id == reply.id) replyTarget = null
                                                        viewModel.handleResult(
                                                            viewModel.socialRepo.deleteComment(post.id, reply.id),
                                                            "Reply deleted"
                                                        )
                                                    }
                                                },
                                                onJumpToParent = { parentId ->
                                                    highlightedCommentId = parentId
                                                    scope.launch {
                                                        listState.animateScrollToItem(threadIdx)
                                                    }
                                                }
                                            )
                                        }

                                        if (thread.replies.size > 2) {
                                            TextButton(
                                                onClick = { expandedOverrides[thread.root.id] = false },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.KeyboardArrowUp,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    text = "Hide replies",
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

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

        AnimatedVisibility(visible = replyTarget != null) {
            val target = replyTarget
            if (target != null) {
                Surface(
                    color = WhatsAppEmerald.copy(alpha = 0.09f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, WhatsAppEmerald.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.5.dp)
                                .height(32.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(WhatsAppEmerald)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Replying to ${target.authorName} (@${target.authorUsername})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = WhatsAppEmerald,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = target.text,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { replyTarget = null },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("cancel_comment_reply")
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cancel Reply",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it.take(600) },
                placeholder = {
                    Text(
                        text = replyTarget?.let { "Reply to @${it.authorUsername}…" } ?: "Write a comment…",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(inputFocusRequester)
                    .testTag("comment_input"),
                maxLines = 3,
                shape = RoundedCornerShape(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (commentText.isNotBlank() && !isSending) WhatsAppEmerald else MaterialTheme.colorScheme.surfaceVariant
            ) {
                IconButton(
                    onClick = {
                        val txt = commentText
                        val target = replyTarget
                        if (txt.isBlank() || isSending) return@IconButton
                        val rootIdForReply = target?.let { findRootCommentId(it.id, commentsById) ?: it.id }
                        isSending = true
                        scope.launch {
                            viewModel.handleResult(
                                viewModel.socialRepo.addComment(
                                    myProfile = myProfile,
                                    post = post,
                                    text = txt,
                                    replyToCommentId = target?.id,
                                    replyToComment = target
                                )
                            ) {
                                commentText = ""
                                replyTarget = null
                                scope.launch {
                                    if (rootIdForReply != null) {
                                        expandedOverrides[rootIdForReply] = true
                                        val idx = threads.indexOfFirst { it.root.id == rootIdForReply }
                                        if (idx >= 0) {
                                            listState.animateScrollToItem(idx)
                                        }
                                    } else if (threads.isNotEmpty()) {
                                        listState.animateScrollToItem(threads.lastIndex)
                                    }
                                }
                            }
                            isSending = false
                        }
                    },
                    enabled = commentText.isNotBlank() && !isSending,
                    modifier = Modifier.testTag("send_comment_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = if (replyTarget != null) "Send Reply" else "Send Comment",
                        tint = if (commentText.isNotBlank() && !isSending) PureWhite else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun CommentItemCard(
    comment: PostComment,
    isPostAuthor: Boolean,
    isMine: Boolean,
    isHighlighted: Boolean,
    authorProfile: PublicProfile?,
    nowMs: Long,
    isReply: Boolean,
    replyParent: PostComment?,
    onAuthorClick: () -> Unit,
    onReplyClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onJumpToParent: (String) -> Unit
) {
    val containerColor = when {
        isHighlighted -> WhatsAppEmerald.copy(alpha = 0.14f)
        isReply -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val border = if (isHighlighted) {
        BorderStroke(1.5.dp, WhatsAppEmerald)
    } else if (isReply) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    } else {
        null
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = border,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.clickable(onClick = onAuthorClick)) {
                    WeConnectAvatar(
                        displayName = comment.authorName,
                        avatarBase64 = authorProfile?.avatarBase64,
                        size = if (isReply) 26.dp else 32.dp,
                        showOnlineDot = authorProfile != null && !isMine && authorProfile.onlineStatusVisible,
                        isOnline = authorProfile?.isEffectivelyOnline(nowMs) == true
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onAuthorClick)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = comment.authorName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isPostAuthor) {
                            Surface(
                                color = WhatsAppGreen.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Author",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WhatsAppEmerald,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "@${comment.authorUsername}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formatRelativeTime(comment.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isReply && replyParent != null) {
                Spacer(Modifier.height(6.dp))
                Surface(
                    onClick = { onJumpToParent(replyParent.id) },
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(16.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(WhatsAppTeal)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Replying to @${replyParent.authorUsername}: ${replyParent.text}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else if (comment.replyToCommentId != null && replyParent == null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Replying to a comment",
                    style = MaterialTheme.typography.labelSmall,
                    color = WhatsAppTeal
                )
            }

            Spacer(Modifier.height(6.dp))
            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = onReplyClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("reply_comment_${comment.id}")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Reply,
                        contentDescription = null,
                        tint = WhatsAppEmerald,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Reply",
                        style = MaterialTheme.typography.labelMedium,
                        color = WhatsAppEmerald,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (isMine) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("delete_comment_${comment.id}")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete comment",
                            tint = WhatsAppCallRed.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
