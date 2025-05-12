package com.nguyenmoclam.pexelssample.ui.results

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nguyenmoclam.pexelssample.domain.model.Photo
import com.nguyenmoclam.pexelssample.ui.common.ErrorView
import com.nguyenmoclam.pexelssample.ui.common.ImageItem
import com.nguyenmoclam.pexelssample.ui.home.SearchViewModel

// Define common animation transitions
private val enterTransition = fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300), initialOffsetY = { it / 10 })
private val exitTransition = fadeOut(animationSpec = tween(300)) + slideOutVertically(animationSpec = tween(300), targetOffsetY = { it / 10 })

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.SearchResultsListComposable(
    searchViewModel: SearchViewModel,
    onPhotoClick: (Photo) -> Unit,
    modifier: Modifier = Modifier,
    gridCellsCount: Int = 2, // Default for smaller screens, can be adjusted
    snackbarHostState: SnackbarHostState,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val photos by searchViewModel.photos.collectAsStateWithLifecycle()
    val isLoading by searchViewModel.isLoading.collectAsStateWithLifecycle()
    val isLoadingMore by searchViewModel.isLoadingMore.collectAsStateWithLifecycle()
    val canLoadMore by searchViewModel.canLoadMore.collectAsStateWithLifecycle()
    val isResultsEmpty by searchViewModel.isResultsEmpty.collectAsStateWithLifecycle()
    val errorState by searchViewModel.errorState.collectAsStateWithLifecycle()
    val currentQuery by searchViewModel.searchQuery.collectAsStateWithLifecycle()

    // Story 8.1: Pull-to-Refresh state
    val isRefreshing by searchViewModel.isRefreshing.collectAsStateWithLifecycle()
    val pullRefreshState = rememberPullToRefreshState()
    // End Story 8.1

    val listState = rememberSaveable(key = "searchResultsGridState", saver = LazyGridState.Saver) {
        LazyGridState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
    }

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

    LaunchedEffect(errorState, photos) { // Keyed on errorState and photos to react to changes
        val currentError = errorState
        if (currentError != null && photos.isNotEmpty()) {
            snackbarHostState.showSnackbar(
                message = currentError.message,
                duration = SnackbarDuration.Short
            )
            searchViewModel.clearErrorState() // Clear error after showing Snackbar
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { searchViewModel.onRefreshTriggered() }, // This onRefresh is for the Box, not the state directly
        modifier = modifier.fillMaxSize(),
        state = pullRefreshState,
        indicator = {
            Indicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = isRefreshing,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                state = pullRefreshState
            )
        },// Apply the original modifier here
    ) {
        AnimatedVisibility(
            visible = isLoading && photos.isEmpty() && !isRefreshing,
            enter = enterTransition,
            exit = exitTransition,
            modifier = Modifier.align(Alignment.Center)
        ) {
            CircularProgressIndicator()
        }

        AnimatedVisibility(
            visible = errorState != null && photos.isEmpty() && !isRefreshing,
            enter = enterTransition,
            exit = exitTransition,
            modifier = Modifier.fillMaxSize()
        ) {
            ErrorView(
                error = errorState,
                onRetry = searchViewModel::retryLastFailedOperation,
                modifier = Modifier.fillMaxSize()
            )
        }

        AnimatedVisibility(
            visible = isResultsEmpty && !isRefreshing,
            enter = enterTransition,
            exit = exitTransition,
            modifier = Modifier.fillMaxSize()
        ) {
            EmptyResultsDisplay(currentQuery)
        }
        
        val showPhotoGrid = photos.isNotEmpty() &&
                !(isLoading && photos.isEmpty() && !isRefreshing) &&
                !(errorState != null && photos.isEmpty() && !isRefreshing) &&
                !(isResultsEmpty && !isRefreshing)

        if (showPhotoGrid) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridCellsCount),
                state = listState,
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(photos, key = { photo -> photo.id }) { photo ->
                    ImageItem(
                        photo = photo,
                        onItemClick = { onPhotoClick(photo) },
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }
                if (isLoadingMore) {
                    item(span = { GridItemSpan(this.maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
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