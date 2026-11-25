package com.lockself.app.data.repository

import com.lockself.app.data.crypto.VaultCryptoManager
import com.lockself.app.data.database.RecordDao
import com.lockself.app.data.database.RecordImportBundle
import com.lockself.app.data.model.ChecklistItemEntity
import com.lockself.app.data.model.FieldType
import com.lockself.app.data.model.FullRecord
import com.lockself.app.data.model.PasswordHistoryEntity
import com.lockself.app.data.model.RecordEntity
import com.lockself.app.data.model.RecordFieldEntity
import com.lockself.app.data.model.RecordType
import com.lockself.app.util.VaultCsvParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class VaultRepository(
    private val dao: RecordDao,
    private val crypto: VaultCryptoManager
) {

    // High-performance in-memory cache of decrypted records
    // Avoids decrypting the same fields repeatedly during UI recompositions, searches, and filters
    private val decryptedCache = ConcurrentHashMap<Long, CachedDecryptedRecord>()

    private data class CachedDecryptedRecord(
        val lastModified: Long,
        val fullRecord: FullRecord
    )

    fun clearDecryptionCache() {
        decryptedCache.clear()
    }

    /**
     * Efficiently observes all records.
     * Uses batch queries (4 queries total) rather than N+1 queries,
     * and utilizes in-memory decryption caching to avoid expensive PBKDF2/AES-GCM repeats.
     */
    fun getAllFullRecords(): Flow<List<FullRecord>> {
        return dao.getAllRecords().map { entities ->
            if (entities.isEmpty()) {
                return@map emptyList<FullRecord>()
            }

            // Batch fetch all child fields, checklists, and histories in 3 queries
            val allFieldsByRecord = dao.getAllFieldsSync().groupBy { it.recordId }
            val allChecklistsByRecord = dao.getAllChecklistsSync().groupBy { it.recordId }
            val allHistoryByRecord = dao.getAllPasswordHistorySync().groupBy { it.recordId }

            entities.map { entity ->
                val cached = decryptedCache[entity.id]
                if (cached != null && cached.lastModified == entity.lastModified) {
                    cached.fullRecord
                } else {
                    val decrypted = decryptRecordInternal(
                        entity = entity,
                        encryptedFields = allFieldsByRecord[entity.id] ?: emptyList(),
                        checklist = allChecklistsByRecord[entity.id] ?: emptyList(),
                        encryptedHistory = allHistoryByRecord[entity.id] ?: emptyList()
                    )
                    decryptedCache[entity.id] = CachedDecryptedRecord(entity.lastModified, decrypted)
                    decrypted
                }
            }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getFullRecordById(id: Long): FullRecord = withContext(Dispatchers.IO) {
        val cached = decryptedCache[id]
        val recordEntity = dao.getRecordById(id) ?: throw IllegalArgumentException("Record not found: $id")

        if (cached != null && cached.lastModified == recordEntity.lastModified) {
            return@withContext cached.fullRecord
        }

        val encryptedFields = dao.getFieldsForRecordSync(id)
        val checklist = dao.getChecklistForRecordSync(id)
        val encryptedHistory = dao.getPasswordHistoryForRecordSync(id)

        val decrypted = decryptRecordInternal(
            entity = recordEntity,
            encryptedFields = encryptedFields,
            checklist = checklist,
            encryptedHistory = encryptedHistory
        )
        decryptedCache[id] = CachedDecryptedRecord(recordEntity.lastModified, decrypted)
        decrypted
    }

    private fun decryptRecordInternal(
        entity: RecordEntity,
        encryptedFields: List<RecordFieldEntity>,
        checklist: List<ChecklistItemEntity>,
        encryptedHistory: List<PasswordHistoryEntity>
    ): FullRecord {
        val decryptedFields = mutableMapOf<FieldType, String>()
        val customFields = mutableListOf<Pair<String, String>>()

        for (field in encryptedFields) {
            val decryptedVal = if (crypto.isVaultUnlocked()) {
                runCatching { crypto.decrypt(field.encryptedValue) }.getOrDefault(field.encryptedValue)
            } else {
                field.encryptedValue
            }

            if (field.fieldType == FieldType.CUSTOM) {
                customFields.add((field.fieldName ?: "Custom") to decryptedVal)
            } else {
                decryptedFields[field.fieldType] = decryptedVal
            }
        }

        val decryptedHistory = encryptedHistory.map { hist ->
            val dec = if (crypto.isVaultUnlocked()) {
                runCatching { crypto.decrypt(hist.encryptedPassword) }.getOrDefault(hist.encryptedPassword)
            } else {
                hist.encryptedPassword
            }
            hist.copy(encryptedPassword = dec)
        }

        return FullRecord(
            entity = entity,
            fields = decryptedFields,
            customFields = customFields,
            checklist = checklist,
            history = decryptedHistory
        )
    }

    suspend fun saveRecord(
        title: String,
        type: RecordType,
        isFavorite: Boolean,
        tags: List<String>,
        fields: Map<FieldType, String>,
        customFields: List<Pair<String, String>> = emptyList(),
        checklist: List<String> = emptyList(),
        existingId: Long? = null
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis() / 1000
        val recordId: Long

        if (existingId == null || existingId == 0L) {
            val entity = RecordEntity(
                title = title,
                type = type,
                isFavorite = isFavorite,
                lastModified = now,
                uid = UUID.randomUUID().toString(),
                tags = tags.joinToString(", ")
            )
            recordId = dao.insertRecord(entity)
        } else {
            val existing = dao.getRecordById(existingId) ?: throw IllegalArgumentException("Record not found: $existingId")

            // Password history tracking: if password was changed, preserve the old one
            val oldFields = dao.getFieldsForRecordSync(existingId)
            val oldPasswordField = oldFields.find { it.fieldType == FieldType.PASSWORD }
            val newPassword = fields[FieldType.PASSWORD]
            if (oldPasswordField != null && newPassword != null) {
                val oldDecrypted = runCatching { crypto.decrypt(oldPasswordField.encryptedValue) }.getOrNull()
                if (oldDecrypted != null && oldDecrypted != newPassword) {
                    dao.insertPasswordHistory(
                        PasswordHistoryEntity(
                            recordId = existingId,
                            encryptedPassword = oldPasswordField.encryptedValue,
                            changedAt = now
                        )
                    )
                }
            }

            val updatedEntity = existing.copy(
                title = title,
                type = type,
                isFavorite = isFavorite,
                lastModified = now,
                tags = tags.joinToString(", ")
            )
            dao.updateRecord(updatedEntity)
            recordId = existingId
        }

        // Encrypt and persist fields
        val fieldEntities = mutableListOf<RecordFieldEntity>()
        for ((fType, value) in fields) {
            if (value.isNotEmpty()) {
                fieldEntities.add(
                    RecordFieldEntity(
                        recordId = recordId,
                        fieldType = fType,
                        fieldName = null,
                        encryptedValue = crypto.encrypt(value)
                    )
                )
            }
        }
        for ((cName, cVal) in customFields) {
            if (cVal.isNotEmpty()) {
                fieldEntities.add(
                    RecordFieldEntity(
                        recordId = recordId,
                        fieldType = FieldType.CUSTOM,
                        fieldName = cName,
                        encryptedValue = crypto.encrypt(cVal)
                    )
                )
            }
        }
        dao.replaceFields(recordId, fieldEntities)

        // Persist checklist
        if (type == RecordType.LIST) {
            val existingItems = if (existingId != null) dao.getChecklistForRecordSync(existingId) else emptyList()
            val existingMap = existingItems.associateBy { it.text }
            val checklistEntities = checklist.mapIndexed { index, text ->
                ChecklistItemEntity(
                    recordId = recordId,
                    text = text,
                    isChecked = existingMap[text]?.isChecked ?: false,
                    sortOrder = index
                )
            }
            dao.replaceChecklist(recordId, checklistEntities)
        }

        decryptedCache.remove(recordId)
        recordId
    }

    suspend fun toggleFavorite(recordId: Long): Unit = withContext(Dispatchers.IO) {
        val record = dao.getRecordById(recordId) ?: return@withContext
        dao.updateRecord(record.copy(isFavorite = !record.isFavorite))
        decryptedCache.remove(recordId)
        Unit
    }

    suspend fun toggleChecklistItem(item: ChecklistItemEntity): Unit = withContext(Dispatchers.IO) {
        dao.updateChecklistItem(item.copy(isChecked = !item.isChecked))
        decryptedCache.remove(item.recordId)
        Unit
    }

    suspend fun deleteRecord(recordId: Long): Unit = withContext(Dispatchers.IO) {
        dao.deleteRecordById(recordId)
        decryptedCache.remove(recordId)
        Unit
    }

    suspend fun exportCsv(): String = withContext(Dispatchers.IO) {
        val records = getAllFullRecords().first()
        VaultCsvParser.exportToCsv(records)
    }

    /**
     * Imports records from a CSV backup in a single atomic transaction.
     * Preserves:
     * - Checklist items with their checked state and sort order
     * - Historical passwords (encrypted with the vault key)
     * - Original record lastModified timestamps and UIDs
     * - Custom key-value fields and security questions
     */
    suspend fun importCsv(csvContent: String): Int = withContext(Dispatchers.IO) {
        if (!crypto.isVaultUnlocked()) {
            throw IllegalStateException("Vault is locked! Please unlock your vault before importing.")
        }

        val parsedRecords = VaultCsvParser.parseCsv(csvContent)
        if (parsedRecords.isEmpty()) return@withContext 0

        val bundles = mutableListOf<RecordImportBundle>()

        for (parsed in parsedRecords) {
            val entity = RecordEntity(
                id = 0,
                title = parsed.title,
                type = parsed.type,
                isFavorite = parsed.isFavorite,
                lastModified = parsed.entity.lastModified,
                uid = parsed.entity.uid,
                tags = parsed.entity.tags
            )

            val fieldEntities = mutableListOf<RecordFieldEntity>()
            for ((fType, value) in parsed.fields) {
                if (value.isNotEmpty()) {
                    fieldEntities.add(
                        RecordFieldEntity(
                            recordId = 0,
                            fieldType = fType,
                            fieldName = null,
                            encryptedValue = crypto.encrypt(value)
                        )
                    )
                }
            }
            for ((cName, cVal) in parsed.customFields) {
                if (cVal.isNotEmpty()) {
                    fieldEntities.add(
                        RecordFieldEntity(
                            recordId = 0,
                            fieldType = FieldType.CUSTOM,
                            fieldName = cName,
                            encryptedValue = crypto.encrypt(cVal)
                        )
                    )
                }
            }

            // Preserve checklist items with checked state and sort order
            val checklistEntities = parsed.checklist.mapIndexed { idx, item ->
                ChecklistItemEntity(
                    recordId = 0,
                    text = item.text,
                    isChecked = item.isChecked,
                    sortOrder = if (item.sortOrder != 0) item.sortOrder else idx
                )
            }

            // Encrypt and preserve historical passwords
            val historyEntities = parsed.history.map { hist ->
                PasswordHistoryEntity(
                    recordId = 0,
                    encryptedPassword = crypto.encrypt(hist.encryptedPassword),
                    changedAt = hist.changedAt
                )
            }

            bundles.add(
                RecordImportBundle(
                    record = entity,
                    fields = fieldEntities,
                    checklist = checklistEntities,
                    history = historyEntities
                )
            )
        }

        val importedCount = dao.importAllRecordsBatch(bundles)
        clearDecryptionCache()
        importedCount
    }
}
