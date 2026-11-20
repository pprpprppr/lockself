package com.lockself.app.util

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("lockself_user_preferences", Context.MODE_PRIVATE)

    companion object {
        const val KEY_LOCK_ON_MINIMIZE = "pref_lock_on_minimize"
        const val KEY_LOCK_ON_SCREEN_TIMEOUT = "pref_lock_on_screen_timeout"
        const val KEY_LOCK_TIMEOUT_SECONDS = "pref_lock_on_timeout"

        const val KEY_ALLOW_CLIPBOARD_COPY = "pref_allow_clipboard_copy"
        const val KEY_CLEAR_CLIPBOARD_TIMEOUT_SECONDS = "pref_clear_clipboard_timeout"
        const val KEY_CLEAR_CLIPBOARD_ON_LOCK = "pref_clear_clipboard_on_lock"

        const val KEY_RANDOM_LENGTH = "pref_random_length"
        const val KEY_RANDOM_LETTERS = "pref_random_letters"
        const val KEY_RANDOM_NUMBERS = "pref_random_numbers"
        const val KEY_RANDOM_SYMBOLS = "pref_random_symbols"

        const val KEY_DARK_THEME = "pref_dark_theme"
        const val KEY_BIOMETRIC_AUTH = "pref_fingerprint_authentication"
        const val KEY_BIOMETRIC_REMINDER = "pref_fingerprint_password_reminder"
        const val KEY_REMINDER_COUNTER = "password_reminder_counter"

        val LOCK_TIMEOUT_OPTIONS = listOf(
            30 to "30 seconds",
            60 to "1 minute",
            120 to "2 minutes",
            300 to "5 minutes",
            600 to "10 minutes",
            900 to "15 minutes (Default)",
            1800 to "30 minutes",
            3600 to "1 hour",
            -1 to "Never"
        )

        val CLIPBOARD_TIMEOUT_OPTIONS = listOf(
            30 to "30 seconds",
            60 to "1 minute",
            120 to "2 minutes",
            300 to "5 minutes",
            600 to "10 minutes (Default)",
            900 to "15 minutes",
            -1 to "Never"
        )
    }

    var lockOnMinimize: Boolean
        get() = prefs.getBoolean(KEY_LOCK_ON_MINIMIZE, true)
        set(v) = prefs.edit().putBoolean(KEY_LOCK_ON_MINIMIZE, v).apply()

    var lockOnScreenTimeout: Boolean
        get() = prefs.getBoolean(KEY_LOCK_ON_SCREEN_TIMEOUT, true)
        set(v) = prefs.edit().putBoolean(KEY_LOCK_ON_SCREEN_TIMEOUT, v).apply()

    var lockTimeoutSeconds: Int
        get() = prefs.getInt(KEY_LOCK_TIMEOUT_SECONDS, 900)
        set(v) = prefs.edit().putInt(KEY_LOCK_TIMEOUT_SECONDS, v).apply()

    var allowClipboardCopy: Boolean
        get() = prefs.getBoolean(KEY_ALLOW_CLIPBOARD_COPY, true)
        set(v) = prefs.edit().putBoolean(KEY_ALLOW_CLIPBOARD_COPY, v).apply()

    var clearClipboardTimeoutSeconds: Int
        get() = prefs.getInt(KEY_CLEAR_CLIPBOARD_TIMEOUT_SECONDS, 600)
        set(v) = prefs.edit().putInt(KEY_CLEAR_CLIPBOARD_TIMEOUT_SECONDS, v).apply()

    var clearClipboardOnLock: Boolean
        get() = prefs.getBoolean(KEY_CLEAR_CLIPBOARD_ON_LOCK, true)
        set(v) = prefs.edit().putBoolean(KEY_CLEAR_CLIPBOARD_ON_LOCK, v).apply()

    var randomLength: Int
        get() = prefs.getInt(KEY_RANDOM_LENGTH, 12)
        set(v) = prefs.edit().putInt(KEY_RANDOM_LENGTH, v).apply()

    var randomLetters: Boolean
        get() = prefs.getBoolean(KEY_RANDOM_LETTERS, true)
        set(v) = prefs.edit().putBoolean(KEY_RANDOM_LETTERS, v).apply()

    var randomNumbers: Boolean
        get() = prefs.getBoolean(KEY_RANDOM_NUMBERS, true)
        set(v) = prefs.edit().putBoolean(KEY_RANDOM_NUMBERS, v).apply()

    var randomSymbols: Boolean
        get() = prefs.getBoolean(KEY_RANDOM_SYMBOLS, true)
        set(v) = prefs.edit().putBoolean(KEY_RANDOM_SYMBOLS, v).apply()

    var darkTheme: Boolean
        get() = prefs.getBoolean(KEY_DARK_THEME, false)
        set(v) = prefs.edit().putBoolean(KEY_DARK_THEME, v).apply()

    var biometricAuthEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_AUTH, false)
        set(v) = prefs.edit().putBoolean(KEY_BIOMETRIC_AUTH, v).apply()

    var biometricReminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_REMINDER, true)
        set(v) = prefs.edit().putBoolean(KEY_BIOMETRIC_REMINDER, v).apply()

    var reminderCounter: Int
        get() = prefs.getInt(KEY_REMINDER_COUNTER, 15)
        set(v) = prefs.edit().putInt(KEY_REMINDER_COUNTER, v).apply()

    fun decrementReminderCounter(): Boolean {
        val current = reminderCounter
        val next = (current - 1).coerceAtLeast(0)
        reminderCounter = next
        return next == 0
    }

    fun resetReminderCounter() {
        reminderCounter = 15
    }
}
