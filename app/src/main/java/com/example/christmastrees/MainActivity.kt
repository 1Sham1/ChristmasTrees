package com.example.christmastrees

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.christmastrees.ui.navigation.AppNavigation
import com.example.christmastrees.ui.theme.ChristmasTreesTheme
import com.example.christmastrees.util.AudioManager
import com.example.christmastrees.util.LocalAudioManager
import com.example.christmastrees.viewmodel.MarkersViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChristmasTreesTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ChristmasTreesApp()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AudioManager.clear()
    }
}

@Composable
fun ChristmasTreesApp(
    viewModel: MarkersViewModel = viewModel(factory = MarkersViewModel.Factory(LocalContext.current.applicationContext))
) {
    val audioManager = AudioManager.get()

    CompositionLocalProvider(
        LocalAudioManager provides audioManager,
        MarkersViewModel.LocalMarkersViewModel provides viewModel
    ) {
        AppNavigation()
    }
}
