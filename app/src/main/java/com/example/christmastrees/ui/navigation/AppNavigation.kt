package com.example.christmastrees.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.christmastrees.util.AudioManager
import com.example.christmastrees.util.LocalAudioManager
import com.example.christmastrees.viewmodel.MarkersViewModel
import com.example.christmastrees.ui.components.SettingsDialog
import com.example.christmastrees.ui.screens.ListScreen
import com.example.christmastrees.ui.screens.MapScreen
import com.example.christmastrees.ui.theme.ChristmasGold
import com.example.christmastrees.ui.theme.ChristmasGreen

sealed class Screen(val route: String, val label: String, val icon: String) {
    object Map : Screen("map", "Карта", "🗺️")
    object List : Screen("list", "Список", "📋")
}

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

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    when {
        currentRoute?.startsWith(Screen.Map.route) == true -> selectedScreen = Screen.Map
        currentRoute?.startsWith(Screen.List.route) == true -> selectedScreen = Screen.List
    }

    // Auto-start music when app enters foreground
    DisposableEffect(audioManager, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                audioManager?.start()
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
        TopAppBar(
            title = {
                Text(
                    "Новогодние ёлки",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            actions = {
                IconButton(
                    onClick = { showSettingsDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Настройки",
                        tint = ChristmasGold
                    )
                }
            },
            colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                containerColor = ChristmasGreen,
                titleContentColor = Color.White
            )
        )

        NavHost(
            navController = navController,
            startDestination = Screen.Map.route
        ) {
            composable(Screen.Map.route) {
                MapScreen(viewModel = viewModel)
            }
            composable(Screen.List.route) {
                ListScreen(viewModel = viewModel)
            }
        }

        // Settings Dialog
        SettingsDialog(
            isOpen = showSettingsDialog,
            isAudioPlaying = audioManager?.isPlaying ?: false,
            isSnowEnabled = isSnowEnabled,
            onAudioToggle = { audioManager?.toggle() },
            onSnowToggle = { viewModel.toggleSnow() },
            onClose = { showSettingsDialog = false }
        )

        NavigationBar(
            containerColor = ChristmasGreen,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            val screens = listOf(Screen.Map, Screen.List)
            screens.forEach { screen ->
                NavigationBarItem(
                    icon = {
                        Text(
                            screen.icon,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    },
                    label = {
                        Text(
                            screen.label,
                            fontWeight = if (selectedScreen == screen) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedScreen == screen) ChristmasGold else Color.White.copy(alpha = 0.7f)
                        )
                    },
                    selected = selectedScreen == screen,
                    onClick = {
                        if (selectedScreen != screen) {
                            selectedScreen = screen
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                        selectedIconColor = ChristmasGold,
                        unselectedIconColor = Color.White.copy(alpha = 0.7f),
                        selectedTextColor = ChristmasGold,
                        unselectedTextColor = Color.White.copy(alpha = 0.7f),
                        indicatorColor = ChristmasGreen
                    )
                )
            }
        }
    }
}
