package com.example.christmastrees.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.appcompat.widget.AppCompatImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.christmastrees.R
import com.example.christmastrees.data.Category
import com.example.christmastrees.data.MarkerType
import com.example.christmastrees.ui.components.GarlandOverlay
import com.example.christmastrees.ui.components.SnowfallOverlay
import com.example.christmastrees.viewmodel.MarkersViewModel
import com.example.christmastrees.viewmodel.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    viewModel: MarkersViewModel = MarkersViewModel.LocalMarkersViewModel.current ?: throw IllegalStateException("MarkersViewModel not provided")
) {
    val context = LocalContext.current
    val markers = viewModel.markers

    // Group markers by category
    val groupedMarkers = markers.groupBy { marker ->
        try {
            marker.markerType().category
        } catch (e: Exception) {
            Category.TREES
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        context.getString(R.string.list_title),
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        GarlandOverlay(modifier = Modifier.fillMaxWidth().height(50.dp))
        SnowfallOverlay(modifier = Modifier.fillMaxSize())
        
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
        if (markers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = MarkerType.CHRISTMAS_TREE.emoji,
                        fontSize = 64.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = context.getString(R.string.list_empty),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = context.getString(R.string.list_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                groupedMarkers.forEach { (category, markers) ->
                    item {
                        CategoryHeader(category = category)
                    }
                    items(markers, key = { it.id }) { marker ->
                        MarkerListItem(
                            marker = marker,
                            onDelete = { viewModel.deleteMarker(marker) }
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun CategoryHeader(category: Category) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text = "✨ ${category.displayName}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun MarkerListItem(
    marker: Marker,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val type by remember(marker.type) {
        try {
            mutableStateOf(marker.markerType())
        } catch (e: Exception) {
            mutableStateOf(com.example.christmastrees.data.MarkerType.CHRISTMAS_TREE)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (type == MarkerType.GRINCH) {
                    AndroidView(
                        factory = { ctx ->
                            androidx.appcompat.widget.AppCompatImageView(ctx).apply {
                                layoutParams = android.view.ViewGroup.LayoutParams(32, 32)
                                setImageBitmap(createGrinchFaceBitmap(32))
                            }
                        }
                    )
                } else {
                    androidx.compose.material3.Text(
                        text = type.emoji,
                        fontSize = 32.sp,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
                Column {
                    Text(
                        text = type.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "%.4f, %.4f".format(marker.latitude, marker.longitude),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = context.getString(R.string.list_item_delete),
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun createGrinchFaceBitmap(size: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = size / 2f
    val cy = size / 2f
    val faceSize = size * 0.35f
    
    val greenPaint = Paint().apply { color = AndroidColor.parseColor("#4CAF50"); isAntiAlias = true }
    val lightGreenPaint = Paint().apply { color = AndroidColor.parseColor("#66BB6A"); isAntiAlias = true }
    val darkGreenPaint = Paint().apply { color = AndroidColor.parseColor("#2E7D32"); isAntiAlias = true }
    val whitePaint = Paint().apply { color = AndroidColor.parseColor("#FFFFFF"); isAntiAlias = true }
    val darkEyePaint = Paint().apply { color = AndroidColor.parseColor("#1B5E20"); isAntiAlias = true }
    val redPaint = Paint().apply { color = AndroidColor.parseColor("#C62828"); isAntiAlias = true }
    val hatWhitePaint = Paint().apply { color = AndroidColor.parseColor("#F5F5F5"); isAntiAlias = true }
    val mouthPaint = Paint().apply { color = AndroidColor.parseColor("#1B5E20"); isAntiAlias = true }
    
    canvas.drawCircle(cx, cy + faceSize * 0.1f, faceSize, greenPaint)
    canvas.drawCircle(cx - faceSize * 0.5f, cy + faceSize * 0.3f, faceSize * 0.3f, lightGreenPaint)
    canvas.drawCircle(cx + faceSize * 0.5f, cy + faceSize * 0.3f, faceSize * 0.3f, lightGreenPaint)
    
    val eyeW = faceSize * 0.3f
    val eyeH = faceSize * 0.28f
    val leftEyeX = cx - faceSize * 0.35f
    val rightEyeX = cx + faceSize * 0.35f
    val eyeY = cy - faceSize * 0.15f
    canvas.drawOval(android.graphics.RectF(leftEyeX - eyeW, eyeY - eyeH, leftEyeX + eyeW, eyeY + eyeH), whitePaint)
    canvas.drawOval(android.graphics.RectF(rightEyeX - eyeW, eyeY - eyeH, rightEyeX + eyeW, eyeY + eyeH), whitePaint)
    canvas.drawCircle(leftEyeX + eyeW * 0.15f, eyeY, eyeW * 0.35f, darkEyePaint)
    canvas.drawCircle(rightEyeX + eyeW * 0.15f, eyeY, eyeW * 0.35f, darkEyePaint)
    canvas.drawCircle(leftEyeX + eyeW * 0.3f, eyeY - eyeH * 0.3f, eyeW * 0.12f, whitePaint)
    canvas.drawCircle(rightEyeX + eyeW * 0.3f, eyeY - eyeH * 0.3f, eyeW * 0.12f, whitePaint)
    
    val browPaint = Paint().apply { color = AndroidColor.parseColor("#1B5E20"); strokeWidth = faceSize * 0.08f; isAntiAlias = true }
    canvas.drawLine(leftEyeX - eyeW, eyeY - eyeH - faceSize * 0.05f, leftEyeX + eyeW * 0.5f, eyeY - eyeH - faceSize * 0.2f, browPaint)
    canvas.drawLine(rightEyeX - eyeW * 0.5f, eyeY - eyeH - faceSize * 0.2f, rightEyeX + eyeW, eyeY - eyeH - faceSize * 0.05f, browPaint)
    
    canvas.drawCircle(cx, cy + faceSize * 0.05f, faceSize * 0.12f, darkGreenPaint)
    
    val mouthY = cy + faceSize * 0.4f
    val mouthPath = android.graphics.Path()
    mouthPath.moveTo(cx - faceSize * 0.65f, mouthY - faceSize * 0.05f)
    mouthPath.quadTo(cx, mouthY + faceSize * 0.4f, cx + faceSize * 0.65f, mouthY - faceSize * 0.05f)
    mouthPath.lineTo(cx + faceSize * 0.65f, mouthY + faceSize * 0.15f)
    mouthPath.quadTo(cx, mouthY + faceSize * 0.55f, cx - faceSize * 0.65f, mouthY + faceSize * 0.15f)
    mouthPath.close()
    canvas.drawPath(mouthPath, mouthPaint)
    
    val toothPaint = Paint().apply { color = AndroidColor.parseColor("#FFFFFF"); isAntiAlias = true }
    for (i in 0..4) {
        val tx = cx - faceSize * 0.5f + i * (faceSize * 0.2f)
        canvas.drawRect(android.graphics.RectF(tx, mouthY + faceSize * 0.05f, tx + faceSize * 0.1f, mouthY + faceSize * 0.22f), toothPaint)
    }
    
    val earPaint = Paint().apply { color = AndroidColor.parseColor("#388E3C"); isAntiAlias = true }
    val leftEarPath = android.graphics.Path()
    leftEarPath.moveTo(cx - faceSize * 0.85f, cy - faceSize * 0.3f)
    leftEarPath.lineTo(cx - faceSize * 1.2f, cy - faceSize * 0.9f)
    leftEarPath.lineTo(cx - faceSize * 0.6f, cy - faceSize * 0.7f)
    leftEarPath.close()
    canvas.drawPath(leftEarPath, earPaint)
    val rightEarPath = android.graphics.Path()
    rightEarPath.moveTo(cx + faceSize * 0.85f, cy - faceSize * 0.3f)
    rightEarPath.lineTo(cx + faceSize * 1.2f, cy - faceSize * 0.9f)
    rightEarPath.lineTo(cx + faceSize * 0.6f, cy - faceSize * 0.7f)
    rightEarPath.close()
    canvas.drawPath(rightEarPath, earPaint)
    
    val hatBaseY = cy - faceSize * 0.7f
    val hatTopY = cy - faceSize * 1.7f
    val hatPath = android.graphics.Path()
    hatPath.moveTo(cx - faceSize * 0.7f, hatBaseY)
    hatPath.quadTo(cx - faceSize * 0.9f, (hatBaseY + hatTopY) / 2f, cx + faceSize * 0.1f, hatTopY)
    hatPath.lineTo(cx + faceSize * 0.3f, hatTopY - faceSize * 0.05f)
    hatPath.quadTo(cx + faceSize * 0.5f, (hatBaseY + hatTopY) / 2f, cx + faceSize * 0.7f, hatBaseY)
    hatPath.close()
    canvas.drawPath(hatPath, redPaint)
    canvas.drawRoundRect(android.graphics.RectF(cx - faceSize * 0.8f, hatBaseY - faceSize * 0.1f, cx + faceSize * 0.8f, hatBaseY + faceSize * 0.1f), faceSize * 0.15f, faceSize * 0.15f, hatWhitePaint)
    canvas.drawCircle(cx + faceSize * 0.2f, hatTopY, faceSize * 0.15f, hatWhitePaint)
    
    return bitmap
}
