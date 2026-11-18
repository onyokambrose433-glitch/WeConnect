package com.example.data.repository

import com.example.data.model.CallRecord
import com.example.data.model.Community
import com.example.data.model.CommunityMember
import com.example.data.model.CommunityPost
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

class CommunityCallRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google.")
    }

    fun observePublicCommunities(): Flow<List<Community>> = flow {
        val currentUid = requireUserId()
        val path = "communities"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "PUBLIC")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Community::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.memberCount }
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

    fun observeCommunityMembers(communityId: String): Flow<List<CommunityMember>> = flow {
        val currentUid = requireUserId()
        val path = "communities/$communityId/members"
        emitAll(
            db.collection("communities").document(communityId).collection("members")
                .whereEqualTo("communityId", communityId)
                .snapshots()
                .map { it.toObjects(CommunityMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
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

    fun observeCommunityPosts(communityId: String): Flow<List<CommunityPost>> = flow {
        val currentUid = requireUserId()
        val path = "communities/$communityId/posts"
        emitAll(
            db.collection("communities").document(communityId).collection("posts")
                .whereEqualTo("communityId", communityId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CommunityPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(compareByDescending<CommunityPost> { it.pinned }.thenByDescending { it.createdAt?.seconds ?: 0L })
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

    suspend fun createCommunity(
        owner: PublicProfile,
        name: String,
        description: String,
        kind: String,
        visibility: String,
        rulesText: String
    ): Result<String> {
        var path = "communities"
        return try {
            val uid = requireUserId()
            val cleanName = name.trim().take(80)
            require(cleanName.length >= 2) { "Name must be at least 2 characters." }
            val commRef = db.collection("communities").document()
            path = commRef.path
            val safeDisplayName = owner.displayName.trim().ifBlank { "WeConnect User" }.take(50)
            val cleanUsername = owner.username.trim().lowercase().replace(Regex("[^a-z0-9_]"), "").ifBlank { "user_" + uid.take(6).lowercase() }
            val safeUsername = if (cleanUsername.length in 3..30) cleanUsername else ("user_" + uid.take(6).lowercase()).take(30)

            commRef.set(
                mapOf(
                    "name" to cleanName,
                    "description" to description.trim().take(400),
                    "kind" to kind,
                    "visibility" to visibility,
                    "rulesText" to rulesText.trim().ifBlank { "Be respectful and protect community privacy." }.take(600),
                    "ownerId" to uid,
                    "adminUids" to listOf(uid),
                    "memberCount" to 1L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            commRef.collection("members").document(uid).set(
                mapOf(
                    "communityId" to commRef.id,
                    "uid" to uid,
                    "displayName" to safeDisplayName,
                    "username" to safeUsername,
                    "role" to "ADMIN",
                    "joinedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(commRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun joinOrLeaveCommunity(
        community: Community,
        profile: PublicProfile,
        isMember: Boolean
    ): Result<Unit> {
        var path = "communities/${community.id}/members"
        return try {
            val uid = requireUserId()
            val memberRef = db.collection("communities").document(community.id)
                .collection("members").document(uid)
            path = memberRef.path
            val batch = db.batch()
            if (isMember) {
                batch.delete(memberRef)
                batch.update(
                    db.collection("communities").document(community.id),
                    mapOf("memberCount" to FieldValue.increment(-1L), "updatedAt" to FieldValue.serverTimestamp())
                )
            } else {
                batch.set(
                    memberRef,
                    mapOf(
                        "communityId" to community.id,
                        "uid" to uid,
                        "displayName" to profile.displayName.take(50),
                        "username" to profile.username,
                        "role" to "MEMBER",
                        "joinedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    db.collection("communities").document(community.id),
                    mapOf("memberCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp())
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

    suspend fun publishCommunityPost(
        communityId: String,
        author: PublicProfile,
        title: String,
        body: String,
        pinned: Boolean = false
    ): Result<Unit> {
        var path = "communities/$communityId/posts"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(120)
            val cleanBody = body.trim().take(2000)
            require(cleanTitle.isNotEmpty() && cleanBody.isNotEmpty()) { "Title and message are required." }
            val ref = db.collection("communities").document(communityId).collection("posts").document()
            path = ref.path
            ref.set(
                mapOf(
                    "communityId" to communityId,
                    "authorId" to uid,
                    "authorName" to author.displayName.take(50),
                    "authorUsername" to author.username,
                    "title" to cleanTitle,
                    "body" to cleanBody,
                    "pinned" to pinned,
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

    suspend fun togglePinCommunityPost(communityId: String, postId: String, currentlyPinned: Boolean): Result<Unit> {
        var path = "communities/$communityId/posts/$postId"
        return try {
            requireUserId()
            val ref = db.collection("communities").document(communityId).collection("posts").document(postId)
            path = ref.path
            ref.update(mapOf("pinned" to !currentlyPinned)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- VOICE & VIDEO CALL SIGNALING / HISTORY ---
    fun observeMyCalls(uid: String): Flow<List<CallRecord>> = flow {
        val currentUid = requireUserId()
        val path = "calls"
        emitAll(
            db.collection(path)
                .whereArrayContains("participantUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CallRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.startedAt?.seconds ?: 0L }
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

    suspend fun startCall(caller: PublicProfile, callee: PublicProfile, callType: String): Result<String> {
        var path = "calls"
        return try {
            val uid = requireUserId()
            val ref = db.collection("calls").document()
            path = ref.path
            val cleanType = if (callType.equals("VIDEO", ignoreCase = true)) "VIDEO" else "VOICE"
            val safeCallerName = caller.displayName.trim().ifBlank { "WeConnect User" }.take(60)
            val safeCalleeName = callee.displayName.trim().ifBlank { "WeConnect User" }.take(60)
            val targetCalleeUid = callee.uid.trim().ifBlank { "${uid}_test" }
            val participants = if (targetCalleeUid == uid) listOf(uid, "${uid}_peer") else listOf(uid, targetCalleeUid)

            ref.set(
                mapOf(
                    "callerId" to uid,
                    "callerName" to safeCallerName,
                    "calleeId" to targetCalleeUid,
                    "calleeName" to safeCalleeName,
                    "participantUids" to participants,
                    "callType" to cleanType,
                    "status" to "RINGING",
                    "offerSdp" to "webrtc-signaling-ready:${uid}:${System.currentTimeMillis()}",
                    "startedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Best-effort notification delivery to callee
            if (targetCalleeUid != uid && !targetCalleeUid.endsWith("_test") && !targetCalleeUid.endsWith("_peer")) {
                try {
                    db.collection("users").document(targetCalleeUid).collection("notifications").document().set(
                        mapOf(
                            "recipientUid" to targetCalleeUid,
                            "actorUid" to uid,
                            "actorName" to safeCallerName,
                            "type" to "CALL_MISSED",
                            "targetId" to ref.id,
                            "summary" to "$safeCallerName called you (${cleanType.lowercase()}).",
                            "read" to false,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } catch (_: Exception) {
                    // Do not fail the call if notifications subcollection write fails
                }
            }
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateCallStatus(callId: String, newStatus: String): Result<Unit> {
        if (callId.isBlank()) return Result.success(Unit)
        var path = "calls/$callId"
        return try {
            val uid = requireUserId()
            val ref = db.collection("calls").document(callId)
            path = ref.path
            val updates = mutableMapOf<String, Any>(
                "status" to newStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (newStatus == "CONNECTED") {
                updates["answerSdp"] = "webrtc-answer:${uid}:${System.currentTimeMillis()}"
            }
            if (newStatus in listOf("ENDED", "DECLINED", "MISSED")) {
                updates["endedAt"] = FieldValue.serverTimestamp()
            }
            try {
                ref.update(updates).await()
            } catch (e: Exception) {
                // If call is already ended/declined/missed, terminal update should not fail
                val fe = generateSequence(e as Throwable) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fe != null && newStatus in listOf("ENDED", "DECLINED", "MISSED")) {
                    return Result.success(Unit)
                }
                throw e
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
