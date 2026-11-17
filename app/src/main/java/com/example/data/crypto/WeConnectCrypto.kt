package com.example.data.crypto

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private val Context.cryptoDataStore by preferencesDataStore(name = "weconnect_crypto_keys")

data class EncryptedPayload(
    val ciphertextBase64: String,
    val ivBase64: String,
    val senderPublicKeyBase64: String
)

data class DeviceIdentity(
    val deviceId: String,
    val publicKeyBase64: String,
    val fingerprint: String
)

class WeConnectCrypto(private val context: Context) {

    private fun privKeyPref(uid: String) = stringPreferencesKey("ec_priv_$uid")
    private fun pubKeyPref(uid: String) = stringPreferencesKey("ec_pub_$uid")
    private fun deviceIdPref(uid: String) = stringPreferencesKey("device_id_$uid")

    suspend fun getOrCreateDeviceIdentity(uid: String): DeviceIdentity {
        val prefs = context.cryptoDataStore.data.first()
        var deviceId = prefs[deviceIdPref(uid)]
        var privB64 = prefs[privKeyPref(uid)]
        var pubB64 = prefs[pubKeyPref(uid)]

        if (deviceId.isNullOrBlank() || privB64.isNullOrBlank() || pubB64.isNullOrBlank()) {
            val kpg = KeyPairGenerator.getInstance("EC")
            kpg.initialize(ECGenParameterSpec("secp256r1"))
            val keyPair = kpg.generateKeyPair()
            privB64 = Base64.encodeToString(keyPair.private.encoded, Base64.NO_WRAP)
            pubB64 = Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)
            deviceId = deviceId ?: ("dev_" + UUID.randomUUID().toString().replace("-", "").take(16))

            context.cryptoDataStore.edit { editPrefs ->
                editPrefs[deviceIdPref(uid)] = deviceId
                editPrefs[privKeyPref(uid)] = privB64
                editPrefs[pubKeyPref(uid)] = pubB64
            }
        }

        return DeviceIdentity(
            deviceId = deviceId,
            publicKeyBase64 = pubB64,
            fingerprint = computeFingerprint(pubB64)
        )
    }

    fun computeFingerprint(publicKeyBase64: String): String {
        return try {
            val bytes = Base64.decode(publicKeyBase64, Base64.NO_WRAP)
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            digest.take(16).joinToString(":") { "%02X".format(it) }
        } catch (_: Exception) {
            "UNVERIFIED"
        }
    }

    suspend fun encryptDirectMessage(
        uid: String,
        peerPublicKeyBase64: String,
        plaintext: String
    ): EncryptedPayload {
        val identity = getOrCreateDeviceIdentity(uid)
        val privKey = loadPrivateKey(uid)
        val peerPubKey = decodePublicKey(peerPublicKeyBase64)
        val sharedKey = deriveAesKey(privKey, peerPubKey)

        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, sharedKey, GCMParameterSpec(128, iv))
        val encryptedBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        return EncryptedPayload(
            ciphertextBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP),
            ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP),
            senderPublicKeyBase64 = identity.publicKeyBase64
        )
    }

    suspend fun decryptDirectMessage(
        uid: String,
        peerOrSenderPublicKeyBase64: String,
        ciphertextBase64: String,
        ivBase64: String
    ): String? {
        return try {
            val privKey = loadPrivateKey(uid)
            val otherPubKey = decodePublicKey(peerOrSenderPublicKeyBase64)
            val sharedKey = deriveAesKey(privKey, otherPubKey)

            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val cipherBytes = Base64.decode(ciphertextBase64, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, sharedKey, GCMParameterSpec(128, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun loadPrivateKey(uid: String): PrivateKey {
        val prefs = context.cryptoDataStore.data.first()
        val privB64 = prefs[privKeyPref(uid)]
            ?: throw IllegalStateException("Missing device private key")
        val bytes = Base64.decode(privB64, Base64.NO_WRAP)
        return KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(bytes))
    }

    private fun decodePublicKey(base64Key: String): PublicKey {
        val bytes = Base64.decode(base64Key, Base64.NO_WRAP)
        return KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(bytes))
    }

    private fun deriveAesKey(privateKey: PrivateKey, publicKey: PublicKey): SecretKeySpec {
        val agreement = KeyAgreement.getInstance("ECDH")
        agreement.init(privateKey)
        agreement.doPhase(publicKey, true)
        val sharedSecret = agreement.generateSecret()
        val aesKeyBytes = MessageDigest.getInstance("SHA-256").digest(sharedSecret)
        return SecretKeySpec(aesKeyBytes, "AES")
    }
}
