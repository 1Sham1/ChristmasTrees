package com.example.christmastrees.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.christmastrees.data.MarkerTypeOrCustom
import com.example.christmastrees.viewmodel.MarkersViewModel

@Composable
fun MarkerPickerBottomSheet(
    onTypeSelected: (MarkerTypeOrCustom) -> Unit,
    onDismiss: () -> Unit,
    customTypes: List<com.example.christmastrees.data.CustomMarkerType> = emptyList(),
    viewModel: MarkersViewModel? = MarkersViewModel.LocalMarkersViewModel.current
) {
    var showEditor by remember { mutableStateOf(false) }
    var editingType by remember { mutableStateOf<com.example.christmastrees.data.CustomMarkerType?>(null) }

    if (showEditor) {
        CustomMarkerTypeEditor(
            editingType = editingType,
            onSave = { name: String, iconBase64: String, bgColor: Int, borderColor: Int, accentColor: Int ->
                if (editingType != null) {
                    viewModel?.deleteCustomType(editingType!!.id)
                }
                viewModel?.addCustomType(name, iconBase64, bgColor, borderColor, accentColor)
                showEditor = false
                editingType = null
            },
            onDelete = {
                editingType?.let { type -> viewModel?.deleteCustomType(type.id) }
                showEditor = false
                editingType = null
            },
            onDismiss = { showEditor = false; editingType = null }
        )
    } else {
        CustomMarkerTypePicker(
            customTypes = customTypes,
            onTypeSelected = onTypeSelected,
            onShowEditor = { showEditor = true; editingType = null },
            onEditType = { type -> editingType = type; showEditor = true },
            onDeleteType = { type -> viewModel?.deleteCustomType(type.id) },
            onDismiss = onDismiss
        )
    }
}
