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
import com.nguyenmoclam.pexelssample.ui.common.ErrorView
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
    val isLoadingValue by searchViewModel.isLoading.collectAsStateWithLifecycle()
    val isLoadingMoreValue by searchViewModel.isLoadingMore.collectAsStateWithLifecycle()
    val canLoadMoreValue by searchViewModel.canLoadMore.collectAsStateWithLifecycle()
    val isEmptyResults by searchViewModel.isResultsEmpty.collectAsStateWithLifecycle()
    val currentQuery by searchViewModel.searchQuery.collectAsStateWithLifecycle()
    val currentError by searchViewModel.errorState.collectAsStateWithLifecycle()

    val gridState = rememberLazyGridState()

    val buffer = 3
    val shouldLoadMore by remember(photoList.size, canLoadMoreValue, isLoadingValue, isLoadingMoreValue, currentError) {
        derivedStateOf {
            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && photoList.isNotEmpty() &&
            lastVisibleItem.index >= photoList.size - 1 - buffer &&
            canLoadMoreValue && !isLoadingValue && !isLoadingMoreValue && currentError == null // Do not load more if there's an error
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            searchViewModel.loadNextPage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = if (currentQuery.isNotBlank()) "Results for '$currentQuery'" else "Search Results") })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoadingValue && photoList.isEmpty() && currentError == null -> {
                    CircularProgressIndicator()
                }
                currentError != null -> {
                    ErrorView(error = currentError) {
                        searchViewModel.retryLastFailedOperation()
                    }
                }
                isEmptyResults && photoList.isEmpty() -> {
                    Text(
                        text = "No images found for '$currentQuery'. Try another search.",
                        modifier = Modifier.padding(16.dp)
                    )
                }
                photoList.isNotEmpty() -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(photoList, key = { photo -> photo.id }) { photo ->
                            ImageItem(
                                photo = photo,
                                onItemClick = { selectedPhoto ->
                                    navController.navigate(ScreenRoutes.IMAGE_DETAIL + "/${selectedPhoto.id}")
                                },
                                modifier = Modifier.padding(4.dp) // Item padding
                            )
                        }

                        if (isLoadingMoreValue && currentError == null) {
                            item(span = { GridItemSpan(this.maxLineSpan) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp), // Padding for the loader itself
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
                // Fallback for initial state before any search or if query is cleared - might be an empty screen or a prompt.
                // For now, if query is blank and no error/loading/results, it implies we are back to a blank state for this screen.
                currentQuery.isBlank() && !isLoadingValue && currentError == null && photoList.isEmpty() -> {
                    Text(
                        text = "Search results will appear here.",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SearchResultsScreenPreview_Empty() {
    PexelsSampleTheme {
        // Mock ViewModel or provide dummy data for preview
        // This preview shows an empty state
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text("No images found for 'Preview Query'. Try another search.")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SearchResultsScreenPreview_Loading() {
    PexelsSampleTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SearchResultsScreenPreview_Error() {
    PexelsSampleTheme {
        ErrorView(error = com.nguyenmoclam.pexelssample.ui.model.UserFacingError("Preview Error: Something went wrong", true)) {}
    }
}


@Preview(showBackground = true)
@Composable
fun SearchResultsScreenPreview_WithData() {
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