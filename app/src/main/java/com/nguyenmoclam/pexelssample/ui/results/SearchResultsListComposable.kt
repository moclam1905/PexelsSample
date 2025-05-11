package com.nguyenmoclam.pexelssample.ui.results

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nguyenmoclam.pexelssample.domain.model.Photo
import com.nguyenmoclam.pexelssample.ui.home.SearchViewModel
import com.nguyenmoclam.pexelssample.ui.model.UserFacingError

@Composable
fun SearchResultsListComposable(
    searchViewModel: SearchViewModel,
    onPhotoClick: (Photo) -> Unit,
    modifier: Modifier = Modifier,
    gridCellsCount: Int = 2 // Default for smaller screens, can be adjusted
) {
    val photos by searchViewModel.photos.collectAsStateWithLifecycle()
    val isLoading by searchViewModel.isLoading.collectAsStateWithLifecycle()
    val isLoadingMore by searchViewModel.isLoadingMore.collectAsStateWithLifecycle()
    val canLoadMore by searchViewModel.canLoadMore.collectAsStateWithLifecycle()
    val isResultsEmpty by searchViewModel.isResultsEmpty.collectAsStateWithLifecycle()
    val errorState by searchViewModel.errorState.collectAsStateWithLifecycle()
    val currentQuery by searchViewModel.searchQuery.collectAsStateWithLifecycle()

    val listState = rememberLazyGridState()

    // Trigger load more when near the end of the list
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index
            if (lastVisibleItemIndex != null) {
                lastVisibleItemIndex >= layoutInfo.totalItemsCount - 1 - (gridCellsCount * 2) // Load when 2 rows from end
            } else {
                false
            }
        }
    }

    LaunchedEffect(shouldLoadMore, canLoadMore, isLoadingMore) {
        if (shouldLoadMore && canLoadMore && !isLoadingMore && !isLoading) {
            searchViewModel.loadNextPage()
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading && photos.isEmpty() -> { // Initial full screen loading
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            errorState != null -> {
                ErrorDisplay(errorState, searchViewModel::retryLastFailedOperation)
            }
            isResultsEmpty -> {
                EmptyResultsDisplay(currentQuery)
            }
            photos.isNotEmpty() -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridCellsCount),
                    state = listState,
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(photos, key = { photo -> photo.id }) { photo ->
                        PhotoGridItem(photo = photo, onClick = { onPhotoClick(photo) })
                    }
                    if (isLoadingMore) {
                        item { // Span across all columns for the loading indicator
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoGridItem(photo: Photo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photo.src.medium)
                .crossfade(true)
                .build(),
            contentDescription = photo.alt.ifBlank { "Photo by ${photo.photographer}" },
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp) // Adjust height as needed
        )
    }
}

@Composable
fun ErrorDisplay(error: UserFacingError?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = error?.message ?: "An unknown error occurred.",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (error?.isRetryable == true) {
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
fun EmptyResultsDisplay(query: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (query.isBlank()) "Start by typing a search query above." else "No results found for \"${query}\". Try a different search.",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
    }
} 