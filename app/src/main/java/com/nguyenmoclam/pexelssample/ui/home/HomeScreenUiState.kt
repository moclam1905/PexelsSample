package com.nguyenmoclam.pexelssample.ui.home

import com.nguyenmoclam.pexelssample.domain.model.Photo
import com.nguyenmoclam.pexelssample.ui.model.UserFacingError

/**
 * Represents the different UI states for the initial load and content display
 * on the HomeScreen, specifically for the trending photos feed.
 */
sealed interface HomeScreenUiState {
    /** Represents the initial loading state before any content is shown. */
    data object InitialLoading : HomeScreenUiState

    /** Represents the state where trending photos are successfully loaded and displayed. */
    data class Content(val photos: List<Photo>) : HomeScreenUiState

    /** Represents the state where an error occurred during the initial load. */
    data class Error(val errorDetails: UserFacingError) : HomeScreenUiState

    /** Represents the state where the initial load completed successfully but returned no photos. */
    data object Empty : HomeScreenUiState
} 