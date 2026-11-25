package com.lockself.app.util

import java.text.Normalizer
import kotlin.math.abs

object PasswordStrengthMeter {

    enum class WarningReason {
        NONE,
        TOO_SHORT,
        TOO_COMMON
    }

    data class StrengthResult(
        val score: Int,
        val warning: WarningReason
    )

    private val FORBIDDEN_PASSWORDS = setOf(
        "password", "123456", "12345678", "qwerty", "admin", "welcome", "password123"
    )

    fun evaluate(password: String): StrengthResult {
        if (password.isEmpty()) {
            return StrengthResult(0, WarningReason.NONE)
        }

        val normalized = Normalizer.normalize(password, Normalizer.Form.NFC)
        if (FORBIDDEN_PASSWORDS.contains(normalized.lowercase())) {
            return StrengthResult(0, WarningReason.TOO_COMMON)
        }

        val codePointCount = normalized.codePointCount(0, normalized.length)
        if (codePointCount < 6) {
            val score = (codePointCount * 6).coerceIn(0, 100)
            return StrengthResult(score, WarningReason.TOO_SHORT)
        }

        var upper = 0
        var lower = 0
        var digits = 0
        var symbols = 0
        var middleDigitsOrSymbols = 0

        var consecutiveUpper = 0
        var consecutiveLower = 0
        var consecutiveDigits = 0
        var consecutiveSymbols = 0
        var sequentialCount = 0
        var repeatCount = 0

        var prevType = 0 // 1: upper, 2: lower, 3: digit, 4: symbol
        var prevCode = -1
        var prevPrevCode = -1

        var index = 0
        while (index < normalized.length) {
            val cp = normalized.codePointAt(index)
            val isFirstOrLast = (index == 0 || index + Character.charCount(cp) >= normalized.length)

            when {
                Character.isUpperCase(cp) -> {
                    upper++
                    if (prevType == 1) consecutiveUpper++
                    prevType = 1
                }
                Character.isLowerCase(cp) -> {
                    lower++
                    if (prevType == 2) consecutiveLower++
                    prevType = 2
                }
                Character.isDigit(cp) -> {
                    digits++
                    if (!isFirstOrLast) middleDigitsOrSymbols++
                    if (prevType == 3) consecutiveDigits++
                    prevType = 3
                }
                else -> {
                    symbols++
                    if (!isFirstOrLast) middleDigitsOrSymbols++
                    if (prevType == 4) consecutiveSymbols++
                    prevType = 4
                }
            }

            if (prevCode != -1 && prevPrevCode != -1) {
                val diff1 = abs(prevCode - cp)
                val diff2 = abs(prevPrevCode - prevCode)
                if (diff1 == 1 && diff2 == 1) sequentialCount++
                if (diff1 == 0 || diff2 == 0) repeatCount++
            }

            prevPrevCode = prevCode
            prevCode = cp
            index += Character.charCount(cp)
        }

        var rawScore = codePointCount * 4
        var requirementCount = 0

        if (upper > 0) {
            rawScore += (codePointCount - upper) * 2
            requirementCount++
        }
        if (lower > 0) {
            rawScore += (codePointCount - lower) * 2
            requirementCount++
        }
        if (digits > 0) {
            if (digits != codePointCount) rawScore += digits * 3
            requirementCount++
        }
        if (symbols > 0) {
            rawScore += symbols * 5
            requirementCount++
        }
        if (middleDigitsOrSymbols > 0) {
            rawScore += middleDigitsOrSymbols * 2
        }
        if (codePointCount >= 8 && requirementCount >= 3) {
            rawScore += (requirementCount + 1) * 2
        }

        // Penalties
        if (upper + lower == codePointCount) rawScore -= codePointCount // Letters only
        if (digits == codePointCount) rawScore -= codePointCount        // Numbers only
        if (symbols == codePointCount) rawScore -= codePointCount      // Symbols only

        rawScore -= consecutiveUpper
        rawScore -= consecutiveLower
        rawScore -= (consecutiveDigits * 2)
        rawScore -= (consecutiveSymbols * 4)
        rawScore -= (sequentialCount * 3)
        rawScore -= (repeatCount * 5)

        val finalScore = rawScore.coerceIn(0, 100)
        return StrengthResult(finalScore, WarningReason.NONE)
    }
}
