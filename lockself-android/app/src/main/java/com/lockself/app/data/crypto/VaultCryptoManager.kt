package com.lockself.app.data.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VaultCryptoManager(private val context: Context) {

    private val secureRandom = SecureRandom()
    @Volatile
    private var activeVaultKey: SecretKey? = null

    var onVaultLockedListener: (() -> Unit)? = null

    companion object {
        private const val PREFS_NAME = "lockself_security_prefs"
        private const val KEY_SALT = "vault_master_salt"
        private const val KEY_VERIFIER = "vault_master_verifier"
        private const val KEY_BIOMETRIC_WRAPPED = "vault_biometric_wrapped"
        private const val KEYSTORE_ALIAS_BIOMETRIC = "lockself_biometric_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"

        private const val PBKDF2_ITERATIONS = 600000
        private const val KEY_LENGTH_BITS = 256
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val VERIFIER_PLAINTEXT = "LOCKSELF_VAULT_VERIFIER_STRING_V1"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isVaultInitialized(): Boolean {
        return prefs.contains(KEY_SALT) && prefs.contains(KEY_VERIFIER)
    }

    fun isVaultUnlocked(): Boolean {
        return activeVaultKey != null
    }

    fun lockVault() {
        activeVaultKey = null
        onVaultLockedListener?.invoke()
    }

    suspend fun setupNewVaultAsync(masterPassword: CharArray): Boolean = withContext(Dispatchers.Default) {
        setupNewVault(masterPassword)
    }

    suspend fun unlockWithPasswordAsync(masterPassword: CharArray): Boolean = withContext(Dispatchers.Default) {
        unlockWithPassword(masterPassword)
    }

    suspend fun changeMasterPasswordAsync(oldPassword: CharArray, newPassword: CharArray): Boolean = withContext(Dispatchers.Default) {
        changeMasterPassword(oldPassword, newPassword)
    }

    fun setupNewVault(masterPassword: CharArray): Boolean {
        val salt = ByteArray(32).also { secureRandom.nextBytes(it) }
        val derivedKey = deriveKey(masterPassword, salt)
        val verifierCiphertext = encryptInternal(VERIFIER_PLAINTEXT, derivedKey)

        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_VERIFIER, verifierCiphertext)
            .remove(KEY_BIOMETRIC_WRAPPED)
            .apply()

        activeVaultKey = derivedKey
        return true
    }

    fun unlockWithPassword(masterPassword: CharArray): Boolean {
        val saltBase64 = prefs.getString(KEY_SALT, null) ?: return false
        val verifierCiphertext = prefs.getString(KEY_VERIFIER, null) ?: return false
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)

        val candidateKey = deriveKey(masterPassword, salt)
        return try {
            val decrypted = decryptInternal(verifierCiphertext, candidateKey)
            if (decrypted == VERIFIER_PLAINTEXT) {
                activeVaultKey = candidateKey
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun changeMasterPassword(oldPassword: CharArray, newPassword: CharArray): Boolean {
        if (!unlockWithPassword(oldPassword)) return false
        val newSalt = ByteArray(32).also { secureRandom.nextBytes(it) }
        val newKey = deriveKey(newPassword, newSalt)
        val newVerifier = encryptInternal(VERIFIER_PLAINTEXT, newKey)

        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(newSalt, Base64.NO_WRAP))
            .putString(KEY_VERIFIER, newVerifier)
            .remove(KEY_BIOMETRIC_WRAPPED)
            .apply()

        activeVaultKey = newKey
        return true
    }

    fun encrypt(plainText: String): String {
        val key = activeVaultKey ?: throw IllegalStateException("Vault is locked! Cannot encrypt.")
        return encryptInternal(plainText, key)
    }

    fun decrypt(cipherText: String): String {
        val key = activeVaultKey ?: throw IllegalStateException("Vault is locked! Cannot decrypt.")
        return decryptInternal(cipherText, key)
    }

    private fun encryptInternal(plainText: String, secretKey: SecretKey): String {
        val iv = ByteArray(GCM_IV_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val byteBuffer = ByteBuffer.allocate(iv.size + cipherBytes.size)
        byteBuffer.put(iv)
        byteBuffer.put(cipherBytes)
        return Base64.encodeToString(byteBuffer.array(), Base64.NO_WRAP)
    }

    private fun decryptInternal(cipherText: String, secretKey: SecretKey): String {
        val fullData = Base64.decode(cipherText, Base64.NO_WRAP)
        if (fullData.size < GCM_IV_LENGTH_BYTES) throw IllegalArgumentException("Invalid ciphertext data")

        val iv = fullData.copyOfRange(0, GCM_IV_LENGTH_BYTES)
        val cipherBytes = fullData.copyOfRange(GCM_IV_LENGTH_BYTES, fullData.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val decryptedBytes = cipher.doFinal(cipherBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        return try {
            val keyBytes = factory.generateSecret(spec).encoded
            SecretKeySpec(keyBytes, "AES")
        } finally {
            spec.clearPassword()
            Arrays.fill(password, '\u0000')
        }
    }

    // --- Android KeyStore Biometric Key Wrapping ---

    fun isBiometricUnlockAvailable(): Boolean {
        return prefs.contains(KEY_BIOMETRIC_WRAPPED)
    }

    fun enableBiometricUnlock(): Boolean {
        val currentKey = activeVaultKey ?: return false
        val keyStoreKey = getOrCreateKeyStoreKey()
        val wrappedKey = encryptInternal(Base64.encodeToString(currentKey.encoded, Base64.NO_WRAP), keyStoreKey)
        prefs.edit().putString(KEY_BIOMETRIC_WRAPPED, wrappedKey).apply()
        return true
    }

    fun disableBiometricUnlock() {
        prefs.edit().remove(KEY_BIOMETRIC_WRAPPED).apply()
        runCatching {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            keyStore.deleteEntry(KEYSTORE_ALIAS_BIOMETRIC)
        }
    }

    fun unlockWithBiometric(): Boolean {
        val wrappedKey = prefs.getString(KEY_BIOMETRIC_WRAPPED, null) ?: return false
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val keyStoreKey = keyStore.getKey(KEYSTORE_ALIAS_BIOMETRIC, null) as? SecretKey ?: return false
            val rawKeyBase64 = decryptInternal(wrappedKey, keyStoreKey)
            val rawKeyBytes = Base64.decode(rawKeyBase64, Base64.NO_WRAP)
            activeVaultKey = SecretKeySpec(rawKeyBytes, "AES")
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun getOrCreateKeyStoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEYSTORE_ALIAS_BIOMETRIC, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS_BIOMETRIC,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}
