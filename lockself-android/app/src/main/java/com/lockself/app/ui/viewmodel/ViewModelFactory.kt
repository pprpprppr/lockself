package com.lockself.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lockself.app.data.crypto.VaultCryptoManager
import com.lockself.app.data.repository.VaultRepository
import com.lockself.app.util.AppPreferences
import com.lockself.app.util.ClipboardHelper

class ViewModelFactory(
    private val repository: VaultRepository,
    private val crypto: VaultCryptoManager,
    private val preferences: AppPreferences,
    private val clipboardHelper: ClipboardHelper,
    private val recordId: Long? = null,
    private val initialTypeStr: String = "PASSWORD"
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(VaultMainViewModel::class.java) -> {
                VaultMainViewModel(repository, crypto) as T
            }
            modelClass.isAssignableFrom(RecordDetailViewModel::class.java) -> {
                RecordDetailViewModel(repository, clipboardHelper, recordId ?: 0L) as T
            }
            modelClass.isAssignableFrom(EditRecordViewModel::class.java) -> {
                EditRecordViewModel(repository, recordId, initialTypeStr) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(repository, crypto, preferences) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
