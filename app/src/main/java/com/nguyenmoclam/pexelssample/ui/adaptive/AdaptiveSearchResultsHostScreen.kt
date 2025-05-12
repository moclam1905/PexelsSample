package com.nguyenmoclam.pexelssample.ui.adaptive

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nguyenmoclam.pexelssample.core.navigation.ScreenRoutes
import com.nguyenmoclam.pexelssample.ui.detail.ImageDetailPaneComposable
import com.nguyenmoclam.pexelssample.ui.home.SearchViewModel
import com.nguyenmoclam.pexelssample.ui.results.SearchResultsListComposable
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.AdaptiveSearchResultsHostScreen(
    navController: NavController,
    searchViewModel: SearchViewModel, // Re-use the instance from AppNavigation
    windowSizeClass: WindowSizeClass,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val selectedPhotoForDetail by searchViewModel.selectedPhotoForDetail.collectAsStateWithLifecycle()
    val currentQuery by searchViewModel.searchQuery.collectAsStateWithLifecycle()
    val isLoading by searchViewModel.isLoading.collectAsStateWithLifecycle()
    val showRecentSearches by searchViewModel.showRecentSearchesSuggestions.collectAsStateWithLifecycle()
    val recentSearchesList by searchViewModel.recentSearches.collectAsStateWithLifecycle()

    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

    val snackbarHostState = remember { SnackbarHostState() }

    // Handle back press for two-pane view
    BackHandler(enabled = isExpanded && selectedPhotoForDetail != null) {
        searchViewModel.clearDetailSelection()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Search")
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
                // Consider placing search bar here instead of content if desired
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        // Main content area includes search bar, recent searches, and results
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp) // Adjust padding as needed
        ) {
            // Search Input Row (similar to old HomeScreen)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = currentQuery,
                    onValueChange = { searchViewModel.onQueryChanged(it) },
                    placeholder = { Text("Search for images...") },
                    label = { Text("Search") },
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { focusState ->
                            searchViewModel.onSearchBarFocusChanged(focusState.isFocused)
                        },
                    singleLine = true,
                    enabled = !isLoading,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search Icon"
                        )
                    },
                    trailingIcon = {
                        if (currentQuery.isNotEmpty()) {
                            IconButton(onClick = { searchViewModel.onQueryChanged("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear Search")
                            }
                        }
                    }
                )
                IconButton(onClick = {
                    searchViewModel.onSearchClicked()
                }, enabled = !isLoading,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search button"
                    )
                }
            }

            // Recent Searches or Loading/Results
            Box(modifier = Modifier.weight(1f)) {
                if (showRecentSearches) {
                    // Display Recent Searches (similar to old HomeScreen)
                    if (recentSearchesList.isNotEmpty()) {
                        LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            items(recentSearchesList, key = { it }) { term ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { searchViewModel.onHistoryItemClicked(term) }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.History,
                                        contentDescription = "Recent search item",
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                    Text(text = term, modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = { searchViewModel.deleteHistoryItem(term) },
                                        modifier = Modifier.sizeIn(minWidth = 40.dp, minHeight = 40.dp)
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "Delete search term: $term")
                                    }
                                }
                            }
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.CenterEnd) {
                                    Button(onClick = { searchViewModel.clearAllHistory() }) {
                                        Text("Clear All History")
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No recent searches",
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .align(Alignment.Center)
                        )
                    }
                } else if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    // Display Search Results (using the adaptive logic)
                    if (isExpanded) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 8.dp) // Add padding between search bar and results
                        ) {
                            Box(modifier = Modifier.weight(0.4f).fillMaxHeight().padding(end = 4.dp)) { // List pane
                                SearchResultsListComposable(
                                    searchViewModel = searchViewModel,
                                    onPhotoClick = { photo -> searchViewModel.onPhotoSelected(photo) },
                                    gridCellsCount = 1,
                                    snackbarHostState = snackbarHostState,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                            }
                            Box(modifier = Modifier.weight(0.6f).fillMaxHeight().padding(start = 4.dp)) { // Detail pane
                                ImageDetailPaneComposable(
                                    selectedPhoto = selectedPhotoForDetail,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                            }
                        }
                    } else {
                        // Compact or Medium: Show only search results list
                        SearchResultsListComposable(
                            searchViewModel = searchViewModel,
                            onPhotoClick = { photo -> navController.navigate(ScreenRoutes.IMAGE_DETAIL + "/${photo.id}") },
                            modifier = Modifier.padding(top = 8.dp), // Add padding between search bar and results
                            gridCellsCount = if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium) 3 else 2,
                            snackbarHostState = snackbarHostState,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }
                }
            }
        }
    }
} 