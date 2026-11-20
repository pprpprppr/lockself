package com.lockself.app.data.database

import androidx.room.TypeConverter
import com.lockself.app.data.model.FieldType
import com.lockself.app.data.model.RecordType

class Converters {
    @TypeConverter
    fun fromRecordType(value: RecordType): String = value.name

    @TypeConverter
    fun toRecordType(value: String): RecordType = runCatching { RecordType.valueOf(value) }.getOrDefault(RecordType.PASSWORD)

    @TypeConverter
    fun fromFieldType(value: FieldType): String = value.name

    @TypeConverter
    fun toFieldType(value: String): FieldType = runCatching { FieldType.valueOf(value) }.getOrDefault(FieldType.CUSTOM)
}
