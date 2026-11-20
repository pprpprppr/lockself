package com.lockself.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockself.app.data.model.FieldType
import com.lockself.app.data.model.RecordType
import com.lockself.app.data.repository.VaultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditRecordViewModel(
    private val repository: VaultRepository,
    private val recordId: Long?,
    initialTypeStr: String
) : ViewModel() {

    val recordType: RecordType = runCatching { RecordType.valueOf(initialTypeStr) }.getOrDefault(RecordType.PASSWORD)

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _website = MutableStateFlow("")
    val website: StateFlow<String> = _website.asStateFlow()

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note.asStateFlow()

    private val _tags = MutableStateFlow("")
    val tags: StateFlow<String> = _tags.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _customFields = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val customFields: StateFlow<List<Pair<String, String>>> = _customFields.asStateFlow()

    private val _checklistItems = MutableStateFlow<List<String>>(emptyList())
    val checklistItems: StateFlow<List<String>> = _checklistItems.asStateFlow()

    private val _hasChanges = MutableStateFlow(false)
    val hasChanges: StateFlow<Boolean> = _hasChanges.asStateFlow()

    private var initialSnapshot: String = ""

    init {
        if (recordId != null && recordId > 0L) {
            viewModelScope.launch {
                runCatching {
                    val record = repository.getFullRecordById(recordId)
                    _title.value = record.title
                    _username.value = record.username ?: ""
                    _password.value = record.password ?: ""
                    _website.value = record.website ?: ""
                    _note.value = record.note ?: ""
                    _tags.value = record.entity.tags
                    _isFavorite.value = record.isFavorite
                    _customFields.value = record.customFields
                    _checklistItems.value = record.checklist.map { it.text }
                    initialSnapshot = computeCurrentSnapshot()
                }
            }
        } else {
            initialSnapshot = computeCurrentSnapshot()
        }
    }

    private fun computeCurrentSnapshot(): String {
        return "${_title.value}|${_username.value}|${_password.value}|${_website.value}|${_note.value}|${_tags.value}|${_isFavorite.value}|${_customFields.value}|${_checklistItems.value}"
    }

    private fun checkChanges() {
        _hasChanges.value = (computeCurrentSnapshot() != initialSnapshot)
    }

    fun onTitleChange(v: String) { _title.value = v; checkChanges() }
    fun onUsernameChange(v: String) { _username.value = v; checkChanges() }
    fun onPasswordChange(v: String) { _password.value = v; checkChanges() }
    fun onWebsiteChange(v: String) { _website.value = v; checkChanges() }
    fun onNoteChange(v: String) { _note.value = v; checkChanges() }
    fun onTagsChange(v: String) { _tags.value = v; checkChanges() }
    fun onFavoriteToggle() { _isFavorite.value = !_isFavorite.value; checkChanges() }

    fun addCustomField(name: String, value: String) {
        if (name.isNotBlank()) {
            _customFields.value = _customFields.value + (name to value)
            checkChanges()
        }
    }

    fun removeCustomField(index: Int) {
        if (index in _customFields.value.indices) {
            _customFields.value = _customFields.value.toMutableList().also { it.removeAt(index) }
            checkChanges()
        }
    }

    fun addChecklistItem(text: String) {
        if (text.isNotBlank()) {
            _checklistItems.value = _checklistItems.value + text.trim()
            checkChanges()
        }
    }

    fun removeChecklistItem(index: Int) {
        if (index in _checklistItems.value.indices) {
            _checklistItems.value = _checklistItems.value.toMutableList().also { it.removeAt(index) }
            checkChanges()
        }
    }

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun saveRecord(onSuccess: (Long) -> Unit) {
        val finalTitle = _title.value.ifBlank { "Untitled" }
        viewModelScope.launch {
            runCatching {
                val fieldsMap = mutableMapOf<FieldType, String>()
                if (_username.value.isNotBlank()) fieldsMap[FieldType.USERNAME] = _username.value
                if (_password.value.isNotBlank()) fieldsMap[FieldType.PASSWORD] = _password.value
                if (_website.value.isNotBlank()) fieldsMap[FieldType.WEBSITE] = _website.value
                if (_note.value.isNotBlank()) fieldsMap[FieldType.NOTE] = _note.value

                val tagsList = _tags.value.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                repository.saveRecord(
                    title = finalTitle,
                    type = recordType,
                    isFavorite = _isFavorite.value,
                    tags = tagsList,
                    fields = fieldsMap,
                    customFields = _customFields.value,
                    checklist = _checklistItems.value,
                    existingId = recordId
                )
            }.onSuccess { savedId ->
                onSuccess(savedId)
            }.onFailure { err ->
                _errorMessage.value = "Failed to save record: ${err.localizedMessage}"
            }
        }
    }
}
