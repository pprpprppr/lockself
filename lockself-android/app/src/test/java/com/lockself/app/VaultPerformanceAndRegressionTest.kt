package com.lockself.app

import com.lockself.app.data.model.ChecklistItemEntity
import com.lockself.app.data.model.FieldType
import com.lockself.app.data.model.FullRecord
import com.lockself.app.data.model.PasswordHistoryEntity
import com.lockself.app.data.model.RecordEntity
import com.lockself.app.data.model.RecordType
import com.lockself.app.util.BlackBerryCsvParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class VaultPerformanceAndRegressionTest {

    private val secureRandom = SecureRandom()

    @Test
    fun testSubtitlePrioritizationLogic() {
        val baseEntity = RecordEntity(title = "Test", type = RecordType.PASSWORD)

        // 1. Both username and website
        val recBoth = FullRecord(
            entity = baseEntity,
            fields = mapOf(
                FieldType.USERNAME to "alice",
                FieldType.WEBSITE to "example.com",
                FieldType.PASSWORD to "secret"
            )
        )
        assertEquals("alice • example.com", recBoth.computeSubtitle())

        // 2. Only username
        val recUserOnly = FullRecord(
            entity = baseEntity,
            fields = mapOf(FieldType.USERNAME to "alice", FieldType.PASSWORD to "secret")
        )
        assertEquals("alice", recUserOnly.computeSubtitle())

        // 3. Only website
        val recSiteOnly = FullRecord(
            entity = baseEntity,
            fields = mapOf(FieldType.WEBSITE to "example.com", FieldType.PASSWORD to "secret")
        )
        assertEquals("example.com", recSiteOnly.computeSubtitle())

        // 4. Only password -> ••••••••
        val recPassOnly = FullRecord(
            entity = baseEntity,
            fields = mapOf(FieldType.PASSWORD to "secret")
        )
        assertEquals("••••••••", recPassOnly.computeSubtitle())

        // 5. Neither -> empty
        val recEmpty = FullRecord(entity = baseEntity, fields = emptyMap())
        assertEquals("", recEmpty.computeSubtitle())
    }

    @Test
    fun testCsvParserHandlesWhitespaceAndCustomCapitalizedHeaders() {
        val csv = """ "NAME" , "USER ID" , "PASSWORD" , "WEB ADDRESS" , "EXTRA"
"Work Email","employee@company.com","EmpPass!2024","https://mail.company.com","Exchange Server"
"""
        val records = BlackBerryCsvParser.parseCsv(csv)
        assertEquals(1, records.size)
        val r = records[0]
        assertEquals("Work Email", r.title)
        assertEquals("employee@company.com", r.username)
        assertEquals("EmpPass!2024", r.password)
        assertEquals("https://mail.company.com", r.website)
        assertEquals("Exchange Server", r.note)
    }

    @Test
    fun testCsvParserPreservesMultipleChecklistStatesAndOrders() {
        val cfJson = """{"cf":[
            {"cft":28675,"chk_list":{"chk_label":"Deploy DB","chk_checked":true,"chk_order":0}},
            {"cft":28675,"chk_list":{"chk_label":"Run Migrations","chk_checked":true,"chk_order":1}},
            {"cft":28675,"chk_list":{"chk_label":"Notify Users","chk_checked":false,"chk_order":2}}
        ]}"""
        val csv = """"","","","","Release Checklist","list","0","${cfJson.replace("\"", "\"\"")}",1715000000,"uid-rel","","","","","1715000000","0","0","1"\n"""
        val records = BlackBerryCsvParser.parseCsv(csv)
        assertEquals(1, records.size)
        val r = records[0]
        assertEquals(RecordType.LIST, r.type)
        assertEquals(3, r.checklist.size)

        assertEquals("Deploy DB", r.checklist[0].text)
        assertTrue(r.checklist[0].isChecked)
        assertEquals(0, r.checklist[0].sortOrder)

        assertEquals("Run Migrations", r.checklist[1].text)
        assertTrue(r.checklist[1].isChecked)
        assertEquals(1, r.checklist[1].sortOrder)

        assertEquals("Notify Users", r.checklist[2].text)
        assertFalse(r.checklist[2].isChecked)
        assertEquals(2, r.checklist[2].sortOrder)
    }

    @Test
    fun testCoroutinesAsyncCryptoOperations() = runBlocking {
        val testPassword = "MyStrongMasterPassword#2026".toCharArray()
        val salt = ByteArray(32).also { secureRandom.nextBytes(it) }

        // Test PBKDF2 derivation on background thread
        val key = withContext(Dispatchers.Default) {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = PBEKeySpec(testPassword, salt, 10000, 256)
            val keyBytes = factory.generateSecret(spec).encoded
            SecretKeySpec(keyBytes, "AES")
        }
        assertNotNull(key)

        // Test AES-256-GCM encryption & decryption
        val plainText = "Secret BlackBerry Data 12345"
        val iv = ByteArray(12).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val buffer = ByteBuffer.allocate(iv.size + cipherBytes.size)
        buffer.put(iv)
        buffer.put(cipherBytes)
        val ciphertextB64 = Base64.getEncoder().encodeToString(buffer.array())

        // Decrypt
        val fullData = Base64.getDecoder().decode(ciphertextB64)
        val decIv = fullData.copyOfRange(0, 12)
        val decCipherBytes = fullData.copyOfRange(12, fullData.size)

        val decCipher = Cipher.getInstance("AES/GCM/NoPadding")
        decCipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, decIv))
        val decrypted = String(decCipher.doFinal(decCipherBytes), Charsets.UTF_8)

        assertEquals(plainText, decrypted)
    }
}
