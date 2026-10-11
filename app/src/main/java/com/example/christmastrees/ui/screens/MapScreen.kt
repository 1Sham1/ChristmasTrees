package com.example.christmastrees.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.christmastrees.data.MarkerType
import com.example.christmastrees.ui.components.MarkerPickerBottomSheet
import com.example.christmastrees.viewmodel.MarkersViewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker as OsmdroidMarker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MarkersViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showPicker by remember { mutableStateOf(false) }
    val pendingMarkerType = remember { mutableStateOf<MarkerType?>(null) }
    val mapReady = remember { mutableStateOf(false) }
    val userLocation = remember { mutableStateOf<GeoPoint?>(null) }
    var userMarker by remember { mutableStateOf<OsmdroidMarker?>(null) }
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    val prefs = context.getSharedPreferences("map_settings", android.content.Context.MODE_PRIVATE)
    var selectedMarkerId by remember { mutableStateOf<Long?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuPosition by remember { mutableStateOf(android.graphics.PointF(0f, 0f)) }
    var showLocationButton by remember { mutableStateOf(true) }
    
    // Compute counters reactively at TOP of composable
    val markers = uiState.markers
    val treesCount by remember(markers) { derivedStateOf { markers.count { it.markerType() == MarkerType.CHRISTMAS_TREE } } }
    val snowmenCount by remember(markers) { derivedStateOf { markers.count { it.markerType() == MarkerType.SNOWMAN } } }
    val decorationsCount by remember(markers) { derivedStateOf { markers.count { it.markerType() == MarkerType.CITY_DECORATION } } }
    
    // Function to refresh all markers on the map
    fun refreshMapMarkers() {
        val mapView = mapViewRef.value ?: return
        try {
            // Remove all custom markers (keep user location marker)
            val markersToRemove = mutableListOf<OsmdroidMarker>()
            for (overlay in mapView.overlays) {
                if (overlay is OsmdroidMarker && overlay.title != null && overlay.title != "Вы здесь") {
                    markersToRemove.add(overlay)
                }
            }
            mapView.overlays.removeAll(markersToRemove)
            
            // Re-add all markers from ViewModel state
            for (marker in markers) {
                val type = marker.markerType()
                val latLng = GeoPoint(marker.latitude, marker.longitude)
                val bitmap = createMarkerBitmap(type.emoji)
                
                val markerIcon = BitmapDrawable(mapView.resources, bitmap)
                val osmdroidMarker = OsmdroidMarker(mapView).apply {
                    position = latLng
                    setIcon(markerIcon)
                    setAnchor(OsmdroidMarker.ANCHOR_CENTER, OsmdroidMarker.ANCHOR_BOTTOM)
                    title = marker.id.toString()
                    snippet = "${type.emoji} x${marker.quantity}"
                }
                mapView.overlays.add(osmdroidMarker)
            }
            
            mapView.invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onPermissionResult(fineGranted || coarseGranted)
    }

    // Request permission once on start
    LaunchedEffect(Unit) {
        if (!uiState.isLocationPermissionGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            // Try to load location immediately if permission granted
            viewModel.loadCurrentLocation()
        }
    }
    
    // Set callback to add new markers directly on map
    LaunchedEffect(Unit) {
        viewModel.onMarkerAdded = { newMarker ->
            Handler(Looper.getMainLooper()).post {
                try {
                    val mapView = mapViewRef.value ?: return@post
                    val type = newMarker.markerType()
                    val latLng = GeoPoint(newMarker.latitude, newMarker.longitude)
                    val bitmap = createMarkerBitmap(type.emoji)
                    
                    val markerIcon = BitmapDrawable(mapView.resources, bitmap)
                    val osmdroidMarker = OsmdroidMarker(mapView).apply {
                        position = latLng
                        setIcon(markerIcon)
                        setAnchor(OsmdroidMarker.ANCHOR_CENTER, OsmdroidMarker.ANCHOR_BOTTOM)
                        title = newMarker.id.toString()
                        snippet = type.emoji
                    }
                    mapView.overlays.add(osmdroidMarker)
                    mapView.invalidate()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    // Hide context menu when tapping elsewhere
    if (showContextMenu) {
        LaunchedEffect(Unit) {
            // Auto-hide after 3 seconds
            delay(3000)
            showContextMenu = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                try {
                    val mapView = MapView(ctx)
                    
                    // Use standard OSM tile source
                    mapView.tileProvider = org.osmdroid.tileprovider.MapTileProviderBasic(
                        ctx,
                        TileSourceFactory.MAPNIK
                    )
                    
                    // Debug: show toast with tile provider info
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(ctx, "W:${mapView.width} H:${mapView.height} Z:${mapView.zoomLevelDouble}", Toast.LENGTH_SHORT).show()
                    }
                    
                    // Debug: log tile provider info
                    android.util.Log.d("MapScreen", "TileProvider: ${mapView.tileProvider}")
                    android.util.Log.d("MapScreen", "TileSource: ${mapView.tileProvider.tileSource}")
                    android.util.Log.d("MapScreen", "MapSize: ${mapView.width}x${mapView.height}")
                    android.util.Log.d("MapScreen", "Center: ${mapView.mapCenter}")
                    
                    // Load saved position or default to Moscow
                    val savedLat = prefs.getFloat("lat", 55.7558f)
                    val savedLon = prefs.getFloat("lon", 37.6173f)
                    val savedZoom = prefs.getFloat("zoom", 12f)
                    
                    // Set initial position
                    mapView.controller.setZoom(savedZoom.toDouble())
                    mapView.controller.setCenter(GeoPoint(savedLat.toDouble(), savedLon.toDouble()))
                    
                    // Add user location marker
                    userMarker = OsmdroidMarker(mapView).apply {
                        setAnchor(OsmdroidMarker.ANCHOR_CENTER, OsmdroidMarker.ANCHOR_BOTTOM)
                        title = "Вы здесь"
                    }
                    mapView.overlays.add(userMarker)
                    
                    // Enable pinch-to-zoom and hide zoom buttons
                    mapView.setMultiTouchControls(true)
                    
                    // Hide zoom buttons by finding them in the view hierarchy
                    fun hideZoomButtons(view: android.view.View) {
                        if (view is android.widget.ImageButton) {
                            view.visibility = android.view.View.GONE
                        } else if (view is android.view.ViewGroup) {
                            for (i in 0 until view.childCount) {
                                hideZoomButtons(view.getChildAt(i))
                            }
                        }
                    }
                    hideZoomButtons(mapView)
                    
                    // Save position when map is moved or zoomed
                    fun saveMapPosition(mv: MapView) {
                        prefs.edit().apply {
                            putFloat("lat", mv.mapCenter.latitude.toFloat())
                            putFloat("lon", mv.mapCenter.longitude.toFloat())
                            putFloat("zoom", mv.zoomLevelDouble.toFloat())
                            apply()
                        }
                    }
                    
                    // Add tap listener with null check
                    var lastSaveTime = 0L
                    mapView.setOnTouchListener(object : View.OnTouchListener {
                        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                            if (event != null) {
                                when (event.action) {
                                    MotionEvent.ACTION_MOVE -> {
                                        // Debounce saves during movement
                                        val now = System.currentTimeMillis()
                                        if (now - lastSaveTime > 500) {
                                            saveMapPosition(mapView)
                                            lastSaveTime = now
                                        }
                                    }
                                    MotionEvent.ACTION_UP -> {
                                        saveMapPosition(mapView)
                                        
                                        if (pendingMarkerType.value != null) {
                                            // Placing a new marker
                                            val proj = mapView.projection
                                            if (proj != null) {
                                                val latLng = proj.fromPixels(event.x.toInt(), event.y.toInt())
                                                if (latLng != null) {
                                                    try {
                                                        viewModel.placeMarker(
                                                            latLng.latitude,
                                                            latLng.longitude,
                                                            pendingMarkerType.value!!
                                                        )
                                                        pendingMarkerType.value = null

                                                    } catch (e: Exception) {
                                                        Handler(Looper.getMainLooper()).post {
                                                            Toast.makeText(ctx, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                                                        }
                                                        e.printStackTrace()
                                                    }
                                                }
                                            }
                                        } else {
                                            // Check if tap is on an existing marker
                                            val proj = mapView.projection
                                            if (proj != null) {
                                                val tappedGeoPoint = proj.fromPixels(event.x.toInt(), event.y.toInt())
                                                if (tappedGeoPoint != null) {
                                                    val currentMarkers = mapView.overlays.filterIsInstance<OsmdroidMarker>()
                                                    for (marker in currentMarkers) {
                                                        if (marker.title != null && marker.title != "Вы здесь") {
                                                            val markerPos = GeoPoint(marker.position.latitude, marker.position.longitude)
                                                            val distances = FloatArray(1)
                                                            android.location.Location.distanceBetween(
                                                                tappedGeoPoint.latitude,
                                                                tappedGeoPoint.longitude,
                                                                markerPos.latitude,
                                                                markerPos.longitude,
                                                                distances
                                                            )
                                                            if (distances[0] < 100f) {
                                                                selectedMarkerId = marker.title.toLongOrNull()
                                                                showContextMenu = true
                                                                contextMenuPosition = android.graphics.PointF(event.x, event.y)
                                                                viewModel.refreshUiState()
                                                                break
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            return false
                        }
                    })
                    
                    // Load saved markers from SharedPreferences
                    try {
                        val markerPrefs = ctx.getSharedPreferences("markers_data", android.content.Context.MODE_PRIVATE)
                        val markerCount = markerPrefs.getInt("marker_count", 0)
                        
                        for (i in 0 until markerCount) {
                            val id = markerPrefs.getLong("marker_$i.id", -1)
                            val lat = java.lang.Double.longBitsToDouble(markerPrefs.getLong("marker_$i.lat", java.lang.Double.doubleToLongBits(0.0)))
                            val lon = java.lang.Double.longBitsToDouble(markerPrefs.getLong("marker_$i.lon", java.lang.Double.doubleToLongBits(0.0)))
                            val type = markerPrefs.getString("marker_$i.type", "CHRISTMAS_TREE") ?: "CHRISTMAS_TREE"
                            val quantity = markerPrefs.getInt("marker_$i.quantity", 1)
                            
                            val markerType = MarkerType.fromString(type)
                            val latLng = GeoPoint(lat, lon)
                            val bitmap = createMarkerBitmap(markerType.emoji)
                            
                            val markerIcon = BitmapDrawable(mapView.resources, bitmap)
                            val osmdroidMarker = OsmdroidMarker(mapView).apply {
                                position = latLng
                                setIcon(markerIcon)
                                setAnchor(OsmdroidMarker.ANCHOR_CENTER, OsmdroidMarker.ANCHOR_BOTTOM)
                                title = id.toString()
                                snippet = markerType.emoji
                            }
                            mapView.overlays.add(osmdroidMarker)
                        }
                        
                        if (markerCount > 0) {
                            mapView.invalidate()
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(ctx, "Загружено $markerCount маркеров", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    
                    // Mark map as ready
                    mapReady.value = true
                    
                    mapViewRef.value = mapView
                    mapView
                } catch (e: Exception) {
                    e.printStackTrace()
                    MapView(ctx)
                }
            },
            update = { mapView ->
                // Update user location marker
                uiState.currentLocation?.let { location ->
                    val geoPoint = GeoPoint(location.latitude, location.longitude)
                    userMarker?.position = geoPoint
                    val currentZoom = mapView.zoomLevelDouble
                    val orangeBitmap = createLocationBitmap(currentZoom)
                    userMarker?.setIcon(BitmapDrawable(context.resources, orangeBitmap))
                }
            }
        )

        // FAB to go to user location
        if (pendingMarkerType.value == null && showLocationButton) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        viewModel.requestLocationUpdate()
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            uiState.currentLocation?.let { location ->
                                val geoPoint = GeoPoint(location.latitude, location.longitude)
                                mapViewRef.value?.controller?.setCenter(geoPoint)
                                mapViewRef.value?.controller?.setZoom(16.0)
                            }
                        }, 2000)
                    },
                    modifier = Modifier.padding(bottom = 16.dp),
                    containerColor = ComposeColor(0xFF1B5E20),
                    contentColor = ComposeColor(0xFFFFD700)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Go to my location", tint = ComposeColor(0xFFFFD700))
                }
                
                FloatingActionButton(
                    onClick = { showPicker = true },
                    containerColor = ComposeColor(0xFF1B5E20),
                    contentColor = ComposeColor(0xFFFFD700)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add marker", tint = ComposeColor(0xFFFFD700))
                }
            }
        } else {
            // Show tap hint and cancel button
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = pendingMarkerType.value!!.emoji,
                            fontSize = 24.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Нажмите на карту, чтобы поставить ${pendingMarkerType.value!!.label}",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                FloatingActionButton(
                    onClick = { pendingMarkerType.value = null }
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel placement")
                }
            }
        }

        // Context menu for marker
        if (showContextMenu && selectedMarkerId != null) {
            val selectedMarker = uiState.markers.find { it.id == selectedMarkerId }
            if (selectedMarker != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { showContextMenu = false }
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = ComposeColor(0xFFFFF8E1)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = selectedMarker.markerType().emoji,
                                    fontSize = 32.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = selectedMarker.markerType().label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Количество: ${selectedMarker.quantity}",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // +1 button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.incrementQuantity(selectedMarker.id)
                                            showContextMenu = false
                                            refreshMapMarkers()
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ComposeColor(0xFF1B5E20)
                                        )
                                    ) {
                                        Text("+1", color = ComposeColor(0xFFFFD700))
                                    }
                                    
                                    Button(
                                        onClick = {
                                            viewModel.decrementQuantity(selectedMarker.id)
                                            showContextMenu = false
                                            refreshMapMarkers()
                                        },
                                        modifier = Modifier.weight(1f),
                                        enabled = selectedMarker.quantity > 1,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ComposeColor(0xFF1B5E20)
                                        )
                                    ) {
                                        Text("-1", color = ComposeColor(0xFFFFD700))
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Delete button
                                OutlinedButton(
                                    onClick = {
                                        viewModel.deleteMarker(selectedMarker)
                                        showContextMenu = false
                                        refreshMapMarkers()
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = ComposeColor(0xFFC62828)
                                    )
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Удалить")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Picker bottom sheet
        if (showPicker) {
            MarkerPickerBottomSheet(
                onTypeSelected = { type ->
                    showPicker = false
                    pendingMarkerType.value = type
                },
                onDismiss = { showPicker = false }
            )
        }

        // Loading indicator
        if (uiState.isLoadingLocation) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        
        // Marker counters at the top (drawn last = on top)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CounterCard(
                emoji = "🎄",
                count = treesCount,
                label = "Ёлки",
                modifier = Modifier
            )
            CounterCard(
                emoji = "⛄",
                count = snowmenCount,
                label = "Снеговики",
                modifier = Modifier
            )
            CounterCard(
                emoji = "🎊",
                count = decorationsCount,
                label = "Украшения",
                modifier = Modifier
            )
        }
    }
}



private fun createLocationBitmap(zoomLevel: Double): Bitmap {
    // Fixed size marker - always same size on screen
    val size = 64
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    val cx = size / 2f
    val cy = size / 2f
    
    // Draw stick figure (person) - orange color, no background circle
    val personPaint = Paint().apply {
        color = Color.parseColor("#FF6B35")
        strokeWidth = 6f
        isAntiAlias = true
        style = Paint.Style.STROKE
    }
    
    val headRadius = 8f
    val headY = cy - 14f
    
    // Head circle
    personPaint.style = Paint.Style.STROKE
    canvas.drawCircle(cx, headY, headRadius, personPaint)
    
    // Body
    canvas.drawLine(cx, headY + headRadius, cx, cy + 16f, personPaint)
    
    // Arms
    canvas.drawLine(cx - 14f, cy - 4f, cx + 14f, cy - 4f, personPaint)
    
    // Legs
    canvas.drawLine(cx, cy + 16f, cx - 14f, cy + 30f, personPaint)
    canvas.drawLine(cx, cy + 16f, cx + 14f, cy + 30f, personPaint)
    
    return bitmap
}

@Composable
private fun CounterCard(
    emoji: String,
    count: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(60.dp).height(60.dp),
        colors = CardDefaults.cardColors(
            containerColor = ComposeColor(0xFF1B5E20)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = emoji,
                    fontSize = 20.sp
                )
                Text(
                    text = count.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ComposeColor(0xFFFFD700)
                )
            }
        }
    }
}

private fun createMarkerBitmap(emoji: String): Bitmap {
    val size = 100
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    val paint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    
    val borderPaint = Paint().apply {
        color = Color.GRAY
        strokeWidth = 4f
        isAntiAlias = true
        style = Paint.Style.STROKE
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, borderPaint)
    
    val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = size * 0.55f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(emoji, size / 2f, size * 0.65f, textPaint)
    
    return bitmap
}
