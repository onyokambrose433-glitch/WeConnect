package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import com.example.ui.theme.WhatsAppCallRed
import com.example.ui.theme.WhatsAppEmerald
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppOfflineGray
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.crypto.WeConnectCrypto
import com.example.data.model.Community
import com.example.data.model.Conversation
import com.example.data.model.PublicProfile
import com.example.data.model.UiState
import com.example.data.notification.WeConnectNotifier
import com.example.data.remote.NetworkMonitor
import com.example.data.remote.configureAppCheck
import com.example.data.repository.AuthProfileRepository
import com.example.data.repository.CommunityCallRepository
import com.example.data.repository.MessagingRepository
import com.example.data.repository.SocialRepository
import com.example.ui.auth.AuthPhotoBackdrop
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.OnboardingProfileScreen
import com.example.ui.auth.signOutWithCleanup
import com.example.ui.calls.ActiveCallDialog
import com.example.ui.calls.CallsScreen
import com.example.ui.chats.ChatThreadScreen
import com.example.ui.chats.ChatsListScreen
import com.example.ui.communities.CommunitiesScreen
import com.example.ui.communities.CommunityDetailScreen
import com.example.ui.components.GeminiAssistantDialog
import com.example.ui.components.NotificationsCenterSheet
import com.example.ui.components.WeConnectLogoMark
import com.example.ui.home.HomeScreen
import com.example.ui.main.AiViewModel
import com.example.ui.main.WeConnectViewModel
import com.example.ui.profile.GlobalSearchDialog
import com.example.ui.profile.ProfileScreen
import com.example.ui.profile.UserPublicProfileDialog
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WeConnectTheme
import com.example.ui.theme.WhatsAppTeal
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure Firebase App Check with launch intent before any UI, ViewModels, or Firebase calls
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            WeConnectTheme {
                AppNavigation()
            }
        }
    }
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

private enum class MainTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    CHATS("Chats", Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat, "nav_chats"),
    COMMUNITIES("Community", Icons.Filled.Groups, Icons.Outlined.Groups, "nav_communities"),
    CALLS("Calls", Icons.Filled.Call, Icons.Outlined.Call, "nav_calls"),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedRoot(
                currentUserId = user.uid,
                initialDisplayName = user.displayName ?: ""
            )
        }
    }
}

@Composable
private fun AuthenticatedRoot(
    currentUserId: String,
    initialDisplayName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: WeConnectViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val crypto = WeConnectCrypto(app)
                val netMonitor = NetworkMonitor(app)
                WeConnectViewModel(
                    currentUserId = currentUserId,
                    authRepo = AuthProfileRepository(db, crypto),
                    messagingRepo = MessagingRepository(
                        db = db,
                        crypto = crypto,
                        isNetworkOnline = { netMonitor.isOnline.value && netMonitor.checkCurrentlyConnected() }
                    ),
                    socialRepo = SocialRepository(db),
                    communityCallRepo = CommunityCallRepository(db),
                    crypto = crypto,
                    networkMonitor = netMonitor,
                    notifPrefs = app.getSharedPreferences(
                        "weconnect_notif_prefs_$currentUserId",
                        android.content.Context.MODE_PRIVATE
                    )
                )
            }
        }
    )

    val myProfileState by viewModel.myProfileState.collectAsStateWithLifecycle()

    val performSignOut: () -> Unit = {
        signOutWithCleanup(
            credentialManager = credentialManager,
            scope = scope,
            beforeSignOut = { viewModel.authRepo.setOnlineStatus(false) }
        )
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    var lastNotifId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(notifications) {
        val latest = notifications.firstOrNull { !it.read }
        if (latest != null && latest.id != lastNotifId) {
            lastNotifId = latest.id
            WeConnectNotifier.showNotification(
                context = context,
                title = latest.actorName.ifBlank { "WeConnect" },
                message = latest.summary,
                notificationId = latest.id.hashCode()
            )
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, currentUserId) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    scope.launch {
                        if (viewModel.networkMonitor.checkCurrentlyConnected()) {
                            viewModel.authRepo.setOnlineStatus(true)
                        }
                    }
                }
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    scope.launch { viewModel.authRepo.setOnlineStatus(false) }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        scope.launch {
            if (viewModel.networkMonitor.checkCurrentlyConnected()) {
                viewModel.authRepo.setOnlineStatus(true)
            }
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            scope.launch { viewModel.authRepo.setOnlineStatus(false) }
        }
    }

    LaunchedEffect(currentUserId) {
        while (isActive) {
            delay(45_000)
            if (viewModel.networkMonitor.checkCurrentlyConnected()) {
                viewModel.authRepo.setOnlineStatus(true)
            }
        }
    }

    when (val state = myProfileState) {
        is UiState.Loading -> {
            AuthPhotoBackdrop {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                        color = PureWhite.copy(alpha = 0.92f),
                        shadowElevation = 6.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            WeConnectLogoMark(size = 72.dp)
                            Spacer(Modifier.height(16.dp))
                            CircularProgressIndicator(color = WhatsAppTeal)
                        }
                    }
                }
            }
        }
        is UiState.Error -> {
            AuthPhotoBackdrop {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                        color = PureWhite.copy(alpha = 0.94f),
                        tonalElevation = 2.dp,
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            WeConnectLogoMark(size = 64.dp)
                            Spacer(Modifier.height(12.dp))
                            Text("Connection Issue", style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.height(6.dp))
                            Text(state.message, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(18.dp))
                            OutlinedButton(onClick = performSignOut) { Text("Sign Out") }
                        }
                    }
                }
            }
        }
        is UiState.Success -> {
            val myProfile = state.data
            if (myProfile == null) {
                OnboardingProfileScreen(
                    initialDisplayName = initialDisplayName,
                    repo = viewModel.authRepo,
                    onProfileCreated = {},
                    onSignOut = performSignOut
                )
            } else {
                WeConnectShell(
                    viewModel = viewModel,
                    myProfile = myProfile,
                    onSignOut = performSignOut
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeConnectShell(
    viewModel: WeConnectViewModel,
    myProfile: PublicProfile,
    onSignOut: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val aiViewModel: AiViewModel = viewModel()

    var activeTab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    var openChatId by rememberSaveable { mutableStateOf<String?>(null) }
    var openCommunityId by rememberSaveable { mutableStateOf<String?>(null) }
    var inspectUserUid by rememberSaveable { mutableStateOf<String?>(null) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showGeminiDialog by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }

    val unreadChatsCount by viewModel.unreadChatsCount.collectAsStateWithLifecycle()
    val unreadNotifCount by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle()
    val notificationsList by viewModel.notifications.collectAsStateWithLifecycle()
    val recentNewcomers by viewModel.recentNewcomers.collectAsStateWithLifecycle()
    val conversationsState by viewModel.conversationsState.collectAsStateWithLifecycle()

    LaunchedEffect(openChatId) {
        openChatId?.let { viewModel.markChatAsRead(it) }
    }

    var knownLastMessages by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    LaunchedEffect(conversationsState) {
        val state = conversationsState
        if (state is UiState.Success<List<Conversation>>) {
            val list = state.data
            list.forEach { conv ->
                val prevMsg = knownLastMessages[conv.id]
                val currentMsg = conv.lastMessageText
                if (prevMsg != null && currentMsg != null && prevMsg != currentMsg &&
                    conv.lastMessageSenderId != null && conv.lastMessageSenderId != myProfile.uid &&
                    openChatId != conv.id
                ) {
                    val senderName = conv.memberNames[conv.lastMessageSenderId] ?: "Someone"
                    scope.launch {
                        snackbarHostState.showSnackbar("💬 $senderName: $currentMsg")
                    }
                }
            }
            knownLastMessages = list.filter { it.lastMessageText != null }
                .associate { it.id to (it.lastMessageText ?: "") }
        }
    }

    val communitiesState by viewModel.communitiesState.collectAsStateWithLifecycle()
    val openCommunity: Community? = remember(openCommunityId, communitiesState) {
        val list = (communitiesState as? UiState.Success)?.data ?: emptyList()
        list.firstOrNull { it.id == openCommunityId }
    }

    val callsState by viewModel.callsState.collectAsStateWithLifecycle()
    val activeCall = remember(callsState) {
        val calls = (callsState as? UiState.Success)?.data ?: emptyList()
        calls.firstOrNull { it.status == "RINGING" || it.status == "CONNECTED" }
    }

    LaunchedEffect(myProfile.uid) {
        viewModel.syncCurrentDevice()
        viewModel.authRepo.ensureWelcomeNotification(myProfile.displayName)
    }

    var knownNotifIds by remember { mutableStateOf<Set<String>?>(null) }
    LaunchedEffect(notificationsList) {
        val currentIds = notificationsList.map { it.id }.toSet()
        val prev = knownNotifIds
        if (prev == null) {
            knownNotifIds = currentIds
        } else {
            val newUnread = notificationsList.firstOrNull { !it.read && it.id !in prev }
            knownNotifIds = currentIds
            if (newUnread != null) {
                snackbarHostState.showSnackbar("🔔 ${newUnread.summary}")
            }
        }
    }

    var knownNewcomerIds by remember { mutableStateOf<Set<String>?>(null) }
    LaunchedEffect(recentNewcomers) {
        val currentIds = recentNewcomers.map { it.uid }.toSet()
        val prev = knownNewcomerIds
        if (prev == null) {
            knownNewcomerIds = currentIds
        } else {
            val joined = recentNewcomers.firstOrNull { it.uid !in prev }
            knownNewcomerIds = currentIds
            if (joined != null) {
                snackbarHostState.showSnackbar("🎉 ${joined.displayName} (@${joined.username}) joined WeConnect! Tap 🔔 to welcome them.")
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val startCallWithPeer: (PublicProfile, String) -> Unit = { peer, callType ->
        scope.launch {
            viewModel.handleResult(viewModel.communityCallRepo.startCall(myProfile, peer, callType))
        }
    }

    val openDirectChatWithPeer: (PublicProfile) -> Unit = { peer ->
        scope.launch {
            viewModel.handleResult(viewModel.messagingRepo.createOrOpenDirectChat(myProfile, peer)) { chatId ->
                activeTab = MainTab.CHATS
                openChatId = chatId
            }
        }
    }

    // Secondary Full-Screen Destinations with BackHandler
    if (openChatId != null) {
        BackHandler { openChatId = null }
        ChatThreadScreen(
            chatId = openChatId!!,
            viewModel = viewModel,
            aiViewModel = aiViewModel,
            myProfile = myProfile,
            onBack = { openChatId = null },
            onStartCall = startCallWithPeer
        )
    } else if (openCommunity != null) {
        BackHandler { openCommunityId = null }
        CommunityDetailScreen(
            community = openCommunity,
            viewModel = viewModel,
            myProfile = myProfile,
            onBack = { openCommunityId = null },
            onOpenUserProfile = { inspectUserUid = it },
            snackbarHostState = snackbarHostState
        )
    } else {
        if (activeTab != MainTab.HOME) {
            BackHandler { activeTab = MainTab.HOME }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= 600.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                WeConnectLogoMark(size = 34.dp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "WeConnect",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = activeTab.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { showNotificationsSheet = true },
                                modifier = Modifier.testTag("top_notifications_button")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (unreadNotifCount > 0) {
                                            Badge(
                                                containerColor = WhatsAppCallRed,
                                                contentColor = PureWhite
                                            ) {
                                                Text(if (unreadNotifCount > 99) "99+" else "$unreadNotifCount")
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (unreadNotifCount > 0) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                        contentDescription = "Notifications",
                                        tint = if (unreadNotifCount > 0) WhatsAppEmerald else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            IconButton(
                                onClick = { showSearchDialog = true },
                                modifier = Modifier.testTag("global_search_button")
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Global Search", tint = MaterialTheme.colorScheme.onSurface)
                            }
                            IconButton(
                                onClick = { showGeminiDialog = true },
                                modifier = Modifier.testTag("open_gemini_button")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "WeConnect AI (Gemini)", tint = WhatsAppTeal)
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!isWideScreen) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            MainTab.entries.forEach { tab ->
                                val selected = activeTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { activeTab = tab },
                                    icon = {
                                        if (tab == MainTab.CHATS && unreadChatsCount > 0) {
                                            BadgedBox(
                                                badge = {
                                                    Badge(
                                                        containerColor = WhatsAppGreen,
                                                        contentColor = PureWhite
                                                    ) {
                                                        Text("$unreadChatsCount")
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = tab.label
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        }
                                    },
                                    label = { Text(tab.label, maxLines = 1, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    modifier = Modifier.testTag(tab.tag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isWideScreen) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            MainTab.entries.forEach { tab ->
                                val selected = activeTab == tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { activeTab = tab },
                                    icon = {
                                        if (tab == MainTab.CHATS && unreadChatsCount > 0) {
                                            BadgedBox(
                                                badge = {
                                                    Badge(
                                                        containerColor = WhatsAppGreen,
                                                        contentColor = PureWhite
                                                    ) {
                                                        Text("$unreadChatsCount")
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = tab.label
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        }
                                    },
                                    label = { Text(tab.label) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    modifier = Modifier.testTag(tab.tag)
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (activeTab) {
                            MainTab.HOME -> HomeScreen(
                                viewModel = viewModel,
                                myProfile = myProfile,
                                onOpenUserProfile = { inspectUserUid = it },
                                onMessageUser = openDirectChatWithPeer
                            )
                            MainTab.CHATS -> ChatsListScreen(
                                viewModel = viewModel,
                                myProfile = myProfile,
                                onOpenChat = { openChatId = it },
                                onOpenGemini = { showGeminiDialog = true }
                            )
                            MainTab.COMMUNITIES -> CommunitiesScreen(
                                viewModel = viewModel,
                                myProfile = myProfile,
                                onOpenCommunity = { openCommunityId = it.id }
                            )
                            MainTab.CALLS -> CallsScreen(
                                viewModel = viewModel,
                                myProfile = myProfile,
                                onStartCall = startCallWithPeer
                            )
                            MainTab.PROFILE -> ProfileScreen(
                                viewModel = viewModel,
                                myProfile = myProfile,
                                onSignOut = onSignOut,
                                onMessageUser = openDirectChatWithPeer,
                                onOpenNotifications = { showNotificationsSheet = true }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSearchDialog) {
        GlobalSearchDialog(
            viewModel = viewModel,
            myUid = myProfile.uid,
            onDismiss = { showSearchDialog = false },
            onSelectUser = { uid -> inspectUserUid = uid },
            onSelectCommunity = { comm ->
                activeTab = MainTab.COMMUNITIES
                openCommunityId = comm.id
            }
        )
    }

    if (showGeminiDialog) {
        GeminiAssistantDialog(
            aiViewModel = aiViewModel,
            onDismiss = { showGeminiDialog = false }
        )
    }

    inspectUserUid?.let { targetUid ->
        UserPublicProfileDialog(
            targetUid = targetUid,
            myProfile = myProfile,
            viewModel = viewModel,
            onDismiss = { inspectUserUid = null },
            onMessageUser = openDirectChatWithPeer,
            onCallUser = startCallWithPeer
        )
    }

    if (showNotificationsSheet) {
        NotificationsCenterSheet(
            viewModel = viewModel,
            myProfile = myProfile,
            onDismiss = { showNotificationsSheet = false },
            onOpenUserProfile = { uid -> inspectUserUid = uid },
            onOpenChat = { chatId ->
                activeTab = MainTab.CHATS
                openChatId = chatId
            },
            onOpenCommunity = { commId ->
                activeTab = MainTab.COMMUNITIES
                openCommunityId = commId
            },
            onOpenHome = { activeTab = MainTab.HOME },
            onOpenCalls = { activeTab = MainTab.CALLS },
            onMessageUser = openDirectChatWithPeer
        )
    }

    activeCall?.let { call ->
        ActiveCallDialog(
            call = call,
            myUid = myProfile.uid,
            viewModel = viewModel,
            onDismiss = {}
        )
    }
}
