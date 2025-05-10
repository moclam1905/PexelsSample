package com.nguyenmoclam.pexelssample.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.nguyenmoclam.pexelssample.domain.model.Photo
import com.nguyenmoclam.pexelssample.ui.home.SearchViewModel // Assuming shared VM for simplicity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageDetailScreen(
    navController: NavController,
    photoId: Int,
    // For MVP, re-use SearchViewModel to get already loaded photo data
    // A dedicated ImageDetailViewModel could be introduced for more complex logic or independent fetching
    searchViewModel: SearchViewModel = hiltViewModel() // Or activity Hilt view model if scoped
) {
    var photo by remember { mutableStateOf<Photo?>(null) }

    LaunchedEffect(photoId) {
        photo = searchViewModel.getPhotoById(photoId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(photo?.photographer ?: "Detail") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (photo != null) {
                Text("Image Detail for Photo ID: $photoId")
                Text("Photographer: ${photo!!.photographer}")
                // Image display will be added in Story 4.3
            } else {
                Text("Loading photo details or photo not found (ID: $photoId)...")
            }
        }
    }
} 