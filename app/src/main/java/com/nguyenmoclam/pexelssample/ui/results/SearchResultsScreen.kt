package com.nguyenmoclam.pexelssample.ui.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nguyenmoclam.pexelssample.domain.model.Photo
import com.nguyenmoclam.pexelssample.domain.model.PhotoSrc
import com.nguyenmoclam.pexelssample.ui.common.ImageItem
import com.nguyenmoclam.pexelssample.ui.home.SearchViewModel
import com.nguyenmoclam.pexelssample.ui.theme.PexelsSampleTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsScreen(
    viewModel: SearchViewModel
) {
    val photoList by viewModel.photos.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Search Results") })
        }
    ) { paddingValues ->
        if (photoList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No results found, or search not yet performed.")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(photoList, key = { photo -> photo.id }) { photo ->
                    ImageItem(
                        photo = photo,
                        onItemClick = { selectedPhoto ->
                            // Handle click in Story 4.1
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SearchResultsScreenPreview() {
    PexelsSampleTheme {
        val samplePhotoSrc = PhotoSrc(
            original = "https://via.placeholder.com/1500",
            large2x = "https://via.placeholder.com/800",
            large = "https://via.placeholder.com/600",
            medium = "https://via.placeholder.com/350",
            small = "https://via.placeholder.com/200",
            portrait = "https://via.placeholder.com/350x500",
            landscape = "https://via.placeholder.com/500x350",
            tiny = "https://via.placeholder.com/100"
        )
        val samplePhoto = Photo(
            id = 1,
            width = 350,
            height = 350,
            url = "https://www.example.com/photo1",
            photographer = "Sample Photographer 1",
            photographerUrl = "https://www.example.com/photographer1",
            photographerId = 101,
            avgColor = "#73998D",
            src = samplePhotoSrc,
            alt = "A beautiful sample photo"
        )
        val samplePhoto2 = samplePhoto.copy(
            id = 2,
            photographer = "Sample Photographer 2",
            photographerUrl = "https://www.example.com/photographer2",
            photographerId = 102,
            avgColor = "#8D7399",
            alt = "Another nice sample photo"
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.padding(8.dp),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listOf(samplePhoto, samplePhoto2)) { photo ->
                 ImageItem(photo = photo, onItemClick = {})
            }
        }
    }
}