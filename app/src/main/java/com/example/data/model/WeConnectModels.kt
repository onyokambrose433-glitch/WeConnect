package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class PublicProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val avatarBase64: String? = null,
    val publicKeyBase64: String? = null,
    val accountVisibility: String = "PUBLIC",
    val onlineStatusVisible: Boolean = true,
    val online: Boolean = true,
    val discoverable: Boolean = true,
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val postsCount: Long = 0L,
    val lastSeenAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PrivateUser(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String? = null,
    val phone: String? = null,
    val messagePermission: String = "EVERYONE",
    val lastSeenVisible: Boolean = true,
    val readReceiptsEnabled: Boolean = true,
    val storyVisibility: String = "PUBLIC",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DeviceRecord(
    @DocumentId val id: String = "",
    val deviceId: String = "",
    val deviceName: String = "",
    val platform: String = "Android",
    val publicKeyBase64: String = "",
    val active: Boolean = true,
    @ServerTimestamp val lastActiveAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BlockedUser(
    @DocumentId val id: String = "",
    val blockedUid: String = "",
    val blockedUsername: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val recipientUid: String = "",
    val actorUid: String = "",
    val actorName: String = "",
    val type: String = "FOLLOW",
    val targetId: String = "",
    val summary: String = "",
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val type: String = "DIRECT",
    val title: String = "",
    val description: String = "",
    val avatarBase64: String? = null,
    val memberUids: List<String> = emptyList(),
    val adminUids: List<String> = emptyList(),
    val memberNames: Map<String, String> = emptyMap(),
    val memberKeys: Map<String, String> = emptyMap(),
    val archivedBy: List<String> = emptyList(),
    val mutedBy: List<String> = emptyList(),
    val disappearingHours: Long = 0L,
    val inviteCode: String? = null,
    val pinnedMessageId: String? = null,
    val pinnedMessagePreview: String? = null,
    val lastMessageText: String? = null,
    val lastMessageSenderId: String? = null,
    val lastMessageAt: Timestamp? = null,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val memberUids: List<String> = emptyList(),
    val messageType: String = "TEXT",
    val ciphertext: String = "",
    val iv: String? = null,
    val encrypted: Boolean = false,
    val senderPublicKey: String? = null,
    val mediaBase64: String? = null,
    val mediaMimeType: String? = null,
    val fileName: String? = null,
    val replyToMessageId: String? = null,
    val replyToPreview: String? = null,
    val forwarded: Boolean = false,
    val edited: Boolean = false,
    val deletedForAll: Boolean = false,
    val reactions: Map<String, String> = emptyMap(),
    val readBy: List<String> = emptyList(),
    val deliveredTo: List<String> = emptyList(),
    val expiresAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TypingStatus(
    @DocumentId val id: String = "",
    val uid: String = "",
    val displayName: String = "",
    val memberUids: List<String> = emptyList(),
    val typing: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SocialPost(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorName: String = "",
    val authorAvatarBase64: String? = null,
    val caption: String = "",
    val mediaBase64List: List<String> = emptyList(),
    val hashtags: List<String> = emptyList(),
    val visibility: String = "PUBLIC",
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val sharesCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PostComment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val text: String = "",
    val replyToCommentId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StoryItem(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val authorAvatarBase64: String? = null,
    val mediaType: String = "TEXT",
    val caption: String = "",
    val mediaBase64: String? = null,
    val bgColorHex: String = "#075E54",
    val visibility: String = "PUBLIC",
    val viewsCount: Long = 0L,
    val expiresAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StoryView(
    @DocumentId val id: String = "",
    val storyId: String = "",
    val viewerUid: String = "",
    val viewerName: String = "",
    val reaction: String? = null,
    @ServerTimestamp val viewedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Community(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val kind: String = "COMMUNITY",
    val visibility: String = "PUBLIC",
    val rulesText: String = "",
    val ownerId: String = "",
    val adminUids: List<String> = emptyList(),
    val memberCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CommunityMember(
    @DocumentId val id: String = "",
    val communityId: String = "",
    val uid: String = "",
    val displayName: String = "",
    val username: String = "",
    val role: String = "MEMBER",
    @ServerTimestamp val joinedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CommunityPost(
    @DocumentId val id: String = "",
    val communityId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val title: String = "",
    val body: String = "",
    val pinned: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CallRecord(
    @DocumentId val id: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val calleeId: String = "",
    val calleeName: String = "",
    val participantUids: List<String> = emptyList(),
    val callType: String = "VOICE",
    val status: String = "RINGING",
    val offerSdp: String? = null,
    val answerSdp: String? = null,
    @ServerTimestamp val startedAt: Timestamp? = null,
    val endedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
