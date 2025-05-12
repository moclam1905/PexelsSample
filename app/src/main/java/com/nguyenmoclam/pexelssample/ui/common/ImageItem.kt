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
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val context = LocalContext.current
    val imageRequest = remember(photo.src.medium, photo.id, context) {
        ImageRequest.Builder(context)
            .data(photo.src.medium)
            .transitionFactory(CrossfadeTransition.Factory(durationMillis = 300, preferExactIntrinsicSize = true))
            .listener(
                onStart = { _ ->
                    Log.d("ImageItem_CoilListener", "Photo ID: ${photo.id} (AsyncImage) - Coil Request Started")
                },
                onSuccess = { _, result ->
                    Log.d("ImageItem_CoilListener", "Photo ID: ${photo.id} (AsyncImage) - Coil Request Success. Metadata: ${result.memoryCacheKey}, ${result.diskCacheKey}, ${result.dataSource}")
                },
                onError = { _, result ->
                    Log.e("ImageItem_CoilListener", "Photo ID: ${photo.id} (AsyncImage) - Coil Request Error", result.throwable)
                },
                onCancel = { _ ->
                    Log.d("ImageItem_CoilListener", "Photo ID: ${photo.id} (AsyncImage) - Coil Request Cancelled")
                }
            )
            .build()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp),
        onClick = { onItemClick(photo) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            SubcomposeAsyncImage(
                model = imageRequest,
                contentDescription = photo.alt.ifBlank { "Photo by ${photo.photographer}" },
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
                            Modifier.sharedElement(
                                sharedContentState = sharedTransitionScope.rememberSharedContentState(key = "image-${photo.id}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        } else {
                            Modifier
                        }
                    ),
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(parseColor(photo.avgColor)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp), // Adjusted size for grid item
                            strokeWidth = 3.dp
                        )
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
                            modifier = Modifier.size(40.dp) // Adjusted size for grid item
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