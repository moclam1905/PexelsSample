package com.nguyenmoclam.pexelssample.ui.detail

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.nguyenmoclam.pexelssample.domain.model.Photo
import com.nguyenmoclam.pexelssample.ui.common.parseColor
import com.nguyenmoclam.pexelssample.ui.home.SearchViewModel
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val MIN_SCALE = 1.0f
private const val MAX_SCALE = 3.0f // Example: Max zoom 3x
private const val INTERMEDIATE_SCALE = (MIN_SCALE + MAX_SCALE) / 2f // Or MIN_SCALE * 2f, clamped
private const val ANIMATION_DURATION_MILLIS = 300

val ImageScale = SemanticsPropertyKey<Float>("ImageScale")
var SemanticsPropertyReceiver.imageScale by ImageScale

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

    val coroutineScope = rememberCoroutineScope()

    val scale = remember { Animatable(MIN_SCALE) }
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    var layoutSize by remember { mutableStateOf(IntSize.Zero) }

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
            coroutineScope.launch {
                scale.snapTo(MIN_SCALE) // Set to min_scale
                offsetX.snapTo(0f)
                offsetY.snapTo(0f)
            }
        } else {
            // Reset if photo or layout becomes invalid
            contentRenderedWidth = 0f
            contentRenderedHeight = 0f
            coroutineScope.launch {
                scale.snapTo(MIN_SCALE)
                offsetX.snapTo(0f)
                offsetY.snapTo(0f)
            }
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
                        .testTag("zoomableImage")
                        .semantics { this.imageScale = scale.value }
                        .onSizeChanged { newSize ->
                            layoutSize = newSize
                        }
                        .pointerInput(Unit) { // Added for double-tap gestures
                            detectTapGestures(
                                onDoubleTap = { tapOffset ->
                                    coroutineScope.launch {
                                        val currentScale = scale.value
                                        if (abs(currentScale - MIN_SCALE) < 0.01f) {
                                            // Zoom In to Intermediate Scale, centered on tapOffset
                                            val targetScale = INTERMEDIATE_SCALE

                                            // Ensure view and content dimensions are valid before calculation
                                            if (layoutSize == IntSize.Zero || contentRenderedWidth == 0f || contentRenderedHeight == 0f) return@launch

                                            val viewWidth = layoutSize.width.toFloat()
                                            val viewHeight = layoutSize.height.toFloat()

                                            // Calculate target offsets for centered zoom (AC4)
                                            // Formula from story: targetOffsetX = (viewWidth / 2f - tapOffset.x) * (targetScale / currentScale - 1) + offsetX.value * (targetScale / currentScale)
                                            // Note: The formula in the story for offset assumes the tapOffset is relative to the Composable's top-left.
                                            // The `tapOffset: Offset` from detectTapGestures is indeed in the local coordinates of the composable.
                                            // The graphicsLayer transform origin is by default the center of the composable (0.5f, 0.5f).
                                            // However, the translation X and Y are applied *after* scaling if the default transformOrigin is used.
                                            // For precise centering, we need to account for the current translation and how scale affects the tap point's perceived position.
                                            // Let's adjust the formula slightly to be clearer: if offsetX.value and offsetY.value are current translations
                                            // of the *center* of the image, then tapOffset is relative to the view bounds. The formulas from the story seem plausible
                                            // if offsetX/Y are the translations applied to the graphicsLayer.

                                            // Current position of the tap point in the *scaled content's* coordinate system
                                            val tapXInContent = (tapOffset.x - viewWidth / 2f - offsetX.value) / currentScale
                                            val tapYInContent = (tapOffset.y - viewHeight / 2f - offsetY.value) / currentScale

                                            // Target offsets should make this tapXInContent, tapYInContent appear at tapOffset.x, tapOffset.y in the view after scaling to targetScale
                                            // So, new offsetX = tapOffset.x - viewWidth / 2f - (tapXInContent * targetScale)
                                            // This needs careful derivation. Let's use the story's provided formula first and test.
                                            // The story formula: targetOffsetX = (viewWidth / 2f - tapOffset.x) * (targetScale / currentScale - 1) + offsetX.value * (targetScale / currentScale)
                                            // This can be rewritten: targetOffsetX = offsetX.value * (targetScale/currentScale) - (tapOffset.x - viewWidth/2f) * (targetScale/currentScale -1)
                                            // This formula seems to adjust the existing offset based on the scale change and the tap position relative to the view center.

                                            var targetOffsetX = offsetX.value * (targetScale / currentScale) - (tapOffset.x - viewWidth / 2f) * (targetScale / currentScale - 1)
                                            var targetOffsetY = offsetY.value * (targetScale / currentScale) - (tapOffset.y - viewHeight / 2f) * (targetScale / currentScale - 1)

                                            // Clamp targetOffsetX and targetOffsetY
                                            val targetScaledContentActualWidth = contentRenderedWidth * targetScale
                                            val targetScaledContentActualHeight = contentRenderedHeight * targetScale

                                            val maxPanXTarget = if (targetScaledContentActualWidth > viewWidth) (targetScaledContentActualWidth - viewWidth) / 2f else 0f
                                            val maxPanYTarget = if (targetScaledContentActualHeight > viewHeight) (targetScaledContentActualHeight - viewHeight) / 2f else 0f

                                            targetOffsetX = targetOffsetX.coerceIn(-maxPanXTarget, maxPanXTarget)
                                            targetOffsetY = targetOffsetY.coerceIn(-maxPanYTarget, maxPanYTarget)
                                            
                                            // Center if scaled image content is smaller than view after zoom
                                            if (targetScaledContentActualWidth <= viewWidth) targetOffsetX = 0f
                                            if (targetScaledContentActualHeight <= viewHeight) targetOffsetY = 0f

                                            launch { scale.animateTo(targetScale, animationSpec = tween(durationMillis = ANIMATION_DURATION_MILLIS, easing = FastOutSlowInEasing)) }
                                            launch { offsetX.animateTo(targetOffsetX, animationSpec = tween(durationMillis = ANIMATION_DURATION_MILLIS, easing = FastOutSlowInEasing)) }
                                            launch { offsetY.animateTo(targetOffsetY, animationSpec = tween(durationMillis = ANIMATION_DURATION_MILLIS, easing = FastOutSlowInEasing)) }

                                        } else {
                                            // Zoom Out to Min Scale
                                            launch { scale.animateTo(MIN_SCALE, animationSpec = tween(durationMillis = ANIMATION_DURATION_MILLIS, easing = FastOutSlowInEasing)) }
                                            launch { offsetX.animateTo(0f, animationSpec = tween(durationMillis = ANIMATION_DURATION_MILLIS, easing = FastOutSlowInEasing)) }
                                            launch { offsetY.animateTo(0f, animationSpec = tween(durationMillis = ANIMATION_DURATION_MILLIS, easing = FastOutSlowInEasing)) }
                                        }
                                    }
                                }
                            )
                        }
                        .graphicsLayer(
                            scaleX = scale.value,
                            scaleY = scale.value,
                            translationX = offsetX.value,
                            translationY = offsetY.value
                        )
                        .pointerInput(photo, layoutSize, contentRenderedWidth, contentRenderedHeight) { // Add relevant keys
                            detectTransformGestures { centroid, pan, zoom, rotation ->
                                val currentPhotoGestures = photo // Safely capture for use in gesture
                                if (currentPhotoGestures == null || layoutSize == IntSize.Zero || contentRenderedWidth == 0f || contentRenderedHeight == 0f) {
                                    return@detectTransformGestures // Not ready for gestures
                                }

                                coroutineScope.launch {
                                    val newScaleTarget = (scale.value * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                                    scale.snapTo(newScaleTarget)

                                    if (abs(scale.value - MIN_SCALE) < 0.01f) { // If scale is (almost) min_scale, reset offsets
                                        offsetX.snapTo(0f)
                                        offsetY.snapTo(0f)
                                    } else {
                                        val viewWidth = layoutSize.width.toFloat()
                                        val viewHeight = layoutSize.height.toFloat()

                                        // Scaled dimensions of the actual rendered image content
                                        val scaledContentActualWidth = contentRenderedWidth * scale.value
                                        val scaledContentActualHeight = contentRenderedHeight * scale.value

                                        val maxPanX = if (scaledContentActualWidth > viewWidth) (scaledContentActualWidth - viewWidth) / 2f else 0f
                                        val maxPanY = if (scaledContentActualHeight > viewHeight) (scaledContentActualHeight - viewHeight) / 2f else 0f

                                        offsetX.snapTo((offsetX.value + pan.x).coerceIn(-maxPanX, maxPanX))
                                        offsetY.snapTo((offsetY.value + pan.y).coerceIn(-maxPanY, maxPanY))

                                        // Center if scaled image content is smaller than view
                                        if (scaledContentActualWidth <= viewWidth) offsetX.snapTo(0f)
                                        if (scaledContentActualHeight <= viewHeight) offsetY.snapTo(0f)
                                    }
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