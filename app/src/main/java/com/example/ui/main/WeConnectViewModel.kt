package com.example.ui.main

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.crypto.DeviceIdentity
import com.example.data.crypto.WeConnectCrypto
import com.example.data.model.AppNotification
import com.example.data.model.BlockedUser
import com.example.data.model.CallRecord
import com.example.data.model.Community
import com.example.data.model.Conversation
import com.example.data.model.DeviceRecord
import com.example.data.model.PrivateUser
import com.example.data.model.PublicProfile
import com.example.data.model.SocialPost
import com.example.data.model.StoryItem
import com.example.data.model.UiState
import com.example.data.remote.NetworkMonitor
import com.example.data.remote.extractFirestoreMessage
import com.example.data.repository.AuthProfileRepository
import com.example.data.repository.CommunityCallRepository
import com.example.data.repository.MessagingRepository
import com.example.data.repository.SocialRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

data class PendingOutgoingMessage(
    val id: String = UUID.randomUUID().toString(),
    val chatId: String,
    val conversation: Conversation,
    val senderProfile: PublicProfile,
    val plaintext: String,
    val messageType: String = "TEXT",
    val mediaBase64: String? = null,
    val mediaMimeType: String? = null,
    val fileName: String? = null,
    val replyToMessageId: String? = null,
    val replyToPreview: String? = null,
    val forwarded: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis()
)

class WeConnectViewModel(
    val currentUserId: String,
    val authRepo: AuthProfileRepository,
    val messagingRepo: MessagingRepository,
    val socialRepo: SocialRepository,
    val communityCallRepo: CommunityCallRepository,
    val crypto: WeConnectCrypto,
    val networkMonitor: NetworkMonitor,
    private val notifPrefs: SharedPreferences? = null
) : ViewModel() {

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    val isOnlineNetwork: StateFlow<Boolean> = networkMonitor.isOnline

    private val _nowMsTicker = MutableStateFlow(System.currentTimeMillis())
    val nowMsTicker: StateFlow<Long> = _nowMsTicker.asStateFlow()

    private val _pendingMessages = MutableStateFlow<List<PendingOutgoingMessage>>(emptyList())
    val pendingMessages: StateFlow<List<PendingOutgoingMessage>> = _pendingMessages.asStateFlow()

    private val _deviceIdentity = MutableStateFlow<DeviceIdentity?>(null)
    val deviceIdentity: StateFlow<DeviceIdentity?> = _deviceIdentity.asStateFlow()

    val myProfileState: StateFlow<UiState<PublicProfile?>> = authRepo.observeProfile(currentUserId)
        .map<PublicProfile?, UiState<PublicProfile?>> { UiState.Success(it) }
        .catch { error ->
            if (error is CancellationException && extractFirestoreMessage(error, "").isBlank()) throw error
            emit(UiState.Error(extractFirestoreMessage(error, "Failed to load profile")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val privateUserState: StateFlow<PrivateUser?> = authRepo.observePrivateUser(currentUserId)
        .catch { error -> if (error is CancellationException) throw error }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val allProfilesState: StateFlow<UiState<List<PublicProfile>>> = authRepo.observeDiscoverableProfiles()
        .map<List<PublicProfile>, UiState<List<PublicProfile>>> { UiState.Success(it) }
        .catch { error ->
            emit(UiState.Error(extractFirestoreMessage(error, "Failed to load users")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val followingIds: StateFlow<Set<String>> = authRepo.observeFollowingIds(currentUserId)
        .catch { error -> if (error is CancellationException) throw error }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val followerIds: StateFlow<Set<String>> = authRepo.observeFollowerIds(currentUserId)
        .catch { error -> if (error is CancellationException) throw error }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val blockedUsers: StateFlow<List<BlockedUser>> = authRepo.observeBlockedUsers(currentUserId)
        .catch { error -> if (error is CancellationException) throw error }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val devices: StateFlow<List<DeviceRecord>> = authRepo.observeDevices(currentUserId)
        .catch { error -> if (error is CancellationException) throw error }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notifications: StateFlow<List<AppNotification>> = authRepo.observeNotifications(currentUserId)
        .catch { error -> if (error is CancellationException) throw error }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _welcomedNewcomerIds = MutableStateFlow(
        notifPrefs?.getStringSet("welcomed_newcomers", emptySet())?.toSet() ?: emptySet()
    )
    val welcomedNewcomerIds: StateFlow<Set<String>> = _welcomedNewcomerIds.asStateFlow()

    val recentNewcomers: StateFlow<List<PublicProfile>> = combine(
        allProfilesState,
        blockedUsers,
        _nowMsTicker
    ) { profilesUi, blocked, nowMs ->
        val profiles = (profilesUi as? UiState.Success)?.data ?: emptyList()
        val blockedSet = blocked.map { it.blockedUid }.toSet()
        val cutoffSec = (nowMs / 1000L) - 14L * 24L * 3600L
        profiles.filter { p ->
            p.uid != currentUserId &&
                p.uid !in blockedSet &&
                ((p.createdAt?.seconds ?: 0L) >= cutoffSec)
        }.sortedByDescending { it.createdAt?.seconds ?: 0L }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val unwelcomedNewcomerIds: StateFlow<Set<String>> = combine(
        recentNewcomers,
        _welcomedNewcomerIds,
        followingIds
    ) { newcomers, welcomed, following ->
        newcomers.map { it.uid }
            .filter { it !in welcomed && it !in following }
            .toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val unreadNotificationsCount: StateFlow<Int> = combine(
        notifications,
        unwelcomedNewcomerIds
    ) { notifs, newcomerIds ->
        notifs.count { !it.read } + newcomerIds.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    fun markNewcomerWelcomed(uid: String) {
        if (uid.isBlank()) return
        _welcomedNewcomerIds.update { current ->
            val updated = current + uid
            notifPrefs?.edit()?.putStringSet("welcomed_newcomers", updated)?.apply()
            updated
        }
    }

    fun markAllNewcomersWelcomed() {
        val ids = recentNewcomers.value.map { it.uid }.toSet()
        if (ids.isEmpty()) return
        _welcomedNewcomerIds.update { current ->
            val updated = current + ids
            notifPrefs?.edit()?.putStringSet("welcomed_newcomers", updated)?.apply()
            updated
        }
    }

    val conversationsState: StateFlow<UiState<List<Conversation>>> = messagingRepo.observeConversations(currentUserId)
        .map<List<Conversation>, UiState<List<Conversation>>> { UiState.Success(it) }
        .catch { error ->
            emit(UiState.Error(extractFirestoreMessage(error, "Failed to load conversations")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _readChatTimestamps = MutableStateFlow<Map<String, Long>>(emptyMap())
    val readChatTimestamps: StateFlow<Map<String, Long>> = _readChatTimestamps.asStateFlow()

    fun markChatAsRead(chatId: String, ts: Long = System.currentTimeMillis()) {
        _readChatTimestamps.update { it + (chatId to ts) }
    }

    val unreadChatsCount: StateFlow<Int> = combine(conversationsState, _readChatTimestamps) { state, readMap ->
        if (state is UiState.Success) {
            state.data.count { conv ->
                val sender = conv.lastMessageSenderId
                val lastMsgTs = (conv.lastMessageAt?.seconds ?: 0L) * 1000L
                val lastRead = readMap[conv.id] ?: 0L
                sender != null && sender != currentUserId && lastMsgTs > lastRead
            }
        } else 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    val publicPostsState: StateFlow<UiState<List<SocialPost>>> = socialRepo.observePublicPosts()
        .map<List<SocialPost>, UiState<List<SocialPost>>> { UiState.Success(it) }
        .catch { error ->
            emit(UiState.Error(extractFirestoreMessage(error, "Failed to load feed")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val savedPostIds: StateFlow<Set<String>> = socialRepo.observeSavedPostIds(currentUserId)
        .catch { error -> if (error is CancellationException) throw error }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val storiesState: StateFlow<UiState<List<StoryItem>>> = socialRepo.observePublicStories()
        .map<List<StoryItem>, UiState<List<StoryItem>>> { UiState.Success(it) }
        .catch { error ->
            emit(UiState.Error(extractFirestoreMessage(error, "Failed to load stories")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val communitiesState: StateFlow<UiState<List<Community>>> = communityCallRepo.observePublicCommunities()
        .map<List<Community>, UiState<List<Community>>> { UiState.Success(it) }
        .catch { error ->
            emit(UiState.Error(extractFirestoreMessage(error, "Failed to load communities")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val callsState: StateFlow<UiState<List<CallRecord>>> = communityCallRepo.observeMyCalls(currentUserId)
        .map<List<CallRecord>, UiState<List<CallRecord>>> { UiState.Success(it) }
        .catch { error ->
            emit(UiState.Error(extractFirestoreMessage(error, "Failed to load calls")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    init {
        viewModelScope.launch {
            _deviceIdentity.value = crypto.getOrCreateDeviceIdentity(currentUserId)
        }
        // Periodic ticker to keep peer online/offline staleness UI accurate
        viewModelScope.launch {
            while (isActive) {
                delay(25_000L)
                _nowMsTicker.value = System.currentTimeMillis()
            }
        }
        // Automatically flush queued offline messages & restore online presence on reconnect
        viewModelScope.launch {
            networkMonitor.isOnline
                .collect { online ->
                    if (online) {
                        authRepo.setOnlineStatus(true)
                        flushPendingMessages()
                    }
                }
        }
    }

    suspend fun sendMessageOrQueueOffline(
        conversation: Conversation,
        senderProfile: PublicProfile,
        plaintext: String,
        messageType: String = "TEXT",
        mediaBase64: String? = null,
        mediaMimeType: String? = null,
        fileName: String? = null,
        replyToMessageId: String? = null,
        replyToPreview: String? = null,
        forwarded: Boolean = false
    ) {
        val cleanText = plaintext.trim()
        if (cleanText.isEmpty() && mediaBase64 == null) return

        val onlineNow = networkMonitor.checkCurrentlyConnected() && networkMonitor.isOnline.value
        if (!onlineNow) {
            val queued = PendingOutgoingMessage(
                chatId = conversation.id,
                conversation = conversation,
                senderProfile = senderProfile,
                plaintext = cleanText,
                messageType = messageType,
                mediaBase64 = mediaBase64,
                mediaMimeType = mediaMimeType,
                fileName = fileName,
                replyToMessageId = replyToMessageId,
                replyToPreview = replyToPreview,
                forwarded = forwarded
            )
            _pendingMessages.update { it + queued }
            emitNotice("You are offline · Message not sent yet (waiting for network)")
            return
        }

        handleResult(
            messagingRepo.sendMessage(
                conversation = conversation,
                senderProfile = senderProfile,
                plaintext = cleanText,
                messageType = messageType,
                mediaBase64 = mediaBase64,
                mediaMimeType = mediaMimeType,
                fileName = fileName,
                replyToMessageId = replyToMessageId,
                replyToPreview = replyToPreview,
                forwarded = forwarded
            )
        )
    }

    fun cancelPendingMessage(pendingId: String) {
        _pendingMessages.update { list -> list.filterNot { it.id == pendingId } }
        emitNotice("Unsent message cancelled")
    }

    private suspend fun flushPendingMessages() {
        val queue = _pendingMessages.value.toList()
        if (queue.isEmpty()) return
        var sentCount = 0
        for (item in queue) {
            if (!networkMonitor.checkCurrentlyConnected()) break
            val res = messagingRepo.sendMessage(
                conversation = item.conversation,
                senderProfile = item.senderProfile,
                plaintext = item.plaintext,
                messageType = item.messageType,
                mediaBase64 = item.mediaBase64,
                mediaMimeType = item.mediaMimeType,
                fileName = item.fileName,
                replyToMessageId = item.replyToMessageId,
                replyToPreview = item.replyToPreview,
                forwarded = item.forwarded
            )
            if (res.isSuccess) {
                _pendingMessages.update { list -> list.filterNot { it.id == item.id } }
                sentCount++
            }
        }
        if (sentCount > 0) {
            emitNotice("Back online · Sent $sentCount waiting message${if (sentCount > 1) "s" else ""}")
        }
    }

    fun syncCurrentDevice() {
        viewModelScope.launch {
            if (_deviceIdentity.value == null) {
                _deviceIdentity.value = crypto.getOrCreateDeviceIdentity(currentUserId)
            }
            if (networkMonitor.checkCurrentlyConnected()) {
                authRepo.syncCurrentDeviceKey()
            }
        }
    }

    fun emitNotice(text: String) {
        _messages.tryEmit(text)
    }

    fun <T> handleResult(result: Result<T>, successMessage: String? = null, onSuccess: (T) -> Unit = {}) {
        result.onSuccess {
            if (!successMessage.isNullOrBlank()) emitNotice(successMessage)
            onSuccess(it)
        }.onFailure { err ->
            emitNotice(err.localizedMessage ?: "Operation failed")
        }
    }

    override fun onCleared() {
        super.onCleared()
        networkMonitor.unregister()
    }
}
