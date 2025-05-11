package com.nguyenmoclam.pexelssample.ui.detail

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.nguyenmoclam.pexelssample.domain.model.Photo
import com.nguyenmoclam.pexelssample.ui.common.parseColor
import kotlinx.coroutines.launch
import kotlin.math.abs

// Constants for pinch-to-zoom, can be shared or duplicated from ImageDetailScreen
private const val ANIMATION_DURATION_MILLIS_PANE = 300
private const val INTERMEDIATE_SCALE_FACTOR_PANE = 2.0f

@Composable
fun ImageDetailPaneComposable(
    selectedPhoto: Photo?,
    imageDetailViewModel: ImageDetailViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    LaunchedEffect(selectedPhoto) {
        imageDetailViewModel.setPhotoDetails(selectedPhoto)
        // Reset transform when a new photo is selected or deselected
        // This handles the case where a photo is deselected (selectedPhoto becomes null)
        // or a new photo is selected.
        if (selectedPhoto != null) {
             // If a photo is selected, we don't necessarily reset immediately.
             // The reset based on layout/content size change is more robust.
        } else {
             imageDetailViewModel.resetTransform() // Clear zoom/pan if no photo
        }
    }

    val photoFromVM by imageDetailViewModel.photo.collectAsStateWithLifecycle()

    val targetScale by imageDetailViewModel.scale.collectAsStateWithLifecycle()
    val targetOffsetX by imageDetailViewModel.offsetX.collectAsStateWithLifecycle()
    val targetOffsetY by imageDetailViewModel.offsetY.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val scaleAnimatable = remember { Animatable(targetScale) }
    val offsetXAnimatable = remember { Animatable(targetOffsetX) }
    val offsetYAnimatable = remember { Animatable(targetOffsetY) }

    LaunchedEffect(targetScale) {
        scaleAnimatable.animateTo(targetScale, animationSpec = tween(durationMillis = ANIMATION_DURATION_MILLIS_PANE, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(targetOffsetX) {
        offsetXAnimatable.animateTo(targetOffsetX, animationSpec = tween(durationMillis = ANIMATION_DURATION_MILLIS_PANE, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(targetOffsetY) {
        offsetYAnimatable.animateTo(targetOffsetY, animationSpec = tween(durationMillis = ANIMATION_DURATION_MILLIS_PANE, easing = FastOutSlowInEasing))
    }

    var layoutSize by remember { mutableStateOf(IntSize.Zero) }
    var contentRenderedWidth by remember { mutableStateOf(0f) }
    var contentRenderedHeight by remember { mutableStateOf(0f) }

    // This effect recalculates content dimensions and resets transform if photo or layout changes.
    LaunchedEffect(photoFromVM, layoutSize, imageDetailViewModel) {
        val capturedPhoto = photoFromVM
        if (capturedPhoto != null && capturedPhoto.width > 0 && capturedPhoto.height > 0 && layoutSize.width > 0 && layoutSize.height > 0) {
            val imgW = capturedPhoto.width.toFloat()
            val imgH = capturedPhoto.height.toFloat()
            val viewW = layoutSize.width.toFloat()
            val viewH = layoutSize.height.toFloat()
            val imgAspectRatio = imgW / imgH
            val viewAspectRatio = viewW / viewH

            val calcContentWidth: Float
            val calcContentHeight: Float

            if (imgAspectRatio > viewAspectRatio) { // Image is wider than view, letterbox top/bottom
                calcContentWidth = viewW
                calcContentHeight = viewW / imgAspectRatio
            } else { // Image is taller than view (or same aspect ratio), pillarbox left/right
                calcContentHeight = viewH
                calcContentWidth = viewH * imgAspectRatio
            }
            val changed = abs(contentRenderedWidth - calcContentWidth) > 0.1f || abs(contentRenderedHeight - calcContentHeight) > 0.1f
            contentRenderedWidth = calcContentWidth
            contentRenderedHeight = calcContentHeight

            if (changed) {
                imageDetailViewModel.resetTransform() 
            }
        } else { // No photo or invalid dimensions
            val changed = contentRenderedWidth != 0f || contentRenderedHeight != 0f
            contentRenderedWidth = 0f
            contentRenderedHeight = 0f
            if (changed) {
                 imageDetailViewModel.resetTransform()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        if (photoFromVM != null) {
            val currentPhoto = photoFromVM!!
            val uriHandler = LocalUriHandler.current
            val viewConf = LocalViewConfiguration.current
            
            // Determine aspect ratio for the image container
            val imageContainerAspectRatio = if (currentPhoto.height > 0) {
                currentPhoto.width.toFloat() / currentPhoto.height.toFloat()
            } else {
                16f / 9f // Default aspect ratio if photo dimensions are invalid
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()) // Allow scrolling for details if content overflows
                    .padding(16.dp)
            ) {
                SubcomposeAsyncImage(
                    model = currentPhoto.src.large2x,
                    contentDescription = currentPhoto.alt.ifBlank { "Image by ${currentPhoto.photographer}" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(imageContainerAspectRatio.coerceIn(0.5f, 3f)) // Apply calculated aspect ratio
                        .testTag("zoomableImagePane")
                        .semantics { this.imageScale = scaleAnimatable.value }
                        .onSizeChanged { newSize -> layoutSize = newSize }
                        .pointerInput(imageDetailViewModel, contentRenderedWidth, contentRenderedHeight, layoutSize) { // Pass necessary state for gestures
                            detectTapGestures(
                                onDoubleTap = { tapOffset ->
                                    if (layoutSize == IntSize.Zero || contentRenderedWidth == 0f || contentRenderedHeight == 0f) return@detectTapGestures

                                    val viewWidth = layoutSize.width.toFloat()
                                    val viewHeight = layoutSize.height.toFloat()

                                    val currentActualScale = scaleAnimatable.value
                                    val minScale = imageDetailViewModel.minScale
                                    val maxScale = imageDetailViewModel.maxScale
                                    val intermediateScale = (minScale * INTERMEDIATE_SCALE_FACTOR_PANE).coerceAtMost(maxScale)

                                    val targetScaleValue: Float
                                    val targetOffsetXValue: Float
                                    val targetOffsetYValue: Float

                                    if (abs(currentActualScale - minScale) < 0.01f) { // Zoom In
                                        targetScaleValue = intermediateScale
                                        targetOffsetXValue = offsetXAnimatable.value * (targetScaleValue / currentActualScale) - (tapOffset.x - viewWidth / 2f) * (targetScaleValue / currentActualScale - 1)
                                        targetOffsetYValue = offsetYAnimatable.value * (targetScaleValue / currentActualScale) - (tapOffset.y - viewHeight / 2f) * (targetScaleValue / currentActualScale - 1)
                                    } else { // Zoom Out
                                        targetScaleValue = minScale
                                        targetOffsetXValue = 0f
                                        targetOffsetYValue = 0f
                                    }

                                    val effectiveContentWidth = contentRenderedWidth * targetScaleValue
                                    val effectiveContentHeight = contentRenderedHeight * targetScaleValue
                                    val maxPanX = if (effectiveContentWidth > viewWidth) (effectiveContentWidth - viewWidth) / 2f else 0f
                                    val maxPanY = if (effectiveContentHeight > viewHeight) (effectiveContentHeight - viewHeight) / 2f else 0f

                                    val clampedTargetOffsetX = targetOffsetXValue.coerceIn(-maxPanX, maxPanX)
                                    val clampedTargetOffsetY = targetOffsetYValue.coerceIn(-maxPanY, maxPanY)

                                    val finalTargetOffsetX = if (effectiveContentWidth <= viewWidth) 0f else clampedTargetOffsetX
                                    val finalTargetOffsetY = if (effectiveContentHeight <= viewHeight) 0f else clampedTargetOffsetY

                                    imageDetailViewModel.onDoubleTap(
                                        currentScale = currentActualScale,
                                        currentOffsetX = offsetXAnimatable.value,
                                        currentOffsetY = offsetYAnimatable.value,
                                        targetScaleValue = targetScaleValue,
                                        targetOffsetXValue = finalTargetOffsetX,
                                        targetOffsetYValue = finalTargetOffsetY
                                    )
                                }
                            )
                        }
                        .graphicsLayer(
                            scaleX = scaleAnimatable.value,
                            scaleY = scaleAnimatable.value,
                            translationX = offsetXAnimatable.value,
                            translationY = offsetYAnimatable.value
                        )
                        .pointerInput(imageDetailViewModel, contentRenderedWidth, contentRenderedHeight, layoutSize) {
                            val touchSlop = viewConf.touchSlop
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                do {
                                    val event = awaitPointerEvent()

                                    val zoomChange = event.calculateZoom()
                                    val panChange = event.calculatePan()
                                    val centroid = event.calculateCentroid(useCurrent = true)

                                    val zoomMoved = abs(zoomChange - 1f) > 0.001f
                                    val panMoved  = panChange.getDistance() > touchSlop
                                    val hasMovement = zoomMoved || panMoved

                                    val minScale = imageDetailViewModel.minScale
                                    val shouldConsume =
                                          event.changes.size > 1 ||
                                                  (imageDetailViewModel.scale.value > minScale + 0.01f && hasMovement)
                                    if (shouldConsume) {

                                        val viewW = layoutSize.width.toFloat()
                                        val viewH = layoutSize.height.toFloat()
                                        val maxScale = imageDetailViewModel.maxScale

                                        val currentScale    = imageDetailViewModel.scale.value
                                        val currentOffsetX  = imageDetailViewModel.offsetX.value
                                        val currentOffsetY  = imageDetailViewModel.offsetY.value

                                        val pivotX = if (centroid.isSpecified) centroid.x else viewW / 2f
                                        val pivotY = if (centroid.isSpecified) centroid.y else viewH / 2f

                                        val newScale = (currentScale * zoomChange).coerceIn(minScale, maxScale)

                                        val oldScaleForFormula = currentScale
                                        var newOffsetX = currentOffsetX * (newScale / oldScaleForFormula) -
                                                (pivotX - viewW / 2f) * (newScale / oldScaleForFormula - 1) +
                                                panChange.x
                                        var newOffsetY = currentOffsetY * (newScale / oldScaleForFormula) -
                                                (pivotY - viewH / 2f) * (newScale / oldScaleForFormula - 1) +
                                                panChange.y

                                        if (abs(newScale - minScale) < 0.01f) {
                                            newOffsetX = 0f
                                            newOffsetY = 0f
                                        } else {
                                            val scaledContentW = contentRenderedWidth * newScale
                                            val scaledContentH = contentRenderedHeight * newScale
                                            val maxPanX = if (scaledContentW > viewW) (scaledContentW - viewW) / 2f else 0f
                                            val maxPanY = if (scaledContentH > viewH) (scaledContentH - viewH) / 2f else 0f
                                            newOffsetX = newOffsetX.coerceIn(-maxPanX, maxPanX)
                                            newOffsetY = newOffsetY.coerceIn(-maxPanY, maxPanY)
                                            if (scaledContentW <= viewW) newOffsetX = 0f
                                            if (scaledContentH <= viewH) newOffsetY = 0f
                                        }


                                        imageDetailViewModel.updateTransform(newScale, newOffsetX, newOffsetY)

                                        coroutineScope.launch {
                                            scaleAnimatable.snapTo(newScale)
                                            offsetXAnimatable.snapTo(newOffsetX)
                                            offsetYAnimatable.snapTo(newOffsetY)
                                        }

                                        event.changes.forEach { it.consume() }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        },
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
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.BrokenImage, contentDescription = "Error loading image", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    contentScale = ContentScale.Fit // Fit within the bounds defined by aspectRatio
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Photo by ${currentPhoto.photographer}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                currentPhoto.alt.takeIf { it.isNotBlank() }?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .sizeIn(minHeight = 48.dp)
                        .clickable { uriHandler.openUri(currentPhoto.url) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "View on Pexels",
                        style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.padding(8.dp),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                val avgColor = remember(currentPhoto.avgColor) { parseColor(currentPhoto.avgColor) }
                if (avgColor != Color.Transparent) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(avgColor)
                    ) {
                        Text(
                            text = "Avg Color: ${currentPhoto.avgColor}",
                            modifier = Modifier.align(Alignment.Center),
                            color = if (avgColor.luminance() > 0.5f) Color.Black else Color.White
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.BrokenImage, contentDescription = "No image selected", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Select an image from the list to view details.",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Extension function to calculate luminance, useful for text color on dynamic background
fun Color.luminance(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue)
} 