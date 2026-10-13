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
import com.example.christmastrees.data.MarkerSize
import com.example.christmastrees.data.MarkerType
import com.example.christmastrees.util.SettingsManager

data class UiState(
    val markers: List<Marker> = emptyList(),
    val treesCount: Int = 0,
    val snowmenCount: Int = 0,
    val decorationsCount: Int = 0,
    val treesSmallCount: Int = 0,
    val treesNormalCount: Int = 0,
    val treesBossCount: Int = 0,
    val snowmenSmallCount: Int = 0,
    val snowmenNormalCount: Int = 0,
    val snowmenBossCount: Int = 0,
    val decorationsSmallCount: Int = 0,
    val decorationsNormalCount: Int = 0,
    val decorationsBossCount: Int = 0,
    val currentLocation: Location? = null,
    val isLocationPermissionGranted: Boolean = false,
    val isLoadingLocation: Boolean = false,
    val placeMarkerMode: PlaceMarkerMode = PlaceMarkerMode.Idle,
    val isSnowEnabled: Boolean = true,
    val showOwnerSearchHint: Boolean = false,
    val centerOnLocation: Boolean = false
)

private fun computeCounts(m: List<Marker>): List<IntArray> {
    var tTotal = 0
    var sTotal = 0
    var dTotal = 0
    var tSmall = 0
    var tNormal = 0
    var tBoss = 0
    var sSmall = 0
    var sNormal = 0
    var sBoss = 0
    var dSmall = 0
    var dNormal = 0
    var dBoss = 0

    // Accumulate small quantities separately for correct division
    var tSmallAcc = 0
    var sSmallAcc = 0
    var dSmallAcc = 0

    for (marker in m) {
        val mSize = marker.markerSize()
        when (marker.markerType()) {
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
                        tBoss++
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
                        sBoss++
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
                        dBoss++
                        dTotal += marker.quantity
                    }
                }
            }
        }
    }
    // SMALL markers contribute 1/10 of their quantity to the total count.
    // This is because small markers represent fractional coverage.
    tTotal += tSmallAcc / 10
    sTotal += sSmallAcc / 10
    dTotal += dSmallAcc / 10

    return listOf(
        intArrayOf(tTotal, sTotal, dTotal),
        intArrayOf(tSmall, tNormal, tBoss),
        intArrayOf(sSmall, sNormal, sBoss),
        intArrayOf(dSmall, dNormal, dBoss)
    )
}

sealed interface PlaceMarkerMode {
    object Idle : PlaceMarkerMode
    data class Selecting(val selectedType: MarkerType) : PlaceMarkerMode
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
}

class MarkersViewModel(
    private val context: Context
) : ViewModel() {

    var markers by mutableStateOf<List<Marker>>(emptyList())
        private set
    
    var uiState by mutableStateOf(UiState())
        internal set
    
    private var nextId = 1L
    var onMarkerAdded: ((Marker) -> Unit)? = null
    var onMarkersLoaded: ((Int) -> Unit)? = null

    init {
        try {
            checkLocationPermission()
            loadMarkersFromStorage()
            val snowEnabled = loadSnowState()
            uiState = uiState.copy(isSnowEnabled = snowEnabled)
            syncUiState()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadMarkersFromStorage() {
        try {
            val appCtx = context.applicationContext
            val prefs = appCtx.getSharedPreferences("markers_data", android.content.Context.MODE_PRIVATE)
            
            val markerCount = prefs.getInt("marker_count", 0)
            android.util.Log.d("MarkersViewModel", "loadMarkersFromStorage: markerCount=$markerCount")
            
            val markerList = mutableListOf<Marker>()
            
            for (i in 0 until markerCount) {
                val id = prefs.getLong("marker_$i.id", -1)
                val lat = java.lang.Double.longBitsToDouble(prefs.getLong("marker_$i.lat", java.lang.Double.doubleToLongBits(0.0)))
                val lon = java.lang.Double.longBitsToDouble(prefs.getLong("marker_$i.lon", java.lang.Double.doubleToLongBits(0.0)))
                val type = prefs.getString("marker_$i.type", null) ?: "CHRISTMAS_TREE"
                val name = prefs.getString("marker_$i.name", null) ?: MarkerType.fromString(type).label
                val timestamp = prefs.getLong("marker_$i.timestamp", 0L)
                val quantity = prefs.getInt("marker_$i.quantity", 1)
                val sizeName = prefs.getString("marker_$i.size", null) ?: MarkerSize.NORMAL.name
                
                markerList.add(Marker(id, lat, lon, type, name, timestamp, quantity, sizeName))
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
        val totals = counts[0]
        val treeSizes = counts[1]
        val snowmanSizes = counts[2]
        val decorationSizes = counts[3]

        uiState = uiState.copy(
            markers = markers,
            treesCount = totals[0],
            snowmenCount = totals[1],
            decorationsCount = totals[2],
            treesSmallCount = treeSizes[0],
            treesNormalCount = treeSizes[1],
            treesBossCount = treeSizes[2],
            snowmenSmallCount = snowmanSizes[0],
            snowmenNormalCount = snowmanSizes[1],
            snowmenBossCount = snowmanSizes[2],
            decorationsSmallCount = decorationSizes[0],
            decorationsNormalCount = decorationSizes[1],
            decorationsBossCount = decorationSizes[2]
        )
        android.util.Log.d("MarkersViewModel", "syncUiState: trees=${totals[0]} snowmen=${totals[1]} decorations=${totals[2]}")
    }
    
    fun refreshUiState() {
        syncUiState()
    }

    fun toggleSnow() {
        val newState = !uiState.isSnowEnabled
        uiState = uiState.copy(isSnowEnabled = newState)
        SettingsManager.saveSnowEnabled(context, newState)
    }

    private fun loadSnowState(): Boolean {
        return SettingsManager.isSnowEnabled(context)
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
            val prefs = appCtx.getSharedPreferences("markers_data", android.content.Context.MODE_PRIVATE)
            
            val editor = prefs.edit()
            editor.putInt("marker_count", markers.size)
            
            markers.forEachIndexed { index, marker ->
                editor.putLong("marker_$index.id", marker.id)
                editor.putLong("marker_$index.lat", java.lang.Double.doubleToLongBits(marker.latitude))
                editor.putLong("marker_$index.lon", java.lang.Double.doubleToLongBits(marker.longitude))
                editor.putString("marker_$index.type", marker.type)
                editor.putString("marker_$index.name", marker.name)
                editor.putLong("marker_$index.timestamp", marker.timestamp)
                editor.putInt("marker_$index.quantity", marker.quantity)
                editor.putString("marker_$index.size", marker.size)
            }
            editor.apply()
            
            android.util.Log.d("MarkersViewModel", "Saved ${markers.size} markers to storage")
        } catch (e: Exception) {
            android.util.Log.e("MarkersViewModel", "Failed to save markers", e)
        }
    }

    fun placeMarker(latitude: Double, longitude: Double, type: MarkerType) {
        android.util.Log.d("MarkersViewModel", "placeMarker called: lat=$latitude, lon=$longitude, type=${type.name}")
        val newMarker = Marker(
            id = nextId++,
            latitude = latitude,
            longitude = longitude,
            type = type.name,
            name = type.label,
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
        markers = markers.filter { it.id != marker.id }
        syncUiState()
        saveMarkers()
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

    fun startPlaceMarker(type: MarkerType) {
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

        fun Factory(context: android.content.Context): androidx.lifecycle.ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(MarkersViewModel::class.java)) {
                        return MarkersViewModel(context) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
}
