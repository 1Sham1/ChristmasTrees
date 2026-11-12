package com.example.christmastrees.data

import com.example.christmastrees.viewmodel.Marker

const val EXPORT_VERSION = 2

data class ExportProfile(
    val version: Int = EXPORT_VERSION,
    val exportedProfile: String,
    val markers: List<Marker>,
    val customMarkerTypes: List<CustomMarkerType> = emptyList(),
    val musicEnabled: Boolean = true,
    val snowEnabled: Boolean = true,
    val mapLat: Float = 0f,
    val mapLon: Float = 0f,
    val mapZoom: Float = 0f
)
