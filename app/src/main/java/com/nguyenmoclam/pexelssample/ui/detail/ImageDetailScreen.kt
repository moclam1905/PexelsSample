package com.nguyenmoclam.pexelssample.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.nguyenmoclam.pexelssample.domain.model.Photo
import com.nguyenmoclam.pexelssample.ui.common.parseColor
import com.nguyenmoclam.pexelssample.ui.home.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageDetailScreen(
    navController: NavController,
    photoId: Int,
    searchViewModel: SearchViewModel = hiltViewModel()
) {
    var photo by remember { mutableStateOf<Photo?>(null) }

    LaunchedEffect(photoId) {
        photo = searchViewModel.getPhotoById(photoId)
    }

    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(photo?.photographer?.let { "Photo by $it" } ?: "Image Detail") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp) // Content padding
        ) {
            if (photo != null) {
                val currentPhoto = photo!!
                val uriHandler = LocalUriHandler.current

                // Image Placeholder Box
                val aspectRatio = if (currentPhoto.height > 0) currentPhoto.width.toFloat() / currentPhoto.height.toFloat() else 1f

                SubcomposeAsyncImage(
                    model = currentPhoto.src.large2x,
                    contentDescription = currentPhoto.alt.ifBlank { "Full image by ${currentPhoto.photographer}" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio.coerceIn(0.5f, 2f))
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                        .pointerInput(Unit) {
                            detectTransformGestures { centroid, pan, zoom, rotation ->
                                scale *= zoom
                                // Basic panning restriction (AC5)
                                if (scale > 1f) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                } else {
                                    // Reset offsets if scale is too small, to prevent panning when not zoomed
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        },
                    contentScale = ContentScale.Fit,
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(parseColor(currentPhoto.avgColor))
                        ) {
                             CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.BrokenImage,
                                contentDescription = "Error loading image",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    },
                    success = {
                        SubcomposeAsyncImageContent()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Photographer Name - Clickable
                val photographerText = "Photo by: ${currentPhoto.photographer}"
                Text(
                    text = photographerText,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (currentPhoto.photographerUrl.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            enabled = currentPhoto.photographerUrl.isNotBlank(),
                            onClick = { uriHandler.openUri(currentPhoto.photographerUrl) }
                        )
                        .padding(vertical = 12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Alt Text/Description
                if (currentPhoto.alt.isNotBlank()) {
                    Text(
                        text = "Description: ${currentPhoto.alt}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp)) // Add spacer after description if present
                }

            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading photo details or photo not found (ID: $photoId)...")
                }
            }
        }
    }
} 