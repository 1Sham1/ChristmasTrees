package com.example.christmastrees.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.christmastrees.data.CustomMarkerType
import com.example.christmastrees.data.MarkerSize
import com.example.christmastrees.data.MarkerType
import com.example.christmastrees.data.MarkerTypeOrCustom
import com.example.christmastrees.data.CUSTOM_MARKER_PREFIX
import com.example.christmastrees.util.SettingsManager

data class CustomMarkerCountEntry(
    val customId: String,
    val customName: String,
    val total: Int = 0,
    val small: Int = 0,
    val normal: Int = 0,
    val boss: Int = 0
)

data class UiState(
    val markers: List<Marker> = emptyList(),
    val treesCount: Int = 0,
    val snowmenCount: Int = 0,
    val decorationsCount: Int = 0,
    val grinchCount: Int = 0,
    val treesSmallCount: Int = 0,
    val treesNormalCount: Int = 0,
    val treesBossCount: Int = 0,
    val snowmenSmallCount: Int = 0,
    val snowmenNormalCount: Int = 0,
    val snowmenBossCount: Int = 0,
    val decorationsSmallCount: Int = 0,
    val decorationsNormalCount: Int = 0,
    val decorationsBossCount: Int = 0,
    val grinchSmallCount: Int = 0,
    val grinchNormalCount: Int = 0,
    val grinchBossCount: Int = 0,
    val customMarkerCounts: List<CustomMarkerCountEntry> = emptyList(),
    val currentLocation: Location? = null,
    val isLocationPermissionGranted: Boolean = false,
    val isLoadingLocation: Boolean = false,
    val placeMarkerMode: PlaceMarkerMode = PlaceMarkerMode.Idle,
    val isSnowEnabled: Boolean = true,
    val showOwnerSearchHint: Boolean = false,
    val centerOnLocation: Boolean = false
)

private fun computeCounts(m: List<Marker>, customTypes: List<CustomMarkerType> = emptyList()): List<Any> {
    var tTotal = 0
    var sTotal = 0
    var dTotal = 0
    var gTotal = 0
    var tSmall = 0
    var tNormal = 0
    var tBoss = 0
    var sSmall = 0
    var sNormal = 0
    var sBoss = 0
    var dSmall = 0
    var dNormal = 0
    var dBoss = 0
    var gSmall = 0
    var gNormal = 0
    var gBoss = 0

    // Accumulate small quantities separately for correct division
    var tSmallAcc = 0
    var sSmallAcc = 0
    var dSmallAcc = 0
    var gSmallAcc = 0

    // Custom marker counts
    val customCountsMap = mutableMapOf<String, CustomMarkerCountEntry>()

    for (marker in m) {
        val mSize = marker.markerSize()

        // Handle custom markers first
        if (marker.type.startsWith(CUSTOM_MARKER_PREFIX)) {
            val customId = marker.type.removePrefix(CUSTOM_MARKER_PREFIX)
            val customType = customTypes.find { it.id == customId }
            val customName = customType?.name ?: "Unknown"

            val existing = customCountsMap[customId] ?: CustomMarkerCountEntry(customId, customName)
            val newEntry = when (mSize) {
                MarkerSize.SMALL -> existing.copy(
                    small = existing.small + marker.quantity,
                    total = existing.total + marker.quantity / 10
                )
                MarkerSize.NORMAL -> existing.copy(
                    normal = existing.normal + marker.quantity,
                    total = existing.total + marker.quantity
                )
                MarkerSize.BOSS -> existing.copy(
                    boss = existing.boss + 1,
                    total = existing.total + marker.quantity
                )
            }
            customCountsMap[customId] = newEntry
            continue
        }

        val markerType = marker.markerType()
        when (markerType) {
            MarkerType.CHRISTMAS_TREE -> {
                when (mSize) {
                    MarkerSize.SMALL -> {
                        tSmall += marker.quantity
                        tSmallAcc += marker.quantity
                    }
                    MarkerSize.NORMAL -> {
                        tNormal += marker.quantity
                        tTotal += marker.quantity
                    }
                    MarkerSize.BOSS -> {
                        tBoss += marker.quantity
                        tTotal += marker.quantity
                    }
                }
            }
            MarkerType.SNOWMAN -> {
                when (mSize) {
                    MarkerSize.SMALL -> {
                        sSmall += marker.quantity
                        sSmallAcc += marker.quantity
                    }
                    MarkerSize.NORMAL -> {
                        sNormal += marker.quantity
                        sTotal += marker.quantity
                    }
                    MarkerSize.BOSS -> {
                        sBoss += marker.quantity
                        sTotal += marker.quantity
                    }
                }
            }
            MarkerType.CITY_DECORATION -> {
                when (mSize) {
                    MarkerSize.SMALL -> {
                        dSmall += marker.quantity
                        dSmallAcc += marker.quantity
                    }
                    MarkerSize.NORMAL -> {
                        dNormal += marker.quantity
                        dTotal += marker.quantity
                    }
                    MarkerSize.BOSS -> {
                        dBoss += marker.quantity
                        dTotal += marker.quantity
                    }
                }
            }
            MarkerType.GRINCH -> {
                when (mSize) {
                    MarkerSize.SMALL -> {
                        gSmall += marker.quantity
                        gSmallAcc += marker.quantity
                    }
                    MarkerSize.NORMAL -> {
                        gNormal += marker.quantity
                        gTotal += marker.quantity
                    }
                    MarkerSize.BOSS -> {
                        gBoss += marker.quantity
                        gTotal += marker.quantity
                    }
                }
            }
            else -> {
                android.util.Log.w("computeCounts", "Unknown marker type for marker ${marker.id}")
            }
        }
    }
    // SMALL markers contribute 1/10 of their quantity to the total count.
    // This is because small markers represent fractional coverage.
    tTotal += tSmallAcc / 10
    sTotal += sSmallAcc / 10
    dTotal += dSmallAcc / 10
    gTotal += gSmallAcc / 10

    return listOf(
        intArrayOf(tTotal, sTotal, dTotal, gTotal),
        intArrayOf(tSmall, tNormal, tBoss),
        intArrayOf(sSmall, sNormal, sBoss),
        intArrayOf(dSmall, dNormal, dBoss),
        intArrayOf(gSmall, gNormal, gBoss),
        customCountsMap.values.toList()
    )
}

sealed interface PlaceMarkerMode {
    object Idle : PlaceMarkerMode
    data class Selecting(val selectedType: MarkerTypeOrCustom) : PlaceMarkerMode
}

data class Marker(
    val id: Long,
    val latitude: Double,
    val longitude: Double,
    val type: String,
    val name: String,
    val timestamp: Long,
    val quantity: Int = 1,
    val size: String = MarkerSize.NORMAL.name
) {
    fun markerType(): MarkerType = MarkerType.fromString(type)
    fun markerSize(): MarkerSize = MarkerSize.entries.find { it.name == size } ?: MarkerSize.NORMAL

    fun markerTypeOrCustom(customTypes: List<CustomMarkerType>): MarkerTypeOrCustom {
        return if (type.startsWith(CUSTOM_MARKER_PREFIX)) {
            val customId = type.removePrefix(CUSTOM_MARKER_PREFIX)
            val custom = customTypes.find { it.id == customId }
            if (custom != null) {
                MarkerTypeOrCustom.Custom(custom)
            } else {
                // Custom type not found, default to CHRISTMAS_TREE
                MarkerTypeOrCustom.BuiltIn(MarkerType.CHRISTMAS_TREE)
            }
        } else {
            MarkerTypeOrCustom.BuiltIn(markerType())
        }
    }
}

class MarkersViewModel(
    private val context: Context,
    private val profileName: String
) : ViewModel() {

    var markers by mutableStateOf<List<Marker>>(emptyList())
        private set
    
    var customTypes by mutableStateOf<List<CustomMarkerType>>(emptyList())
        private set
    
    var uiState by mutableStateOf(UiState())
        internal set
    
    private var nextId = 1L
    var onMarkerAdded: ((Marker) -> Unit)? = null
    var onMarkersLoaded: ((Int) -> Unit)? = null
    var onMarkerDeleted: ((Long) -> Unit)? = null

    init {
        try {
            checkLocationPermission()
            loadMarkersFromStorage()
            loadCustomTypesFromStorage()
            val snowEnabled = loadSnowState()
            uiState = uiState.copy(isSnowEnabled = snowEnabled)
            syncUiState()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadCustomTypesFromStorage() {
        try {
            val appCtx = context.applicationContext
            val loaded = com.example.christmastrees.util.CustomMarkerManager.loadCustomTypes(appCtx, profileName)
            customTypes = loaded
            android.util.Log.d("MarkersViewModel", "Loaded ${loaded.size} custom types for profile '$profileName'")
        } catch (e: Exception) {
            android.util.Log.e("MarkersViewModel", "Failed to load custom types", e)
        }
    }

    private fun saveCustomTypesToStorage() {
        try {
            val appCtx = context.applicationContext
            com.example.christmastrees.util.CustomMarkerManager.saveCustomTypes(appCtx, profileName, customTypes)
        } catch (e: Exception) {
            android.util.Log.e("MarkersViewModel", "Failed to save custom types", e)
        }
    }

    fun addCustomType(name: String, iconBase64: String, bgColor: Int, borderColor: Int, accentColor: Int) {
        val newId = java.util.UUID.randomUUID().toString()
        val newType = CustomMarkerType(
            id = newId,
            name = name,
            iconBase64 = iconBase64,
            bgColor = bgColor,
            borderColor = borderColor,
            accentColor = accentColor,
            profileName = profileName
        )
        customTypes = customTypes + newType
        saveCustomTypesToStorage()
    }

    fun deleteCustomType(id: String) {
        // Delete all markers of this custom type
        val markersWithoutCustom = markers.filter { !it.type.startsWith(CUSTOM_MARKER_PREFIX) || it.type.removePrefix(CUSTOM_MARKER_PREFIX) != id }
        if (markersWithoutCustom.size != markers.size) {
            markers = markersWithoutCustom
            saveMarkers()
        }
        customTypes = customTypes.filter { it.id != id }
        saveCustomTypesToStorage()
        syncUiState()
    }

    private fun loadMarkersFromStorage() {
        try {
            val appCtx = context.applicationContext
            val prefs = appCtx.getSharedPreferences("markers_data_$profileName", android.content.Context.MODE_PRIVATE)
            
            val json = prefs.getString("markers_json", null)
            android.util.Log.d("MarkersViewModel", "loadMarkersFromStorage: json=$json")
            
            val markerList = mutableListOf<Marker>()
            
            if (!json.isNullOrEmpty()) {
                val gson = com.google.gson.Gson()
                val markerType = object : com.google.gson.reflect.TypeToken<List<Marker>>() {}.type
                val loaded: List<Marker>? = gson.fromJson(json, markerType)
                if (loaded != null) {
                    markerList.addAll(loaded)
                }
            }
            
            if (markerList.isNotEmpty()) {
                markers = markerList
                
                // Set nextId to be larger than any existing marker id
                val maxId = markerList.maxOfOrNull { it.id } ?: 0
                nextId = maxId + 1
                
                android.util.Log.d("MarkersViewModel", "Loaded ${markerList.size} markers from storage")
                
                // Notify UI
                onMarkersLoaded?.invoke(markerList.size)
            }
        } catch (e: Exception) {
            android.util.Log.e("MarkersViewModel", "Failed to load markers", e)
        }
    }
    
    private fun syncUiState() {
        val counts = computeCounts(markers)
        val totals = counts[0] as IntArray
        val treeSizes = counts[1] as IntArray
        val snowmanSizes = counts[2] as IntArray
        val decorationSizes = counts[3] as IntArray
        val grinchSizes = counts[4] as IntArray
        val customCounts = counts[5] as List<CustomMarkerCountEntry>

        uiState = uiState.copy(
            markers = markers,
            treesCount = totals[0],
            snowmenCount = totals[1],
            decorationsCount = totals[2],
            grinchCount = totals[3],
            treesSmallCount = treeSizes[0],
            treesNormalCount = treeSizes[1],
            treesBossCount = treeSizes[2],
            snowmenSmallCount = snowmanSizes[0],
            snowmenNormalCount = snowmanSizes[1],
            snowmenBossCount = snowmanSizes[2],
            decorationsSmallCount = decorationSizes[0],
            decorationsNormalCount = decorationSizes[1],
            decorationsBossCount = decorationSizes[2],
            grinchSmallCount = grinchSizes[0],
            grinchNormalCount = grinchSizes[1],
            grinchBossCount = grinchSizes[2],
            customMarkerCounts = customCounts
        )
        android.util.Log.d("MarkersViewModel", "syncUiState: trees=${totals[0]} snowmen=${totals[1]} decorations=${totals[2]} grinch=${totals[3]}")
    }
    
    fun refreshUiState() {
        syncUiState()
    }

    fun toggleSnow() {
        val newState = !uiState.isSnowEnabled
        uiState = uiState.copy(isSnowEnabled = newState)
        SettingsManager.saveSnowEnabled(context, newState, profileName)
    }

    private fun loadSnowState(): Boolean {
        return SettingsManager.isSnowEnabled(context, profileName)
    }

    fun toggleOwnerSearchHint() {
        uiState = uiState.copy(showOwnerSearchHint = !uiState.showOwnerSearchHint)
    }

    fun centerOnCurrentLocation() {
        uiState = uiState.copy(centerOnLocation = true)
    }

    private fun saveMarkers() {
        try {
            val appCtx = context.applicationContext
            val prefs = appCtx.getSharedPreferences("markers_data_$profileName", android.content.Context.MODE_PRIVATE)
            
            val gson = com.google.gson.Gson()
            val json = gson.toJson(markers)
            
            prefs.edit().putString("markers_json", json).commit()
            
            android.util.Log.d("MarkersViewModel", "Saved ${markers.size} markers to storage: $json")
        } catch (e: Exception) {
            android.util.Log.e("MarkersViewModel", "Failed to save markers", e)
        }
    }

    fun placeMarker(latitude: Double, longitude: Double, type: MarkerTypeOrCustom) {
        val markerTypeStr = when (type) {
            is MarkerTypeOrCustom.BuiltIn -> type.type.name
            is MarkerTypeOrCustom.Custom -> CUSTOM_MARKER_PREFIX + type.custom.id
        }
        val markerName = when (type) {
            is MarkerTypeOrCustom.BuiltIn -> type.type.label
            is MarkerTypeOrCustom.Custom -> type.custom.name
        }

        android.util.Log.d("MarkersViewModel", "placeMarker called: lat=$latitude, lon=$longitude, type=$markerTypeStr")
        val newMarker = Marker(
            id = nextId++,
            latitude = latitude,
            longitude = longitude,
            type = markerTypeStr,
            name = markerName,
            timestamp = System.currentTimeMillis(),
            quantity = 1,
            size = MarkerSize.NORMAL.name
        )
        markers = markers + newMarker
        android.util.Log.d("MarkersViewModel", "Markers count now: ${markers.size}")
        uiState = uiState.copy(placeMarkerMode = PlaceMarkerMode.Idle)
        syncUiState()
        
        // Save markers to storage
        saveMarkers()
        
        // Callback to add marker directly on map
        onMarkerAdded?.invoke(newMarker)
    }

    fun deleteMarker(marker: Marker) {
        val removedId = marker.id
        markers = markers.filter { it.id != marker.id }
        syncUiState()
        saveMarkers()
        
        // Notify UI to remove labeled marker ids for deleted marker
        onMarkerDeleted?.invoke(removedId)
    }

    fun setMarkerQuantity(markerId: Long, newQuantity: Int) {
        val newList = markers.toMutableList()
        val idx = newList.indexOfFirst { it.id == markerId }
        if (idx != -1 && newQuantity >= 1) {
            newList[idx] = newList[idx].copy(quantity = newQuantity)
        }
        markers = newList
        syncUiState()
        saveMarkers()
    }

    fun incrementQuantity(markerId: Long) {
        val newList = markers.toMutableList()
        val idx = newList.indexOfFirst { it.id == markerId }
        if (idx != -1) {
            newList[idx] = newList[idx].copy(quantity = newList[idx].quantity + 1)
        }
        markers = newList
        syncUiState()
        saveMarkers()
    }

    fun updateMarkerName(markerId: Long, newName: String) {
        val newList = markers.toMutableList()
        val idx = newList.indexOfFirst { it.id == markerId }
        if (idx != -1) {
            newList[idx] = newList[idx].copy(name = newName)
        }
        markers = newList
        saveMarkers()
    }

    fun decrementQuantity(markerId: Long) {
        val newList = markers.toMutableList()
        val idx = newList.indexOfFirst { it.id == markerId }
        if (idx != -1 && newList[idx].quantity > 1) {
            newList[idx] = newList[idx].copy(quantity = newList[idx].quantity - 1)
        }
        markers = newList
        syncUiState()
        saveMarkers()
    }

    fun moveMarker(markerId: Long, newLatitude: Double, newLongitude: Double) {
        val newList = markers.toMutableList()
        val idx = newList.indexOfFirst { it.id == markerId }
        if (idx != -1) {
            newList[idx] = newList[idx].copy(latitude = newLatitude, longitude = newLongitude)
        }
        markers = newList
        syncUiState()
        saveMarkers()
    }

    fun updateMarkerSize(markerId: Long, newSize: MarkerSize) {
        val newList = markers.toMutableList()
        val idx = newList.indexOfFirst { it.id == markerId }
        if (idx != -1) {
            newList[idx] = newList[idx].copy(size = newSize.name)
        }
        markers = newList
        syncUiState()
        saveMarkers()
    }

    fun cancelPlaceMarker() {
        uiState = uiState.copy(placeMarkerMode = PlaceMarkerMode.Idle)
    }

    fun startPlaceMarker(type: MarkerTypeOrCustom) {
        uiState = uiState.copy(
            placeMarkerMode = PlaceMarkerMode.Selecting(type)
        )
    }

    private fun checkLocationPermission() {
        try {
            val appCtx = context.applicationContext
            val fineGranted = ContextCompat.checkSelfPermission(
                appCtx, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val coarseGranted = ContextCompat.checkSelfPermission(
                appCtx, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            uiState = uiState.copy(
                isLocationPermissionGranted = fineGranted || coarseGranted
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun onPermissionResult(granted: Boolean) {
        uiState = uiState.copy(isLocationPermissionGranted = granted)
        if (granted) {
            loadCurrentLocation()
        }
    }

    fun loadCurrentLocation() {
        if (uiState.isLocationPermissionGranted) {
            uiState = uiState.copy(isLoadingLocation = true)
            
            try {
                val appCtx = context.applicationContext
                val locationManager = appCtx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                
                // Try NETWORK_PROVIDER first (faster, works without GPS)
                var networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                
                // Fallback to GPS
                if (networkLocation == null) {
                    val gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (gpsLocation != null) {
                        networkLocation = gpsLocation
                    }
                }
                
                // Fallback to passive (other apps' location updates)
                if (networkLocation == null) {
                    val passiveLocation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                    if (passiveLocation != null) {
                        networkLocation = passiveLocation
                    }
                }
                
                if (networkLocation != null) {
                    uiState = uiState.copy(
                        currentLocation = networkLocation,
                        isLoadingLocation = false
                    )
                } else {
                    uiState = uiState.copy(isLoadingLocation = false)
                }
            } catch (e: Exception) {
                uiState = uiState.copy(isLoadingLocation = false)
            }
        }
    }

    fun requestLocationUpdate() {
        if (uiState.isLocationPermissionGranted) {
            try {
                val appCtx = context.applicationContext
                val locationManager = appCtx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                
                val locationListener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        uiState = uiState.copy(currentLocation = location, isLoadingLocation = false)
                    }
                    override fun onProviderDisabled(provider: String) {}
                    override fun onProviderEnabled(provider: String) {}
                    @Suppress("DEPRECATION")
                    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
                        // TODO: Migrate from deprecated LocationListener to FusedLocationProviderClient
                    }
                }
                
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, locationListener)
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
                
                // Try to get immediate location
                val location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                
                if (location != null) {
                    uiState = uiState.copy(currentLocation = location, isLoadingLocation = false)
                } else {
                    uiState = uiState.copy(isLoadingLocation = true)
                }
            } catch (e: Exception) {
                uiState = uiState.copy(isLoadingLocation = false)
            }
        }
    }

    companion object {
        val LocalMarkersViewModel = compositionLocalOf<MarkersViewModel?> { null }

        fun Factory(context: android.content.Context, profileName: String): androidx.lifecycle.ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(MarkersViewModel::class.java)) {
                        return MarkersViewModel(context, profileName) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
}
