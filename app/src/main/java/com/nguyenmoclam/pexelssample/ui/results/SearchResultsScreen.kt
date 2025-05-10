package com.nguyenmoclam.pexelssample.ui.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nguyenmoclam.pexelssample.domain.model.Photo
import com.nguyenmoclam.pexelssample.domain.model.PhotoSrc
import com.nguyenmoclam.pexelssample.core.navigation.ScreenRoutes
import com.nguyenmoclam.pexelssample.ui.common.ImageItem
import com.nguyenmoclam.pexelssample.ui.home.SearchViewModel
import com.nguyenmoclam.pexelssample.ui.theme.PexelsSampleTheme
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsScreen(
    searchViewModel: SearchViewModel,
    navController: NavController
) {
    val photoList by searchViewModel.photos.collectAsStateWithLifecycle()
    val isLoadingValue by searchViewModel.isLoading.collectAsStateWithLifecycle() // Initial load
    val isLoadingMoreValue by searchViewModel.isLoadingMore.collectAsStateWithLifecycle() // Pagination load
    val canLoadMoreValue by searchViewModel.canLoadMore.collectAsStateWithLifecycle()
    val isEmptyResults by searchViewModel.isResultsEmpty.collectAsStateWithLifecycle()
    val currentQuery by searchViewModel.searchQuery.collectAsStateWithLifecycle()

    val gridState = rememberLazyGridState()

    // Scroll detection logic
    val buffer = 3 // Number of items from end to trigger load
    val shouldLoadMore by remember(photoList.size, canLoadMoreValue, isLoadingValue, isLoadingMoreValue) {
        derivedStateOf {
            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && photoList.isNotEmpty() && // Ensure photoList is not empty
            lastVisibleItem.index >= photoList.size - 1 - buffer &&
            canLoadMoreValue && !isLoadingValue && !isLoadingMoreValue
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            searchViewModel.loadNextPage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "Search Results for $currentQuery") })
        }
    ) { paddingValues ->
        if (isLoadingValue && photoList.isEmpty()) { // Show loading for initial search
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (isEmptyResults) { // Display "No images found for '[currentQuery]'. Try another search." Text
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp), // Added padding for the message
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No images found for '$currentQuery'. Try another search.")
            }
        } else if (photoList.isNotEmpty()) { // Display LazyVerticalGrid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState, // Pass the gridState
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
                            navController.navigate(ScreenRoutes.IMAGE_DETAIL + "/${selectedPhoto.id}")
                        },
                        modifier = Modifier.padding(4.dp)
                    )
                }

                if (isLoadingMoreValue && photoList.isNotEmpty()) {
                    item(span = { GridItemSpan(this.maxLineSpan) }) { // Span across all columns
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
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