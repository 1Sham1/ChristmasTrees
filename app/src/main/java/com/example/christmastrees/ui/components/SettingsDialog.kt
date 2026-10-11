package com.example.christmastrees.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.christmastrees.ui.theme.ChristmasGold
import com.example.christmastrees.ui.theme.ChristmasGreen

/**
 * Диалог настроек приложения с управлением аудио и анимацией снега.
 * Закрывается при нажатии на крестик или на область за пределами диалога.
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onClose() }
        ) {
            // Карточка диалога — кликабельность отменяет, чтобы клик по карточке не закрывал
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .align(Alignment.Center)
                    .clickable(enabled = false) {},
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Заголовок с кнопкой закрытия
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Настройки",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = ChristmasGreen,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Закрыть",
                                tint = ChristmasGreen
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Управление музыкой
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
                        IconButton(onClick = onAudioToggle) {
                            Icon(
                                imageVector = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isAudioPlaying) "Пауза" else "Воспроизвести",
                                tint = ChristmasGold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Управление снегом
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "❄️",
                            fontSize = 20.sp,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            text = if (isSnowEnabled) "Снег включён" else "Снег выключен",
                            modifier = Modifier.weight(1f),
                            fontSize = 14.sp
                        )
                        IconButton(onClick = onSnowToggle) {
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
}
