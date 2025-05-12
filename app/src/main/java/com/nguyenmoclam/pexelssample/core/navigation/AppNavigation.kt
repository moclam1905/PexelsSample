package com.nguyenmoclam.pexelssample.core.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nguyenmoclam.pexelssample.ui.detail.ImageDetailScreen
import com.nguyenmoclam.pexelssample.ui.home.HomeScreen
import com.nguyenmoclam.pexelssample.ui.home.SearchViewModel
import com.nguyenmoclam.pexelssample.ui.results.SearchResultsScreen
import com.nguyenmoclam.pexelssample.ui.adaptive.AdaptiveSearchResultsHostScreen

@OptIn(ExperimentalComposeUiApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavigation(windowSizeClass: WindowSizeClass) {
    val navController = rememberNavController()
    val searchViewModel: SearchViewModel = hiltViewModel()

    // Define default transitions
    val subtleFadeEnter = fadeIn(animationSpec = tween(durationMillis = 250))
    val subtleFadeExit = fadeOut(animationSpec = tween(durationMillis = 200))

    SharedTransitionLayout(
        modifier = Modifier.fillMaxSize()
    ) {
        NavHost(
            navController = navController,
            startDestination = ScreenRoutes.HOME,
            // Apply default transitions
            enterTransition = { subtleFadeEnter },
            exitTransition = { subtleFadeExit },
            popEnterTransition = { subtleFadeEnter },
            popExitTransition = { subtleFadeExit }
        ) {
            composable(ScreenRoutes.HOME) {
                HomeScreen(
                    navController = navController,
                    windowSizeClass = windowSizeClass,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable
                )
            }
            composable(ScreenRoutes.SEARCH_RESULTS) {
                SearchResultsScreen(
                    searchViewModel = searchViewModel,
                    navController = navController,
                    windowSizeClass = windowSizeClass,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable
                )
            }
            composable(ScreenRoutes.ADAPTIVE_SEARCH_RESULTS) {
                AdaptiveSearchResultsHostScreen(
                    navController = navController,
                    searchViewModel = searchViewModel,
                    windowSizeClass = windowSizeClass,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable
                )
            }
            composable(
                route = ScreenRoutes.IMAGE_DETAIL + "/{${ScreenRoutes.IMAGE_DETAIL_PHOTO_ID_ARG}}",
                arguments = listOf(navArgument(ScreenRoutes.IMAGE_DETAIL_PHOTO_ID_ARG) { type = NavType.IntType })
            ) { backStackEntry ->
                val photoId = backStackEntry.arguments?.getInt(ScreenRoutes.IMAGE_DETAIL_PHOTO_ID_ARG)
                if (photoId != null) {
                    ImageDetailScreen(
                        navController = navController,
                        photoId = photoId,
                        searchViewModel = searchViewModel,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable
                    )
                } else {
                    // Handle error: photoId not found, perhaps navigate back or show error
                    Text("Error: Photo ID missing.")
                }
            }
        }
    }
} 