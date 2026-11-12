package com.example.christmastrees.data

import com.example.christmastrees.viewmodel.Marker

const val CUSTOM_MARKER_PREFIX = "CUSTOM_"

data class CustomMarkerType(
    val id: String,
    val name: String,
    val iconBase64: String,
    val bgColor: Int,
    val borderColor: Int,
    val accentColor: Int,
    val profileName: String
) {
    companion object {
        fun fromMarker(marker: Marker, customTypes: List<CustomMarkerType>): CustomMarkerType? {
            if (!marker.type.startsWith(CUSTOM_MARKER_PREFIX)) return null
            val customId = marker.type.removePrefix(CUSTOM_MARKER_PREFIX)
            return customTypes.find { it.id == customId }
        }
    }
}

sealed class MarkerTypeOrCustom {
    data class BuiltIn(val type: MarkerType) : MarkerTypeOrCustom()
    data class Custom(val custom: CustomMarkerType) : MarkerTypeOrCustom()

    val label: String
        get() = when (this) {
            is BuiltIn -> type.label
            is Custom -> custom.name
        }

    val bgColor: Int
        get() = when (this) {
            is BuiltIn -> type.bgColor
            is Custom -> custom.bgColor
        }

    val borderColor: Int
        get() = when (this) {
            is BuiltIn -> type.borderColor
            is Custom -> custom.borderColor
        }

    val accentColor: Int
        get() = when (this) {
            is BuiltIn -> type.accentColor
            is Custom -> custom.accentColor
        }

    val emoji: String
        get() = when (this) {
            is BuiltIn -> type.emoji
            is Custom -> "📌"
        }

    val category: Category
        get() = when (this) {
            is BuiltIn -> type.category
            is Custom -> Category.CUSTOM
        }

    val id: String
        get() = when (this) {
            is BuiltIn -> type.name
            is Custom -> custom.id
        }
}
