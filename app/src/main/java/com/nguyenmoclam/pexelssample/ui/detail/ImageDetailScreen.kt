package com.nguyenmoclam.pexelssample.ui.detail

import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(photo?.photographer?.let { "Photo by $it" } ?: "Detail") },
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
                .padding(paddingValues) // Apply padding from Scaffold
                .padding(16.dp) // Apply additional overall padding
                .verticalScroll(rememberScrollState())
        ) {
            if (photo != null) {
                val currentPhoto = photo!! // Safe due to the null check

                // Image Placeholder Box
                val aspectRatio = if (currentPhoto.height > 0) currentPhoto.width.toFloat() / currentPhoto.height.toFloat() else 1f

                SubcomposeAsyncImage(
                    model = currentPhoto.src.large2x,
                    contentDescription = currentPhoto.alt.ifBlank { "Full image by ${currentPhoto.photographer}" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio.coerceIn(0.5f, 2f)),
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
                                .background(Color.DarkGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.BrokenImage,
                                contentDescription = "Error loading image",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    },
                    success = {
                        SubcomposeAsyncImageContent() // This renders the actual image
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Photographer Name
                Text(
                    text = "Photographer: ${currentPhoto.photographer}",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Alt Text/Description
                if (currentPhoto.alt.isNotBlank()) {
                    Text(
                        text = "Description: ${currentPhoto.alt}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                // Fallback content if photo is null (e.g., loading indicator or error message)
                // For this story, we'll keep it simple, but a Box with Alignment.Center could be used.
                Box(modifier = Modifier.fillMaxSize()) { // Occupy available space
                     Text("Loading photo details or photo not found (ID: $photoId)...")
                }
            }
        }
    }
} 