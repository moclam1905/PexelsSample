package com.nguyenmoclam.pexelssample.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nguyenmoclam.pexelssample.ui.home.HomeScreen
import com.nguyenmoclam.pexelssample.ui.results.SearchResultsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = ScreenRoutes.HOME) {
        composable(ScreenRoutes.HOME) {
            HomeScreen(
                onNavigateToSearchResults = {
                    navController.navigate(ScreenRoutes.SEARCH_RESULTS)
                }
            )
        }
        composable(ScreenRoutes.SEARCH_RESULTS) {
            SearchResultsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
} 