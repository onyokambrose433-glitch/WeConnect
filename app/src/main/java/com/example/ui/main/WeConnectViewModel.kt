package com.example.ui.main

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
import com.example.data.remote.extractFirestoreMessage
import com.example.data.repository.AuthProfileRepository
import com.example.data.repository.CommunityCallRepository
import com.example.data.repository.MessagingRepository
import com.example.data.repository.SocialRepository
import kotlinx.coroutines.CancellationException
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
import kotlinx.coroutines.launch

class WeConnectViewModel(
    val currentUserId: String,
    val authRepo: AuthProfileRepository,
    val messagingRepo: MessagingRepository,
    val socialRepo: SocialRepository,
    val communityCallRepo: CommunityCallRepository,
    val crypto: WeConnectCrypto
) : ViewModel() {

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

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

    val blockedUsers: StateFlow<List<BlockedUser>> = authRepo.observeBlockedUsers(currentUserId)
        .catch { error -> if (error is CancellationException) throw error }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val devices: StateFlow<List<DeviceRecord>> = authRepo.observeDevices(currentUserId)
        .catch { error -> if (error is CancellationException) throw error }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notifications: StateFlow<List<AppNotification>> = authRepo.observeNotifications(currentUserId)
        .catch { error -> if (error is CancellationException) throw error }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

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
    }

    fun syncCurrentDevice() {
        viewModelScope.launch {
            if (_deviceIdentity.value == null) {
                _deviceIdentity.value = crypto.getOrCreateDeviceIdentity(currentUserId)
            }
            authRepo.syncCurrentDeviceKey()
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
}
