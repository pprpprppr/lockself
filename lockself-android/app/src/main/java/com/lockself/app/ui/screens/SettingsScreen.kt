package com.lockself.app.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lockself.app.LockSelfApp
import com.lockself.app.ui.theme.BrandBlue
import com.lockself.app.ui.viewmodel.SettingsViewModel
import com.lockself.app.util.AppPreferences
import com.lockself.app.util.CsvFileHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val lockTimeout by viewModel.lockTimeout.collectAsState()
    val clipboardTimeout by viewModel.clipboardTimeout.collectAsState()
    val lockOnMinimize by viewModel.lockOnMinimize.collectAsState()
    val lockOnScreenTimeout by viewModel.lockOnScreenTimeout.collectAsState()
    val clearClipboardOnLock by viewModel.clearClipboardOnLock.collectAsState()
    val biometricEnabled by viewModel.biometricEnabled.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()

    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showLockTimeoutDialog by remember { mutableStateOf(false) }
    var showClipboardTimeoutDialog by remember { mutableStateOf(false) }
    var showExportOptionsDialog by remember { mutableStateOf(false) }
    var showImportOptionsDialog by remember { mutableStateOf(false) }
    var showPasteImportDialog by remember { mutableStateOf(false) }
    var pastedCsvText by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    // Universal Document / File Export Launcher (supports Device & Google Drive)
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        LockSelfApp.instance.isSafPickerActive = false
        val uri = if (result.resultCode == Activity.RESULT_OK) result.data?.data else null
        val csvContent = viewModel.cachedExportCsv
        if (uri != null && csvContent != null) {
            viewModel.exportCsvToUri(context.contentResolver, uri, csvContent) { success, errMsg ->
                if (success) {
                    Toast.makeText(context, "Backup exported successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Export failed: $errMsg", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            viewModel.cachedExportCsv = null
        }
    }

    // Universal Document / File Import Launcher (supports Device Storage, Downloads, & Google Drive)
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        LockSelfApp.instance.isSafPickerActive = false
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
                ?: result.data?.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
            if (uri != null) {
                viewModel.importCsvFromUri(context.contentResolver, uri) { count, errMsg ->
                    if (errMsg != null) {
                        Toast.makeText(context, "Import failed: $errMsg", Toast.LENGTH_LONG).show()
                    } else if (count > 0) {
                        Toast.makeText(context, "Imported $count records successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "No valid records found in CSV", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context, "No file returned by picker", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun shareExportContent(csv: String) {
        runCatching {
            context.startActivity(CsvFileHelper.buildShareBackupIntent(context, csv))
        }.onFailure {
            Toast.makeText(context, "Unable to share: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchDeviceCsvPicker() {
        LockSelfApp.instance.isSafPickerActive = true
        val launched = runCatching {
            importFileLauncher.launch(CsvFileHelper.buildOpenDocumentIntent())
        }.isSuccess || runCatching {
            importFileLauncher.launch(CsvFileHelper.buildGetContentIntent("*/*"))
        }.isSuccess || runCatching {
            importFileLauncher.launch(CsvFileHelper.buildGetContentIntent("text/*"))
        }.isSuccess

        if (!launched) {
            LockSelfApp.instance.isSafPickerActive = false
            Toast.makeText(
                context,
                "No file manager app found on this device. Opening text paste.",
                Toast.LENGTH_LONG
            ).show()
            pastedCsvText = ""
            showPasteImportDialog = true
        }
    }

    fun launchGoogleDriveCsvPicker() {
        if (!CsvFileHelper.isGoogleDriveInstalled(context.packageManager)) {
            Toast.makeText(
                context,
                "Google Drive app not detected; opening system file picker",
                Toast.LENGTH_SHORT
            ).show()
        }
        LockSelfApp.instance.isSafPickerActive = true
        val launched = runCatching {
            val driveIntent = CsvFileHelper.buildGoogleDrivePickerIntent(context.packageManager)
            importFileLauncher.launch(driveIntent)
        }.isSuccess

        if (!launched) {
            launchDeviceCsvPicker()
        }
    }

    fun launchExportPicker(fileName: String, csv: String) {
        LockSelfApp.instance.isSafPickerActive = true
        val launched = runCatching {
            exportFileLauncher.launch(CsvFileHelper.buildCreateDocumentIntent(fileName, "text/csv"))
        }.isSuccess || runCatching {
            exportFileLauncher.launch(CsvFileHelper.buildCreateDocumentIntent(fileName, "*/*"))
        }.isSuccess

        if (!launched) {
            LockSelfApp.instance.isSafPickerActive = false
            viewModel.cachedExportCsv = null
            Toast.makeText(context, "File picker unavailable. Sharing instead.", Toast.LENGTH_SHORT).show()
            shareExportContent(csv)
        }
    }

    // Change Password Dialog
    if (showChangePasswordDialog) {
        var currentPass by remember { mutableStateOf("") }
        var newPass by remember { mutableStateOf("") }
        var confirmPass by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text("Change Master Password") },
            text = {
                Column {
                    OutlinedTextField(
                        value = currentPass,
                        onValueChange = { currentPass = it; error = null },
                        label = { Text("Current Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it; error = null },
                        label = { Text("New Password (min 6 chars)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPass,
                        onValueChange = { confirmPass = it; error = null },
                        label = { Text("Confirm New Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (error != null) {
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPass != confirmPass) {
                            error = "Passwords do not match"
                            return@TextButton
                        }
                        viewModel.changeMasterPassword(currentPass, newPass) { success ->
                            if (success) {
                                showChangePasswordDialog = false
                            } else {
                                error = "Current password incorrect"
                            }
                        }
                    }
                ) {
                    Text("Change")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Lock Timeout Dialog
    if (showLockTimeoutDialog) {
        AlertDialog(
            onDismissRequest = { showLockTimeoutDialog = false },
            title = { Text("Vault Lock Timeout") },
            text = {
                Column {
                    AppPreferences.LOCK_TIMEOUT_OPTIONS.forEach { (seconds, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateLockTimeout(seconds)
                                    showLockTimeoutDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = lockTimeout == seconds,
                                onClick = {
                                    viewModel.updateLockTimeout(seconds)
                                    showLockTimeoutDialog = false
                                }
                            )
                            Text(text = label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLockTimeoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clipboard Timeout Dialog
    if (showClipboardTimeoutDialog) {
        AlertDialog(
            onDismissRequest = { showClipboardTimeoutDialog = false },
            title = { Text("Clear Clipboard Timeout") },
            text = {
                Column {
                    AppPreferences.CLIPBOARD_TIMEOUT_OPTIONS.forEach { (seconds, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateClipboardTimeout(seconds)
                                    showClipboardTimeoutDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = clipboardTimeout == seconds,
                                onClick = {
                                    viewModel.updateClipboardTimeout(seconds)
                                    showClipboardTimeoutDialog = false
                                }
                            )
                            Text(text = label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showClipboardTimeoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Export Options Dialog
    if (showExportOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showExportOptionsDialog = false },
            title = { Text("Export Vault Backup") },
            text = {
                Column {
                    Text(
                        text = "Choose how to export your records as a standard 18-column CSV backup:",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    SettingsClickableRow(
                        title = "Save to Device or Google Drive",
                        subtitle = "Choose folder in Device storage or Google Drive",
                        onClick = {
                            showExportOptionsDialog = false
                            viewModel.exportCsv { csv ->
                                launchExportPicker(
                                    "LockSelf_Backup_${System.currentTimeMillis() / 1000}.csv",
                                    csv
                                )
                            }
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SettingsClickableRow(
                        title = "Share Backup File",
                        subtitle = "Send via Google Drive, Email, Cloud, or Messaging",
                        onClick = {
                            showExportOptionsDialog = false
                            viewModel.exportCsv { csv ->
                                shareExportContent(csv)
                            }
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportOptionsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Import Options Dialog (Device Storage, Google Drive, or Paste)
    if (showImportOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showImportOptionsDialog = false },
            title = { Text("Import Vault Backup") },
            text = {
                Column {
                    Text(
                        text = "Choose where to import your CSV backup file from:",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    SettingsIconClickableRow(
                        icon = Icons.Default.FolderOpen,
                        title = "Device Storage & Files",
                        subtitle = "Pick a .csv file from Downloads, Documents, or Storage",
                        onClick = {
                            showImportOptionsDialog = false
                            launchDeviceCsvPicker()
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SettingsIconClickableRow(
                        icon = Icons.Default.CloudDownload,
                        title = "Google Drive",
                        subtitle = "Browse and import a .csv file from Google Drive",
                        onClick = {
                            showImportOptionsDialog = false
                            launchGoogleDriveCsvPicker()
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SettingsIconClickableRow(
                        icon = Icons.Default.ContentPaste,
                        title = "Paste CSV Text",
                        subtitle = "Paste raw CSV content directly into the app",
                        onClick = {
                            showImportOptionsDialog = false
                            pastedCsvText = ""
                            showPasteImportDialog = true
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImportOptionsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Paste CSV Text Import Dialog
    if (showPasteImportDialog) {
        AlertDialog(
            onDismissRequest = { showPasteImportDialog = false },
            title = { Text("Paste CSV Content") },
            text = {
                Column {
                    Text("Paste your 18-column CSV backup text below:", fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                    OutlinedTextField(
                        value = pastedCsvText,
                        onValueChange = { pastedCsvText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        label = { Text("CSV Text") },
                        maxLines = 10
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (pastedCsvText.isNotBlank()) {
                            viewModel.importCsv(pastedCsvText) { count ->
                                Toast.makeText(context, "Imported $count records successfully", Toast.LENGTH_SHORT).show()
                            }
                            showPasteImportDialog = false
                        }
                    }
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Security Section
            SettingsSectionHeader(title = "Security", icon = Icons.Default.Security)
            SettingsClickableRow(
                title = "Change Master Password",
                subtitle = "Update vault master password and re-derive encryption key",
                onClick = { showChangePasswordDialog = true }
            )
            SettingsSwitchRow(
                title = "Biometric Unlock",
                subtitle = "Use fingerprint or face recognition to unlock",
                checked = biometricEnabled,
                onCheckedChange = { viewModel.toggleBiometric() }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Auto-Lock Section
            SettingsSectionHeader(title = "Auto-Lock", icon = Icons.Default.Lock)
            val lockTimeoutLabel = AppPreferences.LOCK_TIMEOUT_OPTIONS.find { it.first == lockTimeout }?.second ?: "$lockTimeout seconds"
            SettingsClickableRow(
                title = "Inactivity Lock Timeout",
                subtitle = lockTimeoutLabel,
                onClick = { showLockTimeoutDialog = true }
            )
            SettingsSwitchRow(
                title = "Lock on Minimize",
                subtitle = "Lock vault immediately when leaving the app",
                checked = lockOnMinimize,
                onCheckedChange = { viewModel.toggleLockOnMinimize() }
            )
            SettingsSwitchRow(
                title = "Lock on Screen Timeout",
                subtitle = "Lock vault when device screen turns off",
                checked = lockOnScreenTimeout,
                onCheckedChange = { viewModel.toggleLockOnScreenTimeout() }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Clipboard Section
            SettingsSectionHeader(title = "Clipboard", icon = Icons.Default.Restore)
            val clipboardTimeoutLabel = AppPreferences.CLIPBOARD_TIMEOUT_OPTIONS.find { it.first == clipboardTimeout }?.second ?: "$clipboardTimeout seconds"
            SettingsClickableRow(
                title = "Clear Clipboard Timeout",
                subtitle = clipboardTimeoutLabel,
                onClick = { showClipboardTimeoutDialog = true }
            )
            SettingsSwitchRow(
                title = "Clear Clipboard on Lock",
                subtitle = "Wipe clipboard immediately when vault locks",
                checked = clearClipboardOnLock,
                onCheckedChange = { viewModel.toggleClearClipboardOnLock() }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Backup & Restore Section
            SettingsSectionHeader(title = "Import & Export", icon = Icons.Default.Upload)
            SettingsClickableRow(
                title = "Export CSV Backup",
                subtitle = "Save records to Device, Google Drive, or share via apps",
                onClick = {
                    showExportOptionsDialog = true
                }
            )
            SettingsClickableRow(
                title = "Import CSV Backup",
                subtitle = "Import from Device files, Google Drive, or paste text",
                onClick = {
                    showImportOptionsDialog = true
                }
            )

            if (isBusy) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp))
                    Text("Processing vault records...", fontSize = 14.sp)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // About Section
            SettingsSectionHeader(title = "About", icon = Icons.Default.Info)
            Text(
                text = "LockSelf Android v${com.lockself.app.BuildConfig.VERSION_NAME}",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Independent secure offline password and credentials manager.\n100% Offline • Zero Telemetry • Zero Network Permissions • AES-256-GCM Authenticated Encryption.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.padding(end = 8.dp))
        Text(title, color = BrandBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun SettingsClickableRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        Text(title, fontWeight = FontWeight.Medium, fontSize = 15.sp)
        Text(subtitle, color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
fun SettingsIconClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = BrandBlue,
            modifier = Modifier.padding(end = 12.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(subtitle, color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
fun SettingsSwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(subtitle, color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
