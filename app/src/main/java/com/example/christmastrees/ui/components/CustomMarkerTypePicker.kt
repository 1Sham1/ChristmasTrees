package com.example.christmastrees.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.christmastrees.data.CustomMarkerType
import com.example.christmastrees.data.MarkerType
import com.example.christmastrees.data.MarkerTypeOrCustom

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomMarkerTypePicker(
    customTypes: List<CustomMarkerType>,
    onTypeSelected: (MarkerTypeOrCustom) -> Unit,
    onShowEditor: () -> Unit,
    onEditType: (CustomMarkerType) -> Unit,
    onDeleteType: (CustomMarkerType) -> Unit,
    onDismiss: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf<CustomMarkerType?>(null) }

    showDeleteConfirm?.let { type ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Удалить тип?") },
            text = { Text("Тип \"${type.name}\" будет удален вместе со всеми размещенными маркерами этого типа.") },
            confirmButton = {
                TextButton(onClick = { onDeleteType(type); showDeleteConfirm = null }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("Отмена") }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.primary,
        tonalElevation = 8.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = "Выберите тип маркера",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Встроенные", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f), modifier = Modifier.padding(start = 4.dp))
            Spacer(modifier = Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(MarkerType.values().toList()) { type ->
                    BuiltInTypeButton(type) {
                        onTypeSelected(MarkerTypeOrCustom.BuiltIn(type))
                        onDismiss()
                    }
                }
            }

            if (customTypes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Кастомные", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f), modifier = Modifier.padding(start = 4.dp))
                Spacer(modifier = Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(customTypes) { customType ->
                        CustomTypeButton(
                            customType = customType,
                            onClick = { onTypeSelected(MarkerTypeOrCustom.Custom(customType)); onDismiss() },
                            onEdit = { onEditType(customType) },
                            onDelete = { showDeleteConfirm = customType }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onShowEditor,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Создать кастомный тип")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun BuiltInTypeButton(type: MarkerType, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
            Text(text = type.emoji, fontSize = 36.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = type.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun CustomTypeButton(
    customType: CustomMarkerType,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val bitmap by remember(customType.iconBase64) {
        mutableStateOf(
            if (customType.iconBase64.isNotBlank()) {
                try {
                    val bytes = android.util.Base64.decode(customType.iconBase64, android.util.Base64.NO_WRAP)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } catch (e: Exception) { null }
            } else { null }
        )
    }

    var showContextMenu by remember { mutableStateOf(false) }
    var buttonSize by remember { mutableStateOf(Size(0f, 0f)) }

    Box {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .onGloballyPositioned { coordinates ->
                    buttonSize = Size(coordinates.size.width.toFloat(), coordinates.size.height.toFloat())
                }
                .clickable(onClick = onClick)
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    if (bitmap != null) {
                        Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = customType.name, modifier = Modifier.size(48.dp))
                    } else {
                        Text(text = "📌", fontSize = 36.sp)
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onLongPress = { showContextMenu = true }
                    )
                }
            )
        }

        if (showContextMenu) {
            DropdownMenu(
                expanded = true,
                onDismissRequest = { showContextMenu = false },
                modifier = Modifier.width(150.dp).offset { IntOffset(buttonSize.width.toInt(), 0) }
            ) {
                DropdownMenuItem(
                    text = { Text("Редактировать") },
                    onClick = { onEdit(); showContextMenu = false },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                    onClick = { onDelete(); showContextMenu = false },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                )
            }
        }
    }
}
