package com.nguyenmoclam.pexelssample.ui.common

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import coil.transition.CrossfadeTransition
import com.nguyenmoclam.pexelssample.domain.model.Photo
import com.valentinilk.shimmer.shimmer

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.ImageItem(
    photo: Photo,
    modifier: Modifier = Modifier,
    onItemClick: (Photo) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    dynamicHeight: Boolean = false,
    applyRounding: Boolean = true
) {
    val context = LocalContext.current
    val imageRequest = remember(photo.src.medium, photo.id, context) {
        ImageRequest.Builder(context)
            .data(photo.src.medium)
            .transitionFactory(CrossfadeTransition.Factory(durationMillis = 300, preferExactIntrinsicSize = dynamicHeight))
            .build()
    }

    val cardModifier = if (dynamicHeight) {
        modifier
            .fillMaxWidth()
    } else {
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    }

    val imageWrapperBoxModifier = if (dynamicHeight) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.fillMaxSize()
    }

    val imageContentScale = if (dynamicHeight) ContentScale.FillWidth else ContentScale.Crop

    val imageDisplayModifier = (if (dynamicHeight) Modifier.fillMaxWidth() else Modifier.fillMaxSize())
        .then(
            if (applyRounding) {
                Modifier.clip(RoundedCornerShape(12.dp))
            } else {
                Modifier
            }
        )
        .then(
            if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
                Modifier.sharedElement(
                    sharedContentState = sharedTransitionScope.rememberSharedContentState(key = "image-${photo.id}"),
                    animatedVisibilityScope = animatedVisibilityScope
                )
            } else {
                Modifier
            }
        )

    val placeholderBoxModifier = (if (dynamicHeight) Modifier.fillMaxWidth() else Modifier.fillMaxSize())

    var showShimmer by remember { mutableStateOf(false) }

    Card(
        modifier = cardModifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .then(if (applyRounding) Modifier.clip(RoundedCornerShape(12.dp)) else Modifier),
        onClick = { onItemClick(photo) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = if (applyRounding) RoundedCornerShape(12.dp) else CardDefaults.shape
    ) {
        Box(modifier = imageWrapperBoxModifier, contentAlignment = Alignment.Center) {
            SubcomposeAsyncImage(
                model = imageRequest,
                contentDescription = photo.alt.ifBlank { "Photo by ${photo.photographer}" },
                contentScale = imageContentScale,
                modifier = imageDisplayModifier,
                onState = { state ->
                    showShimmer = state is AsyncImagePainter.State.Loading
                },
                content = {
                    val painterState = painter.state
                    if (painterState is AsyncImagePainter.State.Loading || painterState is AsyncImagePainter.State.Error) {
                        Box(
                            modifier = placeholderBoxModifier
                                .background(parseColor(photo.avgColor))
                                .then(if (showShimmer) Modifier.shimmer() else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            if (painterState is AsyncImagePainter.State.Error) {
                                Icon(
                                    imageVector = Icons.Filled.BrokenImage,
                                    contentDescription = "Error loading image",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(40.dp)
                                )
                            } else if (!showShimmer) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp
                                )
                            }
                        }
                    } else {
                        SubcomposeAsyncImageContent(
                            modifier = Modifier.fillMaxSize(),
                            contentScale = imageContentScale
                        )
                    }
                }
            )
        }
    }
}

fun parseColor(colorString: String): Color {
    return try {
        val sanitizedColorString = if (colorString.startsWith("#")) colorString else "#$colorString"
        Color(sanitizedColorString.toColorInt())
    } catch (e: IllegalArgumentException) {
        Color.LightGray
    }
} 