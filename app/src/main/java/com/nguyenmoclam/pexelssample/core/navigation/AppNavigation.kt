package com.nguyenmoclam.pexelssample.core.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nguyenmoclam.pexelssample.ui.home.HomeScreen
import com.nguyenmoclam.pexelssample.ui.home.SearchViewModel
import com.nguyenmoclam.pexelssample.ui.results.SearchResultsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val searchViewModel: SearchViewModel = hiltViewModel()

    NavHost(navController = navController, startDestination = ScreenRoutes.HOME) {
        composable(ScreenRoutes.HOME) {
            HomeScreen(
                navController = navController, searchViewModel
            )
        }
        composable(ScreenRoutes.SEARCH_RESULTS) {
            SearchResultsScreen(searchViewModel)
        }
    }
} 