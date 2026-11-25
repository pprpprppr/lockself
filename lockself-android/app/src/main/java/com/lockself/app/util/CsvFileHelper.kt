package com.lockself.app.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import java.io.IOException
import java.io.OutputStreamWriter

/**
 * Helper utilities for safely reading/writing CSV backup files via Storage Access Framework,
 * Google Drive document providers, and generic file pickers.
 */
object CsvFileHelper {

    const val GOOGLE_DRIVE_PACKAGE = "com.google.android.apps.docs"
    private const val GOOGLE_DRIVE_AUTHORITY = "com.google.android.apps.docs.storage"

    fun isGoogleDriveInstalled(packageManager: PackageManager): Boolean {
        return runCatching {
            packageManager.getPackageInfo(GOOGLE_DRIVE_PACKAGE, 0)
            true
        }.getOrDefault(false)
    }

    /**
     * Primary SAF picker intent: opens Android System Files (includes Device storage and Google Drive).
     * Uses type = "* / *" without EXTRA_MIME_TYPES so .csv files tagged as text/plain,
     * text/comma-separated-values, or application/octet-stream are never greyed out.
     */
    fun buildOpenDocumentIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Raw ACTION_GET_CONTENT intent for OEM devices or environments without DocumentsUI.
     * Android automatically shows an app chooser when multiple file managers/Drive apps match,
     * and throws ActivityNotFoundException if none match.
     */
    fun buildGetContentIntent(mimeType: String = "*/*"): Intent {
        return Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun buildGetContentChooserIntent(
        mimeType: String = "*/*",
        title: String = "Select CSV File"
    ): Intent {
        return Intent.createChooser(buildGetContentIntent(mimeType), title)
    }

    /**
     * Builds an intent targeting Google Drive when available:
     * 1. Direct Google Drive GET_CONTENT picker if exported by the Drive app.
     * 2. Otherwise ACTION_OPEN_DOCUMENT hinted at Google Drive's DocumentsProvider roots URI.
     */
    fun buildGoogleDrivePickerIntent(packageManager: PackageManager): Intent {
        val directDriveIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            setPackage(GOOGLE_DRIVE_PACKAGE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (directDriveIntent.resolveActivity(packageManager) != null) {
            return directDriveIntent
        }

        val driveSafIntent = buildOpenDocumentIntent().apply {
            putExtra(
                DocumentsContract.EXTRA_INITIAL_URI,
                DocumentsContract.buildRootsUri(GOOGLE_DRIVE_AUTHORITY)
            )
        }
        if (driveSafIntent.resolveActivity(packageManager) != null) {
            return driveSafIntent
        }

        return buildGetContentIntent("*/*")
    }

    fun buildCreateDocumentIntent(
        fileName: String,
        mimeType: String = "text/csv"
    ): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, fileName)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
    }

    fun getDisplayName(contentResolver: ContentResolver, uri: Uri): String? {
        return runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else null
                }
        }.getOrNull() ?: uri.lastPathSegment
    }

    /**
     * Reads UTF-8 CSV content from any content:// or file:// URI (including remote Google Drive streams).
     * Must be called from a background thread (Dispatchers.IO).
     */
    fun readCsvTextFromUri(contentResolver: ContentResolver, uri: Uri): String {
        val primaryAttempt = runCatching {
            contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            }
        }

        val directText = primaryAttempt.getOrNull()
        if (directText != null) {
            return directText
        }

        // Fallback for virtual or typed DocumentsProvider entries (e.g., cloud providers)
        for (mime in listOf("text/csv", "text/comma-separated-values", "text/plain")) {
            val typedText = runCatching {
                contentResolver.openTypedAssetFileDescriptor(uri, mime, null)?.use { afd ->
                    afd.createInputStream().use { stream ->
                        stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    }
                }
            }.getOrNull()
            if (typedText != null) {
                return typedText
            }
        }

        val rootCause = primaryAttempt.exceptionOrNull()
        throw IOException(
            rootCause?.localizedMessage
                ?: "Unable to read the selected file. If selecting from Google Drive, ensure it is a .csv file.",
            rootCause
        )
    }

    /**
     * Writes UTF-8 CSV content to a destination URI on Dispatchers.IO.
     */
    fun writeCsvTextToUri(contentResolver: ContentResolver, uri: Uri, csvContent: String) {
        val stream = runCatching {
            contentResolver.openOutputStream(uri, "wt")
        }.getOrNull() ?: contentResolver.openOutputStream(uri)
        ?: throw IOException("Unable to open output stream for selected destination")

        stream.use { os ->
            OutputStreamWriter(os, Charsets.UTF_8).use { writer ->
                writer.write(csvContent)
                writer.flush()
            }
        }
    }

    fun buildShareBackupIntent(context: Context, csvContent: String): Intent {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "LockSelf Vault Backup")
            putExtra(Intent.EXTRA_TEXT, csvContent)
        }
        return Intent.createChooser(shareIntent, "Share Vault Backup")
    }
}
