package com.nguyenmoclam.pexelssample.ui.common

import android.util.Log
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import coil.transition.CrossfadeTransition
import com.nguyenmoclam.pexelssample.domain.model.Photo
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.ImageItem(
    photo: Photo,
    modifier: Modifier = Modifier,
    onItemClick: (Photo) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    dynamicHeight: Boolean = false
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

    Card(
        modifier = cardModifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
        onClick = { onItemClick(photo) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = imageWrapperBoxModifier, contentAlignment = Alignment.Center) {
            SubcomposeAsyncImage(
                model = imageRequest,
                contentDescription = photo.alt.ifBlank { "Photo by ${photo.photographer}" },
                contentScale = imageContentScale,
                modifier = imageDisplayModifier,
                loading = {
                    Box(
                        modifier = placeholderBoxModifier.background(parseColor(photo.avgColor)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                    }
                },
                error = {
                    Box(
                        modifier = placeholderBoxModifier.background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BrokenImage,
                            contentDescription = "Error loading image",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                },
                success = {
                    SubcomposeAsyncImageContent()
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