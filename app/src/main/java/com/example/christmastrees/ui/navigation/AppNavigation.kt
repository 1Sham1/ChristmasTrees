package com.example.christmastrees.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.christmastrees.ui.screens.ListScreen
import com.example.christmastrees.ui.screens.MapScreen
import com.example.christmastrees.viewmodel.MarkersViewModel

sealed class Screen(val route: String, val label: String, val icon: String) {
    object Map : Screen("map", "Карта", "🗺️")
    object List : Screen("list", "Список", "📋")
}

@Composable
fun AppNavigation(
    navController: NavHostController = androidx.navigation.compose.rememberNavController(),
    viewModel: MarkersViewModel = viewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var selectedScreen: Screen by remember { mutableStateOf(Screen.Map) }

    when {
        currentRoute?.startsWith(Screen.Map.route) == true -> selectedScreen = Screen.Map
        currentRoute?.startsWith(Screen.List.route) == true -> selectedScreen = Screen.List
    }

    Column(modifier = Modifier.fillMaxSize()) {
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

        NavigationBar(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
        ) {
            val screens = listOf(Screen.Map, Screen.List)
            screens.forEach { screen ->
                NavigationBarItem(
                    icon = { Text(screen.icon, fontSize = 20.sp) },
                    label = { Text(screen.label) },
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
                    }
                )
            }
        }
    }
}
