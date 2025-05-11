package com.nguyenmoclam.pexelssample.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nguyenmoclam.pexelssample.data.mappers.toDomain
import com.nguyenmoclam.pexelssample.data.remote.PexelsApiService
import com.nguyenmoclam.pexelssample.domain.model.Photo
import com.nguyenmoclam.pexelssample.logger.Logger
import com.nguyenmoclam.pexelssample.ui.model.UserFacingError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val pexelsApiService: PexelsApiService,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val KEY_SEARCH_QUERY = "search_query"
        private const val KEY_CURRENT_PAGE = "current_page"
        private const val KEY_TOTAL_RESULTS = "total_results"
        private const val KEY_CAN_LOAD_MORE = "can_load_more"
        private const val KEY_SELECTED_PHOTO_ID = "selected_photo_id"
        private const val KEY_SEARCH_ATTEMPTED = "search_attempted"
        private const val KEY_PHOTOS_LIST_IDS = "photos_list_ids" // To store IDs of photos currently in list
    }

    private val ITEMS_PER_PAGE = 20

    private val _searchQuery = MutableStateFlow(savedStateHandle.get<String>(KEY_SEARCH_QUERY) ?: "")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    // Story 8.1: Pull-to-Refresh
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    // End Story 8.1

    private val _photos = MutableStateFlow<List<Photo>>(emptyList())
    val photos: StateFlow<List<Photo>> = _photos.asStateFlow()

    private val _navigateToResults = MutableStateFlow(false)
    val navigateToResults: StateFlow<Boolean> = _navigateToResults.asStateFlow()

    private var currentPage = savedStateHandle.get<Int>(KEY_CURRENT_PAGE) ?: 1
    private var totalResults = savedStateHandle.get<Int>(KEY_TOTAL_RESULTS) ?: 0
    private val _canLoadMore = MutableStateFlow(savedStateHandle.get<Boolean>(KEY_CAN_LOAD_MORE) ?: false)
    val canLoadMore: StateFlow<Boolean> = _canLoadMore.asStateFlow()

    private val _isResultsEmpty = MutableStateFlow(false)
    val isResultsEmpty: StateFlow<Boolean> = _isResultsEmpty.asStateFlow()

    private val _errorState = MutableStateFlow<UserFacingError?>(null)
    val errorState: StateFlow<UserFacingError?> = _errorState.asStateFlow()

    private var searchAttempted = savedStateHandle.get<Boolean>(KEY_SEARCH_ATTEMPTED) ?: false
    private var lastAction: (() -> Unit)? = null // Added for retry mechanism

    // --- Additions for Story 7.3: Adaptive Layout ---
    private val _selectedPhotoForDetail = MutableStateFlow<Photo?>(null)
    val selectedPhotoForDetail: StateFlow<Photo?> = _selectedPhotoForDetail.asStateFlow()

    init {
        val restoredQuery = savedStateHandle.get<String>(KEY_SEARCH_QUERY)
        val restoredCurrentPage = savedStateHandle.get<Int>(KEY_CURRENT_PAGE)
        val restoredSearchAttempted = savedStateHandle.get<Boolean>(KEY_SEARCH_ATTEMPTED)
        val restoredPhotoIds = savedStateHandle.get<List<Int>>(KEY_PHOTOS_LIST_IDS)
        val restoredSelectedPhotoId = savedStateHandle.get<Int>(KEY_SELECTED_PHOTO_ID)

        if (restoredQuery != null && restoredSearchAttempted == true) {
            _searchQuery.value = restoredQuery
            // If photos were previously loaded, we need to restore them.
            // For simplicity, if ViewModel is recreated after process death,
            // we re-fetch up to the restored page.
            // A more complex solution might involve caching photos in a repository.
            if (restoredCurrentPage != null && restoredCurrentPage > 0 && (restoredPhotoIds == null || restoredPhotoIds.isEmpty())) {
                Logger.d("SearchViewModel", "Restoring search state for query: \'$restoredQuery\', up to page: $restoredCurrentPage")
                // Set up initial state for restoration
                this.currentPage = 0 // Will be incremented by performLoadNextPageInternal
                this.totalResults = savedStateHandle.get<Int>(KEY_TOTAL_RESULTS) ?: 0
                this._canLoadMore.value = savedStateHandle.get<Boolean>(KEY_CAN_LOAD_MORE) ?: false // Initial guess
                this.searchAttempted = true
                _photos.value = emptyList()
                _isLoading.value = true // Show loading indicator during restoration
                // Sequentially load pages up to the restored page
                viewModelScope.launch {
                    var success = true
                    for (pageToLoad in 1..restoredCurrentPage) {
                        if (!performRestorePageInternal(restoredQuery, pageToLoad)) {
                            success = false
                            break
                        }
                    }
                    if (success && restoredSelectedPhotoId != null) {
                        _selectedPhotoForDetail.value = _photos.value.find { it.id == restoredSelectedPhotoId }
                    }
                    _isLoading.value = false
                    updateIsResultsEmptyAndNavigateFlag(restoredQuery) // Update flags after restoration
                }
            } else if (restoredPhotoIds != null && restoredPhotoIds.isNotEmpty()){
                // This part is tricky without a local cache/DB.
                // Assuming if photo IDs are saved, we might need to re-fetch them if not in memory.
                // For now, rely on ViewModel retaining _photos.value if not process death.
                // If process death, the above block handles re-fetching query.
                // If we had a way to get photos by IDs directly from service, we could use 'restoredPhotoIds'
                Logger.d("SearchViewModel", "Photo IDs were saved, but full restoration from IDs is complex without cache.")
                // Attempt to restore selected photo if photos list is already populated (e.g. not process death)
                if (restoredSelectedPhotoId != null && _photos.value.isNotEmpty()) {
                     _selectedPhotoForDetail.value = _photos.value.find { it.id == restoredSelectedPhotoId }
                } else if (restoredSelectedPhotoId != null) {
                    // If photos are empty but we have a selected ID, it means we should have re-fetched.
                    // The block above should handle this.
                }
            } else if (restoredSelectedPhotoId != null) { // Query might be empty but a photo was selected (e.g. from a different source if app evolves)
                 // This case is unlikely with current flow but included for robustness if selectedPhotoId is saved without a query.
                 // Attempt to find it if photos list is somehow populated.
                 _selectedPhotoForDetail.value = _photos.value.find { it.id == restoredSelectedPhotoId }
            }
        }
         // If there was a search query from savedStateHandle, set it.
        // _searchQuery is already initialized from savedStateHandle
    }

    fun onPhotoSelected(photo: Photo) {
        _selectedPhotoForDetail.value = photo
        savedStateHandle[KEY_SELECTED_PHOTO_ID] = photo.id
        Logger.d("SearchViewModel", "Photo selected for detail: ID ${photo.id}, saved to state.")
    }

    fun clearDetailSelection() {
        _selectedPhotoForDetail.value = null
        savedStateHandle.remove<Int>(KEY_SELECTED_PHOTO_ID)
        Logger.d("SearchViewModel", "Detail selection cleared, removed from state.")
    }
    // --- End Additions for Story 7.3 ---

    fun onQueryChanged(newQuery: String) {
        if (_searchQuery.value != newQuery) {
            _searchQuery.value = newQuery
            savedStateHandle[KEY_SEARCH_QUERY] = newQuery
            // Reset dependent states when query changes
            searchAttempted = false
            savedStateHandle[KEY_SEARCH_ATTEMPTED] = false
            _photos.value = emptyList()
            savedStateHandle.remove<List<Int>>(KEY_PHOTOS_LIST_IDS) // Clear saved photo IDs
            currentPage = 1
            savedStateHandle[KEY_CURRENT_PAGE] = currentPage
            totalResults = 0
            savedStateHandle[KEY_TOTAL_RESULTS] = totalResults
            _canLoadMore.value = false
            savedStateHandle[KEY_CAN_LOAD_MORE] = _canLoadMore.value
            _isResultsEmpty.value = false
            _errorState.value = null
            _selectedPhotoForDetail.value = null // Clear selection when query changes
            savedStateHandle.remove<Int>(KEY_SELECTED_PHOTO_ID)
            Logger.d("SearchViewModel", "Query changed to: \'$newQuery\', state updated and saved.")
        }
    }

    fun onSearchClicked() {
        if (_searchQuery.value.isNotBlank()) {
            Logger.d("SearchViewModel", "Search initiated for: ${_searchQuery.value}")
            lastAction = { performSearchInternal() }
            performSearchInternal()
        } else {
            Logger.d("SearchViewModel", "Search query is empty.")
        }
    }

    private fun performSearchInternal() {
        _errorState.value = null
        currentPage = 1
        _photos.value = emptyList()
        totalResults = 0
        _canLoadMore.value = false
        _navigateToResults.value = false // Will be set true on success
        _isLoadingMore.value = false
        searchAttempted = true
        _isResultsEmpty.value = false

        savedStateHandle[KEY_CURRENT_PAGE] = currentPage
        savedStateHandle[KEY_SEARCH_ATTEMPTED] = searchAttempted
        savedStateHandle.remove<List<Int>>(KEY_PHOTOS_LIST_IDS) // Clear as it's a new search

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = pexelsApiService.searchPhotos(
                    query = _searchQuery.value,
                    page = currentPage,
                    perPage = ITEMS_PER_PAGE
                )
                if (response.isSuccessful && response.body() != null) {
                    val responseBody = response.body()!!
                    val mappedPhotos = responseBody.photos.map { it.toDomain() }
                    _photos.value = mappedPhotos
                    totalResults = responseBody.totalResults
                    _canLoadMore.value = responseBody.nextPage != null
                    _errorState.value = null
                    _navigateToResults.value = true

                    savedStateHandle[KEY_TOTAL_RESULTS] = totalResults
                    savedStateHandle[KEY_CAN_LOAD_MORE] = _canLoadMore.value
                    savedStateHandle[KEY_PHOTOS_LIST_IDS] = _photos.value.map { it.id }
                    Logger.d("SearchViewModel", "API Success: ${_photos.value.size} photos. Total: $totalResults. Saved state.")

                } else {
                    handleSearchError(response.code(), response.message(), response.errorBody()?.string())
                }
            } catch (e: java.io.IOException) {
                handleNetworkError("search", e)
            } catch (e: Exception) {
                handleGenericError("search", e)
            } finally {
                _isLoading.value = false
                updateIsResultsEmptyAndNavigateFlag(_searchQuery.value)
            }
        }
    }

    fun loadNextPage() {
        if (_isLoading.value || _isLoadingMore.value || !_canLoadMore.value) {
            Logger.d("SearchViewModel", "loadNextPage: Condition not met.")
            return
        }
        lastAction = { performLoadNextPageInternal() }
        performLoadNextPageInternal()
    }

    private fun performLoadNextPageInternal() {
        _errorState.value = null
        _isLoadingMore.value = true
        Logger.d("SearchViewModel", "loadNextPage: Loading page ${currentPage + 1} for query \'${_searchQuery.value}\'")
        viewModelScope.launch {
            val pageToLoad = currentPage + 1
            try {
                val response = pexelsApiService.searchPhotos(
                    query = _searchQuery.value,
                    page = pageToLoad,
                    perPage = ITEMS_PER_PAGE
                )
                if (response.isSuccessful && response.body() != null) {
                    val responseBody = response.body()!!
                    val mappedNewPhotos = responseBody.photos.map { it.toDomain() }
                    _photos.value = _photos.value + mappedNewPhotos
                    currentPage = pageToLoad // Update current page only on success
                    _canLoadMore.value = responseBody.nextPage != null
                    _errorState.value = null

                    savedStateHandle[KEY_CURRENT_PAGE] = currentPage
                    savedStateHandle[KEY_CAN_LOAD_MORE] = _canLoadMore.value
                    savedStateHandle[KEY_PHOTOS_LIST_IDS] = _photos.value.map { it.id }
                     Logger.d("SearchViewModel", "API Success (Page $currentPage): ${mappedNewPhotos.size} new. Total: ${_photos.value.size}. Saved state.")
                } else {
                    // Do not increment currentPage on failure
                    handleSearchError(response.code(), response.message(), response.errorBody()?.string(), isPagination = true)
                }
            } catch (e: java.io.IOException) {
                handleNetworkError("pagination (Page $pageToLoad)", e)
            } catch (e: Exception) {
                handleGenericError("pagination (Page $pageToLoad)", e)
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    // Helper function for restoring pages during init
    private suspend fun performRestorePageInternal(query: String, pageToLoad: Int): Boolean {
        var success = false
        _isLoadingMore.value = true // Use isLoadingMore for individual page loads during restore
        Logger.d("SearchViewModel", "Restoring page $pageToLoad for query \'$query\'")
        try {
            val response = pexelsApiService.searchPhotos(
                query = query,
                page = pageToLoad,
                perPage = ITEMS_PER_PAGE
            )
            if (response.isSuccessful && response.body() != null) {
                val responseBody = response.body()!!
                val mappedNewPhotos = responseBody.photos.map { it.toDomain() }
                _photos.value = _photos.value + mappedNewPhotos // Append photos
                this.currentPage = pageToLoad // Update current page after successful load
                this.totalResults = responseBody.totalResults // Update total results from the latest successful page
                this._canLoadMore.value = responseBody.nextPage != null // Update canLoadMore
                _errorState.value = null
                success = true

                // Save state after each successful page restoration
                savedStateHandle[KEY_CURRENT_PAGE] = this.currentPage
                savedStateHandle[KEY_TOTAL_RESULTS] = this.totalResults
                savedStateHandle[KEY_CAN_LOAD_MORE] = this._canLoadMore.value
                savedStateHandle[KEY_PHOTOS_LIST_IDS] = _photos.value.map { it.id }
                Logger.d("SearchViewModel", "Restored page $pageToLoad successfully. Photos: ${mappedNewPhotos.size}. Total: ${_photos.value.size}")
            } else {
                Logger.e("SearchViewModel", "API Error during page $pageToLoad restoration: ${response.code()} - ${response.message()}")
                // Set error, stop further restoration attempts for this query
                handleSearchError(response.code(), response.message(), response.errorBody()?.string(), isPagination = true, isRestoration = true)
                success = false
            }
        } catch (e: java.io.IOException) {
            Logger.e("SearchViewModel", "Network error during page $pageToLoad restoration: ${e.message}", e)
            handleNetworkError("page $pageToLoad restoration", e, isRestoration = true)
            success = false
        } catch (e: Exception) {
            Logger.e("SearchViewModel", "Other error during page $pageToLoad restoration: ${e.message}", e)
            handleGenericError("page $pageToLoad restoration", e, isRestoration = true)
            success = false
        } finally {
            _isLoadingMore.value = false
        }
        return success
    }

    private fun handleSearchError(code: Int, message: String?, errorBody: String?, isPagination: Boolean = false, isRestoration: Boolean = false) {
        Logger.e("SearchViewModel", "API Error: $code - $message. Body: $errorBody")
        _errorState.value = when (code) {
            401, 403 -> UserFacingError(message = "Authentication error. Please check configuration.", isRetryable = false)
            429 -> UserFacingError(message = "Too many requests. Please try again in an hour.", isRetryable = false)
            in 500..599 -> UserFacingError(message = "Pexels.com seems to be unavailable. Please try again later.", isRetryable = true)
            in 400..499 -> UserFacingError(message = "Invalid request (Error $code). Please try modifying your search.", isRetryable = true)
            else -> UserFacingError(message = "An unknown error occurred (Error $code). Please try again.", isRetryable = true)
        }
        if (isPagination || isRestoration) { // If pagination or restoration fails
            _canLoadMore.value = false // Stop further attempts
            savedStateHandle[KEY_CAN_LOAD_MORE] = false
        } else { // If initial search fails
            _isResultsEmpty.value = false // Not empty, but error
            _navigateToResults.value = true // Navigate to show error
        }
        if (isRestoration) _isLoading.value = false // Ensure main loading is stopped if restoration fails
    }

    private fun handleNetworkError(context: String, e: java.io.IOException, isRestoration: Boolean = false) {
        Logger.e("SearchViewModel", "Network error during $context: ${e.message}", e)
        _errorState.value = UserFacingError(message = "No internet connection. Please check your connection and try again.", isRetryable = true)
        if (context.contains("pagination") || context.contains("restoration")) {
            _canLoadMore.value = false
            savedStateHandle[KEY_CAN_LOAD_MORE] = false
        } else { // Initial search
            _isResultsEmpty.value = false
            _navigateToResults.value = true
        }
         if (isRestoration) _isLoading.value = false
    }

    private fun handleGenericError(context: String, e: Exception, isRestoration: Boolean = false) {
        Logger.e("SearchViewModel", "Other error during $context: ${e.message}", e)
        _errorState.value = UserFacingError(message = "An unexpected error occurred: ${e.localizedMessage ?: "Unknown"}", isRetryable = true)
         if (context.contains("pagination") || context.contains("restoration")) {
            _canLoadMore.value = false
            savedStateHandle[KEY_CAN_LOAD_MORE] = false
        } else { // Initial search
            _isResultsEmpty.value = false
            _navigateToResults.value = true
        }
        if (isRestoration) _isLoading.value = false
    }
    
    private fun updateIsResultsEmptyAndNavigateFlag(query: String) {
        if (_errorState.value == null) {
            _isResultsEmpty.value = _photos.value.isEmpty() && searchAttempted && query.isNotBlank()
        } else {
            _isResultsEmpty.value = false
        }
        // If a search was attempted for a non-blank query, always "navigate" to show results/empty/error
        if (searchAttempted && query.isNotBlank()) {
            _navigateToResults.value = true
        }
    }

    fun onNavigationComplete() {
        _navigateToResults.value = false
    }

    fun getPhotoById(id: Int): Photo? = _photos.value.find { it.id == id }

    fun retryLastFailedOperation() {
        _errorState.value = null 
        Logger.d("SearchViewModel", "retryLastFailedOperation: Attempting to execute last stored action.")
        lastAction?.invoke()
    }

    // Story 8.1: Pull-to-Refresh
    fun onRefreshTriggered() {
        Logger.d("SearchViewModel", "Pull-to-refresh triggered for query: ${_searchQuery.value}")
        // Actual data fetching and setting _isRefreshing to false will be in Story 8.2
        // For now, just set it to true to show the indicator and simulate some work.
        // In a real scenario, you'd call a method similar to performSearchInternal but tailored for refresh.
        _isRefreshing.value = true

        // Placeholder: Simulate refresh completion for now, as Story 8.2 handles actual logic.
        // viewModelScope.launch {
        //     kotlinx.coroutines.delay(2000) // Simulate network delay
        //     _isRefreshing.value = false
        // }
    }
    // End Story 8.1
} 