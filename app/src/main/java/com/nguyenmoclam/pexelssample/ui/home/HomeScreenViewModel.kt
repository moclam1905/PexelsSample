package com.nguyenmoclam.pexelssample.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nguyenmoclam.pexelssample.domain.model.PhotosResult
import com.nguyenmoclam.pexelssample.domain.repository.ImageRepository
import com.nguyenmoclam.pexelssample.logger.Logger
import com.nguyenmoclam.pexelssample.ui.model.UserFacingError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Define items per page here or retrieve from a config if needed
const val ITEMS_PER_PAGE = 30 // Default: 15 Max: 80

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val imageRepository: ImageRepository
) : ViewModel() {

    // Add the combined UI State flow
    private val _uiState = MutableStateFlow<HomeScreenUiState>(HomeScreenUiState.InitialLoading)
    val uiState: StateFlow<HomeScreenUiState> = _uiState.asStateFlow()

    private val _nextPageUrl = MutableStateFlow<String?>(null)
    val nextPageUrl: StateFlow<String?> = _nextPageUrl.asStateFlow()

    private val _isLoadingNextPage = MutableStateFlow(false)
    val isLoadingNextPage: StateFlow<Boolean> = _isLoadingNextPage.asStateFlow()

    // Simple String error message for pagination
    private val _paginationError = MutableStateFlow<String?>(null)
    val paginationError: StateFlow<String?> = _paginationError.asStateFlow()

    // State for manual refresh action (Story 10.4)
    private val _isRefreshingManual = MutableStateFlow(false)
    val isRefreshingManual: StateFlow<Boolean> = _isRefreshingManual.asStateFlow()

    // Event for one-time Snackbar messages (Story 10.4)
    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    // Keep track of current page for curated photos (needed for reset on manual refresh)
    private var currentCuratedPage = 1 // Initial page is 1

    init {
        fetchInitialPhotos()
    }

    // Public function to allow UI to trigger initial load (e.g., retry)
    fun fetchInitialPhotos() {
        // Use _uiState.value to check if already loading
        // Remove the check for InitialLoading state here, as it prevents the call from init.
        // Only prevent if a manual refresh is already in progress.
        if (_isRefreshingManual.value) {
            Logger.d("HomeScreenViewModel", "Skipping fetchInitialPhotos: Manual refresh already in progress.")
            return
        }

        viewModelScope.launch {
            // Set state to InitialLoading - This correctly indicates the fetch is starting.
            _uiState.value = HomeScreenUiState.InitialLoading
            _paginationError.value = null // Clear pagination error on initial load/refresh
            try {
                // Page 1 for initial load
                when (val result = imageRepository.getCuratedPhotos(page = 1, perPage = ITEMS_PER_PAGE)) {
                    is PhotosResult.Success -> {
                        val loadedPhotos = result.photos
                        if (loadedPhotos.isNotEmpty()) {
                            // Set Content state
                            _uiState.value = HomeScreenUiState.Content(loadedPhotos)
                            _nextPageUrl.value = result.nextPageUrl
                            currentCuratedPage = 1 // Reset page number on initial load
                            Logger.d("HomeScreenViewModel", "Initial photos loaded. State: Content. Next page URL: ${result.nextPageUrl}")
                        } else {
                            // Set Empty state
                            _uiState.value = HomeScreenUiState.Empty
                            _nextPageUrl.value = null // No next page if empty
                            currentCuratedPage = 1 // Still page 1 attempt
                            Logger.d("HomeScreenViewModel", "Initial photos loaded. State: Empty.")
                        }
                    }
                    is PhotosResult.Error -> {
                        val errorMsg = "Failed to load trending photos. ${result.message ?: "Please try again."}"
                        Logger.e("HomeScreenViewModel", "Error fetching initial photos: Message=${result.message}")
                        // Set Error state
                        _uiState.value = HomeScreenUiState.Error(
                            UserFacingError(message = errorMsg, isRetryable = true)
                        )
                        _nextPageUrl.value = null
                    }
                }
            } catch (e: CancellationException) {
              throw e // Re-throw cancellation exceptions
            } catch (e: Exception) {
                val errorMsg = "An unexpected error occurred while loading trending photos."
                Logger.e("HomeScreenViewModel", "Unexpected error fetching initial photos: ${e.message}", e)
                 // Set Error state for unexpected errors
                 _uiState.value = HomeScreenUiState.Error(
                    UserFacingError(message = errorMsg, isRetryable = true)
                 )
                _nextPageUrl.value = null
            }
            // No need for finally block to set isLoadingInitial to false
        }
    }

    fun loadMorePhotos() {
        val currentUrl = _nextPageUrl.value
        // Check if the main state is Content before allowing pagination
        val currentState = _uiState.value
        if (currentState !is HomeScreenUiState.Content || _isLoadingNextPage.value || currentUrl == null || _isRefreshingManual.value) {
            Logger.d("HomeScreenViewModel", "Skipping loadMorePhotos: State=$currentState, isLoadingNextPage=$_isLoadingNextPage, nextPageUrl=$currentUrl, isRefreshingManual=$_isRefreshingManual")
            return
        }

        viewModelScope.launch {
            _isLoadingNextPage.value = true
            _paginationError.value = null // Clear previous pagination error

            try {
                Logger.d("HomeScreenViewModel", "Loading more photos from URL: $currentUrl")
                when (val result = imageRepository.getPhotosFromUrl(url = currentUrl)) {
                    is PhotosResult.Success -> {
                        // Use photos from the current Content state
                        val currentPhotos = currentState.photos
                        val newPhotos = result.photos

                        // Check for duplicate photos (optional but good practice)
                        val existingPhotoIds = currentPhotos.map { it.id }.toSet()
                        val uniqueNewPhotos = newPhotos.filter { !existingPhotoIds.contains(it.id) }

                         // Update the Content state with the combined list
                        if (uniqueNewPhotos.isNotEmpty()) {
                            _uiState.value = HomeScreenUiState.Content(currentPhotos + uniqueNewPhotos)
                        } else if (newPhotos.isNotEmpty()) { // All new photos were duplicates
                            Logger.d("HomeScreenViewModel", "All photos from new page already exist in the list. IDs: ${newPhotos.map { it.id }}")
                            // State remains Content with old photos
                        } else {
                            // New page was empty, state remains Content with old photos
                            Logger.d("HomeScreenViewModel", "Loaded empty page, no new photos added.")
                        }

                        _nextPageUrl.value = result.nextPageUrl
                        if (result.nextPageUrl != null) {
                            currentCuratedPage++ // Increment page number only on successful load with a next page
                        }
                        Logger.d("HomeScreenViewModel", "More photos loaded successfully. Unique photos added: ${uniqueNewPhotos.size}. Next page URL: ${result.nextPageUrl}")
                    }
                    is PhotosResult.Error -> {
                        Logger.e("HomeScreenViewModel", "Error loading more photos from URL $currentUrl: Message=${result.message}")
                        _paginationError.value = result.message
                    }
                }
            } catch (e: CancellationException) {
              throw e // Re-throw cancellation exceptions
            } catch (e: Exception) {
                Logger.e("HomeScreenViewModel", "Unexpected error loading more photos from URL $currentUrl: ${e.message}", e)
                _paginationError.value = "An unexpected error occurred while loading more photos."
            } finally {
                _isLoadingNextPage.value = false
            }
        }
    }

    // Story 10.4: User-initiated manual refresh
    fun onManualRefreshTriggered() {
        // Check if already refreshing or in initial loading state
        if (_isRefreshingManual.value || _uiState.value == HomeScreenUiState.InitialLoading || _isLoadingNextPage.value) {
            Logger.d("HomeScreenViewModel", "Skipping manual refresh: Already loading/refreshing.")
            return
        }

        viewModelScope.launch {
            _isRefreshingManual.value = true
            _paginationError.value = null // Clear pagination error on manual refresh
            // Set UI state back to loading to indicate refresh
            _uiState.value = HomeScreenUiState.InitialLoading

            try {
                Logger.d("HomeScreenViewModel", "Manual refresh triggered. Fetching page 1.")
                 // AC2: Re-fetch page 1 of curated photos
                when (val result = imageRepository.getCuratedPhotos(page = 1, perPage = ITEMS_PER_PAGE)) {
                    is PhotosResult.Success -> {
                        val refreshedPhotos = result.photos
                        if (refreshedPhotos.isNotEmpty()) {
                             // AC4: Replace list, reset pagination -> Set Content state
                            _uiState.value = HomeScreenUiState.Content(refreshedPhotos)
                            _nextPageUrl.value = result.nextPageUrl
                            currentCuratedPage = 1 // Reset page number
                             Logger.d("HomeScreenViewModel", "Manual refresh successful. State: Content. Next page URL: ${result.nextPageUrl}")
                        } else {
                            // Set Empty state if refresh returns no photos
                             _uiState.value = HomeScreenUiState.Empty
                             _nextPageUrl.value = null
                             currentCuratedPage = 1
                             Logger.d("HomeScreenViewModel", "Manual refresh successful. State: Empty.")
                        }
                        // AC5: Emit success Snackbar message
                        _snackbarEvent.emit("Trending photos updated.")
                    }
                    is PhotosResult.Error -> {
                        val errorMsg = "Failed to refresh. ${result.message ?: "Please try again."}"
                        Logger.e("HomeScreenViewModel", "Error during manual refresh: Message=${result.message}")
                        // Set Error state on refresh failure
                         _uiState.value = HomeScreenUiState.Error(
                            UserFacingError(message = errorMsg, isRetryable = true)
                        )
                         _nextPageUrl.value = null
                    }
                }
            } catch (e: CancellationException) {
              throw e // Re-throw cancellation exceptions
            } catch (e: Exception) {
                val errorMsg = "Failed to refresh. An unexpected error occurred."
                Logger.e("HomeScreenViewModel", "Unexpected error during manual refresh: ${e.message}", e)
                 // Set Error state on unexpected refresh failure
                 _uiState.value = HomeScreenUiState.Error(
                    UserFacingError(message = errorMsg, isRetryable = true)
                )
                 _nextPageUrl.value = null
            } finally {
                // AC3: Ensure state is reset
                _isRefreshingManual.value = false
            }
        }
    }

    fun getPhotoById(id: Int): com.nguyenmoclam.pexelssample.domain.model.Photo? {
        return (uiState.value as? HomeScreenUiState.Content)?.photos?.find { it.id == id }
    }
} 