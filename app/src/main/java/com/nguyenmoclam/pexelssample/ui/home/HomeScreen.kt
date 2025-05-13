package com.nguyenmoclam.pexelssample.ui.home

import android.util.Log
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nguyenmoclam.pexelssample.core.navigation.ScreenRoutes
import com.nguyenmoclam.pexelssample.ui.common.ImageItem
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.debounce
import androidx.compose.material.icons.filled.Refresh
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class, FlowPreview::class)
@Composable
fun SharedTransitionScope.HomeScreen(
    navController: NavController,
    viewModel: HomeScreenViewModel = hiltViewModel(),
    windowSizeClass: WindowSizeClass,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val photosList by viewModel.photos.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingInitial.collectAsStateWithLifecycle()
    val isLoadingNextPage by viewModel.isLoadingNextPage.collectAsStateWithLifecycle()
    val nextPageUrl by viewModel.nextPageUrl.collectAsStateWithLifecycle()
    val paginationErrorString by viewModel.paginationError.collectAsStateWithLifecycle()
    val isRefreshingManual by viewModel.isRefreshingManual.collectAsStateWithLifecycle()

    // Remember the grid state
    val gridState = rememberLazyStaggeredGridState()

    // Story 10.4: Snackbar host state
    val snackbarHostState = remember { SnackbarHostState() }

    // Calculate column count based on width class (Story 10.3)
    val columnCount = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 2
        WindowWidthSizeClass.Medium -> 3
        WindowWidthSizeClass.Expanded -> 4
        else -> 2 // Sensible default
    }

    LaunchedEffect(windowSizeClass) {
        Log.d("HomeScreen", "WindowSizeClass updated: Width=${windowSizeClass.widthSizeClass}, Height=${windowSizeClass.heightSizeClass}")
    }

    LaunchedEffect(Unit) { // Run once to set up the collector
        snapshotFlow {
            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()
            val currentPhotosSize = photosList.size // Access photosList state here
            val buffer = 5
            lastVisibleItem != null && currentPhotosSize > 0 && lastVisibleItem.index >= currentPhotosSize - 1 - buffer
        }
            .distinctUntilChanged() // Only emit when the near-end state changes
            .filter { isNearEnd -> isNearEnd } // Only react when it becomes true
            .debounce(300L)
            .collect {
                // Check other conditions when near-end becomes true
                if (nextPageUrl != null && !isLoading && !isLoadingNextPage && paginationErrorString == null) {
                    Log.d("HomeScreenScroll", ">>> Calling loadNextCuratedPage (Triggered by isNearEnd becoming true) <<<")
                    viewModel.loadMorePhotos()
                }
            }
    }

    // Story 10.4: Observe Snackbar events
    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collectLatest { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Trending Photos") },
                actions = {
                    IconButton(
                        onClick = { viewModel.onManualRefreshTriggered() },
                        enabled = !isRefreshingManual && !isLoading
                    ) {
                        if (isRefreshingManual) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh Trending Photos")
                        }
                    }
                    IconButton(onClick = {
                        navController.navigate(ScreenRoutes.ADAPTIVE_SEARCH_RESULTS)
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading && photosList.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (photosList.isNotEmpty()) {
                LazyVerticalStaggeredGrid(
                    state = gridState,
                    columns = StaggeredGridCells.Fixed(columnCount),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        top = 8.dp,
                        bottom = 8.dp
                    ),
                    verticalItemSpacing = 8.dp,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        count = photosList.size,
                        key = { index -> photosList[index].id }
                    ) { index ->
                        val photo = photosList[index]
                        ImageItem(
                            photo = photo,
                            onItemClick = { /* TODO: Navigation to detail (Story 10.7) */ },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            dynamicHeight = true
                        )
                    }

                    // Loading indicator item (AC3)
                    if (isLoadingNextPage) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    // Pagination error item (AC6)
                    paginationErrorString?.let { errorMessage ->
                        if (!isLoadingNextPage && nextPageUrl != null) { // Don't show error if we are currently loading
                            item(span = StaggeredGridItemSpan.FullLine) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                                    Button(onClick = { viewModel.loadMorePhotos() }) {
                                        Text("Retry")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
