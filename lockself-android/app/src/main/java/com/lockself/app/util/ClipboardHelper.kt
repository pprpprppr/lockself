package com.lockself.app.util

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle

class ClipboardHelper(private val context: Context, private val preferences: AppPreferences) {

    private val clipboard = runCatching {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    }.getOrNull()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingClearRunnable: Runnable? = null

    fun copyToClipboard(label: String, text: String): Boolean {
        if (!preferences.allowClipboardCopy) return false
        val mgr = clipboard ?: return false

        cancelPendingClear()

        val clip = ClipData.newPlainText(label.ifEmpty { "bb.pk" }, text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }

        return runCatching {
            mgr.setPrimaryClip(clip)
            val timeoutSeconds = preferences.clearClipboardTimeoutSeconds
            if (timeoutSeconds > 0) {
                val runnable = Runnable { clearClipboard() }
                pendingClearRunnable = runnable
                mainHandler.postDelayed(runnable, timeoutSeconds * 1000L)
            }
            true
        }.getOrDefault(false)
    }

    fun clearClipboard() {
        cancelPendingClear()
        val mgr = clipboard ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                mgr.clearPrimaryClip()
            } else {
                mgr.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        }
    }

    fun onVaultLocked() {
        if (preferences.clearClipboardOnLock) {
            clearClipboard()
        }
    }

    private fun cancelPendingClear() {
        pendingClearRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingClearRunnable = null
    }
}
