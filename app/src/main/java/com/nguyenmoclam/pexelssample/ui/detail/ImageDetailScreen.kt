package com.nguyenmoclam.pexelssample.ui.detail

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.nguyenmoclam.pexelssample.domain.model.Photo
import com.nguyenmoclam.pexelssample.ui.common.ErrorView
import com.nguyenmoclam.pexelssample.ui.common.parseColor
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.launch
import kotlin.math.abs
import android.content.res.Configuration
import com.nguyenmoclam.pexelssample.ui.home.HomeScreenViewModel
import com.nguyenmoclam.pexelssample.ui.home.SearchViewModel

private const val MIN_SCALE = 1.0f
private const val MAX_SCALE = 3.0f // Example: Max zoom 3x
private const val INTERMEDIATE_SCALE = (MIN_SCALE + MAX_SCALE) / 2f // Or MIN_SCALE * 2f, clamped
private const val INTERMEDIATE_SCALE_FACTOR = 2.0f
private const val ANIMATION_DURATION_MILLIS = 300

val ImageScale = SemanticsPropertyKey<Float>("ImageScale")
var SemanticsPropertyReceiver.imageScale by ImageScale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.ImageDetailScreen(
    navController: NavController,
    photoId: Int,
    imageDetailViewModel: ImageDetailViewModel = hiltViewModel(),
    homeScreenViewModel: HomeScreenViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val uiState by imageDetailViewModel.uiState.collectAsStateWithLifecycle()
    val photoFromVM = uiState.photo

    LaunchedEffect(photoId, homeScreenViewModel, searchViewModel, imageDetailViewModel) {
        imageDetailViewModel.signalLoadingPhoto()
        var foundPhoto: Photo? = null
        foundPhoto = homeScreenViewModel.getPhotoById(photoId)
        if (foundPhoto == null) {
            foundPhoto = searchViewModel.getPhotoById(photoId)
        }
        imageDetailViewModel.setPhotoDetails(foundPhoto)
    }

    val targetScale by imageDetailViewModel.scale.collectAsStateWithLifecycle()
    val targetOffsetX by imageDetailViewModel.offsetX.collectAsStateWithLifecycle()
    val targetOffsetY by imageDetailViewModel.offsetY.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()

    val scaleAnimatable = remember { Animatable(targetScale) }
    val offsetXAnimatable = remember { Animatable(targetOffsetX) }
    val offsetYAnimatable = remember { Animatable(targetOffsetY) }

    LaunchedEffect(targetScale) {
        scaleAnimatable.animateTo(targetScale, animationSpec = tween(durationMillis = ANIMATION_DURATION_MILLIS, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(targetOffsetX) {
        offsetXAnimatable.animateTo(targetOffsetX, animationSpec = tween(durationMillis = ANIMATION_DURATION_MILLIS, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(targetOffsetY) {
        offsetYAnimatable.animateTo(targetOffsetY, animationSpec = tween(durationMillis = ANIMATION_DURATION_MILLIS, easing = FastOutSlowInEasing))
    }

    var layoutSize by remember { mutableStateOf(IntSize.Zero) }
    var contentRenderedWidth by remember { mutableStateOf(0f) }
    var contentRenderedHeight by remember { mutableStateOf(0f) }

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

            if (imgAspectRatio > viewAspectRatio) {
                calcContentWidth = viewW
                calcContentHeight = viewW / imgAspectRatio
            } else {
                calcContentHeight = viewH
                calcContentWidth = viewH * imgAspectRatio
            }
            val changed = contentRenderedWidth != calcContentWidth || contentRenderedHeight != calcContentHeight
            contentRenderedWidth = calcContentWidth
            contentRenderedHeight = calcContentHeight

            if (changed) {
                imageDetailViewModel.resetTransform()
            }
        } else {
            val changed = contentRenderedWidth != 0f || contentRenderedHeight != 0f
            contentRenderedWidth = 0f
            contentRenderedHeight = 0f
            if (changed) {
                 imageDetailViewModel.resetTransform()
            }
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(photoFromVM?.photographer?.let { "Photo by $it" } ?: if (uiState.isLoading) "Loading..." else "Image Detail") },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                if (photoFromVM != null) {
                    val currentPhoto = photoFromVM
                    val uriHandler = LocalUriHandler.current

                    val aspectRatio = if (currentPhoto.height > 0) currentPhoto.width.toFloat() / currentPhoto.height.toFloat() else 1f

                    SubcomposeAsyncImage(
                        model = currentPhoto.src.large2x,
                        contentDescription = currentPhoto.alt.ifBlank { "Full image by ${currentPhoto.photographer}" },
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .let { mod ->
                                if (isLandscape) {
                                    mod.height(240.dp) // Fixed height in landscape mode
                                } else {
                                    mod.aspectRatio(aspectRatio.coerceIn(0.5f, 2f))
                                }
                            }
                            .then(
                                if (!isLandscape) { // Only apply if NOT landscape (i.e., portrait)
                                    Modifier.sharedElement(
                                        sharedContentState = sharedTransitionScope.rememberSharedContentState(key = "image-${photoFromVM.id}"),
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .testTag("zoomableImage")
                            .semantics { this.imageScale = scaleAnimatable.value }
                            .onSizeChanged { newSize ->
                                layoutSize = newSize
                            }
                            .pointerInput(imageDetailViewModel, contentRenderedWidth, contentRenderedHeight, layoutSize) {
                                detectTapGestures(
                                    onDoubleTap = { tapOffset ->
                                        if (layoutSize == IntSize.Zero || contentRenderedWidth == 0f || contentRenderedHeight == 0f) return@detectTapGestures

                                        val viewWidth = layoutSize.width.toFloat()
                                        val viewHeight = layoutSize.height.toFloat()

                                        val currentActualScale = scaleAnimatable.value
                                        val minScale = imageDetailViewModel.minScale
                                        val maxScale = imageDetailViewModel.maxScale
                                        val intermediateScale = (minScale * INTERMEDIATE_SCALE_FACTOR).coerceAtMost(maxScale)

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
                                detectTransformGestures { centroid, pan, zoom, rotation ->
                                    if (layoutSize == IntSize.Zero || contentRenderedWidth == 0f || contentRenderedHeight == 0f) {
                                        return@detectTransformGestures
                                    }

                                    val viewWidth = layoutSize.width.toFloat()
                                    val viewHeight = layoutSize.height.toFloat()
                                    val minScale = imageDetailViewModel.minScale
                                    val maxScale = imageDetailViewModel.maxScale

                                    val currentStableScale = imageDetailViewModel.scale.value
                                    val currentStableOffsetX = imageDetailViewModel.offsetX.value
                                    val currentStableOffsetY = imageDetailViewModel.offsetY.value

                                    val newScale = (currentStableScale * zoom).coerceIn(minScale, maxScale)
                                    
                                    val oldScaleForFormula = currentStableScale 

                                    var newOffsetX = currentStableOffsetX * (newScale / oldScaleForFormula) - (centroid.x - viewWidth / 2f) * (newScale / oldScaleForFormula - 1) + pan.x
                                    var newOffsetY = currentStableOffsetY * (newScale / oldScaleForFormula) - (centroid.y - viewHeight / 2f) * (newScale / oldScaleForFormula - 1) + pan.y

                                    if (abs(newScale - minScale) < 0.01f) {
                                        newOffsetX = 0f
                                        newOffsetY = 0f
                                    } else {
                                        val scaledContentActualWidth = contentRenderedWidth * newScale
                                        val scaledContentActualHeight = contentRenderedHeight * newScale

                                        val maxPanX = if (scaledContentActualWidth > viewWidth) (scaledContentActualWidth - viewWidth) / 2f else 0f
                                        val maxPanY = if (scaledContentActualHeight > viewHeight) (scaledContentActualHeight - viewHeight) / 2f else 0f

                                        newOffsetX = newOffsetX.coerceIn(-maxPanX, maxPanX)
                                        newOffsetY = newOffsetY.coerceIn(-maxPanY, maxPanY)

                                        if (scaledContentActualWidth <= viewWidth) newOffsetX = 0f
                                        if (scaledContentActualHeight <= viewHeight) newOffsetY = 0f
                                    }
                                    
                                    imageDetailViewModel.updateTransform(newScale, newOffsetX, newOffsetY)

                                    coroutineScope.launch {
                                        scaleAnimatable.snapTo(newScale)
                                        offsetXAnimatable.snapTo(newOffsetX)
                                        offsetYAnimatable.snapTo(newOffsetY)
                                    }
                                }
                            }
                            .background(parseColor(currentPhoto.avgColor)),
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
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.BrokenImage, contentDescription = "Error loading image", tint = MaterialTheme.colorScheme.error)
                            }
                        },
                        success = { state ->
                            SubcomposeAsyncImageContent()
                        }
                    )

                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text("Photographer: ${currentPhoto.photographer}", style = MaterialTheme.typography.titleMedium)
                        Text("Source: Pexels.com", style = MaterialTheme.typography.bodySmall)
                        if (currentPhoto.photographerUrl.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .sizeIn(minHeight = 48.dp)
                                    .clickable { uriHandler.openUri(currentPhoto.photographerUrl) },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "View photographer\'s profile",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                        if (currentPhoto.alt.isNotBlank()) {
                            Text("Description: ${currentPhoto.alt}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            uiState.error?.let {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ErrorView(
                        error = it,
                        onRetry = {
                            imageDetailViewModel.signalLoadingPhoto()
                            var foundPhotoRetry: Photo? = homeScreenViewModel.getPhotoById(photoId)
                            if (foundPhotoRetry == null) {
                                foundPhotoRetry = searchViewModel.getPhotoById(photoId)
                            }
                            imageDetailViewModel.setPhotoDetails(foundPhotoRetry)
                        },
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            if (!uiState.isLoading && uiState.photo == null && uiState.error == null && photoId != 0) {
                 Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                     Text("Photo details not found.")
                 }
            }
        }
    }
} 