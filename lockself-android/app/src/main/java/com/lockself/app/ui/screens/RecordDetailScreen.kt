package com.lockself.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lockself.app.data.model.RecordType
import com.lockself.app.ui.components.ContinuousStrengthMeter
import com.lockself.app.ui.theme.BrandBlue
import com.lockself.app.ui.theme.ListAccent
import com.lockself.app.ui.theme.NoteAccent
import com.lockself.app.ui.theme.PasswordAccent
import com.lockself.app.ui.viewmodel.RecordDetailViewModel
import com.lockself.app.util.PasswordStrengthMeter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDetailScreen(
    viewModel: RecordDetailViewModel,
    onBackClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    onHistoryClick: (Long) -> Unit,
    onRecordDeleted: () -> Unit
) {
    val context = LocalContext.current
    val record by viewModel.record.collectAsState()
    val isPasswordVisible by viewModel.isPasswordVisible.collectAsState()
    val copiedMessage by viewModel.copiedMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(copiedMessage) {
        copiedMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearCopiedMessage()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Record") },
            text = { Text("Are you sure you want to delete this record? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteRecord(onRecordDeleted)
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(record?.title ?: "Record Details", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            imageVector = if (record?.isFavorite == true) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (record?.isFavorite == true) Color(0xFFFFB300) else Color.White
                        )
                    }

                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            if (record?.type == RecordType.PASSWORD) {
                                DropdownMenuItem(
                                    text = { Text("Password History") },
                                    leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        record?.id?.let { onHistoryClick(it) }
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Delete Record", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuExpanded = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandBlue)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { record?.id?.let { onEditClick(it) } },
                containerColor = BrandBlue,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Record")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        val currentRecord = record
        if (currentRecord == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading record…")
            }
        } else {
            val bannerColor = when (currentRecord.type) {
                RecordType.PASSWORD -> PasswordAccent
                RecordType.NOTE -> NoteAccent
                RecordType.LIST -> ListAccent
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // Colored Category Accent Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(bannerColor)
                )

                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = currentRecord.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (currentRecord.type == RecordType.PASSWORD) {
                        // Username Field
                        currentRecord.username?.takeIf { it.isNotBlank() }?.let { user ->
                            DetailFieldRow(
                                label = "Username",
                                value = user,
                                onCopy = { viewModel.copyToClipboard("Username", user) }
                            )
                            Divider(modifier = Modifier.padding(vertical = 12.dp))
                        }

                        // Password Field
                        currentRecord.password?.takeIf { it.isNotBlank() }?.let { pass ->
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Password", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                        Text(
                                            text = if (isPasswordVisible) pass else "••••••••••••",
                                            fontSize = 16.sp,
                                            fontFamily = if (isPasswordVisible) FontFamily.Monospace else FontFamily.Default,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    IconButton(onClick = { viewModel.togglePasswordVisibility() }) {
                                        Icon(
                                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle Visibility"
                                        )
                                    }

                                    IconButton(onClick = { viewModel.copyToClipboard("Password", pass) }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Password")
                                    }
                                }

                                val strength = remember(pass) { PasswordStrengthMeter.evaluate(pass) }
                                Spacer(modifier = Modifier.height(8.dp))
                                ContinuousStrengthMeter(score = strength.score, warning = strength.warning)
                            }
                            Divider(modifier = Modifier.padding(vertical = 12.dp))
                        }

                        // Website Field
                        currentRecord.website?.takeIf { it.isNotBlank() }?.let { site ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            val url = if (!site.startsWith("http://") && !site.startsWith("https://")) "https://$site" else site
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                            }
                                        }
                                ) {
                                    Text("Website", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    Text(
                                        text = site,
                                        fontSize = 16.sp,
                                        color = BrandBlue,
                                        textDecoration = TextDecoration.Underline
                                    )
                                }

                                IconButton(onClick = {
                                    val url = if (!site.startsWith("http://") && !site.startsWith("https://")) "https://$site" else site
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }
                                }) {
                                    Icon(Icons.Default.OpenInBrowser, contentDescription = "Open Website", tint = BrandBlue)
                                }

                                IconButton(onClick = { viewModel.copyToClipboard("Website", site) }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Website")
                                }
                            }
                            Divider(modifier = Modifier.padding(vertical = 12.dp))
                        }
                    }

                    // Note Field
                    currentRecord.note?.takeIf { it.isNotBlank() }?.let { noteText ->
                        Column {
                            Text("Notes", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Text(
                                    text = noteText,
                                    fontSize = 15.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                    }

                    // Checklist Items
                    if (currentRecord.type == RecordType.LIST && currentRecord.checklist.isNotEmpty()) {
                        Text("Checklist", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))

                        for (item in currentRecord.checklist) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleChecklistItem(item) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.isChecked,
                                    onCheckedChange = { viewModel.toggleChecklistItem(item) }
                                )
                                Text(
                                    text = item.text,
                                    fontSize = 16.sp,
                                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (item.isChecked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                    }

                    // Custom Fields
                    if (currentRecord.customFields.isNotEmpty()) {
                        Text("Custom Fields", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))

                        for ((name, value) in currentRecord.customFields) {
                            DetailFieldRow(
                                label = name,
                                value = value,
                                onCopy = { viewModel.copyToClipboard(name, value) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                    }

                    // Tags
                    if (currentRecord.tagsList.isNotEmpty()) {
                        Text("Tags", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(6.dp))
                        Row {
                            for (tag in currentRecord.tagsList) {
                                Box(
                                    modifier = Modifier
                                        .padding(end = 6.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(tag, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailFieldRow(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        IconButton(onClick = onCopy) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy $label")
        }
    }
}
