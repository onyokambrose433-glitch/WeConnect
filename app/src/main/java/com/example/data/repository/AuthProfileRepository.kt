package com.example.data.repository

import android.os.Build
import com.example.data.crypto.WeConnectCrypto
import com.example.data.model.AppNotification
import com.example.data.model.BlockedUser
import com.example.data.model.DeviceRecord
import com.example.data.model.PrivateUser
import com.example.data.model.PublicProfile
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AuthProfileRepository(
    private val db: FirebaseFirestore,
    private val crypto: WeConnectCrypto,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeProfile(uid: String): Flow<PublicProfile?> = flow {
        val currentUid = requireUserId()
        val docRef = db.collection("public_profiles").document(uid)
        emitAll(
            docRef.snapshots()
                .map { it.toObject(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == currentUid) {
                        handleFirestoreError(fe ?: error, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeDiscoverableProfiles(): Flow<List<PublicProfile>> = flow {
        val currentUid = requireUserId()
        val path = "public_profiles"
        emitAll(
            db.collection(path)
                .whereEqualTo("discoverable", true)
                .limit(100)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.displayName.lowercase() }
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == currentUid) {
                        handleFirestoreError(fe ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observePrivateUser(uid: String): Flow<PrivateUser?> = flow {
        val currentUid = requireUserId()
        val docRef = db.collection("users").document(uid)
        emitAll(
            docRef.snapshots()
                .map { it.toObject(PrivateUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == currentUid) {
                        handleFirestoreError(fe ?: error, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeDevices(uid: String): Flow<List<DeviceRecord>> = flow {
        val currentUid = requireUserId()
        val path = "users/$uid/devices"
        emitAll(
            db.collection("users").document(uid).collection("devices")
                .snapshots()
                .map { snap ->
                    snap.toObjects(DeviceRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.lastActiveAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == currentUid) {
                        handleFirestoreError(fe ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeBlockedUsers(uid: String): Flow<List<BlockedUser>> = flow {
        val currentUid = requireUserId()
        val path = "users/$uid/blocked_users"
        emitAll(
            db.collection("users").document(uid).collection("blocked_users")
                .snapshots()
                .map { it.toObjects(BlockedUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == currentUid) {
                        handleFirestoreError(fe ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeNotifications(uid: String): Flow<List<AppNotification>> = flow {
        val currentUid = requireUserId()
        val path = "users/$uid/notifications"
        emitAll(
            db.collection("users").document(uid).collection("notifications")
                .snapshots()
                .map { snap ->
                    snap.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == currentUid) {
                        handleFirestoreError(fe ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeFollowingIds(uid: String): Flow<Set<String>> = flow {
        val currentUid = requireUserId()
        val path = "follows"
        emitAll(
            db.collection(path)
                .whereEqualTo("followerUid", uid)
                .snapshots()
                .map { snap ->
                    snap.documents.mapNotNull { it.getString("followingUid") }.toSet()
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == currentUid) {
                        handleFirestoreError(fe ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeFollowerIds(uid: String): Flow<Set<String>> = flow {
        val currentUid = requireUserId()
        val path = "follows"
        emitAll(
            db.collection(path)
                .whereEqualTo("followingUid", uid)
                .snapshots()
                .map { snap ->
                    snap.documents.mapNotNull { it.getString("followerUid") }.toSet()
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == currentUid) {
                        handleFirestoreError(fe ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun isUsernameAvailable(rawUsername: String): Result<Boolean> {
        val clean = normalizeUsername(rawUsername)
        if (!isValidUsernameFormat(clean)) return Result.success(false)
        val path = "usernames/$clean"
        return try {
            requireUserId()
            val snap = db.collection("usernames").document(clean).get().await()
            Result.success(!snap.exists())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun createInitialProfile(
        displayName: String,
        rawUsername: String,
        bio: String,
        avatarBase64: String?,
        accountVisibility: String,
        phone: String?
    ): Result<Unit> {
        var path = "public_profiles"
        return try {
            val uid = requireUserId()
            val user = auth.currentUser
            val cleanUsername = normalizeUsername(rawUsername)
            require(isValidUsernameFormat(cleanUsername)) {
                "Username must be 3-30 lowercase letters, numbers, or underscores."
            }
            val cleanDisplay = displayName.trim().take(50)
            require(cleanDisplay.isNotEmpty()) { "Display name is required." }

            val usernameDoc = db.collection("usernames").document(cleanUsername).get().await()
            if (usernameDoc.exists() && usernameDoc.getString("uid") != uid) {
                return Result.failure(IllegalStateException("Username @$cleanUsername is already taken."))
            }

            val identity = crypto.getOrCreateDeviceIdentity(uid)
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim().take(80).ifBlank { "Android Device" }

            val publicRef = db.collection("public_profiles").document(uid)
            val privateRef = db.collection("users").document(uid)
            val deviceRef = privateRef.collection("devices").document(identity.deviceId)

            val existingPublic = publicRef.get().await()
            val existingPrivate = privateRef.get().await()
            val existingDevice = deviceRef.get().await()

            val batch = db.batch()
            if (!usernameDoc.exists()) {
                batch.set(
                    db.collection("usernames").document(cleanUsername),
                    mapOf(
                        "uid" to uid,
                        "username" to cleanUsername,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            path = publicRef.path
            if (existingPublic.exists()) {
                val publicUpdates = mutableMapOf<String, Any>(
                    "username" to cleanUsername,
                    "displayName" to cleanDisplay,
                    "bio" to bio.trim().take(200),
                    "publicKeyBase64" to identity.publicKeyBase64,
                    "accountVisibility" to accountVisibility,
                    "onlineStatusVisible" to true,
                    "online" to true,
                    "discoverable" to true,
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (avatarBase64 != null) {
                    publicUpdates["avatarBase64"] = avatarBase64
                }
                batch.update(publicRef, publicUpdates)
            } else {
                val publicPayload = mapOf(
                    "uid" to uid,
                    "username" to cleanUsername,
                    "displayName" to cleanDisplay,
                    "bio" to bio.trim().take(200),
                    "avatarBase64" to avatarBase64,
                    "publicKeyBase64" to identity.publicKeyBase64,
                    "accountVisibility" to accountVisibility,
                    "onlineStatusVisible" to true,
                    "online" to true,
                    "discoverable" to true,
                    "followersCount" to 0L,
                    "followingCount" to 0L,
                    "postsCount" to 0L,
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                batch.set(publicRef, publicPayload)
            }

            val cleanEmail = user?.email?.take(120)?.ifBlank { null }
            val cleanPhone = phone?.trim()?.take(30)?.ifBlank { null }
            if (existingPrivate.exists()) {
                val privateUpdates = mutableMapOf<String, Any>(
                    "messagePermission" to "EVERYONE",
                    "lastSeenVisible" to true,
                    "readReceiptsEnabled" to true,
                    "storyVisibility" to "PUBLIC",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanEmail != null) privateUpdates["email"] = cleanEmail
                if (cleanPhone != null) privateUpdates["phone"] = cleanPhone
                batch.update(privateRef, privateUpdates)
            } else {
                val privatePayload = mapOf(
                    "uid" to uid,
                    "email" to cleanEmail,
                    "phone" to cleanPhone,
                    "messagePermission" to "EVERYONE",
                    "lastSeenVisible" to true,
                    "readReceiptsEnabled" to true,
                    "storyVisibility" to "PUBLIC",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                batch.set(privateRef, privatePayload)
            }

            if (existingDevice.exists()) {
                batch.update(
                    deviceRef,
                    mapOf(
                        "deviceName" to deviceName,
                        "publicKeyBase64" to identity.publicKeyBase64,
                        "active" to true,
                        "lastActiveAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    deviceRef,
                    mapOf(
                        "deviceId" to identity.deviceId,
                        "deviceName" to deviceName,
                        "platform" to "Android",
                        "publicKeyBase64" to identity.publicKeyBase64,
                        "active" to true,
                        "lastActiveAt" to FieldValue.serverTimestamp(),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            if (!existingPublic.exists()) {
                val welcomeRef = privateRef.collection("notifications").document("welcome_${uid}")
                batch.set(
                    welcomeRef,
                    mapOf(
                        "recipientUid" to uid,
                        "actorUid" to uid,
                        "actorName" to "WeConnect",
                        "type" to "COMMUNITY",
                        "targetId" to uid,
                        "summary" to "Welcome to WeConnect, $cleanDisplay! Follow people you know, share updates, and enjoy private chats & calls.".take(200),
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun syncCurrentDeviceKey(): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val identity = crypto.getOrCreateDeviceIdentity(uid)
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim().take(80).ifBlank { "Android Device" }
            val devRef = db.collection("users").document(uid).collection("devices").document(identity.deviceId)
            path = devRef.path
            val existing = devRef.get().await()
            if (existing.exists()) {
                devRef.update(
                    mapOf(
                        "publicKeyBase64" to identity.publicKeyBase64,
                        "active" to true,
                        "lastActiveAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                devRef.set(
                    mapOf(
                        "deviceId" to identity.deviceId,
                        "deviceName" to deviceName,
                        "platform" to "Android",
                        "publicKeyBase64" to identity.publicKeyBase64,
                        "active" to true,
                        "lastActiveAt" to FieldValue.serverTimestamp(),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            val profileRef = db.collection("public_profiles").document(uid)
            if (profileRef.get().await().exists()) {
                profileRef.update(
                    mapOf(
                        "publicKeyBase64" to identity.publicKeyBase64,
                        "online" to true,
                        "lastSeenAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        displayName: String,
        bio: String,
        avatarBase64: String?,
        accountVisibility: String,
        onlineStatusVisible: Boolean
    ): Result<Unit> {
        var path = "public_profiles"
        return try {
            val uid = requireUserId()
            val ref = db.collection("public_profiles").document(uid)
            path = ref.path
            val updates = mutableMapOf<String, Any>(
                "displayName" to displayName.trim().take(50).ifBlank { "WeConnect User" },
                "bio" to bio.trim().take(200),
                "accountVisibility" to accountVisibility,
                "onlineStatusVisible" to onlineStatusVisible,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (avatarBase64 != null) {
                updates["avatarBase64"] = avatarBase64
            }
            ref.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePrivacySettings(
        phone: String,
        messagePermission: String,
        lastSeenVisible: Boolean,
        readReceiptsEnabled: Boolean,
        storyVisibility: String
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid)
            path = ref.path
            val updates = mutableMapOf<String, Any>(
                "messagePermission" to messagePermission,
                "lastSeenVisible" to lastSeenVisible,
                "readReceiptsEnabled" to readReceiptsEnabled,
                "storyVisibility" to storyVisibility,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (phone.isNotBlank()) {
                updates["phone"] = phone.trim().take(30)
            }
            ref.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun revokeDevice(deviceId: String): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("devices").document(deviceId)
            path = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleFollow(myProfile: PublicProfile, targetProfile: PublicProfile, currentlyFollowing: Boolean): Result<Unit> {
        var path = "follows"
        return try {
            val uid = requireUserId()
            if (targetProfile.uid == uid) return Result.success(Unit)
            val followId = "${uid}_${targetProfile.uid}"
            val followRef = db.collection("follows").document(followId)
            path = followRef.path

            val liveExists = try {
                followRef.get().await().exists()
            } catch (_: Exception) {
                currentlyFollowing
            }

            val batch = db.batch()
            if (liveExists) {
                batch.delete(followRef)
                batch.update(
                    db.collection("public_profiles").document(targetProfile.uid),
                    mapOf("followersCount" to FieldValue.increment(-1L), "updatedAt" to FieldValue.serverTimestamp())
                )
                batch.update(
                    db.collection("public_profiles").document(uid),
                    mapOf("followingCount" to FieldValue.increment(-1L), "updatedAt" to FieldValue.serverTimestamp())
                )
            } else {
                batch.set(
                    followRef,
                    mapOf(
                        "followerUid" to uid,
                        "followingUid" to targetProfile.uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    db.collection("public_profiles").document(targetProfile.uid),
                    mapOf("followersCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp())
                )
                batch.update(
                    db.collection("public_profiles").document(uid),
                    mapOf("followingCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp())
                )
                val safeActor = myProfile.displayName.trim().take(60).ifBlank { "WeConnect User" }
                val notifRef = db.collection("users").document(targetProfile.uid).collection("notifications").document()
                batch.set(
                    notifRef,
                    mapOf(
                        "recipientUid" to targetProfile.uid,
                        "actorUid" to uid,
                        "actorName" to safeActor,
                        "type" to "FOLLOW",
                        "targetId" to uid,
                        "summary" to "$safeActor (@${myProfile.username}) started following you.".take(200),
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun blockUser(targetUid: String, targetUsername: String): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("blocked_users").document(targetUid)
            path = ref.path
            ref.set(
                mapOf(
                    "blockedUid" to targetUid,
                    "blockedUsername" to targetUsername.take(40),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun unblockUser(targetUid: String): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("blocked_users").document(targetUid)
            path = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun ensureWelcomeNotification(displayName: String) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val welcomeRef = db.collection("users").document(uid)
                .collection("notifications").document("welcome_${uid}")
            if (!welcomeRef.get().await().exists()) {
                val safeName = displayName.trim().take(50).ifBlank { "Friend" }
                welcomeRef.set(
                    mapOf(
                        "recipientUid" to uid,
                        "actorUid" to uid,
                        "actorName" to "WeConnect",
                        "type" to "COMMUNITY",
                        "targetId" to uid,
                        "summary" to "Welcome to WeConnect, $safeName! Follow people you know, share updates, and enjoy private chats & calls.".take(200),
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (_: Exception) {
        }
    }

    suspend fun markNotificationRead(notifId: String): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("notifications").document(notifId)
            path = ref.path
            ref.update(mapOf("read" to true)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun markAllNotificationsRead(notifIds: List<String>): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val cleanIds = notifIds.filter { it.isNotBlank() }.distinct().take(400)
            if (cleanIds.isEmpty()) return Result.success(Unit)
            val col = db.collection("users").document(uid).collection("notifications")
            path = col.path
            val batch = db.batch()
            cleanIds.forEach { id ->
                batch.update(col.document(id), mapOf("read" to true))
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteNotification(notifId: String): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            require(notifId.isNotBlank()) { "Notification ID cannot be blank." }
            val ref = db.collection("users").document(uid).collection("notifications").document(notifId)
            path = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun clearAllNotifications(notifIds: List<String>): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val cleanIds = notifIds.filter { it.isNotBlank() }.distinct().take(400)
            if (cleanIds.isEmpty()) return Result.success(Unit)
            val col = db.collection("users").document(uid).collection("notifications")
            path = col.path
            val batch = db.batch()
            cleanIds.forEach { id ->
                batch.delete(col.document(id))
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun setOnlineStatus(online: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val ref = db.collection("public_profiles").document(uid)
            if (ref.get().await().exists()) {
                ref.update(
                    mapOf(
                        "online" to online,
                        "lastSeenAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (_: Exception) {
        }
    }

    companion object {
        fun normalizeUsername(raw: String): String =
            raw.trim().lowercase().replace(Regex("[^a-z0-9_]"), "")

        fun isValidUsernameFormat(clean: String): Boolean =
            clean.length in 3..30 && clean.matches(Regex("^[a-z0-9_]+$"))
    }
}
