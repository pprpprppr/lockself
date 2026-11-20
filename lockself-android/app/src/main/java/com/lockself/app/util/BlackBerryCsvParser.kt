package com.lockself.app.util

import com.lockself.app.data.model.ChecklistItemEntity
import com.lockself.app.data.model.FieldType
import com.lockself.app.data.model.FullRecord
import com.lockself.app.data.model.PasswordHistoryEntity
import com.lockself.app.data.model.RecordEntity
import com.lockself.app.data.model.RecordType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.io.BufferedReader
import java.io.StringReader
import java.util.UUID

object BlackBerryCsvParser {

    val HEADERS = listOf(
        "url", "username", "password", "extra", "name", "grouping", "fav",
        "customFields", "lastModifiedTime", "uid", "usernameLabel", "passwordLabel",
        "websiteLabel", "notesLabel", "passwordSetDate", "flags", "imageIndex", "dataVersion"
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun exportToCsv(records: List<FullRecord>): String {
        val sb = StringBuilder()
        sb.append(HEADERS.joinToString(",")).append("\n")

        for (record in records) {
            val url = record.website ?: ""
            val username = record.username ?: ""
            val password = record.password ?: ""
            val extra = record.note ?: ""
            val name = record.title
            val grouping = when (record.type) {
                RecordType.NOTE -> "note"
                RecordType.LIST -> "list"
                RecordType.PASSWORD -> ""
            }
            val fav = if (record.isFavorite) "1" else "0"
            val customFields = buildCustomFieldsJson(record)
            val lastModified = record.entity.lastModified.toString()
            val uid = record.entity.uid
            val usernameLabel = ""
            val passwordLabel = ""
            val websiteLabel = ""
            val notesLabel = ""
            val passwordSetDate = lastModified
            val flags = "0"
            val imageIndex = "0"
            val dataVersion = "1"

            val row = listOf(
                escapeCsv(url),
                escapeCsv(username),
                escapeCsv(password),
                escapeCsv(extra),
                escapeCsv(name),
                escapeCsv(grouping),
                escapeCsv(fav),
                escapeCsv(customFields),
                escapeCsv(lastModified),
                escapeCsv(uid),
                escapeCsv(usernameLabel),
                escapeCsv(passwordLabel),
                escapeCsv(websiteLabel),
                escapeCsv(notesLabel),
                escapeCsv(passwordSetDate),
                escapeCsv(flags),
                escapeCsv(imageIndex),
                escapeCsv(dataVersion)
            )
            sb.append(row.joinToString(",")).append("\n")
        }
        return sb.toString()
    }

    private data class CsvColumnMapping(
        val urlIndex: Int = -1,
        val usernameIndex: Int = -1,
        val passwordIndex: Int = -1,
        val extraIndex: Int = -1,
        val nameIndex: Int = -1,
        val groupingIndex: Int = -1,
        val favIndex: Int = -1,
        val customFieldsIndex: Int = -1,
        val lastModifiedIndex: Int = -1,
        val uidIndex: Int = -1
    )

    fun parseCsv(csvContent: String): List<FullRecord> {
        // Strip UTF-8 BOM if present
        val cleanContent = csvContent.removePrefix("\uFEFF").trimStart()
        if (cleanContent.isBlank()) return emptyList()

        val rows = parseRawCsvRows(cleanContent)
        if (rows.isEmpty()) return emptyList()

        val firstRow = rows.first()
        val isHeader = isHeaderRow(firstRow)

        val mapping = if (isHeader) {
            buildHeaderMapping(firstRow)
        } else {
            buildFallbackMapping(firstRow)
        }

        val startIndex = if (isHeader) 1 else 0
        val records = mutableListOf<FullRecord>()

        for (i in startIndex until rows.size) {
            val row = rows[i]
            if (row.isEmpty() || row.all { it.isBlank() }) continue

            fun getCell(index: Int): String {
                if (index < 0 || index >= row.size) return ""
                return row[index].trim()
            }

            val url = getCell(mapping.urlIndex)
            val username = getCell(mapping.usernameIndex)
            val password = getCell(mapping.passwordIndex)
            val extra = getCell(mapping.extraIndex)
            val rawName = getCell(mapping.nameIndex)
            val rawGrouping = getCell(mapping.groupingIndex)
            val groupingLower = rawGrouping.lowercase()
            val favVal = getCell(mapping.favIndex)
            val fav = favVal == "1" || favVal.equals("true", ignoreCase = true)
            val customFieldsStr = getCell(mapping.customFieldsIndex)
            val lastModifiedRaw = getCell(mapping.lastModifiedIndex).toLongOrNull() ?: (System.currentTimeMillis() / 1000)
            val lastModified = if (lastModifiedRaw > 100000000000L) lastModifiedRaw / 1000 else lastModifiedRaw
            val uid = getCell(mapping.uidIndex).takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()

            val customFields = mutableListOf<Pair<String, String>>()
            val checklist = mutableListOf<ChecklistItemEntity>()
            val history = mutableListOf<PasswordHistoryEntity>()

            if (customFieldsStr.isNotBlank()) {
                parseCustomFieldsJson(customFieldsStr, customFields, checklist, history)
            }

            // Fallback for Title/Name if empty
            val name = when {
                rawName.isNotBlank() -> rawName
                url.isNotBlank() -> url
                username.isNotBlank() -> username
                else -> "Untitled"
            }

            // Determine Record Type
            val type = when {
                groupingLower.contains("list") || checklist.isNotEmpty() -> RecordType.LIST
                groupingLower.contains("note") -> RecordType.NOTE
                password.isBlank() && username.isBlank() && extra.isNotBlank() && url.isBlank() -> RecordType.NOTE
                else -> RecordType.PASSWORD
            }

            val fields = mutableMapOf<FieldType, String>()
            if (username.isNotEmpty()) fields[FieldType.USERNAME] = username
            if (password.isNotEmpty()) fields[FieldType.PASSWORD] = password
            if (url.isNotEmpty()) fields[FieldType.WEBSITE] = url
            if (extra.isNotEmpty()) fields[FieldType.NOTE] = extra

            val entity = RecordEntity(
                id = 0,
                title = name,
                type = type,
                isFavorite = fav,
                lastModified = lastModified,
                uid = uid,
                tags = if (rawGrouping.isNotEmpty() && groupingLower != "note" && groupingLower != "list" && groupingLower != "password") rawGrouping else ""
            )

            records.add(
                FullRecord(
                    entity = entity,
                    fields = fields,
                    customFields = customFields,
                    checklist = checklist,
                    history = history
                )
            )
        }
        return records
    }

    private fun isHeaderRow(row: List<String>): Boolean {
        if (row.size < 3) return false
        val cleanCells = row.map { it.trim().lowercase().removePrefix("\uFEFF").replace("\"", "").replace("_", " ") }

        // Fast check: Standard BlackBerry 18-column header has "url" at col 0 and "username" at col 1
        if (cleanCells.getOrNull(0) == "url" && cleanCells.getOrNull(1) == "username") {
            return true
        }

        val headerKeywords = setOf(
            "url", "website", "web site", "web address",
            "username", "user id", "userid", "login",
            "password", "pwd",
            "extra", "notes", "note", "comments",
            "name", "title", "description",
            "grouping", "category", "folder",
            "fav", "favorite",
            "customfields", "custom fields",
            "lastmodifiedtime", "last modified"
        )

        // Require at least 3 matching header column names to avoid false-positive on data rows
        val matchCount = cleanCells.count { cell -> headerKeywords.contains(cell) }
        return matchCount >= 3
    }

    private fun buildHeaderMapping(row: List<String>): CsvColumnMapping {
        var urlIdx = -1
        var userIdx = -1
        var passIdx = -1
        var extraIdx = -1
        var nameIdx = -1
        var groupIdx = -1
        var favIdx = -1
        var customIdx = -1
        var modifiedIdx = -1
        var uidIdx = -1

        for ((i, rawCol) in row.withIndex()) {
            val col = rawCol.trim().lowercase().replace("\"", "").replace("_", " ")
            when {
                nameIdx == -1 && (col == "name" || col == "title" || col == "description" || col == "account") -> nameIdx = i
                urlIdx == -1 && (col == "url" || col == "website" || col == "web site" || col == "web address" || col == "web" || col == "site") -> urlIdx = i
                userIdx == -1 && (col == "username" || col == "user id" || col == "userid" || col == "user" || col == "login" || col == "email") -> userIdx = i
                passIdx == -1 && (col == "password" || col == "pwd" || col == "pass") -> passIdx = i
                extraIdx == -1 && (col == "extra" || col == "notes" || col == "note" || col == "comments" || col == "details") -> extraIdx = i
                groupIdx == -1 && (col == "grouping" || col == "category" || col == "folder" || col == "group") -> groupIdx = i
                favIdx == -1 && (col == "fav" || col == "favorite" || col == "starred") -> favIdx = i
                customIdx == -1 && (col == "customfields" || col == "custom fields" || col == "custom") -> customIdx = i
                modifiedIdx == -1 && (col == "lastmodifiedtime" || col == "last modified" || col == "modified" || col == "timestamp" || col == "date") -> modifiedIdx = i
                uidIdx == -1 && (col == "uid" || col == "id" || col == "uuid") -> uidIdx = i
            }
        }

        // If standard 18-column names were matched or fallback to defaults
        if (row.size >= 18 && nameIdx == -1) nameIdx = 4
        if (row.size >= 18 && urlIdx == -1) urlIdx = 0
        if (row.size >= 18 && userIdx == -1) userIdx = 1
        if (row.size >= 18 && passIdx == -1) passIdx = 2
        if (row.size >= 18 && extraIdx == -1) extraIdx = 3
        if (row.size >= 18 && groupIdx == -1) groupIdx = 5
        if (row.size >= 18 && favIdx == -1) favIdx = 6
        if (row.size >= 18 && customIdx == -1) customIdx = 7
        if (row.size >= 18 && modifiedIdx == -1) modifiedIdx = 8
        if (row.size >= 18 && uidIdx == -1) uidIdx = 9

        return CsvColumnMapping(
            urlIndex = urlIdx,
            usernameIndex = userIdx,
            passwordIndex = passIdx,
            extraIndex = extraIdx,
            nameIndex = nameIdx,
            groupingIndex = groupIdx,
            favIndex = favIdx,
            customFieldsIndex = customIdx,
            lastModifiedIndex = modifiedIdx,
            uidIndex = uidIdx
        )
    }

    private fun buildFallbackMapping(row: List<String>): CsvColumnMapping {
        if (row.size >= 18) {
            // Standard BlackBerry Password Keeper 18-column structure
            return CsvColumnMapping(
                urlIndex = 0,
                usernameIndex = 1,
                passwordIndex = 2,
                extraIndex = 3,
                nameIndex = 4,
                groupingIndex = 5,
                favIndex = 6,
                customFieldsIndex = 7,
                lastModifiedIndex = 8,
                uidIndex = 9
            )
        }

        // Legacy format heuristic (e.g. 5 columns: url, username, password, extra, name OR title, username, password, url, extra)
        val col0 = row.getOrNull(0)?.lowercase() ?: ""
        val looksLikeUrl = col0.startsWith("http://") || col0.startsWith("https://") || col0.startsWith("www.") || col0.contains(".com")

        return if (looksLikeUrl) {
            // BlackBerry legacy: url, username, password, extra, name
            CsvColumnMapping(
                urlIndex = 0,
                usernameIndex = 1,
                passwordIndex = 2,
                extraIndex = 3,
                nameIndex = 4
            )
        } else {
            // Generic legacy: title, username, password, url, notes
            CsvColumnMapping(
                nameIndex = 0,
                usernameIndex = 1,
                passwordIndex = 2,
                urlIndex = 3,
                extraIndex = 4
            )
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun parseRawCsvRows(content: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val reader = BufferedReader(StringReader(content))
        var currentRow = mutableListOf<String>()
        var currentCell = StringBuilder()
        var inQuotes = false
        var cellHasQuotes = false

        var chInt = reader.read()
        while (chInt != -1) {
            val ch = chInt.toChar()
            when {
                ch == '\"' -> {
                    if (inQuotes) {
                        reader.mark(1)
                        val nextCh = reader.read()
                        if (nextCh != -1 && nextCh.toChar() == '\"') {
                            currentCell.append('\"')
                        } else {
                            inQuotes = false
                            cellHasQuotes = true
                            if (nextCh != -1) reader.reset()
                        }
                    } else {
                        inQuotes = true
                        cellHasQuotes = true
                    }
                }
                ch == '\\' && inQuotes -> {
                    reader.mark(1)
                    val nextCh = reader.read()
                    if (nextCh != -1 && nextCh.toChar() == '\"') {
                        currentCell.append('\"')
                    } else {
                        currentCell.append('\\')
                        if (nextCh != -1) reader.reset()
                    }
                }
                ch == ',' && !inQuotes -> {
                    val cellText = if (cellHasQuotes) currentCell.toString().trim() else currentCell.toString().trim()
                    currentRow.add(cellText)
                    currentCell = StringBuilder()
                    cellHasQuotes = false
                }
                (ch == '\n' || ch == '\r') && !inQuotes -> {
                    if (ch == '\r') {
                        reader.mark(1)
                        val next = reader.read()
                        if (next != -1 && next.toChar() != '\n') reader.reset()
                    }
                    val cellText = if (cellHasQuotes) currentCell.toString().trim() else currentCell.toString().trim()
                    currentRow.add(cellText)
                    rows.add(currentRow)
                    currentRow = mutableListOf()
                    currentCell = StringBuilder()
                    cellHasQuotes = false
                }
                else -> {
                    if (!cellHasQuotes || inQuotes) {
                        currentCell.append(ch)
                    }
                }
            }
            chInt = reader.read()
        }

        if (currentCell.isNotEmpty() || currentRow.isNotEmpty() || cellHasQuotes) {
            val cellText = if (cellHasQuotes) currentCell.toString().trim() else currentCell.toString().trim()
            currentRow.add(cellText)
            rows.add(currentRow)
        }
        return rows
    }

    private fun buildCustomFieldsJson(record: FullRecord): String {
        if (record.checklist.isEmpty() && record.customFields.isEmpty() && record.history.isEmpty()) {
            return ""
        }

        val cfArray = buildJsonArray {
            // Checklist items (type 28675)
            record.checklist.forEach { item ->
                add(buildJsonObject {
                    put("cft", 28675)
                    put("chk_list", buildJsonObject {
                        put("chk_label", item.text)
                        put("chk_checked", item.isChecked)
                        put("chk_order", item.sortOrder)
                    })
                })
            }

            // Historical Passwords (type 28674)
            record.history.forEach { hist ->
                add(buildJsonObject {
                    put("cft", 28674)
                    put("pqf", buildJsonObject {
                        put("pP", hist.encryptedPassword)
                        put("pmd", hist.changedAt)
                    })
                })
            }

            // Custom fields (type 28673 for security question / key-value)
            record.customFields.forEach { (k, v) ->
                add(buildJsonObject {
                    put("cft", 28673)
                    put("sqf", buildJsonObject {
                        put("q", k)
                        put("a", v)
                    })
                })
            }
        }

        val root = buildJsonObject {
            put("cf", cfArray)
        }
        return root.toString()
    }

    private fun parseCustomFieldsJson(
        jsonStr: String,
        customFields: MutableList<Pair<String, String>>,
        checklist: MutableList<ChecklistItemEntity>,
        history: MutableList<PasswordHistoryEntity>
    ) {
        val trimmed = jsonStr.trim()
        if (trimmed.isBlank() || (!trimmed.startsWith("{") && !trimmed.startsWith("["))) return

        runCatching {
            val rootElement = json.parseToJsonElement(trimmed)
            val cfArray: JsonArray? = when {
                rootElement is JsonObject && rootElement.containsKey("cf") -> rootElement["cf"]?.jsonArray
                rootElement is JsonArray -> rootElement
                else -> null
            }

            cfArray?.forEach { elem ->
                if (elem is JsonObject) {
                    val cftRaw = elem["cft"]?.jsonPrimitive?.contentOrNull
                    val cft = cftRaw?.toIntOrNull() ?: 0

                    when {
                        elem.containsKey("chk_list") || cft == 28675 -> {
                            val chk = elem["chk_list"]?.jsonObject
                            if (chk != null) {
                                val label = chk["chk_label"]?.jsonPrimitive?.contentOrNull ?: ""
                                val checkedStr = chk["chk_checked"]?.jsonPrimitive?.contentOrNull
                                val checked = checkedStr?.equals("true", ignoreCase = true) == true || checkedStr == "1"
                                val order = chk["chk_order"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: checklist.size
                                if (label.isNotBlank()) {
                                    checklist.add(ChecklistItemEntity(text = label, isChecked = checked, sortOrder = order, recordId = 0))
                                }
                            }
                        }
                        elem.containsKey("pqf") || cft == 28674 -> {
                            val pqf = elem["pqf"]?.jsonObject
                            if (pqf != null) {
                                val pass = pqf["pP"]?.jsonPrimitive?.contentOrNull ?: ""
                                val tsRaw = pqf["pmd"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: (System.currentTimeMillis() / 1000)
                                val ts = if (tsRaw > 100000000000L) tsRaw / 1000 else tsRaw
                                if (pass.isNotEmpty()) {
                                    history.add(PasswordHistoryEntity(recordId = 0, encryptedPassword = pass, changedAt = ts))
                                }
                            }
                        }
                        elem.containsKey("sqf") || cft == 28673 -> {
                            val sqf = elem["sqf"]?.jsonObject
                            if (sqf != null) {
                                val q = sqf["q"]?.jsonPrimitive?.contentOrNull ?: ""
                                val a = sqf["a"]?.jsonPrimitive?.contentOrNull ?: ""
                                if (q.isNotBlank()) {
                                    customFields.add(q to a)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
