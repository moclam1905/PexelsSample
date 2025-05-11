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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.nguyenmoclam.pexelssample.domain.model.Photo
import com.nguyenmoclam.pexelssample.ui.common.parseColor
import com.nguyenmoclam.pexelssample.ui.home.SearchViewModel
import androidx.compose.ui.unit.IntSize

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

    var layoutSize by remember { mutableStateOf(IntSize.Zero) }

    // Define zoom limits for the graphicsLayer scale
    val minGraphicsLayerScale = 1.0f
    val maxGraphicsLayerScale = 3.0f // Example: Max zoom 3x

    // States to store the dimensions of the image content as rendered by ContentScale.Fit
    var contentRenderedWidth by remember { mutableStateOf(0f) }
    var contentRenderedHeight by remember { mutableStateOf(0f) }

    // Recalculate content dimensions and reset state when photo or layout size changes
    LaunchedEffect(photo, layoutSize) {
        val currentPhoto = photo
        if (currentPhoto != null && currentPhoto.width > 0 && currentPhoto.height > 0 && layoutSize.width > 0 && layoutSize.height > 0) {
            val imgW = currentPhoto.width.toFloat()
            val imgH = currentPhoto.height.toFloat()
            val viewW = layoutSize.width.toFloat()
            val viewH = layoutSize.height.toFloat()

            val imgAspectRatio = imgW / imgH
            val viewAspectRatio = viewW / viewH

            val calcContentWidth: Float
            val calcContentHeight: Float

            if (imgAspectRatio > viewAspectRatio) { // Image is wider than view container, ContentScale.Fit fits width
                calcContentWidth = viewW
                calcContentHeight = viewW / imgAspectRatio
            } else { // Image is taller or same aspect ratio, ContentScale.Fit fits height
                calcContentHeight = viewH
                calcContentWidth = viewH * imgAspectRatio
            }
            contentRenderedWidth = calcContentWidth
            contentRenderedHeight = calcContentHeight

            // Reset scale and offsets to reflect the new "fit" state
            scale = minGraphicsLayerScale // Set to 1f
            offsetX = 0f
            offsetY = 0f
        } else {
            // Reset if photo or layout becomes invalid
            contentRenderedWidth = 0f
            contentRenderedHeight = 0f
            scale = minGraphicsLayerScale
            offsetX = 0f
            offsetY = 0f
        }
    }

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
                        .onSizeChanged { newSize ->
                            layoutSize = newSize
                        }
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                        .pointerInput(photo, layoutSize, contentRenderedWidth, contentRenderedHeight) { // Add relevant keys
                            detectTransformGestures { centroid, pan, zoom, rotation ->
                                val currentPhotoGestures = photo // Safely capture for use in gesture
                                if (currentPhotoGestures == null || layoutSize == IntSize.Zero || contentRenderedWidth == 0f || contentRenderedHeight == 0f) {
                                    return@detectTransformGestures // Not ready for gestures
                                }

                                val newScale = (scale * zoom).coerceIn(minGraphicsLayerScale, maxGraphicsLayerScale)
                                scale = newScale

                                if (scale == minGraphicsLayerScale) { // AC5: Reset offsets when scale is at base (1f)
                                    offsetX = 0f
                                    offsetY = 0f
                                } else {
                                    val viewWidth = layoutSize.width.toFloat()
                                    val viewHeight = layoutSize.height.toFloat()

                                    // Scaled dimensions of the actual rendered image content
                                    val scaledContentActualWidth = contentRenderedWidth * scale
                                    val scaledContentActualHeight = contentRenderedHeight * scale

                                    val maxPanX = if (scaledContentActualWidth > viewWidth) (scaledContentActualWidth - viewWidth) / 2f else 0f
                                    val maxPanY = if (scaledContentActualHeight > viewHeight) (scaledContentActualHeight - viewHeight) / 2f else 0f

                                    offsetX = (offsetX + pan.x).coerceIn(-maxPanX, maxPanX)
                                    offsetY = (offsetY + pan.y).coerceIn(-maxPanY, maxPanY)

                                    // Center if scaled image content is smaller than view (AC3)
                                    if (scaledContentActualWidth <= viewWidth) offsetX = 0f
                                    if (scaledContentActualHeight <= viewHeight) offsetY = 0f
                                }
                            }
                        },
                    contentScale = ContentScale.Fit, // This is important: SubcomposeAsyncImage handles the FIT.
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