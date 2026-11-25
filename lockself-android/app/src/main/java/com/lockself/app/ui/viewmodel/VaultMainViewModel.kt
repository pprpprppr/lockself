package com.lockself.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockself.app.data.crypto.VaultCryptoManager
import com.lockself.app.data.model.FullRecord
import com.lockself.app.data.model.RecordType
import com.lockself.app.data.repository.VaultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DrawerCategory {
    ALL,
    FAVORITES,
    PASSWORDS,
    NOTES,
    LISTS
}

class VaultMainViewModel(
    private val repository: VaultRepository,
    private val crypto: VaultCryptoManager
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(DrawerCategory.ALL)
    val selectedCategory: StateFlow<DrawerCategory> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val allRecords: StateFlow<List<FullRecord>> = repository.getAllFullRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredRecords: StateFlow<List<FullRecord>> = combine(
        allRecords,
        _selectedCategory,
        _searchQuery
    ) { records, category, query ->
        if (records.isEmpty()) return@combine emptyList()
        val q = query.trim().lowercase()

        records.filter { record ->
            val matchesCategory = when (category) {
                DrawerCategory.ALL -> true
                DrawerCategory.FAVORITES -> record.isFavorite
                DrawerCategory.PASSWORDS -> record.type == RecordType.PASSWORD
                DrawerCategory.NOTES -> record.type == RecordType.NOTE
                DrawerCategory.LISTS -> record.type == RecordType.LIST
            }
            if (!matchesCategory) return@filter false

            if (q.isEmpty()) {
                true
            } else {
                record.title.lowercase().contains(q) ||
                    (record.username?.lowercase()?.contains(q) == true) ||
                    (record.website?.lowercase()?.contains(q) == true) ||
                    (record.note?.lowercase()?.contains(q) == true) ||
                    record.tagsList.any { it.lowercase().contains(q) }
            }
        }
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Category Count Badges matching drawer items
    val categoryCounts: StateFlow<Map<DrawerCategory, Int>> = allRecords.map { records ->
        mapOf(
            DrawerCategory.ALL to records.size,
            DrawerCategory.FAVORITES to records.count { it.isFavorite },
            DrawerCategory.PASSWORDS to records.count { it.type == RecordType.PASSWORD },
            DrawerCategory.NOTES to records.count { it.type == RecordType.NOTE },
            DrawerCategory.LISTS to records.count { it.type == RecordType.LIST }
        )
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun selectCategory(category: DrawerCategory) {
        _selectedCategory.value = category
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavorite(recordId: Long) {
        viewModelScope.launch {
            runCatching {
                repository.toggleFavorite(recordId)
            }
        }
    }

    fun lockVault() {
        crypto.lockVault()
    }
}
