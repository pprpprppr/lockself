package com.lockself.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "records")
data class RecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val type: RecordType,
    val isFavorite: Boolean = false,
    val lastModified: Long = System.currentTimeMillis() / 1000,
    val uid: String = UUID.randomUUID().toString(),
    val tags: String = ""
)

@Entity(
    tableName = "fields",
    foreignKeys = [
        ForeignKey(
            entity = RecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("recordId")]
)
data class RecordFieldEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recordId: Long,
    val fieldType: FieldType,
    val fieldName: String? = null,
    val encryptedValue: String
)

@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = RecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("recordId")]
)
data class ChecklistItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recordId: Long,
    val text: String,
    val isChecked: Boolean = false,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "password_history",
    foreignKeys = [
        ForeignKey(
            entity = RecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("recordId")]
)
data class PasswordHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recordId: Long,
    val encryptedPassword: String,
    val changedAt: Long = System.currentTimeMillis() / 1000
)

data class FullRecord(
    val entity: RecordEntity,
    val fields: Map<FieldType, String> = emptyMap(),
    val customFields: List<Pair<String, String>> = emptyList(),
    val checklist: List<ChecklistItemEntity> = emptyList(),
    val history: List<PasswordHistoryEntity> = emptyList()
) {
    val id: Long get() = entity.id
    val title: String get() = entity.title
    val type: RecordType get() = entity.type
    val isFavorite: Boolean get() = entity.isFavorite
    val tagsList: List<String>
        get() = if (entity.tags.isBlank()) emptyList() else entity.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    val username: String? get() = fields[FieldType.USERNAME]
    val password: String? get() = fields[FieldType.PASSWORD]
    val website: String? get() = fields[FieldType.WEBSITE]
    val note: String? get() = fields[FieldType.NOTE]

    /**
     * BlackBerry Password Keeper subtitle prioritization logic:
     * - user • site (if both exist)
     * - user (if only username exists)
     * - site (if only website exists)
     * - •••••••• (if password exists)
     * - empty string (never show "No username" placeholder)
     */
    fun computeSubtitle(): String {
        val u = username?.takeIf { it.isNotBlank() }
        val w = website?.takeIf { it.isNotBlank() }
        return when {
            u != null && w != null -> "$u • $w"
            u != null -> u
            w != null -> w
            password?.isNotBlank() == true -> "••••••••"
            else -> ""
        }
    }
}
