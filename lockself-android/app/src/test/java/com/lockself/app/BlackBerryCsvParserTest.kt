package com.lockself.app

import com.lockself.app.data.model.ChecklistItemEntity
import com.lockself.app.data.model.FieldType
import com.lockself.app.data.model.FullRecord
import com.lockself.app.data.model.PasswordHistoryEntity
import com.lockself.app.data.model.RecordEntity
import com.lockself.app.data.model.RecordType
import com.lockself.app.util.BlackBerryCsvParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlackBerryCsvParserTest {

    @Test
    fun testParseStandardBlackBerryCsv() {
        val csv = """url,username,password,extra,name,grouping,fav,customFields,lastModifiedTime,uid,usernameLabel,passwordLabel,websiteLabel,notesLabel,passwordSetDate,flags,imageIndex,dataVersion
"https://github.com","octocat","s3cr3tP@ss","Developer account","GitHub","","1","",1700000000,"uuid-1","","","","","1700000000","0","0","1"
"""
        val records = BlackBerryCsvParser.parseCsv(csv)
        assertEquals(1, records.size)
        val r = records[0]
        assertEquals("GitHub", r.title)
        assertEquals(RecordType.PASSWORD, r.type)
        assertTrue(r.isFavorite)
        assertEquals("octocat", r.username)
        assertEquals("s3cr3tP@ss", r.password)
        assertEquals("https://github.com", r.website)
        assertEquals("Developer account", r.note)
    }

    @Test
    fun testParseHeaderlessCsv() {
        val csv = """"https://google.com","user@gmail.com","p@ss123","Main email","Google","","0","",1700000000,"uid-2","","","","","1700000000","0","0","1"
"""
        val records = BlackBerryCsvParser.parseCsv(csv)
        assertEquals(1, records.size)
        assertEquals("Google", records[0].title)
        assertEquals("user@gmail.com", records[0].username)
        assertFalse(records[0].isFavorite)
    }

    @Test
    fun testParseCustomFieldsWithChecklist() {
        val jsonCf = """{"cf":[{"cft":28675,"chk_list":{"chk_label":"Buy milk","chk_checked":true,"chk_order":0}},{"cft":28675,"chk_list":{"chk_label":"Read book","chk_checked":false,"chk_order":1}}]}"""
        val escapedCf = "\"" + jsonCf.replace("\"", "\"\"") + "\""
        val csv = """url,username,password,extra,name,grouping,fav,customFields,lastModifiedTime,uid,usernameLabel,passwordLabel,websiteLabel,notesLabel,passwordSetDate,flags,imageIndex,dataVersion
"","","","","Groceries","list","0",$escapedCf,1700000000,"uid-list","","","","","1700000000","0","0","1"
"""
        val records = BlackBerryCsvParser.parseCsv(csv)
        assertEquals(1, records.size)
        val r = records[0]
        assertEquals("Groceries", r.title)
        assertEquals(RecordType.LIST, r.type)
        assertEquals(2, r.checklist.size)
        assertEquals("Buy milk", r.checklist[0].text)
        assertTrue(r.checklist[0].isChecked)
        assertEquals("Read book", r.checklist[1].text)
        assertFalse(r.checklist[1].isChecked)
    }

    @Test
    fun testParseCustomFieldsWithPasswordHistoryAndSecurityQuestion() {
        val jsonCf = """{"cf":[{"cft":28674,"pqf":{"pP":"oldPass123","pmd":1690000000}},{"cft":28673,"sqf":{"q":"Favorite color?","a":"Blue"}}]}"""
        val escapedCf = "\"" + jsonCf.replace("\"", "\"\"") + "\""
        val csv = """url,username,password,extra,name,grouping,fav,customFields,lastModifiedTime,uid,usernameLabel,passwordLabel,websiteLabel,notesLabel,passwordSetDate,flags,imageIndex,dataVersion
"site.com","admin","newPass","","Service","","0",$escapedCf,1700000000,"uid-srv","","","","","1700000000","0","0","1"
"""
        val records = BlackBerryCsvParser.parseCsv(csv)
        assertEquals(1, records.size)
        val r = records[0]
        assertEquals(1, r.history.size)
        assertEquals("oldPass123", r.history[0].encryptedPassword)
        assertEquals(1690000000L, r.history[0].changedAt)

        assertEquals(1, r.customFields.size)
        assertEquals("Favorite color?", r.customFields[0].first)
        assertEquals("Blue", r.customFields[0].second)
    }

    @Test
    fun testParseSyntheticBlackBerryFormatUnquotedFields() {
        val csv = """url,username,password,extra,name,grouping,fav,customFields,lastModifiedTime,uid,usernameLabel,passwordLabel,websiteLabel,notesLabel,passwordSetDate,flags,imageIndex,dataVersion
https://bank.com,john_doe,secret99,Checking,Bank,note,1,,1700000000,uid-bank,,,,,1700000000,0,0,1
"""
        val records = BlackBerryCsvParser.parseCsv(csv)
        assertEquals(1, records.size)
        val r = records[0]
        assertEquals("Bank", r.title)
        assertEquals(RecordType.NOTE, r.type)
        assertTrue(r.isFavorite)
        assertEquals("john_doe", r.username)
    }

    @Test
    fun testParseTenDigitSecondsTimestamps() {
        val csv = """"","user","pass","","Item","","0","",1712345678,"uid-ts","","","","","1712345678","0","0","1"
"""
        val records = BlackBerryCsvParser.parseCsv(csv)
        assertEquals(1, records.size)
        assertEquals(1712345678L, records[0].entity.lastModified)
    }

    @Test
    fun testParseMultiLineNotesAndEscapedQuotes() {
        val csv = """"site.com","user","pass","Line 1
Line 2 with ""quotes""
Line 3","Complex Note","","0","",1700000000,"uid-ml","","","","","1700000000","0","0","1"
"""
        val records = BlackBerryCsvParser.parseCsv(csv)
        assertEquals(1, records.size)
        val note = records[0].note
        assertNotNull(note)
        assertTrue(note!!.contains("Line 1"))
        assertTrue(note.contains("Line 2 with \"quotes\""))
        assertTrue(note.contains("Line 3"))
    }

    @Test
    fun testParseActualBlackBerryBackupFile() {
        val csv = """url,username,password,extra,name,grouping,fav,customFields,lastModifiedTime,uid,usernameLabel,passwordLabel,websiteLabel,notesLabel,passwordSetDate,flags,imageIndex,dataVersion
"https://my.blackberry.com","bbuser@rim.com","B3rry#2024","BlackBerry ID Account","BlackBerry ID","","1","",1700001000,"rim-001","","","","","1700001000","0","0","1"
"""
        val records = BlackBerryCsvParser.parseCsv(csv)
        assertEquals(1, records.size)
        assertEquals("BlackBerry ID", records[0].title)
        assertEquals("bbuser@rim.com", records[0].username)
        assertEquals("B3rry#2024", records[0].password)
    }

    @Test
    fun testBlackBerryCsvExportAndImportRoundTrip() {
        val originalRecord = FullRecord(
            entity = RecordEntity(
                id = 1,
                title = "RoundTrip Service",
                type = RecordType.PASSWORD,
                isFavorite = true,
                lastModified = 1710000000L,
                uid = "rt-uuid-123"
            ),
            fields = mapOf(
                FieldType.USERNAME to "roundtrip_user",
                FieldType.PASSWORD to "complexP@ssw0rd!",
                FieldType.WEBSITE to "https://roundtrip.io",
                FieldType.NOTE to "Roundtrip test note"
            ),
            customFields = listOf("PIN" to "1234"),
            checklist = listOf(
                ChecklistItemEntity(id = 1, recordId = 1, text = "Task A", isChecked = true, sortOrder = 0)
            ),
            history = listOf(
                PasswordHistoryEntity(id = 1, recordId = 1, encryptedPassword = "oldPassOne", changedAt = 1705000000L)
            )
        )

        val exportedCsv = BlackBerryCsvParser.exportToCsv(listOf(originalRecord))
        assertTrue(exportedCsv.contains("RoundTrip Service"))
        assertTrue(exportedCsv.contains("roundtrip_user"))
        assertTrue(exportedCsv.contains("complexP@ssw0rd!"))

        val imported = BlackBerryCsvParser.parseCsv(exportedCsv)
        assertEquals(1, imported.size)
        val r = imported[0]
        assertEquals("RoundTrip Service", r.title)
        assertEquals("roundtrip_user", r.username)
        assertEquals("complexP@ssw0rd!", r.password)
        assertEquals("https://roundtrip.io", r.website)
        assertEquals("Roundtrip test note", r.note)
        assertTrue(r.isFavorite)
        assertEquals(1, r.checklist.size)
        assertEquals("Task A", r.checklist[0].text)
        assertTrue(r.checklist[0].isChecked)
        assertEquals(1, r.history.size)
        assertEquals("oldPassOne", r.history[0].encryptedPassword)
    }

    @Test
    fun testParseCsvWithUtf8Bom() {
        val csvWithBom = "\uFEFFurl,username,password,extra,name,grouping,fav,customFields,lastModifiedTime,uid,usernameLabel,passwordLabel,websiteLabel,notesLabel,passwordSetDate,flags,imageIndex,dataVersion\n" +
            "\"https://bom-test.com\",\"bom_user\",\"bom_pass\",\"BOM note\",\"BOM Title\",\"\",\"0\",\"\",1710000000,\"uid-bom\",\"\",\"\",\"\",\"\",\"1710000000\",\"0\",\"0\",\"1\"\n"
        val records = BlackBerryCsvParser.parseCsv(csvWithBom)
        assertEquals(1, records.size)
        assertEquals("BOM Title", records[0].title)
        assertEquals("bom_user", records[0].username)
        assertEquals("https://bom-test.com", records[0].website)
    }

    @Test
    fun testParseLegacyBlackBerryFiveColumnFormat() {
        // Old BlackBerry Desktop Software export: URL, Username, Password, Notes, Title
        val csv = "\"https://old-bb.com\",\"old_user\",\"old_pass123\",\"Old secret notes\",\"Old BlackBerry Record\"\n"
        val records = BlackBerryCsvParser.parseCsv(csv)
        assertEquals(1, records.size)
        val r = records[0]
        assertEquals("Old BlackBerry Record", r.title)
        assertEquals("old_user", r.username)
        assertEquals("old_pass123", r.password)
        assertEquals("https://old-bb.com", r.website)
        assertEquals("Old secret notes", r.note)
    }

    @Test
    fun testParseBlackBerryDesktopManagerCustomHeaders() {
        val csv = "\"Title\",\"User ID\",\"Password\",\"Web Address\",\"Notes\",\"Folder\"\n" +
            "\"Work VPN\",\"corp\\alice\",\"VpnSecr3t!\",\"https://vpn.corp.com\",\"Server: vpn1\",\"Corporate\"\n"
        val records = BlackBerryCsvParser.parseCsv(csv)
        assertEquals(1, records.size)
        val r = records[0]
        assertEquals("Work VPN", r.title)
        assertEquals("corp\\alice", r.username)
        assertEquals("VpnSecr3t!", r.password)
        assertEquals("https://vpn.corp.com", r.website)
        assertEquals("Server: vpn1", r.note)
        assertEquals("Corporate", r.entity.tags)
    }

    @Test
    fun testParseBackslashEscapedQuotesInCsv() {
        val csv = """"site.com","user","pass","Note with \"backslash\" quotes","Escaped Record","","0","",1710000000,"uid-esc","","","","","1710000000","0","0","1"\n"""
        val records = BlackBerryCsvParser.parseCsv(csv)
        assertEquals(1, records.size)
        val note = records[0].note
        assertNotNull(note)
        assertTrue(note!!.contains("\"backslash\""))
    }

    @Test
    fun testParseChecklistWithNumericAndStringBooleans() {
        val cfJson = """{"cf":[{"cft":"28675","chk_list":{"chk_label":"Item Alpha","chk_checked":"true","chk_order":"1"}},{"cft":28675,"chk_list":{"chk_label":"Item Beta","chk_checked":1,"chk_order":2}},{"cft":28675,"chk_list":{"chk_label":"Item Gamma","chk_checked":false,"chk_order":3}}]}"""
        val csv = """"","","","","Grocery List","list","0","${cfJson.replace("\"", "\"\"")}",1710000000,"uid-chk","","","","","1710000000","0","0","1"\n"""
        val records = BlackBerryCsvParser.parseCsv(csv)
        assertEquals(1, records.size)
        val r = records[0]
        assertEquals(RecordType.LIST, r.type)
        assertEquals(3, r.checklist.size)
        assertEquals("Item Alpha", r.checklist[0].text)
        assertTrue(r.checklist[0].isChecked)
        assertEquals("Item Beta", r.checklist[1].text)
        assertTrue(r.checklist[1].isChecked)
        assertEquals("Item Gamma", r.checklist[2].text)
        assertFalse(r.checklist[2].isChecked)
    }

    @Test
    fun testParseMultipleHistoricalPasswordsFromCsv() {
        val cfJson = """{"cf":[{"cft":28674,"pqf":{"pP":"historicalPass1","pmd":1600000000}},{"cft":28674,"pqf":{"pP":"historicalPass2","pmd":1650000000}}]}"""
        val csv = """"example.com","user","currentPass","","Account With History","","0","${cfJson.replace("\"", "\"\"")}",1710000000,"uid-hist","","","","","1710000000","0","0","1"\n"""
        val records = BlackBerryCsvParser.parseCsv(csv)
        assertEquals(1, records.size)
        val r = records[0]
        assertEquals(2, r.history.size)
        assertEquals("historicalPass1", r.history[0].encryptedPassword)
        assertEquals(1600000000L, r.history[0].changedAt)
        assertEquals("historicalPass2", r.history[1].encryptedPassword)
        assertEquals(1650000000L, r.history[1].changedAt)
    }
}
