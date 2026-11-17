package com.example.data.repository

import com.example.data.crypto.WeConnectCrypto
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.PublicProfile
import com.example.data.model.TypingStatus
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
import java.util.UUID

class MessagingRepository(
    private val db: FirebaseFirestore,
    private val crypto: WeConnectCrypto,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before messaging.")
    }

    fun observeConversations(uid: String): Flow<List<Conversation>> = flow {
        val currentUid = requireUserId()
        val path = "conversations"
        emitAll(
            db.collection(path)
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { (it.lastMessageAt ?: it.updatedAt ?: it.createdAt)?.seconds ?: 0L }
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

    fun observeConversation(chatId: String): Flow<Conversation?> = flow {
        val currentUid = requireUserId()
        val docRef = db.collection("conversations").document(chatId)
        emitAll(
            docRef.snapshots()
                .map { it.toObject(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
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

    fun observeMessages(chatId: String, uid: String): Flow<List<ChatMessage>> = flow {
        val currentUid = requireUserId()
        val path = "conversations/$chatId/messages"
        emitAll(
            db.collection("conversations").document(chatId).collection("messages")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    val now = Timestamp.now()
                    snap.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { msg -> msg.expiresAt == null || msg.expiresAt.seconds > now.seconds }
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

    fun observeTyping(chatId: String, uid: String): Flow<List<TypingStatus>> = flow {
        val currentUid = requireUserId()
        val path = "conversations/$chatId/typing"
        emitAll(
            db.collection("conversations").document(chatId).collection("typing")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(TypingStatus::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.uid != uid && it.typing }
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

    suspend fun createOrOpenDirectChat(myProfile: PublicProfile, peerProfile: PublicProfile): Result<String> {
        var path = "conversations"
        return try {
            val uid = requireUserId()
            val sorted = listOf(uid, peerProfile.uid).sorted()
            val chatId = "dm_${sorted[0].take(24)}_${sorted[1].take(24)}"
            val docRef = db.collection("conversations").document(chatId)
            path = docRef.path

            val identity = crypto.getOrCreateDeviceIdentity(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val keysMap = mutableMapOf<String, String>()
                keysMap[uid] = identity.publicKeyBase64
                if (!peerProfile.publicKeyBase64.isNullOrBlank()) {
                    keysMap[peerProfile.uid] = peerProfile.publicKeyBase64
                }
                val payload = mapOf(
                    "type" to "DIRECT",
                    "title" to "",
                    "description" to "",
                    "memberUids" to listOf(uid, peerProfile.uid),
                    "adminUids" to listOf(uid, peerProfile.uid),
                    "memberNames" to mapOf(
                        uid to myProfile.displayName,
                        peerProfile.uid to peerProfile.displayName
                    ),
                    "memberKeys" to keysMap,
                    "archivedBy" to emptyList<String>(),
                    "mutedBy" to emptyList<String>(),
                    "disappearingHours" to 0L,
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                // Refresh our public key if needed
                val existingKeys = (existing.get("memberKeys") as? Map<*, *>)
                    ?.mapNotNull { (k, v) -> if (k is String && v is String) k to v else null }
                    ?.toMap()?.toMutableMap() ?: mutableMapOf()
                existingKeys[uid] = identity.publicKeyBase64
                if (!peerProfile.publicKeyBase64.isNullOrBlank()) {
                    existingKeys[peerProfile.uid] = peerProfile.publicKeyBase64
                }
                docRef.update(
                    mapOf(
                        "memberKeys" to existingKeys,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(chatId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun createGroupChat(
        myProfile: PublicProfile,
        title: String,
        description: String,
        members: List<PublicProfile>
    ): Result<String> {
        var path = "conversations"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(80)
            require(cleanTitle.isNotEmpty()) { "Group name is required." }

            val allMembers = (listOf(myProfile) + members).distinctBy { it.uid }.take(50)
            val memberUids = allMembers.map { it.uid }
            val memberNames = allMembers.associate { it.uid to it.displayName }
            val memberKeys = allMembers.mapNotNull { p ->
                p.publicKeyBase64?.let { p.uid to it }
            }.toMap()

            val docRef = db.collection("conversations").document()
            path = docRef.path
            val inviteCode = UUID.randomUUID().toString().replace("-", "").take(8).uppercase()

            docRef.set(
                mapOf(
                    "type" to "GROUP",
                    "title" to cleanTitle,
                    "description" to description.trim().take(250),
                    "memberUids" to memberUids,
                    "adminUids" to listOf(uid),
                    "memberNames" to memberNames,
                    "memberKeys" to memberKeys,
                    "archivedBy" to emptyList<String>(),
                    "mutedBy" to emptyList<String>(),
                    "disappearingHours" to 0L,
                    "inviteCode" to inviteCode,
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
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
    ): Result<Unit> {
        var path = "conversations/${conversation.id}/messages"
        return try {
            val uid = requireUserId()
            val cleanText = plaintext.trim().take(1800)
            require(cleanText.isNotEmpty() || mediaBase64 != null) { "Message cannot be empty." }

            val peerUid = conversation.memberUids.firstOrNull { it != uid }
            val peerPubKey = peerUid?.let { conversation.memberKeys[it] }

            var ciphertext = cleanText
            var iv: String? = null
            var isEncrypted = false
            var senderPubKey: String? = null

            if (conversation.type == "DIRECT" && !peerPubKey.isNullOrBlank() && cleanText.isNotEmpty()) {
                val enc = crypto.encryptDirectMessage(uid, peerPubKey, cleanText)
                ciphertext = enc.ciphertextBase64
                iv = enc.ivBase64
                senderPubKey = enc.senderPublicKeyBase64
                isEncrypted = true
            }

            val msgRef = db.collection("conversations").document(conversation.id)
                .collection("messages").document()
            path = msgRef.path

            val expiresAt = if (conversation.disappearingHours > 0L) {
                Timestamp(Date(System.currentTimeMillis() + conversation.disappearingHours * 3600_000L))
            } else null

            val payload = mapOf(
                "chatId" to conversation.id,
                "senderId" to uid,
                "senderName" to senderProfile.displayName.take(60),
                "memberUids" to conversation.memberUids,
                "messageType" to messageType,
                "ciphertext" to ciphertext,
                "iv" to iv,
                "encrypted" to isEncrypted,
                "senderPublicKey" to senderPubKey,
                "mediaBase64" to mediaBase64,
                "mediaMimeType" to mediaMimeType,
                "fileName" to fileName?.take(120),
                "replyToMessageId" to replyToMessageId,
                "replyToPreview" to replyToPreview?.take(160),
                "forwarded" to forwarded,
                "edited" to false,
                "deletedForAll" to false,
                "reactions" to emptyMap<String, String>(),
                "readBy" to listOf(uid),
                "deliveredTo" to listOf(uid),
                "expiresAt" to expiresAt,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            val lastPreview = when {
                isEncrypted -> "End-to-end encrypted message"
                messageType == "IMAGE" -> "Photo attachment"
                messageType == "VOICE" -> "Voice note"
                messageType == "FILE" -> "File: ${fileName ?: "Document"}"
                else -> cleanText.take(120)
            }

            val batch = db.batch()
            batch.set(msgRef, payload)
            batch.update(
                db.collection("conversations").document(conversation.id),
                mapOf(
                    "lastMessageText" to lastPreview,
                    "lastMessageSenderId" to uid,
                    "lastMessageAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
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

    suspend fun decryptMessageText(conversation: Conversation, message: ChatMessage, myUid: String): String {
        if (message.deletedForAll) return "This message was deleted"
        if (!message.encrypted) return message.ciphertext
        val iv = message.iv ?: return "[Encrypted message]"
        val peerUid = conversation.memberUids.firstOrNull { it != myUid }
        val keyToUse = if (message.senderId == myUid) {
            peerUid?.let { conversation.memberKeys[it] }
        } else {
            message.senderPublicKey ?: conversation.memberKeys[message.senderId]
        }
        if (keyToUse.isNullOrBlank()) return "[Encrypted message]"
        return crypto.decryptDirectMessage(myUid, keyToUse, message.ciphertext, iv)
            ?: "[Encrypted on another device]"
    }

    suspend fun editMessage(conversation: Conversation, message: ChatMessage, newText: String): Result<Unit> {
        var path = "conversations/${conversation.id}/messages/${message.id}"
        return try {
            val uid = requireUserId()
            require(message.senderId == uid) { "Only sender can edit." }
            val clean = newText.trim().take(1800)
            require(clean.isNotEmpty()) { "Message cannot be blank." }
            val peerUid = conversation.memberUids.firstOrNull { it != uid }
            val peerPubKey = peerUid?.let { conversation.memberKeys[it] }

            val updates = if (message.encrypted && !peerPubKey.isNullOrBlank()) {
                val enc = crypto.encryptDirectMessage(uid, peerPubKey, clean)
                mapOf(
                    "ciphertext" to enc.ciphertextBase64,
                    "iv" to enc.ivBase64,
                    "edited" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            } else {
                mapOf(
                    "ciphertext" to clean,
                    "edited" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            }
            val ref = db.collection("conversations").document(conversation.id)
                .collection("messages").document(message.id)
            path = ref.path
            ref.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteMessageForAll(chatId: String, messageId: String): Result<Unit> {
        var path = "conversations/$chatId/messages/$messageId"
        return try {
            requireUserId()
            val ref = db.collection("conversations").document(chatId).collection("messages").document(messageId)
            path = ref.path
            ref.update(
                mapOf(
                    "ciphertext" to "Message deleted",
                    "deletedForAll" to true,
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

    suspend fun toggleReaction(chatId: String, message: ChatMessage, emoji: String): Result<Unit> {
        var path = "conversations/$chatId/messages/${message.id}"
        return try {
            val uid = requireUserId()
            val updated = message.reactions.toMutableMap()
            if (updated[uid] == emoji) updated.remove(uid) else updated[uid] = emoji
            val ref = db.collection("conversations").document(chatId).collection("messages").document(message.id)
            path = ref.path
            ref.update(mapOf("reactions" to updated, "updatedAt" to FieldValue.serverTimestamp())).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun markMessagesRead(chatId: String, messages: List<ChatMessage>, readReceiptsEnabled: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        val toMark = messages.filter { it.senderId != uid && (!it.deliveredTo.contains(uid) || (readReceiptsEnabled && !it.readBy.contains(uid))) }
        if (toMark.isEmpty()) return
        try {
            val batch = db.batch()
            toMark.take(25).forEach { msg ->
                val ref = db.collection("conversations").document(chatId).collection("messages").document(msg.id)
                val map = mutableMapOf<String, Any>(
                    "deliveredTo" to (msg.deliveredTo + uid).distinct(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (readReceiptsEnabled) {
                    map["readBy"] = (msg.readBy + uid).distinct()
                }
                batch.update(ref, map)
            }
            batch.commit().await()
        } catch (_: Exception) {
        }
    }

    suspend fun setTyping(conversation: Conversation, displayName: String, typing: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        try {
            db.collection("conversations").document(conversation.id)
                .collection("typing").document(uid)
                .set(
                    mapOf(
                        "uid" to uid,
                        "displayName" to displayName.take(60),
                        "memberUids" to conversation.memberUids,
                        "typing" to typing,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
        } catch (_: Exception) {
        }
    }

    suspend fun pinMessage(chatId: String, messageId: String, preview: String): Result<Unit> {
        var path = "conversations/$chatId"
        return try {
            requireUserId()
            val ref = db.collection("conversations").document(chatId)
            path = ref.path
            ref.update(
                mapOf(
                    "pinnedMessageId" to messageId,
                    "pinnedMessagePreview" to preview.take(200),
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

    suspend fun toggleArchive(conversation: Conversation): Result<Unit> {
        var path = "conversations/${conversation.id}"
        return try {
            val uid = requireUserId()
            val updated = if (uid in conversation.archivedBy) conversation.archivedBy - uid else conversation.archivedBy + uid
            val ref = db.collection("conversations").document(conversation.id)
            path = ref.path
            ref.update(mapOf("archivedBy" to updated, "updatedAt" to FieldValue.serverTimestamp())).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleMute(conversation: Conversation): Result<Unit> {
        var path = "conversations/${conversation.id}"
        return try {
            val uid = requireUserId()
            val updated = if (uid in conversation.mutedBy) conversation.mutedBy - uid else conversation.mutedBy + uid
            val ref = db.collection("conversations").document(conversation.id)
            path = ref.path
            ref.update(mapOf("mutedBy" to updated, "updatedAt" to FieldValue.serverTimestamp())).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateGroupSettings(
        conversation: Conversation,
        title: String,
        description: String,
        disappearingHours: Long
    ): Result<Unit> {
        var path = "conversations/${conversation.id}"
        return try {
            requireUserId()
            val ref = db.collection("conversations").document(conversation.id)
            path = ref.path
            ref.update(
                mapOf(
                    "title" to title.trim().take(80),
                    "description" to description.trim().take(250),
                    "disappearingHours" to disappearingHours,
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

    suspend fun addGroupMember(conversation: Conversation, member: PublicProfile): Result<Unit> {
        var path = "conversations/${conversation.id}"
        return try {
            requireUserId()
            val updatedUids = (conversation.memberUids + member.uid).distinct().take(50)
            val updatedNames = conversation.memberNames + (member.uid to member.displayName)
            val updatedKeys = if (!member.publicKeyBase64.isNullOrBlank()) {
                conversation.memberKeys + (member.uid to member.publicKeyBase64)
            } else conversation.memberKeys
            val ref = db.collection("conversations").document(conversation.id)
            path = ref.path
            ref.update(
                mapOf(
                    "memberUids" to updatedUids,
                    "memberNames" to updatedNames,
                    "memberKeys" to updatedKeys,
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

    suspend fun removeGroupMember(conversation: Conversation, memberUid: String): Result<Unit> {
        var path = "conversations/${conversation.id}"
        return try {
            requireUserId()
            val updatedUids = conversation.memberUids - memberUid
            val updatedAdmins = conversation.adminUids - memberUid
            require(updatedUids.isNotEmpty() && updatedAdmins.isNotEmpty()) { "Group must retain an admin." }
            val ref = db.collection("conversations").document(conversation.id)
            path = ref.path
            ref.update(
                mapOf(
                    "memberUids" to updatedUids,
                    "adminUids" to updatedAdmins,
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
}
