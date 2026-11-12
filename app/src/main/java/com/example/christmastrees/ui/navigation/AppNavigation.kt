package com.example.christmastrees.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Button
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import android.app.Activity
import android.content.Intent
import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.christmastrees.util.AudioManager
import com.example.christmastrees.util.LocalAudioManager
import com.example.christmastrees.util.ProfileManager
import com.example.christmastrees.util.SettingsManager
import com.example.christmastrees.viewmodel.MarkersViewModel
import com.example.christmastrees.ui.components.GarlandOverlay
import com.example.christmastrees.ui.components.ProfileManagementScreen
import com.example.christmastrees.ui.components.SettingsDialog
import com.example.christmastrees.ui.screens.ListScreen
import com.example.christmastrees.ui.screens.MapScreen
import com.example.christmastrees.MainActivity
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

sealed class Screen(val route: String, val label: String, val icon: String) {
    object Map : Screen("map", "Карта", "🗺️")
    object List : Screen("list", "Список", "📋")
}

// CompositionLocal to communicate settings dialog state to child screens
val LocalShowSettingsDialog = compositionLocalOf { false }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    navController: NavHostController = androidx.navigation.compose.rememberNavController()
) {
    val context = LocalContext.current
    val appCtx = context.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel = MarkersViewModel.LocalMarkersViewModel.current ?: throw IllegalStateException("MarkersViewModel not provided")

    var selectedScreen: Screen by remember { mutableStateOf(Screen.Map) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showMarkerPicker by remember { mutableStateOf(false) }
    var showProfileManagement by remember { mutableStateOf(false) }

    // Profile management state
    var profileNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var activeProfileName by remember { mutableStateOf("") }
    var showProfileSwitchDialog by remember { mutableStateOf(false) }
    var profileToSwitchTo by remember { mutableStateOf("") }

    // Export/Import state
    var profileToExport by remember { mutableStateOf("") }
    var importResultMessage by remember { mutableStateOf<String?>(null) }
    var showImportResultDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Load profiles on composition
    LaunchedEffect(Unit) {
        profileNames = ProfileManager.getProfileNames(appCtx)
        activeProfileName = ProfileManager.getActiveProfile(appCtx)
    }

    // Reload profiles when profile management screen is shown
    LaunchedEffect(showProfileManagement) {
        if (showProfileManagement) {
            profileNames = ProfileManager.getProfileNames(appCtx)
            activeProfileName = ProfileManager.getActiveProfile(appCtx)
        }
    }

    // Export file launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            if (uri != null && profileToExport.isNotBlank()) {
                val result = ProfileManager.exportProfileToJson(appCtx, profileToExport, uri)
                if (result.isSuccess) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(result.getOrNull() ?: "Экспорт выполнен")
                    }
                } else {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Ошибка экспорта: ${result.exceptionOrNull()?.message}", duration = SnackbarDuration.Long)
                    }
                }
                profileToExport = ""
            }
        }
    )

    // Import file launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                if (activeProfileName.isBlank()) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Ошибка: нет активных профилей", duration = SnackbarDuration.Long)
                    }
                    return@rememberLauncherForActivityResult
                }
                val result = ProfileManager.importProfileFromJson(appCtx, uri, activeProfileName)
                importResultMessage = result.fold(
                    onSuccess = { it },
                    onFailure = { "Ошибка импорта: ${it.message}" }
                )
                showImportResultDialog = true
            }
        }
    )

    CompositionLocalProvider(
        LocalShowSettingsDialog provides showSettingsDialog
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        when {
            currentRoute?.startsWith(Screen.Map.route) == true -> selectedScreen = Screen.Map
            currentRoute?.startsWith(Screen.List.route) == true -> selectedScreen = Screen.List
        }

        val audioManager = LocalAudioManager.current

        // Ensure audioManager is not null
        val safeAudioManager = audioManager
            ?: throw IllegalStateException("AudioManager not initialized. Check MainApplication.")

        // Snackbar host for notifications
        SnackbarHost(snackbarHostState) { data ->
            androidx.compose.material3.Snackbar(
                snackbarData = data,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        }

        // Load music state from settings on startup
        var isAudioPlaying by remember { mutableStateOf(SettingsManager.isMusicEnabled(safeAudioManager.appContext, activeProfileName)) }

        // Sync music state on app resume
        DisposableEffect(audioManager, lifecycleOwner, activeProfileName) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    isAudioPlaying = safeAudioManager.isPlaying
                    // Reload music state from the active profile
                    isAudioPlaying = SettingsManager.isMusicEnabled(safeAudioManager.appContext, activeProfileName)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        // Read UI state values
        val isSnowEnabled by viewModel.uiState::isSnowEnabled

        Column(modifier = Modifier.fillMaxSize()) {
            NavHost(
                modifier = Modifier.weight(1f),
                navController = navController,
                startDestination = Screen.Map.route
            ) {
                composable(Screen.Map.route) {
                    MapScreen(
                        viewModel = viewModel,
                        showPicker = showMarkerPicker,
                        onPickerStateChanged = { showMarkerPicker = it }
                    )
                }
                composable(Screen.List.route) {
                    ListScreen(viewModel = viewModel)
                }
            }

            // Profile Management Screen (full screen overlay)
            if (showProfileManagement) {
                ProfileManagementScreen(
                    profileNames = profileNames,
                    activeProfileName = activeProfileName,
                    onCreateProfile = { name ->
                        if (ProfileManager.createProfile(appCtx, name)) {
                            profileNames = ProfileManager.getProfileNames(appCtx)
                        }
                    },
                    onDeleteProfile = { name ->
                        if (ProfileManager.deleteProfile(appCtx, name)) {
                            val oldActive = activeProfileName
                            profileNames = ProfileManager.getProfileNames(appCtx)
                            activeProfileName = ProfileManager.getActiveProfile(appCtx)
                            // If the deleted profile was the active one, the app switched to Default
                            // Need to restart to load the new profile's data
                            if (oldActive != activeProfileName) {
                                showProfileManagement = false
                                val activity = context as? Activity
                                if (activity != null) {
                                    val intent = Intent(context, MainActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        }
                    },
                    onRenameProfile = { oldName, newName ->
                        if (ProfileManager.renameProfile(appCtx, oldName, newName)) {
                            profileNames = ProfileManager.getProfileNames(appCtx)
                            activeProfileName = ProfileManager.getActiveProfile(appCtx)
                            // If we renamed the active profile, update it
                            if (activeProfileName == newName) {
                                // profileName in MainActivity is already updated via onProfileSwitch
                            }
                        }
                    },
                    onSwitchProfile = { name ->
                        if (name != activeProfileName) {
                            profileToSwitchTo = name
                            showProfileSwitchDialog = true
                        }
                    },
                    onExportProfile = { profileName ->
                        profileToExport = profileName
                        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy__HH-mm-ss"))
                        exportLauncher.launch("${profileName}_profile_$timestamp.json")
                    },
                    onImportProfile = {
                        importLauncher.launch("application/json")
                    },
                    onBack = { showProfileManagement = false }
                )
            }

            // Import Result Dialog
            if (showImportResultDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showImportResultDialog = false
                        importResultMessage = null
                    },
                    title = { Text("Импорт профиля") },
                    text = {
                        if (importResultMessage?.contains("успешно импортирован") == true) {
                            Column {
                                Text(importResultMessage ?: "")
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Для применения данных необходимо перезагрузить приложение.",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                        } else {
                            Text(importResultMessage ?: "")
                        }
                    },
                    confirmButton = {
                        if (importResultMessage?.contains("успешно импортирован") == true) {
                            Column {
                                Button(
                                    onClick = {
                                        showImportResultDialog = false
                                        importResultMessage = null
                                        // Restart activity with new data
                                        val activity = context as? Activity
                                        if (activity != null) {
                                            val intent = Intent(context, MainActivity::class.java).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            }
                                            context.startActivity(intent)
                                        }
                                    }
                                ) {
                                    Text("Перезагрузить")
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                TextButton(onClick = {
                                    showImportResultDialog = false
                                    importResultMessage = null
                                }) {
                                    Text("Отмена")
                                }
                            }
                        } else {
                            TextButton(onClick = {
                                showImportResultDialog = false
                                importResultMessage = null
                            }) {
                                Text("OK")
                            }
                        }
                    }
                )
            }

            // Profile Switch Confirmation Dialog
            if (showProfileSwitchDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showProfileSwitchDialog = false
                        profileToSwitchTo = ""
                    },
                    title = { Text("Смена профиля") },
                    text = { Text("Для переключения на профиль \"$profileToSwitchTo\" необходимо перезагрузить приложение. Продолжить?") },
                    confirmButton = {
                        TextButton(onClick = {
                            // Set the new active profile
                            ProfileManager.setActiveProfile(appCtx, profileToSwitchTo)
                            showProfileSwitchDialog = false
                            profileToSwitchTo = ""
                            // Restart activity with new profile
                            val activity = context as? Activity
                            if (activity != null) {
                                val intent = Intent(context, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                context.startActivity(intent)
                            }
                        }) {
                            Text("Перезагрузить")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showProfileSwitchDialog = false
                            profileToSwitchTo = ""
                        }) {
                            Text("Отмена")
                        }
                    }
                )
            }

            // Settings Dialog
            SettingsDialog(
                isOpen = showSettingsDialog && !showProfileManagement,
                isAudioPlaying = isAudioPlaying,
                isSnowEnabled = isSnowEnabled,
                onAudioToggle = {
                    safeAudioManager.toggle()
                    isAudioPlaying = safeAudioManager.isPlaying
                    SettingsManager.saveMusicEnabled(safeAudioManager.appContext, isAudioPlaying, activeProfileName)
                },
                onSnowToggle = { viewModel.toggleSnow() },
                onProfilesClick = { showProfileManagement = true },
                onClose = { showSettingsDialog = false }
            )

            // Navigation bar with 3 buttons and garland overlay
            Box {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Настройки",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        label = {
                            Text(
                                "Настройки",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        selected = false,
                        onClick = { showSettingsDialog = true },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.secondary,
                            unselectedIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                            selectedTextColor = MaterialTheme.colorScheme.secondary,
                            unselectedTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                            indicatorColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    // Add marker button
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Добавить маркер",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        label = {
                            Text(
                                "Добавить маркер",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        selected = false,
                        onClick = { showMarkerPicker = true },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.secondary,
                            unselectedIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                            selectedTextColor = MaterialTheme.colorScheme.secondary,
                            unselectedTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                            indicatorColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    // Search owner button
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Поиск владельца",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        label = {
                            Text(
                                "Поиск владельца",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        selected = false,
                        onClick = {
                            viewModel.requestLocationUpdate()
                            viewModel.centerOnCurrentLocation()
                            viewModel.toggleOwnerSearchHint()
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.secondary,
                            unselectedIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                            selectedTextColor = MaterialTheme.colorScheme.secondary,
                            unselectedTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                            indicatorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Garland overlay on top of navigation bar
                GarlandOverlay(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(35.dp),
                    bulbCount = 10,
                    sagRatio = 0.02f
                )
            }
        }
    }
}

