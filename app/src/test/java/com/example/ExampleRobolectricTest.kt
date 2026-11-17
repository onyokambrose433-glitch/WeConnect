package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.crypto.WeConnectCrypto
import com.example.data.repository.AuthProfileRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
