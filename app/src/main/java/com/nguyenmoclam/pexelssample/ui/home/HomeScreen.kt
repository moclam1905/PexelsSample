package com.nguyenmoclam.pexelssample.ui.home

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.History
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nguyenmoclam.pexelssample.core.navigation.ScreenRoutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    searchViewModel: SearchViewModel,
    windowSizeClass: WindowSizeClass
) {
    val currentQuery by searchViewModel.searchQuery.collectAsStateWithLifecycle()
    val isLoadingValue by searchViewModel.isLoading.collectAsStateWithLifecycle()
    val navigateEffect by searchViewModel.navigateToResults.collectAsStateWithLifecycle()

    // Story 8.5: Collect recent searches state
    val showRecentSearches by searchViewModel.showRecentSearchesSuggestions.collectAsStateWithLifecycle()
    val recentSearchesList by searchViewModel.recentSearches.collectAsStateWithLifecycle()
    // End Story 8.5

    // AC2: Adaptive padding based on window size
    val screenPadding = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 16.dp
        WindowWidthSizeClass.Medium -> 24.dp
        WindowWidthSizeClass.Expanded -> 32.dp
        else -> 16.dp
    }

    LaunchedEffect(windowSizeClass) {
        Log.d("HomeScreen", "WindowSizeClass updated: Width=${windowSizeClass.widthSizeClass}, Height=${windowSizeClass.heightSizeClass}")
    }

    LaunchedEffect(navigateEffect) {
        if (navigateEffect) {
            navController.navigate(ScreenRoutes.ADAPTIVE_SEARCH_RESULTS)
            searchViewModel.onNavigationComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Pexels Image Search") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
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
                    enabled = !isLoadingValue,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search Icon"
                        )
                    }
                )
                IconButton(onClick = {
                    searchViewModel.onSearchClicked()
                }, enabled = !isLoadingValue,
                   modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search button"
                    )
                }
            }

            // Story 8.5: Display Recent Searches
            if (showRecentSearches) {
                if (recentSearchesList.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        items(recentSearchesList) { term ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        searchViewModel.onQueryChanged(term) // Update query
                                        searchViewModel.onSearchClicked()    // Perform search
                                        // Focus should be lost or handled by VM when search starts
                                    }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.History,
                                    contentDescription = "Recent search item",
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Text(term)
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No recent searches",
                        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    )
                }
            }
            // End Story 8.5

            if (isLoadingValue) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
