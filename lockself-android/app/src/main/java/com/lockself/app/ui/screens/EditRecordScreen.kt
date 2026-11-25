package com.lockself.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lockself.app.data.model.RecordType
import com.lockself.app.ui.components.ContinuousStrengthMeter
import com.lockself.app.ui.components.PasswordGeneratorDialog
import com.lockself.app.ui.theme.BrandBlue
import com.lockself.app.ui.viewmodel.EditRecordViewModel
import com.lockself.app.util.PasswordStrengthMeter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRecordScreen(
    viewModel: EditRecordViewModel,
    onBackClick: () -> Unit,
    onSaveSuccess: (Long) -> Unit
) {
    val title by viewModel.title.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()
    val website by viewModel.website.collectAsState()
    val note by viewModel.note.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val customFields by viewModel.customFields.collectAsState()
    val checklistItems by viewModel.checklistItems.collectAsState()
    val hasChanges by viewModel.hasChanges.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }
    var showGeneratorDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showAddCustomFieldDialog by remember { mutableStateOf(false) }
    var newChecklistText by remember { mutableStateOf("") }

    val strength = remember(password) { PasswordStrengthMeter.evaluate(password) }

    fun attemptBack() {
        if (hasChanges) {
            showDiscardDialog = true
        } else {
            onBackClick()
        }
    }

    BackHandler { attemptBack() }

    // Discard Confirmation Dialog (changes_made_message)
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Changes?") },
            text = { Text("You have unsaved changes. Do you want to discard them?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onBackClick()
                    }
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep Editing")
                }
            }
        )
    }

    // Password Generator Dialog
    if (showGeneratorDialog) {
        PasswordGeneratorDialog(
            initialLength = 12,
            onPasswordAccepted = { newPass ->
                viewModel.onPasswordChange(newPass)
            },
            onDismiss = { showGeneratorDialog = false }
        )
    }

    // Add Custom Field Dialog
    if (showAddCustomFieldDialog) {
        var fieldName by remember { mutableStateOf("") }
        var fieldValue by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddCustomFieldDialog = false },
            title = { Text("Add Custom Field") },
            text = {
                Column {
                    OutlinedTextField(
                        value = fieldName,
                        onValueChange = { fieldName = it },
                        label = { Text("Field Name (e.g. PIN, Security Answer)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = fieldValue,
                        onValueChange = { fieldValue = it },
                        label = { Text("Field Value") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (fieldName.isNotBlank()) {
                            viewModel.addCustomField(fieldName.trim(), fieldValue.trim())
                            showAddCustomFieldDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomFieldDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (title.isBlank()) "New Record" else "Edit Record", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { attemptBack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Discard", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.saveRecord(onSaveSuccess) }) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandBlue)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Title Field
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.onTitleChange(it) },
                label = { Text("Title *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            when (viewModel.recordType) {
                RecordType.PASSWORD -> {
                    // Username
                    OutlinedTextField(
                        value = username,
                        onValueChange = { viewModel.onUsernameChange(it) },
                        label = { Text("Username / Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password with strength meter and generator icon
                    OutlinedTextField(
                        value = password,
                        onValueChange = { viewModel.onPasswordChange(it) },
                        label = { Text("Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = if (passwordVisible) FontFamily.Monospace else FontFamily.Default
                        ),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Row {
                                IconButton(onClick = { showGeneratorDialog = true }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Generate Password")
                                }
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Visibility"
                                    )
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    ContinuousStrengthMeter(score = strength.score, warning = strength.warning)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Website URL
                    OutlinedTextField(
                        value = website,
                        onValueChange = { viewModel.onWebsiteChange(it) },
                        label = { Text("Website URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Notes
                    OutlinedTextField(
                        value = note,
                        onValueChange = { viewModel.onNoteChange(it) },
                        label = { Text("Notes") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                RecordType.NOTE -> {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { viewModel.onNoteChange(it) },
                        label = { Text("Note Content") },
                        minLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                RecordType.LIST -> {
                    Text("Checklist Items", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newChecklistText,
                            onValueChange = { newChecklistText = it },
                            placeholder = { Text("Add task or item…") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newChecklistText.isNotBlank()) {
                                    viewModel.addChecklistItem(newChecklistText)
                                    newChecklistText = ""
                                }
                            }
                        ) {
                            Text("Add")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    checklistItems.forEachIndexed { index, itemText ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "• $itemText",
                                modifier = Modifier.weight(1f),
                                fontSize = 15.sp
                            )
                            IconButton(onClick = { viewModel.removeChecklistItem(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete item", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Custom Fields Section
            if (customFields.isNotEmpty()) {
                Text("Custom Fields", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                customFields.forEachIndexed { idx, (k, v) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("$k: $v", modifier = Modifier.weight(1f), fontSize = 14.sp)
                        IconButton(onClick = { viewModel.removeCustomField(idx) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove Field", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedButton(
                onClick = { showAddCustomFieldDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Add Custom Field", modifier = Modifier.padding(start = 8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tags Field
            OutlinedTextField(
                value = tags,
                onValueChange = { viewModel.onTagsChange(it) },
                label = { Text("Tags (comma separated)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
