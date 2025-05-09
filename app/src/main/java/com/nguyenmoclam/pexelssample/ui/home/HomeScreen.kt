package com.nguyenmoclam.pexelssample.ui.home

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Box
// import androidx.compose.material3.Button // Will remove this if not used elsewhere
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
// import androidx.compose.runtime.mutableStateOf // No longer needed for searchQuery
// import androidx.compose.runtime.remember // No longer needed for searchQuery
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nguyenmoclam.pexelssample.ui.theme.PexelsSampleTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import com.nguyenmoclam.pexelssample.core.navigation.ScreenRoutes
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    searchViewModel: SearchViewModel = hiltViewModel()
) {
    val currentQuery by searchViewModel.searchQuery.collectAsStateWithLifecycle()
    val isLoadingValue by searchViewModel.isLoading.collectAsStateWithLifecycle()
    val navigateEffect by searchViewModel.navigateToResults.collectAsStateWithLifecycle()

    LaunchedEffect(navigateEffect) {
        if (navigateEffect) {
            navController.navigate(ScreenRoutes.SEARCH_RESULTS)
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
                .padding(16.dp),
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
                    modifier = Modifier.weight(1f),
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
                }, enabled = !isLoadingValue) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search button"
                    )
                }
            }

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

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    PexelsSampleTheme {
        val navController = rememberNavController()
        HomeScreen(navController = navController)
    }
}