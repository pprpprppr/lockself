package com.lockself.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lockself.app.data.model.RecordType
import com.lockself.app.ui.components.CategoryBadge
import com.lockself.app.ui.components.RecordCardItem
import com.lockself.app.ui.theme.BrandBlue
import com.lockself.app.ui.theme.ListAccent
import com.lockself.app.ui.theme.NoteAccent
import com.lockself.app.ui.theme.PasswordAccent
import com.lockself.app.ui.viewmodel.DrawerCategory
import com.lockself.app.ui.viewmodel.VaultMainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultMainScreen(
    viewModel: VaultMainViewModel,
    onRecordClick: (Long) -> Unit,
    onAddRecordClick: (RecordType) -> Unit,
    onSettingsClick: () -> Unit,
    onLockClick: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val records by viewModel.filteredRecords.collectAsState()
    val counts by viewModel.categoryCounts.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var fabMenuExpanded by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                // BlackBerry Password Keeper Navigation Drawer Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(BrandBlue)
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "LockSelf Shield",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "LockSelf",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Navigation Drawer Items with numeric badges (BlackBerry NavigationDrawerFragment)
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Folder, contentDescription = "All Records") },
                    label = { Text("All Records") },
                    badge = { CategoryBadge(count = counts[DrawerCategory.ALL] ?: 0) },
                    selected = selectedCategory == DrawerCategory.ALL,
                    onClick = {
                        viewModel.selectCategory(DrawerCategory.ALL)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Star, contentDescription = "Favorites", tint = Color(0xFFFFB300)) },
                    label = { Text("Favorites") },
                    badge = { CategoryBadge(count = counts[DrawerCategory.FAVORITES] ?: 0) },
                    selected = selectedCategory == DrawerCategory.FAVORITES,
                    onClick = {
                        viewModel.selectCategory(DrawerCategory.FAVORITES)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.VpnKey, contentDescription = "Passwords", tint = PasswordAccent) },
                    label = { Text("Passwords") },
                    badge = { CategoryBadge(count = counts[DrawerCategory.PASSWORDS] ?: 0) },
                    selected = selectedCategory == DrawerCategory.PASSWORDS,
                    onClick = {
                        viewModel.selectCategory(DrawerCategory.PASSWORDS)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Description, contentDescription = "Notes", tint = NoteAccent) },
                    label = { Text("Notes") },
                    badge = { CategoryBadge(count = counts[DrawerCategory.NOTES] ?: 0) },
                    selected = selectedCategory == DrawerCategory.NOTES,
                    onClick = {
                        viewModel.selectCategory(DrawerCategory.NOTES)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Checklist, contentDescription = "Lists", tint = ListAccent) },
                    label = { Text("Lists") },
                    badge = { CategoryBadge(count = counts[DrawerCategory.LISTS] ?: 0) },
                    selected = selectedCategory == DrawerCategory.LISTS,
                    onClick = {
                        viewModel.selectCategory(DrawerCategory.LISTS)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onSettingsClick()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Lock, contentDescription = "Lock Vault") },
                    label = { Text("Lock Vault") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.lockVault()
                        onLockClick()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if (isSearchActive) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { viewModel.onSearchQueryChanged(it) },
                                placeholder = { Text("Search records…", color = Color.White.copy(alpha = 0.7f)) },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color.White,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            val title = when (selectedCategory) {
                                DrawerCategory.ALL -> "All Records"
                                DrawerCategory.FAVORITES -> "Favorites"
                                DrawerCategory.PASSWORDS -> "Passwords"
                                DrawerCategory.NOTES -> "Notes"
                                DrawerCategory.LISTS -> "Lists"
                            }
                            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Drawer", tint = Color.White)
                        }
                    },
                    actions = {
                        if (isSearchActive) {
                            IconButton(onClick = {
                                isSearchActive = false
                                viewModel.onSearchQueryChanged("")
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Close Search", tint = Color.White)
                            }
                        } else {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandBlue)
                )
            },
            floatingActionButton = {
                Box {
                    FloatingActionButton(
                        onClick = { fabMenuExpanded = true },
                        containerColor = BrandBlue,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Record")
                    }

                    DropdownMenu(
                        expanded = fabMenuExpanded,
                        onDismissRequest = { fabMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = PasswordAccent) },
                            onClick = {
                                fabMenuExpanded = false
                                onAddRecordClick(RecordType.PASSWORD)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Note") },
                            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = NoteAccent) },
                            onClick = {
                                fabMenuExpanded = false
                                onAddRecordClick(RecordType.NOTE)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("List") },
                            leadingIcon = { Icon(Icons.Default.Checklist, contentDescription = null, tint = ListAccent) },
                            onClick = {
                                fabMenuExpanded = false
                                onAddRecordClick(RecordType.LIST)
                            }
                        )
                    }
                }
            }
        ) { paddingValues ->
            if (records.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching records found" else "No records yet",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            fontSize = 16.sp
                        )
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Tap + to add a record",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(top = 8.dp)
                ) {
                    items(records, key = { if (it.id > 0) "rec_${it.id}" else "rec_uid_${it.entity.uid}" }) { record ->
                        RecordCardItem(
                            record = record,
                            onClick = { onRecordClick(record.id) },
                            onToggleFavorite = { viewModel.toggleFavorite(record.id) }
                        )
                    }
                }
            }
        }
    }
}
