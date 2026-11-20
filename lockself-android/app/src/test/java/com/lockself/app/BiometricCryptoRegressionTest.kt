package com.lockself.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.fail
import org.junit.Test
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class BiometricCryptoRegressionTest {

    private val secureRandom = SecureRandom()

    private fun generateAesKey(): SecretKey {
        val kg = KeyGenerator.getInstance("AES")
        kg.init(256, secureRandom)
        return kg.generateKey()
    }

    private fun encryptAesGcm(plainText: String, key: SecretKey): String {
        val iv = ByteArray(12).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val buffer = ByteBuffer.allocate(iv.size + cipherBytes.size)
        buffer.put(iv)
        buffer.put(cipherBytes)
        return Base64.getEncoder().encodeToString(buffer.array())
    }

    private fun decryptAesGcm(cipherText: String, key: SecretKey): String {
        val fullData = Base64.getDecoder().decode(cipherText)
        val iv = fullData.copyOfRange(0, 12)
        val cipherBytes = fullData.copyOfRange(12, fullData.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        val decrypted = cipher.doFinal(cipherBytes)
        return String(decrypted, Charsets.UTF_8)
    }

    @Test
    fun testBiometricKeyWrapAndRestorationFlow() {
        val masterKey = generateAesKey()
        val keyStoreKey = generateAesKey()

        val masterKeyBase64 = Base64.getEncoder().encodeToString(masterKey.encoded)
        val wrappedKeyCiphertext = encryptAesGcm(masterKeyBase64, keyStoreKey)

        assertNotEquals(masterKeyBase64, wrappedKeyCiphertext)

        val unwrappedKeyBase64 = decryptAesGcm(wrappedKeyCiphertext, keyStoreKey)
        assertEquals(masterKeyBase64, unwrappedKeyBase64)

        val restoredKey = SecretKeySpec(Base64.getDecoder().decode(unwrappedKeyBase64), "AES")
        assertEquals(masterKey, restoredKey)

        // Verify data encrypted with masterKey can be decrypted with restoredKey
        val secretData = "SuperSecretVaultPayload"
        val dataCiphertext = encryptAesGcm(secretData, masterKey)
        val decryptedData = decryptAesGcm(dataCiphertext, restoredKey)
        assertEquals(secretData, decryptedData)
    }

    @Test
    fun testBiometricRestorationFailsWithWrongKeyStoreKey() {
        val masterKey = generateAesKey()
        val correctKeyStoreKey = generateAesKey()
        val wrongKeyStoreKey = generateAesKey()

        val masterKeyBase64 = Base64.getEncoder().encodeToString(masterKey.encoded)
        val wrapped = encryptAesGcm(masterKeyBase64, correctKeyStoreKey)

        try {
            decryptAesGcm(wrapped, wrongKeyStoreKey)
            fail("Expected AEADBadTagException with wrong key")
        } catch (_: AEADBadTagException) {
            // Expected
        }
    }

    @Test
    fun testBiometricRestorationFailsWithTamperedKey() {
        val masterKey = generateAesKey()
        val keyStoreKey = generateAesKey()

        val masterKeyBase64 = Base64.getEncoder().encodeToString(masterKey.encoded)
        val wrapped = encryptAesGcm(masterKeyBase64, keyStoreKey)

        val bytes = Base64.getDecoder().decode(wrapped)
        bytes[bytes.size - 1] = (bytes[bytes.size - 1].toInt() xor 0xFF).toByte()
        val tamperedWrapped = Base64.getEncoder().encodeToString(bytes)

        try {
            decryptAesGcm(tamperedWrapped, keyStoreKey)
            fail("Expected AEADBadTagException on tampered ciphertext")
        } catch (_: AEADBadTagException) {
            // Expected
        }
    }
}
