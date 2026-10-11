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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.christmastrees.data.MarkerType
import com.example.christmastrees.ui.components.GarlandOverlay
import com.example.christmastrees.ui.components.MarkerPickerBottomSheet
import com.example.christmastrees.ui.components.SnowfallOverlay
import com.example.christmastrees.ui.navigation.LocalShowSettingsDialog
import com.example.christmastrees.viewmodel.MarkersViewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker as OsmdroidMarker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MarkersViewModel = MarkersViewModel.LocalMarkersViewModel.current ?: throw IllegalStateException("MarkersViewModel not provided"),
    showPicker: Boolean,
    onPickerStateChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState
    val markers = viewModel.markers
    var selectedMarkerId by remember { mutableStateOf<Long?>(null) }
    val mapReady = remember { mutableStateOf(false) }
    val userLocation = remember { mutableStateOf<GeoPoint?>(null) }
    var userMarker by remember { mutableStateOf<OsmdroidMarker?>(null) }
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    val prefs = context.getSharedPreferences("map_settings", android.content.Context.MODE_PRIVATE)
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuMarkerName by remember { mutableStateOf("") }
    var contextMenuMarkerQuantity by remember { mutableStateOf("") }
    var contextMenuOpenedMarkerId by remember { mutableStateOf<Long?>(null) }
    
    // Observe settings dialog state and close context menu when opened
    val showSettings = LocalShowSettingsDialog.current
    
    LaunchedEffect(showSettings) {
        if (showSettings && showContextMenu) {
            showContextMenu = false
            contextMenuOpenedMarkerId = null
        }
    }
    
    // Global counters from uiState - reactive
    val treesCount by derivedStateOf { uiState.treesCount }
    val snowmenCount by derivedStateOf { uiState.snowmenCount }
    val decorationsCount by derivedStateOf { uiState.decorationsCount }
    
    val pendingMarkerType = remember { mutableStateOf<MarkerType?>(null) }
    
    // Drag mode state - activated from context menu
    var isDraggingMarker by remember { mutableStateOf(false) }
    var draggingMarkerId by remember { mutableStateOf<Long?>(null) }
    var dragStartPos by remember { mutableStateOf<GeoPoint?>(null) }
    
    // Owner search hint
    var showOwnerSearchHint by remember { mutableStateOf(false) }
    
    // Sync picker state from parent
    var localShowPicker by remember { mutableStateOf(false) }
    LaunchedEffect(showPicker) {
        localShowPicker = showPicker
    }
    
    // Center map on location when requested
    LaunchedEffect(uiState.centerOnLocation, uiState.currentLocation) {
        if (uiState.centerOnLocation && uiState.currentLocation != null) {
            val geoPoint = GeoPoint(uiState.currentLocation!!.latitude, uiState.currentLocation!!.longitude)
            mapViewRef.value?.controller?.setCenter(geoPoint)
            mapViewRef.value?.controller?.setZoom(16.0)
            viewModel.uiState = viewModel.uiState.copy(centerOnLocation = false)
        }
    }
    
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
                    val bitmap = createEmojiMarkerBitmap(type.emoji, marker.quantity)
                    
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
                    val bitmap = createEmojiMarkerBitmap(type.emoji, newMarker.quantity)
                    
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
    // contextMenuOpenedMarkerId is declared above

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
                                            // Check if tap is on an existing marker (pixel-based hitbox)
                                            val proj = mapView.projection
                                            if (proj != null) {
                                                val currentMarkers = mapView.overlays.filterIsInstance<OsmdroidMarker>()
                                                for (marker in currentMarkers) {
                                                    if (marker.title != null && marker.title != "Вы здесь") {
                                                        val markerScreenPos = proj.toPixels(marker.position, null)
                                                        if (markerScreenPos != null) {
                                                            val dx = event.x - markerScreenPos.x
                                                            val dy = event.y - markerScreenPos.y
                                                            val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                                                            // Hitbox scales with zoom: larger at low zoom for easier tapping
                                                            val hitboxRadius = 60f * (10f / mapView.zoomLevel)
                                                            if (distance < hitboxRadius) {
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

        // Show tap hint when placing a marker
        if (pendingMarkerType.value != null) {
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
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pendingMarkerType.value!!.emoji,
                            fontSize = 24.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Нажмите на карту, чтобы поставить ${pendingMarkerType.value!!.label}",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                FloatingActionButton(
                    onClick = { pendingMarkerType.value = null }
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Отмена размещения")
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
                                    text = "Имя",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        innerTextField()
                                    }
                                )

                                // Editable quantity field
                                Text(
                                    text = "Количество",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                style = MaterialTheme.typography.bodyMedium
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
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }

        // Picker bottom sheet
        if (localShowPicker) {
            MarkerPickerBottomSheet(
                onTypeSelected = { type ->
                    localShowPicker = false
                    onPickerStateChanged(false)
                    pendingMarkerType.value = type
                },
                onDismiss = {
                    localShowPicker = false
                    onPickerStateChanged(false)
                }
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
        
        // Garland overlay at the top
        GarlandOverlay()

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
                markerType = MarkerType.CHRISTMAS_TREE,
                count = treesCount,
                label = "Ёлки"
            )
            CounterCard(
                markerType = MarkerType.SNOWMAN,
                count = snowmenCount,
                label = "Снеговики"
            )
            CounterCard(
                markerType = MarkerType.CITY_DECORATION,
                count = decorationsCount,
                label = "Украшения"
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
    markerType: MarkerType,
    count: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.wrapContentWidth().wrapContentHeight(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = markerType.emoji,
                    fontSize = 20.sp
                )
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

private fun createEmojiMarkerBitmap(emoji: String, quantity: Int): Bitmap {
    val size = 100
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = size / 2f
    val cy = size / 2f
    
    // Draw background circle
    val bgPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#E3F2FD")
        isAntiAlias = true
    }
    canvas.drawCircle(cx, cy, size / 2f, bgPaint)
    
    // Draw border circle
    val borderPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#FFFFFF")
        strokeWidth = 3f
        isAntiAlias = true
        style = Paint.Style.STROKE
    }
    canvas.drawCircle(cx, cy, size / 2f - 2f, borderPaint)
    
    // Draw emoji centered
    val emojiPaint = Paint().apply {
        textSize = 56f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(emoji, cx, cy + 18f, emojiPaint)
    
    // Draw quantity badge at top-right
    if (quantity > 1) {
        val badgeRadius = 20f
        val badgeX = size - 18f
        val badgeY = 18f
        
        val badgePaint = Paint().apply {
            color = Color.parseColor("#C62828")
            isAntiAlias = true
        }
        canvas.drawCircle(badgeX, badgeY, badgeRadius, badgePaint)
        
        val badgeTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(quantity.toString(), badgeX, badgeY + 7f, badgeTextPaint)
    }
    
    return bitmap
}

private fun drawRealisticTree(canvas: Canvas, size: Int) {
    val cx = size / 2f
    val cy = size / 2f
    
    // Background circle - light blue like snowman
    val bgPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#E3F2FD")
        isAntiAlias = true
    }
    canvas.drawCircle(cx, cy, size / 2f, bgPaint)
    
    // Trunk - brown with texture lines
    val trunkPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#5D4037")
        isAntiAlias = true
    }
    val trunkLinePaint = Paint().apply {
        color = android.graphics.Color.parseColor("#4E342E")
        strokeWidth = 1f
        isAntiAlias = true
    }
    canvas.drawRect(cx - 6f, cy + 16f, cx + 6f, cy + 30f, trunkPaint)
    // Texture lines on trunk
    canvas.drawLine(cx - 3f, cy + 18f, cx - 3f, cy + 28f, trunkLinePaint)
    canvas.drawLine(cx + 2f, cy + 18f, cx + 2f, cy + 28f, trunkLinePaint)
    
    // Tree crown - 3 tiers (rich green)
    val tierPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#2E7D32")
        isAntiAlias = true
    }
    val highlightPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#4CAF50")
        isAntiAlias = true
    }
    val shadowPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#1B5E20")
        isAntiAlias = true
    }
    // Bottom tier
    canvas.drawPath(createTriangle(cx, cy + 6f, 30f, 22f), tierPaint)
    canvas.drawPath(createTriangle(cx + 3f, cy + 8f, 24f, 18f), shadowPaint)
    // Middle tier
    canvas.drawPath(createTriangle(cx, cy - 8f, 24f, 20f), tierPaint)
    canvas.drawPath(createTriangle(cx + 2f, cy - 6f, 19f, 16f), shadowPaint)
    // Top tier
    canvas.drawPath(createTriangle(cx, cy - 22f, 18f, 18f), tierPaint)
    canvas.drawPath(createTriangle(cx + 2f, cy - 20f, 14f, 14f), shadowPaint)
    
    // Branch highlights (lighter green edges)
    val edgeHighlightPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#66BB6A")
        strokeWidth = 1.5f
        isAntiAlias = true
    }
    // Bottom tier highlights
    canvas.drawLine(cx - 15f, cy + 18f, cx - 3f, cy + 6f, edgeHighlightPaint)
    canvas.drawLine(cx + 15f, cy + 18f, cx + 3f, cy + 6f, edgeHighlightPaint)
    // Middle tier highlights
    canvas.drawLine(cx - 12f, cy + 4f, cx - 2f, cy - 8f, edgeHighlightPaint)
    canvas.drawLine(cx + 12f, cy + 4f, cx + 2f, cy - 8f, edgeHighlightPaint)
    
    // Gold star on top
    val starPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#FFD700")
        isAntiAlias = true
    }
    val starGlowPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#FFECB3")
        isAntiAlias = true
    }
    canvas.drawPath(createStar(cx, cy - 30f, 4f, 8f), starPaint)
    canvas.drawPath(createStar(cx, cy - 30f, 2f, 5f), starGlowPaint)
    
    // Realistic ornaments (colorful baubles with highlights)
    val ornamentColors = listOf(
        android.graphics.Color.parseColor("#D32F2F"), // Red
        android.graphics.Color.parseColor("#FFD700"), // Gold
        android.graphics.Color.parseColor("#1976D2"), // Blue
        android.graphics.Color.parseColor("#C2185B"), // Pink
        android.graphics.Color.parseColor("#7B1FA2"), // Purple
        android.graphics.Color.parseColor("#D32F2F")  // Red
    )
    val ornamentPositions = listOf(
        Pair(cx - 12f, cy + 12f), Pair(cx + 10f, cy + 16f),
        Pair(cx - 8f, cy - 2f), Pair(cx + 12f, cy - 6f),
        Pair(cx - 6f, cy - 18f), Pair(cx + 7f, cy - 24f)
    )
    
    for (i in ornamentPositions.indices) {
        val (ox, oy) = ornamentPositions[i]
        val ornamentColor = ornamentColors[i]
        
        // Main ornament sphere
        val ornamentPaint = Paint().apply {
            color = ornamentColor
            isAntiAlias = true
        }
        canvas.drawCircle(ox, oy, 3.5f, ornamentPaint)
        
        // Highlight (shine effect)
        val highlightPaint2 = Paint().apply {
            color = android.graphics.Color.parseColor("#FFFFFF")
            isAntiAlias = true
            alpha = 150
        }
        canvas.drawCircle(ox - 1f, oy - 1f, 1.5f, highlightPaint2)
        
        // Small cap on top of ornament
        val capPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#B0BEC5")
            isAntiAlias = true
        }
        canvas.drawRect(ox - 2f, oy - 4.5f, ox + 2f, oy - 3f, capPaint)
    }
    
    // Garland (colorful lights wrapped around tree)
    val garlandColors = listOf(
        android.graphics.Color.parseColor("#FF5252"), // Red
        android.graphics.Color.parseColor("#FFEB3B"), // Yellow
        android.graphics.Color.parseColor("#448AFF"), // Blue
        android.graphics.Color.parseColor("#69F0AE"), // Green
        android.graphics.Color.parseColor("#FF5252"), // Red
        android.graphics.Color.parseColor("#FFEB3B"), // Yellow
        android.graphics.Color.parseColor("#448AFF"), // Blue
        android.graphics.Color.parseColor("#69F0AE")  // Green
    )
    val garlandPositions = listOf(
        Pair(cx - 18f, cy + 14f), Pair(cx + 18f, cy + 14f),
        Pair(cx - 14f, cy + 4f), Pair(cx + 14f, cy + 4f),
        Pair(cx - 10f, cy - 10f), Pair(cx + 10f, cy - 10f),
        Pair(cx - 7f, cy - 20f), Pair(cx + 7f, cy - 20f)
    )
    
    for (i in garlandPositions.indices) {
        val (gx, gy) = garlandPositions[i]
        val garlandColor = garlandColors[i]
        
        // Light bulb
        val bulbPaint = Paint().apply {
            color = garlandColor
            isAntiAlias = true
        }
        canvas.drawCircle(gx, gy, 2f, bulbPaint)
        
        // Glow effect
        val glowPaint = Paint().apply {
            color = garlandColor
            isAntiAlias = true
            alpha = 60
        }
        canvas.drawCircle(gx, gy, 4f, glowPaint)
    }
    
    // Snow particles on branches
    val snowPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#FFFFFF")
        isAntiAlias = true
        alpha = 180
    }
    val snowPositions = listOf(
        Pair(cx - 10f, cy + 10f), Pair(cx + 14f, cy + 12f),
        Pair(cx - 6f, cy - 4f), Pair(cx + 10f, cy - 10f),
        Pair(cx - 4f, cy - 18f), Pair(cx + 5f, cy - 22f),
        Pair(cx - 16f, cy + 16f), Pair(cx + 18f, cy + 8f)
    )
    for ((sx, sy) in snowPositions) {
        canvas.drawCircle(sx, sy, 1.2f, snowPaint)
    }
}

private fun createTriangle(cx: Float, topY: Float, bottomWidth: Float, height: Float): android.graphics.Path {
    val path = android.graphics.Path()
    path.moveTo(cx, topY)
    path.lineTo(cx - bottomWidth / 2f, topY + height)
    path.lineTo(cx + bottomWidth / 2f, topY + height)
    path.close()
    return path
}

private fun createStar(cx: Float, cy: Float, innerRadius: Float, outerRadius: Float): android.graphics.Path {
    val path = android.graphics.Path()
    val points = 5
    for (i in 0 until points * 2) {
        val radius = if (i % 2 == 0) outerRadius else innerRadius
        val angle = (Math.PI * i / points) - Math.PI / 2
        val x = cx + radius * kotlin.math.cos(angle).toFloat()
        val y = cy + radius * kotlin.math.sin(angle).toFloat()
        if (i == 0) path.moveTo(x, y)
        else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun drawRealisticSnowman(canvas: Canvas, size: Int) {
    val cx = size / 2f
    val cy = size / 2f
    
    // Background circle - light blue
    val bgPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#E3F2FD")
        isAntiAlias = true
    }
    canvas.drawCircle(cx, cy, size / 2f, bgPaint)
    
    // Body - 3 spheres
    val bodyPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#FAFAFA")
        isAntiAlias = true
    }
    val shadowPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#E0E0E0")
        isAntiAlias = true
    }
    canvas.drawCircle(cx, cy + 20f, 20f, bodyPaint)
    canvas.drawCircle(cx + 3f, cy + 22f, 16f, shadowPaint)
    canvas.drawCircle(cx, cy, 15f, bodyPaint)
    canvas.drawCircle(cx + 3f, cy + 2f, 12f, shadowPaint)
    canvas.drawCircle(cx, cy - 22f, 13f, bodyPaint)
    canvas.drawCircle(cx + 2f, cy - 20f, 9f, shadowPaint)
    
    // Top hat
    val hatPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#1C1C1C")
        isAntiAlias = true
    }
    val hatBandPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#D32F2F")
        isAntiAlias = true
    }
    canvas.drawRoundRect(android.graphics.RectF(cx - 16f, cy - 32f, cx + 16f, cy - 28f), 3f, 3f, hatPaint)
    canvas.drawRoundRect(android.graphics.RectF(cx - 11f, cy - 46f, cx + 11f, cy - 28f), 3f, 3f, hatPaint)
    canvas.drawRoundRect(android.graphics.RectF(cx - 11f, cy - 34f, cx + 11f, cy - 28f), 2f, 2f, hatBandPaint)
    
    // Scarf
    val scarfPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#D32F2F")
        isAntiAlias = true
    }
    val scarfStripePaint = Paint().apply {
        color = android.graphics.Color.parseColor("#FFC107")
        isAntiAlias = true
    }
    canvas.drawRoundRect(android.graphics.RectF(cx - 14f, cy - 12f, cx + 14f, cy - 4f), 3f, 3f, scarfPaint)
    canvas.drawRoundRect(android.graphics.RectF(cx + 5f, cy - 4f, cx + 14f, cy + 12f), 2f, 2f, scarfPaint)
    canvas.drawRect(cx + 7f, cy + 0f, cx + 12f, cy + 3f, scarfStripePaint)
    
    // Eyes
    val coalPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#212121")
        isAntiAlias = true
    }
    canvas.drawCircle(cx - 5f, cy - 24f, 2.5f, coalPaint)
    canvas.drawCircle(cx + 5f, cy - 24f, 2.5f, coalPaint)
    
    // Carrot nose
    val nosePaint = Paint().apply {
        color = android.graphics.Color.parseColor("#FF6D00")
        isAntiAlias = true
    }
    canvas.drawPath(createTriangle(cx, cy - 18f, 2.5f, 10f), nosePaint)
    
    // Buttons
    canvas.drawCircle(cx, cy - 5f, 2f, coalPaint)
    canvas.drawCircle(cx, cy + 6f, 2f, coalPaint)
    canvas.drawCircle(cx, cy + 16f, 2f, coalPaint)
    
    // Arms
    val armPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#5D4037")
        isAntiAlias = true
        strokeWidth = 3f
    }
    canvas.drawLine(cx - 15f, cy, cx - 26f, cy - 10f, armPaint)
    canvas.drawLine(cx + 15f, cy, cx + 26f, cy - 9f, armPaint)
}

private fun drawRealisticOrnament(canvas: Canvas, size: Int) {
    val cx = size / 2f
    val cy = size / 2f
    
    // Background circle - light blue like snowman
    val bgPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#E3F2FD")
        isAntiAlias = true
    }
    canvas.drawCircle(cx, cy, size / 2f, bgPaint)
    
    // Main red ball
    val ballPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#C62828")
        isAntiAlias = true
    }
    canvas.drawCircle(cx, cy + 2f, 26f, ballPaint)
    
    // Ball highlight
    val ballHighlight = Paint().apply {
        color = android.graphics.Color.parseColor("#EF5350")
        isAntiAlias = true
    }
    canvas.drawCircle(cx - 7f, cy - 10f, 12f, ballHighlight)
    
    // Specular highlight
    val specularPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#FFFFFF")
        isAntiAlias = true
        alpha = 180
    }
    canvas.drawCircle(cx - 9f, cy - 12f, 5f, specularPaint)
    
    // Gold cap
    val goldPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#FFD700")
        isAntiAlias = true
    }
    canvas.drawRoundRect(android.graphics.RectF(cx - 8f, cy - 26f, cx + 8f, cy - 18f), 2f, 2f, goldPaint)
    
    // Hook
    val hookPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#FFD700")
        strokeWidth = 3f
        isAntiAlias = true
        style = Paint.Style.STROKE
    }
    canvas.drawArc(android.graphics.RectF(cx - 6f, cy - 36f, cx + 6f, cy - 18f), 0f, 180f, false, hookPaint)
    
    // Small blue ornament (left)
    val blueOrnamentPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#1565C0")
        isAntiAlias = true
    }
    canvas.drawCircle(cx - 24f, cy + 12f, 10f, blueOrnamentPaint)
    val blueHighlight = Paint().apply {
        color = android.graphics.Color.parseColor("#64B5F6")
        isAntiAlias = true
        alpha = 150
    }
    canvas.drawCircle(cx - 26f, cy + 9f, 4f, blueHighlight)
    
    // Small gold ornament (right)
    val goldOrnamentPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#FFB300")
        isAntiAlias = true
    }
    canvas.drawCircle(cx + 24f, cy + 10f, 9f, goldOrnamentPaint)
    val goldHighlight = Paint().apply {
        color = android.graphics.Color.parseColor("#FFE082")
        isAntiAlias = true
        alpha = 150
    }
    canvas.drawCircle(cx + 22f, cy + 8f, 3f, goldHighlight)
    
    // Sparkles
    val sparklePaint = Paint().apply {
        color = android.graphics.Color.parseColor("#FFFFFF")
        isAntiAlias = true
        alpha = 200
    }
    val sparklePositions = listOf(
        Pair(cx - 18f, cy - 16f), Pair(cx + 20f, cy - 18f),
        Pair(cx - 24f, cy + 2f), Pair(cx + 24f, cy + 2f)
    )
    for ((sx, sy) in sparklePositions) {
        canvas.drawCircle(sx, sy, 2f, sparklePaint)
    }
}
