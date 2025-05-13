package com.nguyenmoclam.pexelssample.ui.home

import android.util.Log
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nguyenmoclam.pexelssample.core.navigation.ScreenRoutes
import com.nguyenmoclam.pexelssample.ui.common.ErrorView
import com.nguyenmoclam.pexelssample.ui.common.ImageItem
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class, FlowPreview::class)
@Composable
fun SharedTransitionScope.HomeScreen(
    navController: NavController,
    viewModel: HomeScreenViewModel = hiltViewModel(),
    windowSizeClass: WindowSizeClass,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val isLoadingNextPage by viewModel.isLoadingNextPage.collectAsStateWithLifecycle()
    val nextPageUrl by viewModel.nextPageUrl.collectAsStateWithLifecycle()
    val paginationErrorString by viewModel.paginationError.collectAsStateWithLifecycle()
    val isRefreshingManual by viewModel.isRefreshingManual.collectAsStateWithLifecycle()

    val gridState = rememberLazyStaggeredGridState()
    val snackbarHostState = remember { SnackbarHostState() }

    val columnCount = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 2
        WindowWidthSizeClass.Medium -> 3
        WindowWidthSizeClass.Expanded -> 4
        else -> 2 // Sensible default
    }

    LaunchedEffect(windowSizeClass) {
        Log.d("HomeScreen", "WindowSizeClass updated: Width=${windowSizeClass.widthSizeClass}, Height=${windowSizeClass.heightSizeClass}")
    }

    LaunchedEffect(uiState, gridState, nextPageUrl, isLoadingNextPage, paginationErrorString) {
        snapshotFlow {
            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()
            val currentPhotosSize = (uiState as? HomeScreenUiState.Content)?.photos?.size ?: 0
            val buffer = 5
            lastVisibleItem != null && currentPhotosSize > 0 && lastVisibleItem.index >= currentPhotosSize - 1 - buffer
        }
            .distinctUntilChanged()
            .filter { isNearEnd -> isNearEnd }
            .debounce(300L)
            .collect {
                if (uiState is HomeScreenUiState.Content && nextPageUrl != null && !isLoadingNextPage && paginationErrorString == null && !isRefreshingManual) {
                    Log.d("HomeScreenScroll", ">>> Calling loadMorePhotos (Triggered by isNearEnd becoming true) <<<")
                    viewModel.loadMorePhotos()
                }
            }
    }

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
                        enabled = uiState != HomeScreenUiState.InitialLoading && !isRefreshingManual && !isLoadingNextPage
                    ) {
                        if (isRefreshingManual || (uiState == HomeScreenUiState.InitialLoading && (uiState as? HomeScreenUiState.Content)?.photos.isNullOrEmpty())) {
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
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (val currentScreenState = uiState) {
                is HomeScreenUiState.InitialLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is HomeScreenUiState.Content -> {
                    val photosList = currentScreenState.photos
                    if (photosList.isNotEmpty()) {
                        LazyVerticalStaggeredGrid(
                            state = gridState,
                            columns = StaggeredGridCells.Fixed(columnCount),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
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
                                    onItemClick = { /* TODO: Story 10.7 */ },
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    dynamicHeight = true,
                                    applyRounding = true
                                )
                            }

                            if (isLoadingNextPage) {
                                item(span = StaggeredGridItemSpan.FullLine) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }

                            paginationErrorString?.let {
                                if (!isLoadingNextPage && nextPageUrl != null) {
                                    item(span = StaggeredGridItemSpan.FullLine) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(it, color = MaterialTheme.colorScheme.error)
                                            Button(onClick = { viewModel.loadMorePhotos() }) {
                                                Text("Retry Page")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        CenteredMessageComposable(message = "No trending photos available at the moment.")
                    }
                }
                is HomeScreenUiState.Empty -> {
                    CenteredMessageComposable(message = "No trending photos to show right now. Try refreshing!")
                }
                is HomeScreenUiState.Error -> {
                    ErrorView(
                        error = currentScreenState.errorDetails,
                        onRetry = { viewModel.fetchInitialPhotos() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun CenteredMessageComposable(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
