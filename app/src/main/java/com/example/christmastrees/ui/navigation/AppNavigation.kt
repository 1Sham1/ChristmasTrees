package com.example.christmastrees.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.christmastrees.util.AudioManager
import com.example.christmastrees.util.LocalAudioManager
import com.example.christmastrees.util.SettingsManager
import com.example.christmastrees.viewmodel.MarkersViewModel
import com.example.christmastrees.ui.components.GarlandOverlay
import com.example.christmastrees.ui.components.SettingsDialog
import com.example.christmastrees.ui.screens.ListScreen
import com.example.christmastrees.ui.screens.MapScreen

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
    val viewModel = MarkersViewModel.LocalMarkersViewModel.current ?: throw IllegalStateException("MarkersViewModel not provided")
    val audioManager = LocalAudioManager.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedScreen: Screen by remember { mutableStateOf(Screen.Map) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showMarkerPicker by remember { mutableStateOf(false) }

    CompositionLocalProvider(
        LocalShowSettingsDialog provides showSettingsDialog
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    when {
        currentRoute?.startsWith(Screen.Map.route) == true -> selectedScreen = Screen.Map
        currentRoute?.startsWith(Screen.List.route) == true -> selectedScreen = Screen.List
    }

    // Ensure audioManager is not null
    val safeAudioManager = audioManager
        ?: throw IllegalStateException("AudioManager not initialized. Check MainApplication.")

    // Load music state from settings on startup
    var isAudioPlaying by remember { mutableStateOf(SettingsManager.isMusicEnabled(safeAudioManager.appContext)) }

    // Sync music state on app resume
    DisposableEffect(audioManager, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAudioPlaying = safeAudioManager.isPlaying
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

        // Settings Dialog
        SettingsDialog(
            isOpen = showSettingsDialog,
            isAudioPlaying = isAudioPlaying,
            isSnowEnabled = isSnowEnabled,
            onAudioToggle = {
                safeAudioManager.toggle()
                isAudioPlaying = safeAudioManager.isPlaying
            },
            onSnowToggle = { viewModel.toggleSnow() },
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


