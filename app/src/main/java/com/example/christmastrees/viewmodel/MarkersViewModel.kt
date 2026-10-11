package com.example.christmastrees.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.christmastrees.data.MarkerType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UiState(
    val markers: List<Marker> = emptyList(),
    val currentLocation: Location? = null,
    val isLocationPermissionGranted: Boolean = false,
    val isLoadingLocation: Boolean = false,
    val placeMarkerMode: PlaceMarkerMode = PlaceMarkerMode.Idle
)

sealed interface PlaceMarkerMode {
    object Idle : PlaceMarkerMode
    data class Selecting(val selectedType: MarkerType) : PlaceMarkerMode
}

data class Marker(
    val id: Long,
    val latitude: Double,
    val longitude: Double,
    val type: String,
    val timestamp: Long,
    val quantity: Int = 1
) {
    fun markerType(): MarkerType = MarkerType.fromString(type)
}

class MarkersViewModel(
    private val context: Context
) : ViewModel() {

    private val _markers = MutableStateFlow<List<Marker>>(emptyList())
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private var nextId = 1L
    var onMarkerAdded: ((Marker) -> Unit)? = null
    var onMarkersLoaded: ((Int) -> Unit)? = null

    init {
        try {
            checkLocationPermission()
            loadMarkersFromStorage()
            // Sync markers to uiState after loading
            _uiState.value = _uiState.value.copy(markers = _markers.value)
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
            
            val markers = mutableListOf<Marker>()
            
            for (i in 0 until markerCount) {
                val id = prefs.getLong("marker_$i.id", -1)
                val lat = java.lang.Double.longBitsToDouble(prefs.getLong("marker_$i.lat", java.lang.Double.doubleToLongBits(0.0)))
                val lon = java.lang.Double.longBitsToDouble(prefs.getLong("marker_$i.lon", java.lang.Double.doubleToLongBits(0.0)))
                val type = prefs.getString("marker_$i.type", null) ?: "CHRISTMAS_TREE"
                val timestamp = prefs.getLong("marker_$i.timestamp", 0L)
                val quantity = prefs.getInt("marker_$i.quantity", 1)
                
                markers.add(Marker(id, lat, lon, type, timestamp, quantity))
            }
            
            if (markers.isNotEmpty()) {
                _markers.value = markers
                
                // Set nextId to be larger than any existing marker id
                val maxId = markers.maxOfOrNull { it.id } ?: 0
                nextId = maxId + 1
                
                android.util.Log.d("MarkersViewModel", "Loaded ${markers.size} markers from storage")
                
                // Notify UI
                onMarkersLoaded?.invoke(markers.size)
            }
        } catch (e: Exception) {
            android.util.Log.e("MarkersViewModel", "Failed to load markers", e)
        }
    }
    
    fun refreshUiState() {
        _uiState.value = _uiState.value.copy(markers = _markers.value)
    }

    private fun saveMarkers() {
        try {
            val appCtx = context.applicationContext
            val prefs = appCtx.getSharedPreferences("markers_data", android.content.Context.MODE_PRIVATE)
            
            val editor = prefs.edit()
            editor.putInt("marker_count", _markers.value.size)
            
            _markers.value.forEachIndexed { index, marker ->
                editor.putLong("marker_$index.id", marker.id)
                editor.putLong("marker_$index.lat", java.lang.Double.doubleToLongBits(marker.latitude))
                editor.putLong("marker_$index.lon", java.lang.Double.doubleToLongBits(marker.longitude))
                editor.putString("marker_$index.type", marker.type)
                editor.putLong("marker_$index.timestamp", marker.timestamp)
                editor.putInt("marker_$index.quantity", marker.quantity)
            }
            editor.apply()
            
            android.util.Log.d("MarkersViewModel", "Saved ${_markers.value.size} markers to storage")
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
            timestamp = System.currentTimeMillis(),
            quantity = 1
        )
        _markers.value = _markers.value + newMarker
        android.util.Log.d("MarkersViewModel", "Markers count now: ${_markers.value.size}")
        _uiState.value = _uiState.value.copy(
            placeMarkerMode = PlaceMarkerMode.Idle,
            markers = _markers.value
        )
        
        // Save markers to storage
        saveMarkers()
        
        // Callback to add marker directly on map
        onMarkerAdded?.invoke(newMarker)
    }

    fun deleteMarker(marker: Marker) {
        _markers.value = _markers.value.filter { it.id != marker.id }
        _uiState.value = _uiState.value.copy(markers = _markers.value)
        saveMarkers()
    }

    fun incrementQuantity(markerId: Long) {
        _markers.value = _markers.value.map { marker ->
            if (marker.id == markerId) {
                marker.copy(quantity = marker.quantity + 1)
            } else {
                marker
            }
        }
        _uiState.value = _uiState.value.copy(markers = _markers.value)
        saveMarkers()
    }

    fun decrementQuantity(markerId: Long) {
        _markers.value = _markers.value.map { marker ->
            if (marker.id == markerId && marker.quantity > 1) {
                marker.copy(quantity = marker.quantity - 1)
            } else {
                marker
            }
        }
        _uiState.value = _uiState.value.copy(markers = _markers.value)
        saveMarkers()
    }

    fun cancelPlaceMarker() {
        _uiState.value = _uiState.value.copy(placeMarkerMode = PlaceMarkerMode.Idle)
    }

    fun startPlaceMarker(type: MarkerType) {
        _uiState.value = _uiState.value.copy(
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
            _uiState.value = _uiState.value.copy(
                isLocationPermissionGranted = fineGranted || coarseGranted
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun onPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(isLocationPermissionGranted = granted)
        if (granted) {
            loadCurrentLocation()
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun loadCurrentLocation() {
        if (_uiState.value.isLocationPermissionGranted) {
            _uiState.value = _uiState.value.copy(isLoadingLocation = true)
            
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
                    _uiState.value = _uiState.value.copy(
                        currentLocation = networkLocation,
                        isLoadingLocation = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoadingLocation = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingLocation = false)
            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun requestLocationUpdate() {
        if (_uiState.value.isLocationPermissionGranted) {
            try {
                val appCtx = context.applicationContext
                val locationManager = appCtx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                
                val locationListener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        _uiState.value = _uiState.value.copy(currentLocation = location, isLoadingLocation = false)
                    }
                    override fun onProviderDisabled(provider: String) {}
                    override fun onProviderEnabled(provider: String) {}
                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
                }
                
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, locationListener)
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
                
                // Try to get immediate location
                val location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                
                if (location != null) {
                    _uiState.value = _uiState.value.copy(currentLocation = location, isLoadingLocation = false)
                } else {
                    _uiState.value = _uiState.value.copy(isLoadingLocation = true)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingLocation = false)
            }
        }
    }

    companion object {
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
