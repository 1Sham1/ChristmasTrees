package com.example.christmastrees.ui.screens

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
        // Garland overlay at the top
        GarlandOverlay(modifier = Modifier.fillMaxWidth().height(50.dp))

        // Snowfall overlay
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
                Text(
                    text = type.emoji,
                    fontSize = 32.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
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


