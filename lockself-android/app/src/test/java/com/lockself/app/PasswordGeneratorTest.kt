package com.lockself.app

import com.lockself.app.util.PasswordGenerator
import com.lockself.app.util.PasswordStrengthMeter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordGeneratorTest {

    @Test
    fun testGenerateLength() {
        for (len in 6..24) {
            val pass = PasswordGenerator.generatePassword(length = len)
            assertEquals(len, pass.length)
        }
    }

    @Test
    fun testIncludesAllPools() {
        val pass = PasswordGenerator.generatePassword(
            length = 16,
            includeLetters = true,
            includeNumbers = true,
            includeSymbols = true
        )
        val hasLetter = pass.any { it.isLetter() }
        val hasDigit = pass.any { it.isDigit() }
        val hasSymbol = pass.any { "!@#$%^&*()-_=+[]{}|;:,.<>?".contains(it) }

        assertTrue("Should include letters", hasLetter)
        assertTrue("Should include numbers", hasDigit)
        assertTrue("Should include symbols", hasSymbol)
    }

    @Test
    fun testExcludeAmbiguous() {
        val pass = PasswordGenerator.generatePassword(
            length = 12,
            includeLetters = false,
            includeNumbers = true,
            includeSymbols = false
        )
        assertTrue("Should be numeric only", pass.all { it.isDigit() })
    }

    @Test
    fun testPasswordStrengthEvaluation() {
        val shortResult = PasswordStrengthMeter.evaluate("abc")
        assertEquals(PasswordStrengthMeter.WarningReason.TOO_SHORT, shortResult.warning)

        val commonResult = PasswordStrengthMeter.evaluate("password")
        assertEquals(PasswordStrengthMeter.WarningReason.TOO_COMMON, commonResult.warning)

        val strongResult = PasswordStrengthMeter.evaluate("K9#mQ!8zP@2xL$")
        assertEquals(PasswordStrengthMeter.WarningReason.NONE, strongResult.warning)
        assertTrue("Expected score >= 80 for strong password", strongResult.score >= 80)
    }

    @Test
    fun testGenerateBestCandidate() {
        val pass = PasswordGenerator.generatePassword(length = 14, candidateCount = 5)
        val score = PasswordStrengthMeter.evaluate(pass).score
        assertTrue("Best candidate score should be >= 60", score >= 60)
    }
}
