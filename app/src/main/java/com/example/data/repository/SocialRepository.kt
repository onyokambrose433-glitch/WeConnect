package com.example.data.repository

import com.example.data.model.PostComment
import com.example.data.model.PublicProfile
import com.example.data.model.SocialPost
import com.example.data.model.StoryItem
import com.example.data.model.StoryView
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Timestamp
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
import java.util.Date

class SocialRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google.")
    }

    fun observePublicPosts(): Flow<List<SocialPost>> = flow {
        val currentUid = requireUserId()
        val path = "posts"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "PUBLIC")
                .limit(80)
                .snapshots()
                .map { snap ->
                    snap.toObjects(SocialPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
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

    fun observeUserPosts(targetUid: String): Flow<List<SocialPost>> = flow {
        val currentUid = requireUserId()
        val path = "posts"
        val baseQuery = db.collection(path).whereEqualTo("authorId", targetUid)
        val alignedQuery = if (targetUid == currentUid) baseQuery else baseQuery.whereEqualTo("visibility", "PUBLIC")
        emitAll(
            alignedQuery.snapshots()
                .map { snap ->
                    snap.toObjects(SocialPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
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

    fun observeSavedPostIds(uid: String): Flow<Set<String>> = flow {
        val currentUid = requireUserId()
        val path = "users/$uid/saved_posts"
        emitAll(
            db.collection("users").document(uid).collection("saved_posts")
                .snapshots()
                .map { snap -> snap.documents.map { it.id }.toSet() }
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

    fun observePostComments(postId: String): Flow<List<PostComment>> = flow {
        val currentUid = requireUserId()
        val path = "posts/$postId/comments"
        emitAll(
            db.collection("posts").document(postId).collection("comments")
                .whereEqualTo("postId", postId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PostComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
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

    fun observeMyReaction(postId: String, uid: String): Flow<String?> = flow {
        val currentUid = requireUserId()
        val docRef = db.collection("posts").document(postId).collection("reactions").document(uid)
        emitAll(
            docRef.snapshots()
                .map { if (it.exists()) it.getString("emoji") else null }
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

    fun observePublicStories(): Flow<List<StoryItem>> = flow {
        val currentUid = requireUserId()
        val path = "stories"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "PUBLIC")
                .snapshots()
                .map { snap ->
                    val nowSec = Timestamp.now().seconds
                    snap.toObjects(StoryItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { (it.expiresAt?.seconds ?: 0L) > nowSec }
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

    fun observeStoryViews(storyId: String): Flow<List<StoryView>> = flow {
        val currentUid = requireUserId()
        val path = "stories/$storyId/views"
        emitAll(
            db.collection("stories").document(storyId).collection("views")
                .whereEqualTo("storyId", storyId)
                .snapshots()
                .map { snap -> snap.toObjects(StoryView::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
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

    suspend fun createPost(
        author: PublicProfile,
        caption: String,
        mediaBase64List: List<String>,
        visibility: String
    ): Result<Unit> {
        var path = "posts"
        return try {
            val uid = requireUserId()
            val cleanCaption = caption.trim().take(2000)
            require(cleanCaption.isNotEmpty() || mediaBase64List.isNotEmpty()) {
                "Add text or an image to publish a post."
            }
            val hashtags = Regex("#([A-Za-z0-9_]{2,35})")
                .findAll(cleanCaption)
                .map { it.groupValues[1].lowercase() }
                .distinct()
                .take(10)
                .toList()

            val postRef = db.collection("posts").document()
            path = postRef.path
            val payload = mapOf(
                "authorId" to uid,
                "authorUsername" to author.username,
                "authorName" to author.displayName.take(50),
                "authorAvatarBase64" to author.avatarBase64,
                "caption" to cleanCaption,
                "mediaBase64List" to mediaBase64List.take(4),
                "hashtags" to hashtags,
                "visibility" to visibility,
                "likesCount" to 0L,
                "commentsCount" to 0L,
                "sharesCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            val batch = db.batch()
            batch.set(postRef, payload)
            batch.update(
                db.collection("public_profiles").document(uid),
                mapOf("postsCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp())
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun editPost(postId: String, newCaption: String, visibility: String): Result<Unit> {
        var path = "posts/$postId"
        return try {
            requireUserId()
            val clean = newCaption.trim().take(2000)
            val hashtags = Regex("#([A-Za-z0-9_]{2,35})").findAll(clean)
                .map { it.groupValues[1].lowercase() }.distinct().take(10).toList()
            val ref = db.collection("posts").document(postId)
            path = ref.path
            ref.update(
                mapOf(
                    "caption" to clean,
                    "hashtags" to hashtags,
                    "visibility" to visibility,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        var path = "posts/$postId"
        return try {
            val uid = requireUserId()
            val ref = db.collection("posts").document(postId)
            path = ref.path
            val batch = db.batch()
            batch.delete(ref)
            batch.update(
                db.collection("public_profiles").document(uid),
                mapOf("postsCount" to FieldValue.increment(-1L), "updatedAt" to FieldValue.serverTimestamp())
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun togglePostLike(myProfile: PublicProfile, post: SocialPost): Result<Unit> {
        var path = "posts/${post.id}/reactions"
        return try {
            val uid = requireUserId()
            val reactionRef = db.collection("posts").document(post.id).collection("reactions").document(uid)
            path = reactionRef.path
            val existing = reactionRef.get().await()
            val batch = db.batch()
            if (existing.exists()) {
                batch.delete(reactionRef)
                batch.update(
                    db.collection("posts").document(post.id),
                    mapOf("likesCount" to FieldValue.increment(-1L), "updatedAt" to FieldValue.serverTimestamp())
                )
            } else {
                batch.set(
                    reactionRef,
                    mapOf(
                        "postId" to post.id,
                        "uid" to uid,
                        "emoji" to "💚",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    db.collection("posts").document(post.id),
                    mapOf("likesCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp())
                )
                if (post.authorId != uid) {
                    val notifRef = db.collection("users").document(post.authorId).collection("notifications").document()
                    batch.set(
                        notifRef,
                        mapOf(
                            "recipientUid" to post.authorId,
                            "actorUid" to uid,
                            "actorName" to myProfile.displayName.take(60),
                            "type" to "LIKE",
                            "targetId" to post.id,
                            "summary" to "${myProfile.displayName} reacted to your post.",
                            "read" to false,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
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

    suspend fun addComment(
        myProfile: PublicProfile,
        post: SocialPost,
        text: String,
        replyToCommentId: String? = null,
        replyToComment: PostComment? = null
    ): Result<Unit> {
        var path = "posts/${post.id}/comments"
        return try {
            val uid = requireUserId()
            val clean = text.trim().take(600)
            require(clean.isNotEmpty()) { "Comment cannot be blank." }
            val safeReplyId = replyToCommentId?.trim()?.takeIf {
                it.isNotEmpty() && it.length <= 128 && it.matches(Regex("^[a-zA-Z0-9_\\-]+$"))
            }
            val safeAuthorName = myProfile.displayName.trim().take(50).ifBlank { "User" }
            val safeActorName = myProfile.displayName.trim().take(60).ifBlank { "WeConnect User" }

            val commentRef = db.collection("posts").document(post.id).collection("comments").document()
            path = commentRef.path

            val batch = db.batch()
            val payload = mapOf(
                "postId" to post.id,
                "authorId" to uid,
                "authorName" to safeAuthorName,
                "authorUsername" to myProfile.username,
                "text" to clean,
                "replyToCommentId" to safeReplyId,
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            batch.set(commentRef, payload)
            batch.update(
                db.collection("posts").document(post.id),
                mapOf("commentsCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp())
            )
            if (post.authorId != uid) {
                val postNotifRef = db.collection("users").document(post.authorId).collection("notifications").document()
                val summaryPrefix = if (replyToComment?.authorId == post.authorId) {
                    "$safeActorName replied to your comment: "
                } else {
                    "$safeActorName commented: "
                }
                batch.set(
                    postNotifRef,
                    mapOf(
                        "recipientUid" to post.authorId,
                        "actorUid" to uid,
                        "actorName" to safeActorName,
                        "type" to "COMMENT",
                        "targetId" to post.id,
                        "summary" to (summaryPrefix + clean.take(80)).take(200),
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            if (replyToComment != null && replyToComment.authorId.isNotBlank() &&
                replyToComment.authorId != uid && replyToComment.authorId != post.authorId
            ) {
                val replyNotifRef = db.collection("users").document(replyToComment.authorId).collection("notifications").document()
                batch.set(
                    replyNotifRef,
                    mapOf(
                        "recipientUid" to replyToComment.authorId,
                        "actorUid" to uid,
                        "actorName" to safeActorName,
                        "type" to "COMMENT",
                        "targetId" to post.id,
                        "summary" to "$safeActorName replied to your comment: ${clean.take(80)}".take(200),
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

    suspend fun deleteComment(postId: String, commentId: String): Result<Unit> {
        var path = "posts/$postId/comments/$commentId"
        return try {
            requireUserId()
            require(postId.isNotBlank() && commentId.isNotBlank()) { "Invalid comment reference." }
            val postRef = db.collection("posts").document(postId)
            val commentRef = postRef.collection("comments").document(commentId)
            path = commentRef.path

            val currentCount = try {
                postRef.get().await().getLong("commentsCount") ?: 0L
            } catch (_: Exception) {
                0L
            }

            val batch = db.batch()
            batch.delete(commentRef)
            if (currentCount > 0L) {
                batch.update(
                    postRef,
                    mapOf("commentsCount" to FieldValue.increment(-1L), "updatedAt" to FieldValue.serverTimestamp())
                )
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

    suspend fun incrementShareCount(postId: String): Result<Unit> {
        var path = "posts/$postId"
        return try {
            requireUserId()
            val ref = db.collection("posts").document(postId)
            path = ref.path
            ref.update(mapOf("sharesCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp())).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleSavePost(postId: String, currentlySaved: Boolean): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("saved_posts").document(postId)
            path = ref.path
            if (currentlySaved) {
                ref.delete().await()
            } else {
                ref.set(mapOf("postId" to postId, "savedAt" to FieldValue.serverTimestamp())).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun createStory(
        author: PublicProfile,
        caption: String,
        mediaBase64: String?,
        bgColorHex: String
    ): Result<Unit> {
        var path = "stories"
        return try {
            val uid = requireUserId()
            val cleanCaption = caption.trim().take(400)
            require(cleanCaption.isNotEmpty() || mediaBase64 != null) {
                "Story needs text or an image."
            }
            val storyRef = db.collection("stories").document()
            path = storyRef.path
            val expiresAt = Timestamp(Date(System.currentTimeMillis() + 24L * 3600_000L))
            val payload = mapOf(
                "authorId" to uid,
                "authorName" to author.displayName.take(50),
                "authorUsername" to author.username,
                "authorAvatarBase64" to author.avatarBase64,
                "mediaType" to if (mediaBase64 != null) "IMAGE" else "TEXT",
                "caption" to cleanCaption,
                "mediaBase64" to mediaBase64,
                "bgColorHex" to bgColorHex,
                "visibility" to "PUBLIC",
                "viewsCount" to 0L,
                "expiresAt" to expiresAt,
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            storyRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordStoryView(story: StoryItem, viewerName: String, reaction: String? = null) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val viewRef = db.collection("stories").document(story.id).collection("views").document(uid)
            val existing = viewRef.get().await()
            val payload = mapOf(
                "storyId" to story.id,
                "viewerUid" to uid,
                "viewerName" to viewerName.take(50),
                "reaction" to reaction,
                "viewedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            val batch = db.batch()
            batch.set(viewRef, payload)
            if (!existing.exists()) {
                batch.update(
                    db.collection("stories").document(story.id),
                    mapOf("viewsCount" to FieldValue.increment(1L))
                )
            }
            batch.commit().await()
        } catch (_: Exception) {
        }
    }

    suspend fun deleteStory(storyId: String): Result<Unit> {
        var path = "stories/$storyId"
        return try {
            requireUserId()
            val ref = db.collection("stories").document(storyId)
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

    suspend fun submitReport(targetType: String, targetId: String, reason: String): Result<Unit> {
        var path = "reports"
        return try {
            val uid = requireUserId()
            val clean = reason.trim().take(400)
            require(clean.length >= 3) { "Please describe the reason (min 3 characters)." }
            val ref = db.collection("reports").document()
            path = ref.path
            ref.set(
                mapOf(
                    "reporterUid" to uid,
                    "targetType" to targetType,
                    "targetId" to targetId,
                    "reason" to clean,
                    "status" to "OPEN",
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
}
