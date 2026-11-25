package com.lockself.app

import android.app.Application
import android.net.Uri
import com.lockself.app.data.crypto.VaultCryptoManager
import com.lockself.app.data.database.LockSelfDatabase
import com.lockself.app.data.repository.VaultRepository
import com.lockself.app.util.AppPreferences
import com.lockself.app.util.ClipboardHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PendingImportData(
    val uri: Uri? = null,
    val rawText: String? = null,
    val displayName: String? = null
)

class LockSelfApp : Application() {

    lateinit var database: LockSelfDatabase
        private set

    lateinit var cryptoManager: VaultCryptoManager
        private set

    lateinit var preferences: AppPreferences
        private set

    lateinit var clipboardHelper: ClipboardHelper
        private set

    lateinit var repository: VaultRepository
        private set

    // Transient flags to prevent false auto-lock during file picker or biometric dialog interactions
    @Volatile
    var isSafPickerActive: Boolean = false

    @Volatile
    var isBiometricPromptActive: Boolean = false

    private val _pendingImport = MutableStateFlow<PendingImportData?>(null)
    val pendingImport: StateFlow<PendingImportData?> = _pendingImport.asStateFlow()

    fun setPendingImport(data: PendingImportData?) {
        _pendingImport.value = data
    }

    fun clearPendingImport() {
        _pendingImport.value = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        preferences = AppPreferences(this)
        clipboardHelper = ClipboardHelper(this, preferences)
        cryptoManager = VaultCryptoManager(this)
        database = LockSelfDatabase.getInstance(this)
        repository = VaultRepository(database.recordDao(), cryptoManager)

        cryptoManager.onVaultLockedListener = {
            repository.clearDecryptionCache()
        }
    }

    companion object {
        lateinit var instance: LockSelfApp
            private set
    }
}
