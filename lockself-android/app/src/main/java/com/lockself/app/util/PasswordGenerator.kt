package com.lockself.app.util

import java.security.SecureRandom

object PasswordGenerator {

    private val secureRandom = SecureRandom()

    private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    private const val NUMBERS = "0123456789"
    private const val SYMBOLS = "!@#$%^&*()-_=+[]{}|;:,.<>?"

    fun generatePassword(
        length: Int = 12,
        includeLetters: Boolean = true,
        includeNumbers: Boolean = true,
        includeSymbols: Boolean = true,
        candidateCount: Int = 3
    ): String {
        val effectiveLength = length.coerceIn(4, 32)
        val pools = mutableListOf<String>()
        if (includeLetters) {
            pools.add(UPPERCASE)
            pools.add(LOWERCASE)
        }
        if (includeNumbers) {
            pools.add(NUMBERS)
        }
        if (includeSymbols) {
            pools.add(SYMBOLS)
        }

        if (pools.isEmpty()) {
            pools.add(LOWERCASE)
        }

        val allChars = pools.joinToString("")
        val candidates = (0 until candidateCount.coerceAtLeast(1)).map {
            generateSingleCandidate(effectiveLength, pools, allChars)
        }

        // Return candidate with highest evaluated security score (entropy-driven selection pattern)
        return candidates.maxByOrNull { PasswordStrengthMeter.evaluate(it).score } ?: candidates.first()
    }

    private fun generateSingleCandidate(length: Int, pools: List<String>, allChars: String): String {
        val chars = CharArray(length)
        var pos = 0

        // Ensure at least one character from each active pool
        for (pool in pools) {
            if (pos < length) {
                chars[pos++] = pool[secureRandom.nextInt(pool.length)]
            }
        }

        // Fill remaining length
        while (pos < length) {
            chars[pos++] = allChars[secureRandom.nextInt(allChars.length)]
        }

        // Fisher-Yates shuffle
        for (i in length - 1 downTo 1) {
            val j = secureRandom.nextInt(i + 1)
            val temp = chars[i]
            chars[i] = chars[j]
            chars[j] = temp
        }

        return String(chars)
    }
}
