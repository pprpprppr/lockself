package com.lockself.app.ui.viewmodel

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockself.app.data.crypto.VaultCryptoManager
import com.lockself.app.data.repository.VaultRepository
import com.lockself.app.util.AppPreferences
import com.lockself.app.util.CsvFileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val repository: VaultRepository,
    private val crypto: VaultCryptoManager,
    private val preferences: AppPreferences
) : ViewModel() {

    private val _lockTimeout = MutableStateFlow(preferences.lockTimeoutSeconds)
    val lockTimeout: StateFlow<Int> = _lockTimeout.asStateFlow()

    private val _clipboardTimeout = MutableStateFlow(preferences.clearClipboardTimeoutSeconds)
    val clipboardTimeout: StateFlow<Int> = _clipboardTimeout.asStateFlow()

    private val _lockOnMinimize = MutableStateFlow(preferences.lockOnMinimize)
    val lockOnMinimize: StateFlow<Boolean> = _lockOnMinimize.asStateFlow()

    private val _lockOnScreenTimeout = MutableStateFlow(preferences.lockOnScreenTimeout)
    val lockOnScreenTimeout: StateFlow<Boolean> = _lockOnScreenTimeout.asStateFlow()

    private val _clearClipboardOnLock = MutableStateFlow(preferences.clearClipboardOnLock)
    val clearClipboardOnLock: StateFlow<Boolean> = _clearClipboardOnLock.asStateFlow()

    private val _biometricEnabled = MutableStateFlow(preferences.biometricAuthEnabled)
    val biometricEnabled: StateFlow<Boolean> = _biometricEnabled.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    // Cached CSV content across configuration changes or file picker lifecycle
    var cachedExportCsv: String? = null

    fun updateLockTimeout(seconds: Int) {
        preferences.lockTimeoutSeconds = seconds
        _lockTimeout.value = seconds
    }

    fun updateClipboardTimeout(seconds: Int) {
        preferences.clearClipboardTimeoutSeconds = seconds
        _clipboardTimeout.value = seconds
    }

    fun toggleLockOnMinimize() {
        val newVal = !preferences.lockOnMinimize
        preferences.lockOnMinimize = newVal
        _lockOnMinimize.value = newVal
    }

    fun toggleLockOnScreenTimeout() {
        val newVal = !preferences.lockOnScreenTimeout
        preferences.lockOnScreenTimeout = newVal
        _lockOnScreenTimeout.value = newVal
    }

    fun toggleClearClipboardOnLock() {
        val newVal = !preferences.clearClipboardOnLock
        preferences.clearClipboardOnLock = newVal
        _clearClipboardOnLock.value = newVal
    }

    fun toggleBiometric() {
        if (_biometricEnabled.value) {
            crypto.disableBiometricUnlock()
            preferences.biometricAuthEnabled = false
            _biometricEnabled.value = false
            _statusMessage.value = "Biometric unlock disabled"
        } else {
            val enabled = crypto.enableBiometricUnlock()
            if (enabled) {
                preferences.biometricAuthEnabled = true
                _biometricEnabled.value = true
                _statusMessage.value = "Biometric unlock enabled"
            } else {
                _statusMessage.value = "Failed to enable biometrics"
            }
        }
    }

    fun changeMasterPassword(oldPass: String, newPass: String, onResult: (Boolean) -> Unit) {
        if (newPass.length < 6) {
            _statusMessage.value = "New password must be at least 6 characters"
            onResult(false)
            return
        }
        viewModelScope.launch {
            val success = crypto.changeMasterPasswordAsync(oldPass.toCharArray(), newPass.toCharArray())
            if (success) {
                _statusMessage.value = "Master password changed successfully"
                if (_biometricEnabled.value) {
                    crypto.enableBiometricUnlock()
                }
            } else {
                _statusMessage.value = "Incorrect current master password"
            }
            onResult(success)
        }
    }

    fun exportCsv(onExported: (String) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            runCatching {
                val csv = repository.exportCsv()
                cachedExportCsv = csv
                onExported(csv)
            }.onFailure { err ->
                _statusMessage.value = "Export failed: ${err.localizedMessage ?: "Unknown error"}"
            }
            _isBusy.value = false
        }
    }

    fun exportCsvToUri(
        contentResolver: ContentResolver,
        uri: Uri,
        csvContent: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    CsvFileHelper.writeCsvTextToUri(contentResolver, uri, csvContent)
                }
            }
            cachedExportCsv = null
            _isBusy.value = false
            result.onSuccess {
                _statusMessage.value = "Backup exported successfully"
                onComplete(true, null)
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Failed to write backup file"
                _statusMessage.value = "Export failed: $msg"
                onComplete(false, msg)
            }
        }
    }

    fun importCsvFromUri(
        contentResolver: ContentResolver,
        uri: Uri,
        onComplete: (Int, String?) -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = runCatching {
                val csvText = withContext(Dispatchers.IO) {
                    CsvFileHelper.readCsvTextFromUri(contentResolver, uri)
                }
                if (csvText.isBlank()) {
                    throw IllegalArgumentException("Selected CSV file is empty")
                }
                repository.importCsv(csvText)
            }
            _isBusy.value = false
            result.onSuccess { count ->
                if (count > 0) {
                    _statusMessage.value = "Imported $count records successfully"
                } else {
                    _statusMessage.value = "No valid records found in the selected CSV file"
                }
                onComplete(count, null)
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Failed to read CSV file"
                _statusMessage.value = "Import failed: $msg"
                onComplete(0, msg)
            }
        }
    }

    fun importCsv(csvContent: String, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            runCatching {
                val count = repository.importCsv(csvContent)
                _statusMessage.value = if (count > 0) {
                    "Imported $count records successfully"
                } else {
                    "No valid records found in CSV text"
                }
                onComplete(count)
            }.onFailure {
                _statusMessage.value = "Failed to import CSV: ${it.localizedMessage ?: "Unknown error"}"
                onComplete(0)
            }
            _isBusy.value = false
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
