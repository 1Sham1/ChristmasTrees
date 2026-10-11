package com.example.christmastrees.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
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
import com.example.christmastrees.data.MarkerType
import com.example.christmastrees.ui.components.MarkerPickerBottomSheet
import com.example.christmastrees.ui.components.SnowfallOverlay
import com.example.christmastrees.viewmodel.MarkersViewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker as OsmdroidMarker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MarkersViewModel = MarkersViewModel.LocalMarkersViewModel.current ?: throw IllegalStateException("MarkersViewModel not provided")
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState
    val markers = viewModel.markers
    var showPicker by remember { mutableStateOf(false) }
    
    // Global counters from uiState - reactive
    val treesCount by derivedStateOf { uiState.treesCount }
    val snowmenCount by derivedStateOf { uiState.snowmenCount }
    val decorationsCount by derivedStateOf { uiState.decorationsCount }
    
    val pendingMarkerType = remember { mutableStateOf<MarkerType?>(null) }
    val mapReady = remember { mutableStateOf(false) }
    val userLocation = remember { mutableStateOf<GeoPoint?>(null) }
    var userMarker by remember { mutableStateOf<OsmdroidMarker?>(null) }
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    val prefs = context.getSharedPreferences("map_settings", android.content.Context.MODE_PRIVATE)
    var selectedMarkerId by remember { mutableStateOf<Long?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuMarkerName by remember { mutableStateOf("") }
    var contextMenuMarkerQuantity by remember { mutableStateOf("") }
    
    // Drag mode state - activated from context menu
    var isDraggingMarker by remember { mutableStateOf(false) }
    var draggingMarkerId by remember { mutableStateOf<Long?>(null) }
    var dragStartPos by remember { mutableStateOf<GeoPoint?>(null) }
    
    // Owner search hint
    var showOwnerSearchHint by remember { mutableStateOf(false) }
    
    // Reactive marker updates on map
    LaunchedEffect(markers) {
        val mapView = mapViewRef.value ?: return@LaunchedEffect
        Handler(Looper.getMainLooper()).post {
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
                    val bitmap = createMarkerBitmapWithQuantity(type.emoji, marker.quantity)
                    
                    val markerIcon = BitmapDrawable(mapView.resources, bitmap)
                    val osmdroidMarker = OsmdroidMarker(mapView).apply {
                        position = latLng
                        setIcon(markerIcon)
                        setAnchor(OsmdroidMarker.ANCHOR_CENTER, OsmdroidMarker.ANCHOR_BOTTOM)
                        title = marker.id.toString()
                        // NO snippet - prevents popup
                        setOnMarkerClickListener { clickedMarker, _ ->
                            // Handled by touch listener
                            false
                        }
                    }
                    mapView.overlays.add(osmdroidMarker)
                }
                
                mapView.invalidate()
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
                    val bitmap = createMarkerBitmapWithQuantity(type.emoji, newMarker.quantity)
                    
                    val markerIcon = BitmapDrawable(mapView.resources, bitmap)
                    val osmdroidMarker = OsmdroidMarker(mapView).apply {
                        position = latLng
                        setIcon(markerIcon)
                        setAnchor(OsmdroidMarker.ANCHOR_CENTER, OsmdroidMarker.ANCHOR_BOTTOM)
                        title = newMarker.id.toString()
                        setOnMarkerClickListener { _, _ -> false }
                    }
                    mapView.overlays.add(osmdroidMarker)
                    mapView.invalidate()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    // Save marker state when context menu is opened (to close it on tap outside)
    var contextMenuOpenedMarkerId by remember { mutableStateOf<Long?>(null) }

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
                    
                    // Disable osmdroid's built-in multi-touch controls (they add zoom buttons)
                    mapView.setMultiTouchControls(false)
                    
                    // Implement custom pinch-to-zoom without zoom buttons
                    val scaleDetector = android.view.ScaleGestureDetector(
                        ctx,
                        object : android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
                            override fun onScale(detector: android.view.ScaleGestureDetector): Boolean {
                                val currentZoom = mapView.zoomLevelDouble
                                val zoomDelta = (detector.scaleFactor - 1f) * 2
                                val newZoom = currentZoom + zoomDelta
                                val clampedZoom = newZoom.coerceIn(2.0, 20.0)
                                mapView.controller.setZoom(clampedZoom)
                                return true
                            }
                        }
                    )
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
                    var lastTapTime = 0L
                    
                    mapView.setOnTouchListener(object : View.OnTouchListener {
                        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                            if (event != null) {
                                // Handle pinch-to-zoom
                                scaleDetector.onTouchEvent(event)
                                
                                // Block double-tap to prevent zoom
                                if (event.action == MotionEvent.ACTION_DOWN) {
                                    val now = System.currentTimeMillis()
                                    if (now - lastTapTime < 300L) {
                                        lastTapTime = now
                                        return true // Block double-tap
                                    }
                                    lastTapTime = now
                                }
                                
                                when (event.action) {
                                    MotionEvent.ACTION_MOVE -> {
                                        if (isDraggingMarker && draggingMarkerId != null) {
                                            // Dragging a marker - only update visual position
                                            val proj = mapView.projection
                                            if (proj != null) {
                                                val newGeoPoint = proj.fromPixels(event.x.toInt(), event.y.toInt())
                                                if (newGeoPoint != null) {
                                                    val currentMarkers = mapView.overlays.filterIsInstance<OsmdroidMarker>()
                                                    for (marker in currentMarkers) {
                                                        if (marker.title != null && marker.title != "Вы здесь") {
                                                            val markerId = marker.title.toLongOrNull()
                                                            if (markerId == draggingMarkerId) {
                                                                val newPos = GeoPoint(newGeoPoint.latitude, newGeoPoint.longitude)
                                                                marker.position = newPos
                                                                mapView.invalidate()
                                                                break
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            return true // Consume event to prevent map panning
                                        }
                                        // Debounce saves during movement
                                        val now = System.currentTimeMillis()
                                        if (now - lastSaveTime > 500) {
                                            saveMapPosition(mapView)
                                            lastSaveTime = now
                                        }
                                    }
                                    MotionEvent.ACTION_UP -> {
                                        saveMapPosition(mapView)
                                        
                                        if (isDraggingMarker && draggingMarkerId != null) {
                                            // Save the final position
                                            val proj = mapView.projection
                                            if (proj != null) {
                                                val tappedGeoPoint = proj.fromPixels(event.x.toInt(), event.y.toInt())
                                                if (tappedGeoPoint != null) {
                                                    val currentMarkers = mapView.overlays.filterIsInstance<OsmdroidMarker>()
                                                    for (marker in currentMarkers) {
                                                        if (marker.title != null && marker.title != "Вы здесь") {
                                                            val markerId = marker.title.toLongOrNull()
                                                            if (markerId == draggingMarkerId) {
                                                                viewModel.moveMarker(markerId!!, tappedGeoPoint.latitude, tappedGeoPoint.longitude)
                                                                break
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            // Finish drag mode
                                            isDraggingMarker = false
                                            draggingMarkerId = null
                                        } else if (pendingMarkerType.value != null) {
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
                                        } else if (showContextMenu) {
                                            // Close context menu when tapping outside of it
                                            showContextMenu = false
                                            contextMenuOpenedMarkerId = null
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
                                                                contextMenuOpenedMarkerId = marker.title.toLongOrNull()
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

        // Bottom action buttons - horizontal row
        if (pendingMarkerType.value == null) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Location button
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
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Go to my location", tint = MaterialTheme.colorScheme.secondary)
                }

                // Add marker button
                FloatingActionButton(
                    onClick = { showPicker = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add marker", tint = MaterialTheme.colorScheme.secondary)
                }

                // Owner search button
                FloatingActionButton(
                    onClick = {
                        showOwnerSearchHint = !showOwnerSearchHint
                        viewModel.toggleOwnerSearchHint()
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search owner", tint = MaterialTheme.colorScheme.secondary)
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
        LaunchedEffect(showContextMenu, selectedMarkerId) {
            if (showContextMenu && selectedMarkerId != null) {
                val marker = markers.find { it.id == selectedMarkerId }
                if (marker != null) {
                    contextMenuMarkerName = marker.name
                    contextMenuMarkerQuantity = marker.quantity.toString()
                }
            }
        }

        if (showContextMenu && selectedMarkerId != null) {
            val selectedMarker = markers.find { it.id == selectedMarkerId }
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
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = selectedMarker.markerType().emoji,
                                    fontSize = 32.sp
                                )

                                // Editable name field
                                Text(
                                    text = "Имя", fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                BasicTextField(
                                    value = contextMenuMarkerName,
                                    onValueChange = { contextMenuMarkerName = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    decorationBox = { innerTextField ->
                                        if (contextMenuMarkerName.isEmpty()) {
                                            Text(
                                                text = selectedMarker.markerType().label,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        innerTextField()
                                    }
                                )

                                // Editable quantity field
                                Text(
                                    text = "Количество", fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            val current = contextMenuMarkerQuantity.toIntOrNull() ?: 1
                                            if (current > 1) {
                                                contextMenuMarkerQuantity = (current - 1).toString()
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Text("−", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    }

                                    BasicTextField(
                                        value = contextMenuMarkerQuantity,
                                        onValueChange = { newValue ->
                                            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                                contextMenuMarkerQuantity = newValue
                                            }
                                        },
                                        modifier = Modifier
                                            .width(50.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(6.dp),
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                    )

                                    IconButton(
                                        onClick = {
                                            val current = contextMenuMarkerQuantity.toIntOrNull() ?: 1
                                            contextMenuMarkerQuantity = (current + 1).toString()
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Save button
                                Button(
                                    onClick = {
                                        viewModel.updateMarkerName(selectedMarker.id, contextMenuMarkerName)
                                        val newQuantity = contextMenuMarkerQuantity.toIntOrNull()
                                        if (newQuantity != null && newQuantity >= 1) {
                                            viewModel.setMarkerQuantity(selectedMarker.id, newQuantity)
                                        }
                                        showContextMenu = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Сохранить")
                                }

                                // Drag button
                                Button(
                                    onClick = {
                                        val selected = markers.find { it.id == selectedMarker.id }
                                        isDraggingMarker = true
                                        draggingMarkerId = selectedMarker.id
                                        dragStartPos = selected?.let { GeoPoint(it.latitude, it.longitude) }
                                        showContextMenu = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Переместить", color = MaterialTheme.colorScheme.onSecondary)
                                }

                                // Delete button
                                OutlinedButton(
                                    onClick = {
                                        viewModel.deleteMarker(selectedMarker)
                                        showContextMenu = false
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.tertiary
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

        // Drag mode hint
        if (isDraggingMarker) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 56.dp, start = 8.dp, end = 8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(),
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.secondary
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Перемещение маркера. Нажмите на карту, чтобы установить новую позицию.",
                                color = MaterialTheme.colorScheme.onSecondary,
                                fontSize = 14.sp
                            )
                        }
                        TextButton(
                            onClick = {
                                // Cancel drag - restore original position
                                val mapView = mapViewRef.value
                                if (mapView != null && dragStartPos != null && draggingMarkerId != null) {
                                    val currentMarkers = mapView.overlays.filterIsInstance<OsmdroidMarker>()
                                    for (marker in currentMarkers) {
                                        if (marker.title != null && marker.title != "Вы здесь") {
                                            val markerId = marker.title.toLongOrNull()
                                            if (markerId == draggingMarkerId) {
                                                marker.position = dragStartPos
                                                viewModel.moveMarker(draggingMarkerId!!, dragStartPos!!.latitude, dragStartPos!!.longitude)
                                                mapView.invalidate()
                                                break
                                            }
                                        }
                                    }
                                }
                                isDraggingMarker = false
                                draggingMarkerId = null
                                dragStartPos = null
                            }
                        ) {
                            Text(
                                text = "Отмена",
                                color = MaterialTheme.colorScheme.onSecondary,
                                fontWeight = FontWeight.Bold
                            )
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
        
        // Snowfall overlay
        if (uiState.isSnowEnabled) {
            SnowfallOverlay(modifier = Modifier.fillMaxSize())
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
    val size = 64
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    val cx = size / 2f
    val cy = size / 2f
    
    val personPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#FF6B35")
        strokeWidth = 6f
        isAntiAlias = true
        style = Paint.Style.STROKE
    }
    
    val headRadius = 8f
    val headY = cy - 14f
    
    personPaint.style = Paint.Style.STROKE
    canvas.drawCircle(cx, headY, headRadius, personPaint)
    canvas.drawLine(cx, headY + headRadius, cx, cy + 16f, personPaint)
    canvas.drawLine(cx - 14f, cy - 4f, cx + 14f, cy - 4f, personPaint)
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
            containerColor = MaterialTheme.colorScheme.primary
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
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

private fun createMarkerBitmapWithQuantity(emoji: String, quantity: Int): Bitmap {
    val size = 100
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    // Draw white circle background
    val paint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    
    // Draw border
    val borderPaint = Paint().apply {
        color = Color.GRAY
        strokeWidth = 4f
        isAntiAlias = true
        style = Paint.Style.STROKE
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, borderPaint)
    
    // Draw emoji
    val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = size * 0.45f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(emoji, size / 2f, size * 0.6f, textPaint)
    
    // Draw quantity badge (larger red circle with white number) at top-right
    if (quantity > 1) {
        val badgeRadius = 28f
        val badgeX = size - 22f
        val badgeY = 22f
        
        val badgePaint = Paint().apply {
            color = Color.parseColor("#C62828") // Red
            isAntiAlias = true
        }
        canvas.drawCircle(badgeX, badgeY, badgeRadius, badgePaint)
        
        val badgeTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 28f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            // Vertical centering
            val textBounds = Rect()
            textPaint.getTextBounds(quantity.toString(), 0, quantity.toString().length, textBounds)
        }
        canvas.drawText(quantity.toString(), badgeX, badgeY + 10f, badgeTextPaint)
    }
    
    return bitmap
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
