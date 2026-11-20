package com.lockself.app.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.lockself.app.data.model.ChecklistItemEntity
import com.lockself.app.data.model.PasswordHistoryEntity
import com.lockself.app.data.model.RecordEntity
import com.lockself.app.data.model.RecordFieldEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {
    @Query("SELECT * FROM records ORDER BY lastModified DESC")
    fun getAllRecords(): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE id = :id LIMIT 1")
    suspend fun getRecordById(id: Long): RecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: RecordEntity): Long

    @Update
    suspend fun updateRecord(record: RecordEntity)

    @Delete
    suspend fun deleteRecord(record: RecordEntity)

    @Query("DELETE FROM records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)

    @Query("SELECT * FROM fields WHERE recordId = :recordId")
    suspend fun getFieldsForRecordSync(recordId: Long): List<RecordFieldEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFields(fields: List<RecordFieldEntity>)

    @Query("DELETE FROM fields WHERE recordId = :recordId")
    suspend fun deleteFieldsForRecord(recordId: Long)

    @Query("SELECT * FROM checklist_items WHERE recordId = :recordId ORDER BY sortOrder ASC")
    suspend fun getChecklistForRecordSync(recordId: Long): List<ChecklistItemEntity>

    @Query("SELECT * FROM checklist_items WHERE recordId = :recordId ORDER BY sortOrder ASC")
    fun getChecklistForRecord(recordId: Long): Flow<List<ChecklistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItems(items: List<ChecklistItemEntity>)

    @Update
    suspend fun updateChecklistItem(item: ChecklistItemEntity)

    @Query("DELETE FROM checklist_items WHERE recordId = :recordId")
    suspend fun deleteChecklistForRecord(recordId: Long)

    @Query("SELECT * FROM password_history WHERE recordId = :recordId ORDER BY changedAt DESC")
    suspend fun getPasswordHistoryForRecordSync(recordId: Long): List<PasswordHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPasswordHistory(history: PasswordHistoryEntity): Long

    @Query("DELETE FROM password_history WHERE recordId = :recordId")
    suspend fun deletePasswordHistoryForRecord(recordId: Long)

    @Query("SELECT * FROM records ORDER BY lastModified DESC")
    suspend fun getAllRecordsSync(): List<RecordEntity>

    @Query("SELECT * FROM records WHERE uid = :uid LIMIT 1")
    suspend fun getRecordByUid(uid: String): RecordEntity?

    @Query("SELECT * FROM fields")
    suspend fun getAllFieldsSync(): List<RecordFieldEntity>

    @Query("SELECT * FROM checklist_items ORDER BY sortOrder ASC")
    suspend fun getAllChecklistsSync(): List<ChecklistItemEntity>

    @Query("SELECT * FROM password_history ORDER BY changedAt DESC")
    suspend fun getAllPasswordHistorySync(): List<PasswordHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecordsBatch(records: List<RecordEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFieldsBatch(fields: List<RecordFieldEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItemsBatch(items: List<ChecklistItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPasswordHistoriesBatch(histories: List<PasswordHistoryEntity>)

    @Query("SELECT COUNT(*) FROM records")
    fun getRecordCount(): Flow<Int>

    @Query("DELETE FROM records")
    suspend fun deleteAllRecords()

    @Transaction
    suspend fun replaceFields(recordId: Long, fields: List<RecordFieldEntity>) {
        deleteFieldsForRecord(recordId)
        insertFields(fields)
    }

    @Transaction
    suspend fun replaceChecklist(recordId: Long, items: List<ChecklistItemEntity>) {
        deleteChecklistForRecord(recordId)
        insertChecklistItems(items)
    }

    @Transaction
    suspend fun importFullRecordTransaction(
        record: RecordEntity,
        fields: List<RecordFieldEntity>,
        checklist: List<ChecklistItemEntity>,
        history: List<PasswordHistoryEntity>
    ): Long {
        val existing = getRecordByUid(record.uid)
        val targetId = if (existing != null) {
            val updated = record.copy(id = existing.id)
            updateRecord(updated)
            existing.id
        } else {
            insertRecord(record)
        }

        deleteFieldsForRecord(targetId)
        if (fields.isNotEmpty()) {
            insertFields(fields.map { it.copy(recordId = targetId) })
        }

        deleteChecklistForRecord(targetId)
        if (checklist.isNotEmpty()) {
            insertChecklistItems(checklist.map { it.copy(recordId = targetId) })
        }

        if (history.isNotEmpty()) {
            deletePasswordHistoryForRecord(targetId)
            for (h in history) {
                insertPasswordHistory(h.copy(recordId = targetId))
            }
        }

        return targetId
    }

    @Transaction
    suspend fun importAllRecordsBatch(
        bundles: List<RecordImportBundle>
    ): Int {
        var count = 0
        for (bundle in bundles) {
            importFullRecordTransaction(
                bundle.record,
                bundle.fields,
                bundle.checklist,
                bundle.history
            )
            count++
        }
        return count
    }
}

data class RecordImportBundle(
    val record: RecordEntity,
    val fields: List<RecordFieldEntity>,
    val checklist: List<ChecklistItemEntity>,
    val history: List<PasswordHistoryEntity>
)
