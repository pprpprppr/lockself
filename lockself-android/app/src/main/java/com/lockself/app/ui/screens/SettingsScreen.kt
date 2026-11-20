package com.lockself.app.ui.screens

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
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lockself.app.LockSelfApp
import com.lockself.app.ui.theme.BrandBlue
import com.lockself.app.ui.viewmodel.SettingsViewModel
import com.lockself.app.util.AppPreferences
import java.io.OutputStreamWriter

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

    val snackbarHostState = remember { SnackbarHostState() }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showLockTimeoutDialog by remember { mutableStateOf(false) }
    var showClipboardTimeoutDialog by remember { mutableStateOf(false) }

    var pendingCsvToExport by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    // Storage Access Framework Export Launcher
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        LockSelfApp.instance.isSafPickerActive = false
        if (uri != null && pendingCsvToExport != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    OutputStreamWriter(os).use { writer ->
                        writer.write(pendingCsvToExport)
                    }
                }
                Toast.makeText(context, "Backup exported successfully", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Export failed: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
            }
            pendingCsvToExport = null
        }
    }

    // Storage Access Framework Import Launcher
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        LockSelfApp.instance.isSafPickerActive = false
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val csvText = inputStream.bufferedReader().use { it.readText() }
                    viewModel.importCsv(csvText) { count ->
                        Toast.makeText(context, "Imported $count records from BlackBerry CSV", Toast.LENGTH_SHORT).show()
                    }
                }
            }.onFailure {
                Toast.makeText(context, "Import failed: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
            }
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
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPass.length < 6) {
                            error = "New password must be at least 6 characters"
                            return@TextButton
                        }
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

    // Lock Timeout Picker Dialog
    if (showLockTimeoutDialog) {
        AlertDialog(
            onDismissRequest = { showLockTimeoutDialog = false },
            title = { Text("Auto-Lock Inactivity Timeout") },
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
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (lockTimeout == seconds),
                                onClick = {
                                    viewModel.updateLockTimeout(seconds)
                                    showLockTimeoutDialog = false
                                }
                            )
                            Text(label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLockTimeoutDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Clipboard Timeout Picker Dialog
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
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (clipboardTimeout == seconds),
                                onClick = {
                                    viewModel.updateClipboardTimeout(seconds)
                                    showClipboardTimeoutDialog = false
                                }
                            )
                            Text(label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showClipboardTimeoutDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandBlue)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Security Section
            SettingsSectionHeader(title = "Security", icon = Icons.Default.Security)
            SettingsClickableRow(
                title = "Change Master Password",
                subtitle = "Re-encrypt vault with a new master password",
                onClick = { showChangePasswordDialog = true }
            )
            SettingsSwitchRow(
                title = "Biometric Authentication",
                subtitle = "Unlock vault using fingerprint or biometrics",
                checked = biometricEnabled,
                onCheckedChange = { viewModel.toggleBiometric() }
            )

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Lock Preferences Section
            SettingsSectionHeader(title = "Auto-Lock", icon = Icons.Default.Lock)
            val lockTimeoutLabel = AppPreferences.LOCK_TIMEOUT_OPTIONS.find { it.first == lockTimeout }?.second ?: "$lockTimeout seconds"
            SettingsClickableRow(
                title = "Lock Timeout",
                subtitle = lockTimeoutLabel,
                onClick = { showLockTimeoutDialog = true }
            )
            SettingsSwitchRow(
                title = "Lock on Minimize",
                subtitle = "Automatically lock vault when sending app to background",
                checked = lockOnMinimize,
                onCheckedChange = { viewModel.toggleLockOnMinimize() }
            )
            SettingsSwitchRow(
                title = "Lock on Screen Timeout",
                subtitle = "Automatically lock vault when device screen turns off",
                checked = lockOnScreenTimeout,
                onCheckedChange = { viewModel.toggleLockOnScreenTimeout() }
            )

            Divider(modifier = Modifier.padding(vertical = 12.dp))

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

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Backup & Restore Section (BlackBerry CSV Compatibility)
            SettingsSectionHeader(title = "Import & Export", icon = Icons.Default.Upload)
            SettingsClickableRow(
                title = "Export BlackBerry CSV Backup",
                subtitle = "Save all records into a compatible 18-column CSV file",
                onClick = {
                    LockSelfApp.instance.isSafPickerActive = true
                    viewModel.exportCsv { csv ->
                        pendingCsvToExport = csv
                        exportFileLauncher.launch("LockSelf_Backup_${System.currentTimeMillis() / 1000}.csv")
                    }
                }
            )
            SettingsClickableRow(
                title = "Import BlackBerry CSV Backup",
                subtitle = "Restore records from an exported BlackBerry CSV backup",
                onClick = {
                    LockSelfApp.instance.isSafPickerActive = true
                    importFileLauncher.launch(arrayOf("text/comma-separated-values", "text/csv", "application/octet-stream", "*/*"))
                }
            )

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // About Section
            SettingsSectionHeader(title = "About", icon = Icons.Default.Info)
            Text(
                text = "LockSelf Android v1.0.0",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Independent high-fidelity reconstruction of BlackBerry Password Keeper.\n100% Offline • Zero Telemetry • Zero Network Permissions • AES-256-GCM Authenticated Encryption.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
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
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
fun SettingsSwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
