package com.nguyenmoclam.pexelssample.ui.common

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nguyenmoclam.pexelssample.domain.model.Photo

@Composable
fun ImageItem(
    photo: Photo,
    modifier: Modifier = Modifier,
    onItemClick: (Photo) -> Unit
) {
    val context = LocalContext.current
    val imageRequest = remember(photo.src.medium, photo.id, context) {
        ImageRequest.Builder(context)
            .data(photo.src.medium)
            .crossfade(true)
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

    val placeholderPainter = remember(photo.avgColor) {
        ColorPainter(parseColor(photo.avgColor))
    }
    val errorPainter = remember {
        ColorPainter(Color.DarkGray)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        onClick = { onItemClick(photo) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AsyncImage(
                model = imageRequest,
                contentDescription = photo.alt,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                placeholder = placeholderPainter,
                error = errorPainter,
                onLoading = { state ->
                    Log.d("ImageItem_DEBUG", "Photo ID: ${photo.id} (AsyncImage) - State: Loading. Placeholder active.")
                },
                onSuccess = { state ->
                    Log.d("ImageItem_DEBUG", "Photo ID: ${photo.id} (AsyncImage) - State: Success. Image loaded from ${state.result.dataSource}.")
                },
                onError = { state ->
                    Log.e("ImageItem_DEBUG", "Photo ID: ${photo.id} (AsyncImage) - State: Error. Error: ", state.result.throwable)
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