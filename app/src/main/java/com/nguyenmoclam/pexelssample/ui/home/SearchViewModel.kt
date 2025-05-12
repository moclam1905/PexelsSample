package com.nguyenmoclam.pexelssample.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nguyenmoclam.pexelssample.data.mappers.toDomain
import com.nguyenmoclam.pexelssample.data.remote.PexelsApiService
import com.nguyenmoclam.pexelssample.domain.model.Photo
import com.nguyenmoclam.pexelssample.domain.repository.SearchHistoryRepository
import com.nguyenmoclam.pexelssample.data.local.datastore.SearchHistoryKeys
import com.nguyenmoclam.pexelssample.logger.Logger
import com.nguyenmoclam.pexelssample.ui.model.UserFacingError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val pexelsApiService: PexelsApiService,
    private val searchHistoryRepository: SearchHistoryRepository,
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

    // Story 8.5: State for recent searches UI
    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    private val _showRecentSearchesSuggestions = MutableStateFlow(false)
    val showRecentSearchesSuggestions: StateFlow<Boolean> = _showRecentSearchesSuggestions.asStateFlow()

    private val _isSearchBarFocused = MutableStateFlow(false)
    // End Story 8.5

    // Story 8.6: Interaction with Search History Items
    fun onHistoryItemClicked(term: String) {
        Logger.d("SearchViewModel", "History item clicked: '$term'")
        _searchQuery.value = term
        savedStateHandle[KEY_SEARCH_QUERY] = term // Keep saved state in sync
        _showRecentSearchesSuggestions.value = false // Hide suggestions after click

        // Reset pagination and flags for a new search based on history item
        _photos.value = emptyList()
        currentPage = 1
        totalResults = 0
        _canLoadMore.value = false
        searchAttempted = false // Mark as a new search attempt
        savedStateHandle[KEY_SEARCH_ATTEMPTED] = false
        _errorState.value = null // Clear previous errors

        performSearchInternal() // Execute the search
    }

    fun deleteHistoryItem(term: String) {
        viewModelScope.launch {
            Logger.d("SearchViewModel", "Deleting history item: '$term'")
            searchHistoryRepository.deleteSearchTerm(term)
            // The recentSearches Flow should automatically update if collected appropriately in the init block
            // or wherever it's being observed. The combine logic in init should re-fetch.
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            Logger.d("SearchViewModel", "Clearing all search history.")
            searchHistoryRepository.clearSearchHistory()
            // Similar to delete, the Flow should update.
            // _showRecentSearchesSuggestions will be set to false by the combine logic
            // if recentSearches becomes empty.
        }
    }
    // End Story 8.6

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

        // Story 8.5: Combine searchQuery and isSearchBarFocused to manage recent searches visibility
        viewModelScope.launch {
            combine(_searchQuery, _isSearchBarFocused) { query, isFocused ->
                Pair(query, isFocused)
            }.collectLatest { (query, isFocused) ->
                if (isFocused && query.isBlank()) {
                    Logger.d("SearchViewModel", "Search bar focused and query empty, fetching recent searches.")
                    searchHistoryRepository.getRecentSearches(SearchHistoryKeys.MAX_HISTORY_SIZE)
                        .collectLatest { history ->
                            _recentSearches.value = history
                            _showRecentSearchesSuggestions.value = history.isNotEmpty()
                            if (history.isNotEmpty()) {
                                Logger.d("SearchViewModel", "Recent searches loaded: ${history.size} items.")
                            } else {
                                Logger.d("SearchViewModel", "No recent searches found.")
                            }
                        }
                } else {
                    if (_showRecentSearchesSuggestions.value) {
                        Logger.d("SearchViewModel", "Hiding recent searches. Focused: $isFocused, Query: \'$query\'")
                    }
                    _showRecentSearchesSuggestions.value = false
                    // Optionally clear recent searches when not shown to free up memory,
                    // but they might be useful if focus comes back quickly.
                    // For now, let's keep them until a new fetch overwrites.
                    // _recentSearches.value = emptyList()
                }
            }
        }
        // End Story 8.5
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
            // Story 8.5: Logic for recent searches is handled by the combine operator.
            // No direct call to hide suggestions here is needed as combine will react.
        }
    }

    // Story 8.5: New method to handle search bar focus changes
    fun onSearchBarFocusChanged(isFocused: Boolean) {
        if (_isSearchBarFocused.value != isFocused) {
            Logger.d("SearchViewModel", "Search bar focus changed to: $isFocused")
            _isSearchBarFocused.value = isFocused
            // Logic for recent searches is handled by the combine operator.
        }
    }
    // End Story 8.5

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
                handleNetworkError(e)
            } catch (e: Exception) {
                handleGenericError(e)
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
                    handleSearchError(response.code(), response.message(), response.errorBody()?.string(), operation = "pagination")
                }
            } catch (e: java.io.IOException) {
                handleNetworkError(e)
            } catch (e: Exception) {
                handleGenericError(e)
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
                handleSearchError(response.code(), response.message(), response.errorBody()?.string(), operation = "restoration")
                success = false
            }
        } catch (e: java.io.IOException) {
            Logger.e("SearchViewModel", "Network error during page $pageToLoad restoration: ${e.message}", e)
            handleNetworkError(e, "page $pageToLoad restoration")
            success = false
        } catch (e: Exception) {
            Logger.e("SearchViewModel", "Other error during page $pageToLoad restoration: ${e.message}", e)
            handleGenericError(e, "page $pageToLoad restoration")
            success = false
        } finally {
            _isLoadingMore.value = false
        }
        return success
    }

    private fun handleSearchError(code: Int, message: String?, errorBody: String?, operation: String = "search") {
        Logger.e("SearchViewModel", "API Error: $code - $message. Body: $errorBody")
        _errorState.value = when (code) {
            401, 403 -> UserFacingError(message = "Authentication error. Please check configuration.", isRetryable = false)
            429 -> UserFacingError(message = "Too many requests. Please try again in an hour.", isRetryable = false)
            in 500..599 -> UserFacingError(message = "Pexels.com seems to be unavailable. Please try again later.", isRetryable = true)
            in 400..499 -> UserFacingError(message = "Invalid request (Error $code). Please try modifying your search.", isRetryable = true)
            else -> UserFacingError(message = "An unknown error occurred (Error $code). Please try again.", isRetryable = true)
        }
        if (operation.contains("pagination") || operation.contains("restoration")) {
            _canLoadMore.value = false
            savedStateHandle[KEY_CAN_LOAD_MORE] = false
        } else {
            _photos.value = emptyList()
            savedStateHandle.remove<List<Int>>(KEY_PHOTOS_LIST_IDS)
            _navigateToResults.value = true
        }
        if (operation.contains("restoration")) _isLoading.value = false
        lastAction = null
    }

    private fun handleNetworkError(e: java.io.IOException, operation: String = "operation") {
        Logger.e("SearchViewModel", "$operation failed due to network error", e)
        _errorState.value = UserFacingError(message = "No internet connection. Please check your connection and try again.", isRetryable = true)
        if (operation.contains("pagination") || operation.contains("restoration")) {
            _canLoadMore.value = false
            savedStateHandle[KEY_CAN_LOAD_MORE] = false
        } else {
            _photos.value = emptyList()
            savedStateHandle.remove<List<Int>>(KEY_PHOTOS_LIST_IDS)
            _navigateToResults.value = true
        }
        if (operation.contains("restoration")) _isLoading.value = false
        lastAction = null
    }

    private fun handleGenericError(e: Exception, operation: String = "operation") {
        Logger.e("SearchViewModel", "$operation failed due to an unexpected error", e)
        _errorState.value = UserFacingError(message = "An unexpected error occurred: ${e.localizedMessage ?: "Unknown"}", isRetryable = true)
        if (operation.contains("pagination") || operation.contains("restoration")) {
            _canLoadMore.value = false
            savedStateHandle[KEY_CAN_LOAD_MORE] = false
        } else {
            _photos.value = emptyList()
            savedStateHandle.remove<List<Int>>(KEY_PHOTOS_LIST_IDS)
            _navigateToResults.value = true
        }
        if (operation.contains("restoration")) _isLoading.value = false
        lastAction = null
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

    fun clearErrorState() {
        _errorState.value = null
        Logger.d("SearchViewModel", "Error state cleared by UI.")
    }

    // Story 8.1: Pull-to-Refresh
    fun onRefreshTriggered() {
        if (_isRefreshing.value) {
            Logger.d("SearchViewModel", "Refresh already in progress, ignoring trigger.")
            return
        }

        _isRefreshing.value = true
        _errorState.value = null
        // lastAction = { onRefreshTriggered() } // Reconsider if retry for refresh is needed and how

        Logger.d("SearchViewModel", "Refresh triggered. Query: '${_searchQuery.value}'")

        viewModelScope.launch {
            try {
                if (_searchQuery.value.isNotBlank()) {
                    val query = _searchQuery.value
                    Logger.d("SearchViewModel", "Refreshing search for query: '$query'")
                    val response = pexelsApiService.searchPhotos(
                        query = query,
                        page = 1, // Always fetch page 1 for refresh
                        perPage = ITEMS_PER_PAGE
                    )

                    if (response.isSuccessful && response.body() != null) {
                        val responseBody = response.body()!!
                        val mappedPhotos = responseBody.photos.map { it.toDomain() }
                        _photos.value = mappedPhotos
                        currentPage = 1 // Reset to page 1
                        totalResults = responseBody.totalResults
                        _canLoadMore.value = responseBody.nextPage != null
                        _errorState.value = null // Clear error on success

                        // Update saved state for consistency, though not strictly required by refresh story
                        savedStateHandle[KEY_CURRENT_PAGE] = currentPage
                        savedStateHandle[KEY_TOTAL_RESULTS] = totalResults
                        savedStateHandle[KEY_CAN_LOAD_MORE] = _canLoadMore.value
                        savedStateHandle[KEY_PHOTOS_LIST_IDS] = _photos.value.map { it.id }

                        Logger.d("SearchViewModel", "Refresh search success: ${mappedPhotos.size} photos. Total: $totalResults.")
                    } else {
                        handleSearchError(response.code(), response.message(), response.errorBody()?.string(), "refresh search")
                    }
                } else {
                    // Assuming curated photos refresh is desired if no search query
                    // This part aligns with AC2, but Pexels API might not have a direct "curated photos for page 1"
                    // without a specific endpoint or if it's the same as their "popular" or default.
                    // For now, let's assume pexelsApiService.getCuratedPhotos exists.
                    // If not, this branch might need adjustment or removal based on actual PexelsApiService.
                    Logger.d("SearchViewModel", "Refreshing curated photos.")
                    // Call updated to getCuratedPhotosTest and removed page parameter
                    val response = pexelsApiService.getCuratedPhotosTest(
                        perPage = ITEMS_PER_PAGE
                    )

                    if (response.isSuccessful && response.body() != null) {
                        val responseBody = response.body()!! // PexelsSearchResponseDto
                        val mappedPhotos = responseBody.photos.map { it.toDomain() }
                        _photos.value = mappedPhotos
                        currentPage = 1 // Reset to page 1 conceptually, even if API doesn't take page for curated
                        totalResults = responseBody.totalResults
                        _canLoadMore.value = responseBody.nextPage != null
                        _errorState.value = null

                        savedStateHandle[KEY_CURRENT_PAGE] = currentPage
                        savedStateHandle[KEY_TOTAL_RESULTS] = totalResults
                        savedStateHandle[KEY_CAN_LOAD_MORE] = _canLoadMore.value
                        savedStateHandle[KEY_PHOTOS_LIST_IDS] = _photos.value.map { it.id }
                        Logger.d("SearchViewModel", "Refresh curated success: ${mappedPhotos.size} photos. Total: $totalResults.")
                    } else {
                         // Use a more specific error handler if available, or generalize handleSearchError
                        handleSearchError(response.code(), response.message(), response.errorBody()?.string(), "refresh curated")
                    }
                }
            } catch (e: java.io.IOException) {
                handleNetworkError(e)
            } catch (e: Exception) {
                handleGenericError(e)
            } finally {
                _isRefreshing.value = false
                // updateIsResultsEmptyAndNavigateFlag might be relevant here too
                // For refresh, navigation is not the primary concern, but emptiness is.
                _isResultsEmpty.value = _photos.value.isEmpty() && searchAttempted // searchAttempted might need re-evaluation here
                Logger.d("SearchViewModel", "Refresh finished. isRefreshing set to false.")
            }
        }
    }
    // End Story 8.1
}