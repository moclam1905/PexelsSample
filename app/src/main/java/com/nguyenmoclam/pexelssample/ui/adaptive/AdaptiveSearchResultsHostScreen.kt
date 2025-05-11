package com.nguyenmoclam.pexelssample.ui.adaptive

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nguyenmoclam.pexelssample.core.navigation.ScreenRoutes
import com.nguyenmoclam.pexelssample.ui.detail.ImageDetailPaneComposable
import com.nguyenmoclam.pexelssample.ui.home.SearchViewModel
import com.nguyenmoclam.pexelssample.ui.results.SearchResultsListComposable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveSearchResultsHostScreen(
    navController: NavController,
    searchViewModel: SearchViewModel, // Re-use the instance from AppNavigation
    windowSizeClass: WindowSizeClass
) {
    val selectedPhotoForDetail by searchViewModel.selectedPhotoForDetail.collectAsStateWithLifecycle()
    val currentQuery by searchViewModel.searchQuery.collectAsStateWithLifecycle()

    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

    // Handle back press for two-pane view
    BackHandler(enabled = isExpanded && selectedPhotoForDetail != null) {
        searchViewModel.clearDetailSelection()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(if (currentQuery.isNotBlank()) "Results for \"$currentQuery\"" else "Search Results") 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 8.dp, vertical = 8.dp) // Add some padding around the Row
            ) {
                Box(modifier = Modifier.weight(0.4f).fillMaxHeight().padding(end = 4.dp)) { // List pane
                    SearchResultsListComposable(
                        searchViewModel = searchViewModel,
                        onPhotoClick = { photo ->
                            searchViewModel.onPhotoSelected(photo)
                        },
                        gridCellsCount = 1 // Single column for the list pane
                    )
                }
                Box(modifier = Modifier.weight(0.6f).fillMaxHeight().padding(start = 4.dp)) { // Detail pane
                    ImageDetailPaneComposable(
                        selectedPhoto = selectedPhotoForDetail,
                        // imageDetailViewModel will be hiltViewModel() within the composable
                    )
                }
            }
        } else {
            // Compact or Medium: Show only search results list
            SearchResultsListComposable(
                searchViewModel = searchViewModel,
                onPhotoClick = { photo ->
                    navController.navigate(ScreenRoutes.IMAGE_DETAIL + "/${photo.id}")
                },
                modifier = Modifier.padding(paddingValues),
                gridCellsCount = if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium) 3 else 2
            )
        }
    }
} 