package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.crypto.WeConnectCrypto
import com.example.data.model.PostComment
import com.example.data.model.PublicProfile
import com.example.data.model.isEffectivelyOnline
import com.example.data.repository.AuthProfileRepository
import com.example.ui.components.formatInboxTimestamp
import com.example.ui.home.buildCommentThreads
import com.example.ui.main.SearchHistoryEntry
import com.example.ui.main.SearchHistoryKind
import com.example.ui.main.WeConnectViewModel
import com.google.firebase.Timestamp
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches WeConnect`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("WeConnect", appName)
    }

    @Test
    fun `username normalization and validation work accurately`() {
        assertEquals("alice_green99", AuthProfileRepository.normalizeUsername(" Alice_Green99! "))
        assertTrue(AuthProfileRepository.isValidUsernameFormat("alice_green99"))
        assertFalse(AuthProfileRepository.isValidUsernameFormat("ab"))
        assertFalse(AuthProfileRepository.isValidUsernameFormat("bad-handle"))
    }

    @Test
    fun `friend online indicator respects offline flag, privacy, and staleness timeout`() {
        val nowMs = 1_700_000_000_000L
        val freshTs = Timestamp((nowMs - 30_000L) / 1000L, 0)
        val staleTs = Timestamp((nowMs - 180_000L) / 1000L, 0)

        // Default constructed profile must NEVER appear online
        val fallbackProfile = PublicProfile(uid = "peer_1", displayName = "Alex")
        assertFalse(fallbackProfile.online)
        assertFalse(fallbackProfile.isEffectivelyOnline(nowMs))

        // Explicitly offline user
        val offlineProfile = PublicProfile(uid = "peer_2", online = false, lastSeenAt = freshTs)
        assertFalse(offlineProfile.isEffectivelyOnline(nowMs))

        // User with online flag = true but stale heartbeat (> 2 minutes old) -> offline (gray dot)
        val staleOnline = PublicProfile(uid = "peer_3", online = true, lastSeenAt = staleTs)
        assertFalse(staleOnline.isEffectivelyOnline(nowMs))

        // User hiding online status -> false
        val hiddenOnline = PublicProfile(uid = "peer_4", online = true, onlineStatusVisible = false, lastSeenAt = freshTs)
        assertFalse(hiddenOnline.isEffectivelyOnline(nowMs))

        // Active online user with fresh heartbeat -> online (green dot)
        val activeOnline = PublicProfile(uid = "peer_5", online = true, onlineStatusVisible = true, lastSeenAt = freshTs)
        assertTrue(activeOnline.isEffectivelyOnline(nowMs))
    }

    @Test
    fun `comment threading groups direct replies, nested replies, and orphan replies safely`() {
        val c1 = PostComment(id = "c1", postId = "p1", authorUsername = "alice", text = "Great photo!")
        val r1 = PostComment(id = "r1", postId = "p1", authorUsername = "bob", text = "Totally agree", replyToCommentId = "c1")
        val r2 = PostComment(id = "r2", postId = "p1", authorUsername = "carol", text = "Thanks Bob!", replyToCommentId = "r1")
        val c2 = PostComment(id = "c2", postId = "p1", authorUsername = "dan", text = "Where was this?")
        val orphan = PostComment(id = "r3", postId = "p1", authorUsername = "eve", text = "Deleted parent reply", replyToCommentId = "missing_id")

        val threads = buildCommentThreads(listOf(c1, r1, r2, c2, orphan))

        assertEquals(3, threads.size)
        assertEquals("c1", threads[0].root.id)
        assertEquals(listOf("r1", "r2"), threads[0].replies.map { it.comment.id })
        assertEquals("c1", threads[0].replies[0].directParent?.id)
        assertEquals("r1", threads[0].replies[1].directParent?.id)

        assertEquals("c2", threads[1].root.id)
        assertTrue(threads[1].replies.isEmpty())

        assertEquals("r3", threads[2].root.id)
        assertNull(threads[2].replies.firstOrNull())
    }

    @Test
    fun `search history deduplicates, promotes recent entries, and roundtrips JSON`() {
        val q1 = SearchHistoryEntry(id = "q_tech", kind = SearchHistoryKind.QUERY, targetId = "Tech", title = "Tech")
        val u1 = SearchHistoryEntry(id = "u_alice", kind = SearchHistoryKind.USER, targetId = "alice", title = "Alice", subtitle = "@alice")
        val c1 = SearchHistoryEntry(id = "c_builders", kind = SearchHistoryKind.COMMUNITY, targetId = "comm1", title = "Builders")
        val q1Repeat = SearchHistoryEntry(id = "q_tech", kind = SearchHistoryKind.QUERY, targetId = "tech", title = "tech")

        var list = emptyList<SearchHistoryEntry>()
        list = WeConnectViewModel.upsertSearchEntry(list, q1)
        list = WeConnectViewModel.upsertSearchEntry(list, u1)
        list = WeConnectViewModel.upsertSearchEntry(list, c1)
        list = WeConnectViewModel.upsertSearchEntry(list, q1Repeat)

        assertEquals(3, list.size)
        assertEquals("tech", list[0].title)
        assertEquals(SearchHistoryKind.COMMUNITY, list[1].kind)
        assertEquals(SearchHistoryKind.USER, list[2].kind)

        val encoded = WeConnectViewModel.encodeSearchHistory(list)
        val decoded = WeConnectViewModel.decodeSearchHistory(encoded)
        assertEquals(3, decoded.size)
        assertEquals(list.map { it.id }, decoded.map { it.id })
        assertEquals(list.map { it.kind }, decoded.map { it.kind })
    }

    @Test
    fun `inbox timestamp formats today yesterday and older dates in WhatsApp style`() {
        val cal = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 18, 14, 30, 0)
        }
        val nowMs = cal.timeInMillis
        val todayTs = Timestamp((nowMs - 3_600_000L) / 1000L, 0)
        val yesterdayTs = Timestamp((nowMs - 24L * 3_600_000L) / 1000L, 0)
        val olderTs = Timestamp((nowMs - 14L * 24L * 3_600_000L) / 1000L, 0)

        assertTrue(formatInboxTimestamp(todayTs, nowMs).contains(":"))
        assertEquals("Yesterday", formatInboxTimestamp(yesterdayTs, nowMs))
        assertTrue(formatInboxTimestamp(olderTs, nowMs).contains("/"))
    }

    @Test
    fun `ecdh aes-gcm end-to-end encryption roundtrip between two real users`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val crypto = WeConnectCrypto(context)
        val alice = crypto.getOrCreateDeviceIdentity("alice_test_uid")
        val bob = crypto.getOrCreateDeviceIdentity("bob_test_uid")

        assertNotEquals(alice.publicKeyBase64, bob.publicKeyBase64)

        val secretMessage = "Hello Bob! Meet at emerald plaza at 14:30."
        val encrypted = crypto.encryptDirectMessage(
            uid = "alice_test_uid",
            peerPublicKeyBase64 = bob.publicKeyBase64,
            plaintext = secretMessage
        )
        assertNotEquals(secretMessage, encrypted.ciphertextBase64)

        val decryptedByBob = crypto.decryptDirectMessage(
            uid = "bob_test_uid",
            peerOrSenderPublicKeyBase64 = encrypted.senderPublicKeyBase64,
            ciphertextBase64 = encrypted.ciphertextBase64,
            ivBase64 = encrypted.ivBase64
        )
        assertEquals(secretMessage, decryptedByBob)

        val decryptedByAlice = crypto.decryptDirectMessage(
            uid = "alice_test_uid",
            peerOrSenderPublicKeyBase64 = bob.publicKeyBase64,
            ciphertextBase64 = encrypted.ciphertextBase64,
            ivBase64 = encrypted.ivBase64
        )
        assertEquals(secretMessage, decryptedByAlice)
    }
}
