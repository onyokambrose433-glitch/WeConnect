package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.media.MediaUtils
import com.example.data.remote.extractFirestoreMessage
import com.example.data.repository.AuthProfileRepository
import com.example.ui.components.WeConnectAvatar
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WeConnectTheme
import com.example.ui.theme.WhatsAppCallRed
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppInkDark
import com.example.ui.theme.WhatsAppLightBg
import com.example.ui.theme.WhatsAppMutedGray
import com.example.ui.theme.WhatsAppNavIndicatorLight
import com.example.ui.theme.WhatsAppTeal
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()
    scope.launch {
        try {
            val cred = credentialManager.getCredential(context, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()
    scope.launch {
        try {
            val activityContext = context as? Activity
                ?: throw IllegalStateException("Activity context required for Google Sign-In")
            val cred = credentialManager.getCredential(activityContext, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type received")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed. Please try again.")
        }
    }
}

fun signOutWithCleanup(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    beforeSignOut: suspend () -> Unit = {},
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                beforeSignOut()
            } catch (_: Exception) {
            }
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}

@Composable
fun AuthPhotoBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    WeConnectTheme(darkTheme = false) {
        Box(modifier = modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.auth_launch_bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to PureWhite.copy(alpha = 0.04f),
                            0.42f to WhatsAppLightBg.copy(alpha = 0.18f),
                            0.68f to WhatsAppLightBg.copy(alpha = 0.72f),
                            1.0f to PureWhite.copy(alpha = 0.96f)
                        )
                    )
            )
            content()
        }
    }
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    AuthPhotoBackdrop {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            contentAlignment = Alignment.BottomCenter
        ) {
            val isWide = maxWidth >= 600.dp
            val heroSpacerHeight = (maxHeight * 0.24f).coerceIn(40.dp, 220.dp)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top floating brand & privacy pills over the clear photo hero
                Row(
                    modifier = Modifier
                        .widthIn(max = 520.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = PureWhite.copy(alpha = 0.93f),
                        shadowElevation = 4.dp,
                        border = BorderStroke(1.dp, WhatsAppTeal.copy(alpha = 0.12f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.weconnect_logo),
                                contentDescription = "WeConnect Logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "WeConnect",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = WhatsAppTeal
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = PureWhite.copy(alpha = 0.90f),
                        shadowElevation = 3.dp,
                        border = BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.30f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(WhatsAppGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Private & Live",
                                style = MaterialTheme.typography.labelMedium,
                                color = WhatsAppInkDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.heightIn(min = heroSpacerHeight))

                // Sleek Bottom Welcome & Sign-In Panel
                Surface(
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .padding(horizontal = if (isWide) 24.dp else 0.dp, vertical = if (isWide) 20.dp else 0.dp),
                    shape = if (isWide) RoundedCornerShape(28.dp) else RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    color = PureWhite.copy(alpha = 0.96f),
                    shadowElevation = 12.dp,
                    border = BorderStroke(1.dp, WhatsAppTeal.copy(alpha = 0.10f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .width(38.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(WhatsAppTeal.copy(alpha = 0.18f))
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = WhatsAppInkDark)) {
                                    append("Welcome to ")
                                }
                                withStyle(SpanStyle(color = WhatsAppTeal)) {
                                    append("WeConnect")
                                }
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Message friends privately, share moments & stories, and chat with Gemini AI — all in one place.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WhatsAppMutedGray,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            AuthFeatureRow(
                                icon = Icons.Default.Lock,
                                title = "End-to-End Encrypted",
                                subtitle = "Private 1-to-1 and group chats secured per device."
                            )
                            AuthFeatureRow(
                                icon = Icons.Default.Public,
                                title = "Feed, Stories & Communities",
                                subtitle = "Share posts, 24h status updates, and voice notes."
                            )
                            AuthFeatureRow(
                                icon = Icons.Default.AutoAwesome,
                                title = "Powered by Gemini AI",
                                subtitle = "Instant smart replies, translations, and visual Q&A."
                            )
                        }

                        AnimatedVisibility(visible = errorMessage != null) {
                            Column {
                                Spacer(modifier = Modifier.height(16.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                    border = BorderStroke(1.dp, WhatsAppCallRed.copy(alpha = 0.35f)),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(
                                        text = errorMessage.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        Button(
                            onClick = {
                                errorMessage = null
                                isLoading = true
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isLoading = false
                                        onAuthSuccess()
                                    },
                                    onAuthError = { msg ->
                                        isLoading = false
                                        errorMessage = msg
                                    },
                                    scope = scope,
                                    onAuthCancelled = {
                                        isLoading = false
                                        errorMessage = "Sign-in was dismissed. Select your Google account to create or access your WeConnect account."
                                    }
                                )
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WhatsAppTeal,
                                contentColor = PureWhite
                            ),
                            shape = RoundedCornerShape(28.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("google_sign_in_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = PureWhite,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Connecting securely…", fontWeight = FontWeight.SemiBold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = WhatsAppGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Continue with Google",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = PureWhite.copy(alpha = 0.9f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = WhatsAppGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Protected by Google Sign-In & Firebase security",
                                style = MaterialTheme.typography.bodySmall,
                                color = WhatsAppMutedGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(WhatsAppNavIndicatorLight)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = WhatsAppTeal,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = WhatsAppInkDark,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = WhatsAppMutedGray
            )
        }
    }
}

@Composable
fun OnboardingProfileScreen(
    initialDisplayName: String,
    repo: AuthProfileRepository,
    onProfileCreated: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val suggestedUsername = remember(initialDisplayName) {
        val norm = AuthProfileRepository.normalizeUsername(initialDisplayName).take(20)
        if (norm.length >= 3) norm else ""
    }

    var displayName by rememberSaveable { mutableStateOf(initialDisplayName.ifBlank { "" }) }
    var username by rememberSaveable { mutableStateOf(suggestedUsername) }
    var bio by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var visibility by rememberSaveable { mutableStateOf("PUBLIC") }
    var avatarBase64 by remember { mutableStateOf<String?>(null) }

    var checkingUsername by remember { mutableStateOf(false) }
    var usernameAvailable by remember { mutableStateOf<Boolean?>(null) }
    var usernameCheckError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val encoded = MediaUtils.compressUriToBase64(context, uri, maxDimension = 480, maxBase64Chars = 220_000)
                if (encoded != null) {
                    avatarBase64 = encoded
                } else {
                    errorMsg = "Selected image was too large. Try a smaller photo."
                }
            }
        }
    }

    LaunchedEffect(username) {
        val clean = AuthProfileRepository.normalizeUsername(username)
        usernameCheckError = null
        if (!AuthProfileRepository.isValidUsernameFormat(clean)) {
            checkingUsername = false
            usernameAvailable = null
            return@LaunchedEffect
        }
        checkingUsername = true
        delay(250L)
        val res = repo.isUsernameAvailable(clean)
        checkingUsername = false
        res.onSuccess { available ->
            usernameAvailable = available
            usernameCheckError = null
        }.onFailure { err ->
            usernameAvailable = null
            usernameCheckError = extractFirestoreMessage(err, "Unable to verify username right now")
        }
    }

    val cleanUsername = AuthProfileRepository.normalizeUsername(username)
    val isFormReady = !isSubmitting &&
        displayName.trim().isNotEmpty() &&
        AuthProfileRepository.isValidUsernameFormat(cleanUsername) &&
        usernameAvailable != false

    AuthPhotoBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = PureWhite.copy(alpha = 0.92f),
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.dp, WhatsAppTeal.copy(alpha = 0.14f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Set up your profile",
                            style = MaterialTheme.typography.headlineSmall,
                            color = WhatsAppTeal,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Provide your name, unique @username, and optional profile photo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = WhatsAppMutedGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = PureWhite.copy(alpha = 0.95f),
                    tonalElevation = 2.dp,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, WhatsAppTeal.copy(alpha = 0.14f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WeConnectAvatar(
                            displayName = displayName.ifBlank { "W" },
                            avatarBase64 = avatarBase64,
                            size = 86.dp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.testTag("onboarding_pick_photo")
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = WhatsAppTeal)
                            Spacer(Modifier.width(8.dp))
                            Text(if (avatarBase64 == null) "Add Profile Photo" else "Change Photo")
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it.take(50) },
                            label = { Text("Display Name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_display_name")
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = AuthProfileRepository.normalizeUsername(it).take(30) },
                            label = { Text("Unique Username") },
                            leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
                            trailingIcon = {
                                when {
                                    checkingUsername -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = WhatsAppTeal)
                                    usernameAvailable == true -> Icon(Icons.Default.CheckCircle, contentDescription = "Available", tint = WhatsAppGreen)
                                    usernameAvailable == false -> Icon(Icons.Default.ErrorOutline, contentDescription = "Taken", tint = WhatsAppCallRed)
                                }
                            },
                            isError = usernameAvailable == false,
                            supportingText = {
                                val msg = when {
                                    cleanUsername.length < 3 -> "3–30 lowercase letters, numbers, or underscores"
                                    checkingUsername -> "Checking @$cleanUsername…"
                                    usernameAvailable == true -> "@$cleanUsername is available"
                                    usernameAvailable == false -> "@$cleanUsername is already taken — choose another"
                                    usernameCheckError != null -> "Check notice: $usernameCheckError"
                                    else -> "Tap Create Account to claim @$cleanUsername"
                                }
                                Text(msg)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_username")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it.take(200) },
                            label = { Text("About / Bio (optional)") },
                            minLines = 2,
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_bio")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it.take(30) },
                            label = { Text("Phone Number (optional, private)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_phone")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Account:",
                                style = MaterialTheme.typography.labelLarge
                            )
                            FilterChip(
                                selected = visibility == "PUBLIC",
                                onClick = { visibility = "PUBLIC" },
                                label = { Text("Public") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WhatsAppTeal,
                                    selectedLabelColor = PureWhite
                                )
                            )
                            FilterChip(
                                selected = visibility == "PRIVATE",
                                onClick = { visibility = "PRIVATE" },
                                label = { Text("Private") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WhatsAppTeal,
                                    selectedLabelColor = PureWhite
                                )
                            )
                        }

                        if (errorMsg != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                border = BorderStroke(1.dp, WhatsAppCallRed.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = errorMsg!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        Button(
                            onClick = {
                                errorMsg = null
                                isSubmitting = true
                                scope.launch {
                                    val res = repo.createInitialProfile(
                                        displayName = displayName,
                                        rawUsername = cleanUsername,
                                        bio = bio,
                                        avatarBase64 = avatarBase64,
                                        accountVisibility = visibility,
                                        phone = phone.ifBlank { null }
                                    )
                                    isSubmitting = false
                                    res.onSuccess { onProfileCreated() }
                                        .onFailure { err ->
                                            errorMsg = extractFirestoreMessage(err, "Failed to create profile. Please try again.")
                                        }
                                }
                            },
                            enabled = isFormReady,
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppTeal, contentColor = PureWhite),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("onboarding_create_account_button")
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(Modifier.size(20.dp), color = PureWhite, strokeWidth = 2.dp)
                                Spacer(Modifier.width(10.dp))
                                Text("Creating account…")
                            } else {
                                Text("Create WeConnect Account", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onSignOut,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Switch Google Account")
                        }
                    }
                }
            }
        }
    }
}
