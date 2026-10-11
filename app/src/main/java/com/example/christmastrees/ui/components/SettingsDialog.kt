package com.example.christmastrees.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.christmastrees.ui.theme.ChristmasGold
import com.example.christmastrees.ui.theme.ChristmasGreen

/**
 * Диалог настроек приложения с управлением аудио и анимацией снега.
 */
@Composable
fun SettingsDialog(
    isOpen: Boolean,
    isAudioPlaying: Boolean,
    isSnowEnabled: Boolean,
    onAudioToggle: () -> Unit,
    onSnowToggle: () -> Unit,
    onClose: () -> Unit
) {
    if (isOpen) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(16.dp),
            onClick = {}
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Настройки",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ChristmasGreen
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Audio control
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isAudioPlaying) Icons.Default.MusicNote else Icons.Default.MusicOff,
                        contentDescription = if (isAudioPlaying) "Выключить музыку" else "Включить музыку",
                        tint = ChristmasGold,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Text(
                        text = if (isAudioPlaying) "Музыка включена" else "Музыка выключена",
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp
                    )
                    IconButton(
                        onClick = onAudioToggle
                    ) {
                        Icon(
                            imageVector = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isAudioPlaying) "Пауза" else "Воспроизвести",
                            tint = ChristmasGold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Snow animation toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = "Анимация снега",
                        tint = ChristmasGold,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Text(
                        text = if (isSnowEnabled) "Снег включён" else "Снег выключен",
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp
                    )
                    IconButton(
                        onClick = onSnowToggle
                    ) {
                        Icon(
                            imageVector = if (isSnowEnabled) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isSnowEnabled) "Остановить снег" else "Запустить снег",
                            tint = ChristmasGold
                        )
                    }
                }
            }
        }
    }
}
