package com.example.christmastrees.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.christmastrees.data.CustomMarkerType
import com.example.christmastrees.viewmodel.MarkersViewModel
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomMarkerTypeEditor(
    editingType: CustomMarkerType?,
    onSave: (name: String, iconBase64: String, bgColor: Int, borderColor: Int, accentColor: Int) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(editingType?.name ?: "") }
    var iconBase64 by remember { mutableStateOf(editingType?.iconBase64 ?: "") }
    var bgColor by remember { mutableStateOf(editingType?.bgColor ?: 0xFF1B5E20.toInt()) }
    var borderColor by remember { mutableStateOf(editingType?.borderColor ?: 0xFFF2C94C.toInt()) }
    var accentColor by remember { mutableStateOf(editingType?.accentColor ?: 0xFFE53935.toInt()) }
    var iconBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val ctx = LocalContext.current

    LaunchedEffect(editingType) {
        if (editingType != null && editingType.iconBase64.isNotBlank()) {
            iconBitmap = decodeBase64ToBitmap(editingType.iconBase64)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val inputStream = ctx.contentResolver.openInputStream(it)
            inputStream?.use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                if (bitmap != null) {
                    val resized = Bitmap.createScaledBitmap(bitmap, 64, 64, true)
                    iconBase64 = bitmapToBase64(resized)
                    iconBitmap = resized
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (editingType != null) "Редактировать тип" else "Новый тип маркера",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.size(120.dp).clip(CircleShape),
                colors = CardDefaults.cardColors(containerColor = ComposeColor(bgColor.toLong()).copy(alpha = 0.3f))
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    if (iconBitmap != null) {
                        Image(bitmap = iconBitmap!!.asImageBitmap(), contentDescription = "Preview", modifier = Modifier.size(56.dp))
                    } else {
                        Text("📌", fontSize = 40.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                Text("Выбрать иконку")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Цвета", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            ColorPickerRow(label = "Фон", selectedColor = bgColor, onColorSelected = { bgColor = it })
            ColorPickerRow(label = "Рамка", selectedColor = borderColor, onColorSelected = { borderColor = it })
            ColorPickerRow(label = "Бейдж", selectedColor = accentColor, onColorSelected = { accentColor = it })

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (editingType != null) {
                    FilledTonalIconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить")
                    }
                }

                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Отмена")
                }

                FilledTonalIconButton(
                    onClick = {
                        if (name.isNotBlank() && iconBase64.isNotBlank()) {
                            onSave(name, iconBase64, bgColor, borderColor, accentColor)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun ColorPickerRow(label: String, selectedColor: Int, onColorSelected: (Int) -> Unit) {
    val colors = listOf(
        0xFFE53935.toInt(), 0xFFD32F2F.toInt(), 0xFFFF6D00.toInt(), 0xFFFFD600.toInt(),
        0xFF66BB6A.toInt(), 0xFF1B5E20.toInt(), 0xFF1976D2.toInt(), 0xFF7B1FA2.toInt(),
        0xFFC2185B.toInt(), 0xFF000000.toInt(), 0xFF757575.toInt(), 0xFFFFFFFF.toInt()
    )

    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            colors.forEach { color ->
                val isSelected = color == selectedColor
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(ComposeColor(color.toLong()))
                        .then(if (isSelected) Modifier.border(3.dp, ComposeColor.White, CircleShape) else Modifier.border(2.dp, ComposeColor.Gray, CircleShape))
                        .clickable { onColorSelected(color) }
                )
            }
        }
    }
}

private fun bitmapToBase64(bitmap: Bitmap): String {
    val byteArray = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 80, byteArray)
    return android.util.Base64.encodeToString(byteArray.toByteArray(), android.util.Base64.NO_WRAP)
}

private fun decodeBase64ToBitmap(base64: String): Bitmap? {
    return try {
        val bytes = android.util.Base64.decode(base64, android.util.Base64.NO_WRAP)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
