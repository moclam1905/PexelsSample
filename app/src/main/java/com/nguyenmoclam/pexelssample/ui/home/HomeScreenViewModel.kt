package com.nguyenmoclam.pexelssample.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nguyenmoclam.pexelssample.domain.model.Photo
import com.nguyenmoclam.pexelssample.domain.model.PhotosResult
import com.nguyenmoclam.pexelssample.domain.repository.ImageRepository
import com.nguyenmoclam.pexelssample.logger.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.CancellationException

// Define items per page here or retrieve from a config if needed
private const val ITEMS_PER_PAGE = 30 // Default: 15 Max: 80

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val imageRepository: ImageRepository
) : ViewModel() {

    private val _photos = MutableStateFlow<List<Photo>>(emptyList())
    val photos: StateFlow<List<Photo>> = _photos.asStateFlow()

    private val _isLoadingInitial = MutableStateFlow(false)
    val isLoadingInitial: StateFlow<Boolean> = _isLoadingInitial.asStateFlow()

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

    private fun fetchInitialPhotos() {
        if (_isLoadingInitial.value) return // Prevent concurrent initial fetches

        viewModelScope.launch {
            _isLoadingInitial.value = true
            _paginationError.value = null // Clear pagination error on initial load/refresh
            try {
                // Page 1 for initial load
                when (val result = imageRepository.getCuratedPhotos(page = 1, perPage = ITEMS_PER_PAGE)) {
                    is PhotosResult.Success -> {
                        _photos.value = result.photos
                        _nextPageUrl.value = result.nextPageUrl
                        currentCuratedPage = 1 // Reset page number on initial load
                        Logger.d("HomeScreenViewModel", "Initial photos loaded. Next page URL: ${result.nextPageUrl}")
                    }
                    is PhotosResult.Error -> {
                        Logger.e("HomeScreenViewModel", "Error fetching initial photos: Message=${result.message}")
                        // Consider setting a specific error state for initial load if needed
                        _photos.value = emptyList()
                        _nextPageUrl.value = null
                    }
                }
            } catch (e: Exception) {
                // Catch any unexpected exceptions from the repository layer upwards
                Logger.e("HomeScreenViewModel", "Unexpected error fetching initial photos: ${e.message}", e)
                // Consider setting a specific error state for initial load if needed
                _photos.value = emptyList()
                _nextPageUrl.value = null
            } finally {
                _isLoadingInitial.value = false
            }
        }
    }

    fun loadMorePhotos() {
        val currentUrl = _nextPageUrl.value
        if (_isLoadingInitial.value || _isLoadingNextPage.value || currentUrl == null) {
            Logger.d("HomeScreenViewModel", "Skipping loadMorePhotos: isLoadingInitial=${_isLoadingInitial.value}, isLoadingNextPage=${_isLoadingNextPage.value}, nextPageUrl=$currentUrl")
            return
        }

        viewModelScope.launch {
            _isLoadingNextPage.value = true
            _paginationError.value = null // Clear previous pagination error

            try {
                _isLoadingNextPage.value = true
                _paginationError.value = null

                Logger.d("HomeScreenViewModel", "Loading more photos from URL: $currentUrl")
                when (val result = imageRepository.getPhotosFromUrl(url = currentUrl)) {
                    is PhotosResult.Success -> {
                        val currentPhotos = _photos.value
                        val newPhotos = result.photos

                        // Check for duplicate photos
                        val existingPhotoIds = currentPhotos.map { it.id }.toSet()

                        // Filter out duplicate photos
                        val uniqueNewPhotos = newPhotos.filter { !existingPhotoIds.contains(it.id) }

                        // Update the list
                        if (uniqueNewPhotos.isNotEmpty() || newPhotos.isEmpty() && currentPhotos.isEmpty()) {
                            _photos.value = currentPhotos + uniqueNewPhotos
                        } else if (newPhotos.isNotEmpty() && uniqueNewPhotos.isEmpty()){
                            Logger.d("HomeScreenViewModel", "All photos from new page already exist in the list. IDs: ${newPhotos.map { it.id }}")
                        }

                        _nextPageUrl.value = result.nextPageUrl
                        if (result.nextPageUrl != null) {
                            currentCuratedPage++ // Increment page number only on successful load with a next page
                        }
                        Logger.d("HomeScreenViewModel", "More photos loaded successfully. Photos added: ${newPhotos.size}. Next page URL: ${result.nextPageUrl}")
                    }
                    is PhotosResult.Error -> {
                        Logger.e("HomeScreenViewModel", "Error loading more photos from URL $currentUrl: Message=${result.message}")
                        _paginationError.value = result.message
                    }
                }
            } catch (e: Exception) {
                Logger.e("HomeScreenViewModel", "Unexpected error loading more photos from URL $currentUrl: ${e.message}", e)
                _paginationError.value = "An unexpected error occurred."

            } finally {
                _isLoadingNextPage.value = false
            }
        }
    }

    // Story 10.4: User-initiated manual refresh
    fun onManualRefreshTriggered() {
        // AC6: Prevent concurrent refresh/load operations
        if (_isLoadingInitial.value || _isRefreshingManual.value || _isLoadingNextPage.value) {
            Logger.d("HomeScreenViewModel", "Skipping manual refresh: Already loading/refreshing.")
            return
        }

        viewModelScope.launch {
            _isRefreshingManual.value = true
            _paginationError.value = null // Clear pagination error on manual refresh
            // Optionally clear main error state if applicable: _errorState.value = null

            try {
                Logger.d("HomeScreenViewModel", "Manual refresh triggered. Fetching page 1.")
                // AC2: Re-fetch page 1 of curated photos
                when (val result = imageRepository.getCuratedPhotos(page = 1, perPage = ITEMS_PER_PAGE)) {
                    is PhotosResult.Success -> {
                        // AC4: Replace list, reset pagination
                        _photos.value = result.photos
                        _nextPageUrl.value = result.nextPageUrl
                        currentCuratedPage = 1 // Reset page number
                        _paginationError.value = null // Clear pagination error on success
                        Logger.d("HomeScreenViewModel", "Manual refresh successful. Next page URL: ${result.nextPageUrl}")
                        // AC5: Emit success Snackbar message
                        _snackbarEvent.emit("Trending photos updated.")
                    }
                    is PhotosResult.Error -> {
                        Logger.e("HomeScreenViewModel", "Error during manual refresh: Message=${result.message}")
                        // AC5: Emit failure Snackbar message
                        _snackbarEvent.emit("Failed to refresh. ${result.message ?: "Please try again."}")
                        // Optionally set main error state if needed, e.g., if photos list becomes empty
                        // if (_photos.value.isEmpty()) { _errorState.value = result.message }
                    }
                }
            } catch (e: CancellationException) {
              throw e // Re-throw cancellation exceptions
            } catch (e: Exception) {
                Logger.e("HomeScreenViewModel", "Unexpected error during manual refresh: ${e.message}", e)
                // AC5: Emit failure Snackbar message for unexpected errors
                _snackbarEvent.emit("Failed to refresh. An unexpected error occurred.")
                // Optionally set main error state
                // if (_photos.value.isEmpty()) { _errorState.value = "An unexpected error occurred." }
            } finally {
                // AC3: Ensure state is reset
                _isRefreshingManual.value = false
            }
        }
    }

    // Consider adding a refresh function later if needed
} 