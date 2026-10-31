package com.example.christmastrees.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.christmastrees.R
import com.example.christmastrees.util.ProfileManager
import com.example.christmastrees.ui.theme.ChristmasGreen
import com.example.christmastrees.ui.theme.ChristmasGreenLight
import com.example.christmastrees.ui.theme.ChristmasGold
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileManagementScreen(
    profileNames: List<String>,
    activeProfileName: String,
    onCreateProfile: (String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onRenameProfile: (String, String) -> Unit,
    onSwitchProfile: (String) -> Unit,
    onExportProfile: (String) -> Unit,
    onImportProfile: () -> Unit,
    onBack: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var profileToRename by remember { mutableStateOf("") }
    var profileToDelete by remember { mutableStateOf("") }
    var expandedMenu by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Профили",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = ChristmasGreen
                    )
                }
            },
            actions = {
                // Export button
                IconButton(onClick = {
                    if (profileNames.size == 1) {
                        showExportDialog = true
                    } else {
                        // Show export dialog for each profile
                        showExportDialog = true
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Экспорт профиля",
                        tint = ChristmasGreen
                    )
                }
                // Import button
                IconButton(onClick = onImportProfile) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = "Импорт профиля",
                        tint = ChristmasGreen
                    )
                }
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Создать профиль",
                        tint = ChristmasGreen
                    )
                }
            },
        )

        // Progress indicator while loading
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Profile list
        if (profileNames.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Профили не найдены",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(profileNames, key = { it }) { profileName ->
                    ProfileCard(
                        profileName = profileName,
                        isActive = profileName == activeProfileName,
                        onSwitch = { if (profileName != activeProfileName) onSwitchProfile(profileName) },
                        onRename = {
                            profileToRename = profileName
                            showRenameDialog = true
                        },
                        onDelete = {
                            profileToDelete = profileName
                            showDeleteDialog = true
                        },
                        onMenuDismissed = { expandedMenu = null }
                    )
                }
            }
        }
    }

    // Create Profile Dialog
    if (showCreateDialog) {
        CreateProfileDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                if (name.isNotBlank()) {
                    onCreateProfile(name)
                }
                showCreateDialog = false
            }
        )
    }

    // Rename Profile Dialog
    if (showRenameDialog) {
        RenameProfileDialog(
            currentName = profileToRename,
            onDismiss = {
                showRenameDialog = false
                profileToRename = ""
            },
            onConfirm = { newName ->
                if (newName.isNotBlank() && newName != profileToRename) {
                    onRenameProfile(profileToRename, newName)
                }
                showRenameDialog = false
                profileToRename = ""
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                profileToDelete = ""
            },
            title = { Text("Удалить профиль?") },
            text = {
                Text("Профиль \"$profileToDelete\" и все его данные будут удалены. Это действие нельзя отменить.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteProfile(profileToDelete)
                        showDeleteDialog = false
                        profileToDelete = ""
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    profileToDelete = ""
                }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Export Dialog - select profile to export
    if (showExportDialog) {
        ExportProfileDialog(
            profileNames = profileNames,
            onDismiss = { showExportDialog = false },
            onExport = { profileName ->
                onExportProfile(profileName)
                showExportDialog = false
            }
        )
    }
}

@Composable
fun ProfileCard(
    profileName: String,
    isActive: Boolean,
    onSwitch: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onMenuDismissed: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }
    val isDefault = (profileName == "Default")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSwitch),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) ChristmasGreenLight.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 4.dp else 2.dp),
        shape = RoundedCornerShape(12.dp),
        border = if (isActive) {
            androidx.compose.foundation.BorderStroke(2.dp, ChristmasGreen)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = if (isActive) ChristmasGreen else Color.Gray,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profileName,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 16.sp,
                    color = if (isActive) ChristmasGreen else MaterialTheme.colorScheme.onSurface
                )
                if (isActive) {
                    Text(
                        text = "Активный",
                        fontSize = 12.sp,
                        color = ChristmasGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // More menu
            Box {
                IconButton(
                    onClick = { expandedMenu = !expandedMenu },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Меню",
                        tint = ChristmasGreen
                    )
                }

                DropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = {
                        expandedMenu = false
                        onMenuDismissed()
                    }
                ) {
                    // Rename option (available for all profiles including Default)
                    DropdownMenuItem(
                        text = { Text("Переименовать") },
                        onClick = {
                            expandedMenu = false
                            onMenuDismissed()
                            onRename()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    )

                    // Delete option (only for non-Default profiles)
                    if (!isDefault) {
                        DropdownMenuItem(
                            text = { Text("Удалить") },
                            onClick = {
                                expandedMenu = false
                                onMenuDismissed()
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreateProfileDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(TextFieldValue("")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый профиль") },
        text = {
            Column {
                Text(
                    text = "Введите имя нового профиля:",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Имя профиля") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = { onConfirm(name.text) },
                enabled = name.text.isNotBlank()
            ) {
                Text("Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun RenameProfileDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(TextFieldValue(currentName)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Переименовать профиль") },
        text = {
            TextField(
                value = newName,
                onValueChange = { newName = it },
                placeholder = { Text("Новое имя") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            OutlinedButton(
                onClick = { onConfirm(newName.text) },
                enabled = newName.text.isNotBlank() && newName.text != currentName
            ) {
                Text("Переименовать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun ExportProfileDialog(
    profileNames: List<String>,
    onDismiss: () -> Unit,
    onExport: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Экспорт профиля") },
        text = {
            Column {
                Text(
                    text = "Выберите профиль для экспорта:",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                profileNames.forEach { profileName ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onExport(profileName)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = ChristmasGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = profileName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}


