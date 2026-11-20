package com.lockself.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockself.app.data.model.ChecklistItemEntity
import com.lockself.app.data.model.FullRecord
import com.lockself.app.data.repository.VaultRepository
import com.lockself.app.util.ClipboardHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecordDetailViewModel(
    private val repository: VaultRepository,
    private val clipboardHelper: ClipboardHelper,
    private val recordId: Long
) : ViewModel() {

    private val _record = MutableStateFlow<FullRecord?>(null)
    val record: StateFlow<FullRecord?> = _record.asStateFlow()

    private val _isPasswordVisible = MutableStateFlow(false)
    val isPasswordVisible: StateFlow<Boolean> = _isPasswordVisible.asStateFlow()

    private val _copiedMessage = MutableStateFlow<String?>(null)
    val copiedMessage: StateFlow<String?> = _copiedMessage.asStateFlow()

    init {
        loadRecord()
    }

    fun loadRecord() {
        viewModelScope.launch {
            runCatching {
                val fullRecord = repository.getFullRecordById(recordId)
                _record.value = fullRecord
            }
        }
    }

    fun togglePasswordVisibility() {
        _isPasswordVisible.value = !_isPasswordVisible.value
    }

    fun copyToClipboard(label: String, value: String) {
        if (value.isNotEmpty()) {
            val copied = clipboardHelper.copyToClipboard(label, value)
            if (copied) {
                _copiedMessage.value = "$label copied to clipboard"
            }
        }
    }

    fun clearCopiedMessage() {
        _copiedMessage.value = null
    }

    fun toggleChecklistItem(item: ChecklistItemEntity) {
        viewModelScope.launch {
            runCatching {
                repository.toggleChecklistItem(item)
                loadRecord()
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            runCatching {
                repository.toggleFavorite(recordId)
                loadRecord()
            }
        }
    }

    fun deleteRecord(onDeleted: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                repository.deleteRecord(recordId)
                onDeleted()
            }
        }
    }
}
