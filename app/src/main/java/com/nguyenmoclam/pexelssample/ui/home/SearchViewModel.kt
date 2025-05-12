package com.nguyenmoclam.pexelssample.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nguyenmoclam.pexelssample.data.local.datastore.SearchHistoryKeys
import com.nguyenmoclam.pexelssample.domain.model.Photo
import com.nguyenmoclam.pexelssample.domain.model.PhotosResult
import com.nguyenmoclam.pexelssample.domain.repository.ImageRepository
import com.nguyenmoclam.pexelssample.domain.repository.SearchHistoryRepository
import com.nguyenmoclam.pexelssample.logger.Logger
import com.nguyenmoclam.pexelssample.ui.model.UserFacingError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val imageRepository: ImageRepository,
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
        private const val ITEMS_PER_PAGE = 20
    }

    private val _searchQuery = MutableStateFlow(savedStateHandle.get<String>(KEY_SEARCH_QUERY) ?: "")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

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
    private var lastAction: (() -> Unit)? = null

    private val _selectedPhotoForDetail = MutableStateFlow<Photo?>(null)
    val selectedPhotoForDetail: StateFlow<Photo?> = _selectedPhotoForDetail.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    private val _showRecentSearchesSuggestions = MutableStateFlow(false)
    val showRecentSearchesSuggestions: StateFlow<Boolean> = _showRecentSearchesSuggestions.asStateFlow()

    private val _isSearchBarFocused = MutableStateFlow(false)

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
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            Logger.d("SearchViewModel", "Clearing all search history.")
            searchHistoryRepository.clearSearchHistory()
        }
    }

    init {
        val restoredQuery = savedStateHandle.get<String>(KEY_SEARCH_QUERY)
        val restoredCurrentPage = savedStateHandle.get<Int>(KEY_CURRENT_PAGE)
        val restoredSearchAttempted = savedStateHandle.get<Boolean>(KEY_SEARCH_ATTEMPTED)
        val restoredPhotoIds = savedStateHandle.get<List<Int>>(KEY_PHOTOS_LIST_IDS)
        val restoredSelectedPhotoId = savedStateHandle.get<Int>(KEY_SELECTED_PHOTO_ID)

        if (restoredQuery != null && restoredSearchAttempted == true) {
            _searchQuery.value = restoredQuery
            if (restoredCurrentPage != null && restoredCurrentPage > 0 && (restoredPhotoIds == null || restoredPhotoIds.isEmpty())) {
                Logger.d("SearchViewModel", "Restoring search state for query: \'$restoredQuery\', up to page: $restoredCurrentPage")
                this.currentPage = 0 // Will be incremented by performRestorePageInternal
                this.totalResults = savedStateHandle.get<Int>(KEY_TOTAL_RESULTS) ?: 0
                this._canLoadMore.value = savedStateHandle.get<Boolean>(KEY_CAN_LOAD_MORE) ?: false
                this.searchAttempted = true
                _photos.value = emptyList()
                _isLoading.value = true
                viewModelScope.launch {
                    var success = true
                    for (pageToLoad in 1..restoredCurrentPage) {
                        if (!performRestorePageInternal(restoredQuery, pageToLoad, ITEMS_PER_PAGE)) {
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
                 Logger.d("SearchViewModel", "Photo IDs were saved, but full restoration from IDs is complex without cache.")
                 if (restoredSelectedPhotoId != null && _photos.value.isNotEmpty()) {
                      _selectedPhotoForDetail.value = _photos.value.find { it.id == restoredSelectedPhotoId }
                 }
            } else if (restoredSelectedPhotoId != null) {
                 _selectedPhotoForDetail.value = _photos.value.find { it.id == restoredSelectedPhotoId }
            }
        }

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
                }
            }
        }
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

    fun onQueryChanged(newQuery: String) {
        if (_searchQuery.value != newQuery) {
            _searchQuery.value = newQuery
            savedStateHandle[KEY_SEARCH_QUERY] = newQuery
            searchAttempted = false
            savedStateHandle[KEY_SEARCH_ATTEMPTED] = false
            _photos.value = emptyList()
            savedStateHandle.remove<List<Int>>(KEY_PHOTOS_LIST_IDS)
            currentPage = 1
            savedStateHandle[KEY_CURRENT_PAGE] = currentPage
            totalResults = 0
            savedStateHandle[KEY_TOTAL_RESULTS] = totalResults
            _canLoadMore.value = false
            savedStateHandle[KEY_CAN_LOAD_MORE] = _canLoadMore.value
            _isResultsEmpty.value = false
            _errorState.value = null
            _selectedPhotoForDetail.value = null
            savedStateHandle.remove<Int>(KEY_SELECTED_PHOTO_ID)
            Logger.d("SearchViewModel", "Query changed to: \'$newQuery\', state updated and saved.")
        }
    }

    fun onSearchBarFocusChanged(isFocused: Boolean) {
        if (_isSearchBarFocused.value != isFocused) {
            Logger.d("SearchViewModel", "Search bar focus changed to: $isFocused")
            _isSearchBarFocused.value = isFocused
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
        _navigateToResults.value = false
        _isLoadingMore.value = false
        searchAttempted = true
        _isResultsEmpty.value = false

        savedStateHandle[KEY_CURRENT_PAGE] = currentPage
        savedStateHandle[KEY_SEARCH_ATTEMPTED] = searchAttempted
        savedStateHandle.remove<List<Int>>(KEY_PHOTOS_LIST_IDS)

        viewModelScope.launch {
            _isLoading.value = true
            val result = imageRepository.searchPhotos(
                query = _searchQuery.value,
                page = currentPage,
                perPage = ITEMS_PER_PAGE
            )

            when (result) {
                is PhotosResult.Success -> {
                    _photos.value = result.photos
                    totalResults = result.totalResults
                    _canLoadMore.value = result.canLoadMore
                    _errorState.value = null
                    _navigateToResults.value = true

                    savedStateHandle[KEY_TOTAL_RESULTS] = totalResults
                    savedStateHandle[KEY_CAN_LOAD_MORE] = _canLoadMore.value
                    savedStateHandle[KEY_PHOTOS_LIST_IDS] = _photos.value.map { it.id }
                    Logger.d("SearchViewModel", "Repo Success: ${_photos.value.size} photos. Total: $totalResults. Saved state.")

                    if (_searchQuery.value.isNotBlank()) {
                        viewModelScope.launch {
                            searchHistoryRepository.addSearchTerm(_searchQuery.value)
                            Logger.d("SearchViewModel", "Search term '${_searchQuery.value}' added to history.")
                        }
                    }
                }
                is PhotosResult.Error -> {
                    handleRepositoryError(result, "search")
                }
            }
            _isLoading.value = false
            updateIsResultsEmptyAndNavigateFlag(_searchQuery.value)
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
        val pageToLoad = currentPage + 1
        Logger.d("SearchViewModel", "loadNextPage: Loading page $pageToLoad for query \'${_searchQuery.value}\'")
        viewModelScope.launch {
            val result = imageRepository.searchPhotos(
                query = _searchQuery.value,
                page = pageToLoad,
                perPage = ITEMS_PER_PAGE
            )

            when (result) {
                is PhotosResult.Success -> {
                    _photos.value = _photos.value + result.photos
                    currentPage = pageToLoad // Update current page only on success
                    totalResults = result.totalResults // Update totalResults as well, might change
                    _canLoadMore.value = result.canLoadMore
                    _errorState.value = null

                    savedStateHandle[KEY_CURRENT_PAGE] = currentPage
                    savedStateHandle[KEY_TOTAL_RESULTS] = totalResults
                    savedStateHandle[KEY_CAN_LOAD_MORE] = _canLoadMore.value
                    savedStateHandle[KEY_PHOTOS_LIST_IDS] = _photos.value.map { it.id }
                    Logger.d("SearchViewModel", "Repo Success (Page $currentPage): ${result.photos.size} new. Total: ${_photos.value.size}. Saved state.")
                }
                is PhotosResult.Error -> {
                    handleRepositoryError(result, "pagination")
                }
            }
            _isLoadingMore.value = false
        }
    }

    private suspend fun performRestorePageInternal(query: String, pageToLoad: Int, perPage: Int): Boolean {
        var success = false
        _isLoadingMore.value = true // Use isLoadingMore for individual page loads during restore
        Logger.d("SearchViewModel", "Restoring page $pageToLoad for query \'$query\' using repo")

        val result = imageRepository.searchPhotos(
            query = query,
            page = pageToLoad,
            perPage = perPage
        )

        when (result) {
            is PhotosResult.Success -> {
                _photos.value = _photos.value + result.photos // Append photos
                this.currentPage = pageToLoad // Update current page after successful load
                this.totalResults = result.totalResults // Update total results
                this._canLoadMore.value = result.canLoadMore // Update canLoadMore
                _errorState.value = null
                success = true

                savedStateHandle[KEY_CURRENT_PAGE] = this.currentPage
                savedStateHandle[KEY_TOTAL_RESULTS] = this.totalResults
                savedStateHandle[KEY_CAN_LOAD_MORE] = this._canLoadMore.value
                savedStateHandle[KEY_PHOTOS_LIST_IDS] = _photos.value.map { it.id }
                Logger.d("SearchViewModel", "Restored page $pageToLoad successfully via repo. Photos: ${result.photos.size}. Total: ${_photos.value.size}")
            }
            is PhotosResult.Error -> {
                Logger.e("SearchViewModel", "Repository Error during page $pageToLoad restoration: ${result.message}")
                handleRepositoryError(result, "restoration")
                success = false
            }
        }
        _isLoadingMore.value = false
        return success
    }

    private fun handleRepositoryError(errorResult: PhotosResult.Error, operation: String) {
        Logger.e("SearchViewModel", "Repository error during $operation: ${errorResult.message}")
        _errorState.value = UserFacingError(message = errorResult.message, isRetryable = errorResult.isRetryable)

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
        if (searchAttempted && query.isNotBlank()) {
            _navigateToResults.value = true
        }
    }

    fun onNavigationComplete() {
        _navigateToResults.value = false
    }

    fun getPhotoById(id: Int): Photo? = _photos.value.find { it.id == id }

    fun retryLastFailedOperation() {
        if (_errorState.value?.isRetryable == true) {
            _errorState.value = null
            Logger.d("SearchViewModel", "Retrying last failed operation.")
            lastAction?.invoke()
        } else {
            Logger.d("SearchViewModel", "Retry attempted but error not retryable or no last action.")
        }
    }

    fun clearErrorState() {
        _errorState.value = null
        Logger.d("SearchViewModel", "Error state cleared by UI.")
    }

    fun onRefreshTriggered() {
        if (_isRefreshing.value) {
            Logger.d("SearchViewModel", "Refresh already in progress, ignoring trigger.")
            return
        }

        _isRefreshing.value = true
        _errorState.value = null

        Logger.d("SearchViewModel", "Refresh triggered. Query: '${_searchQuery.value}'")

        viewModelScope.launch {
            val result: PhotosResult = if (_searchQuery.value.isNotBlank()) {
                val query = _searchQuery.value
                Logger.d("SearchViewModel", "Refreshing search for query: '$query' using repo")
                imageRepository.searchPhotos(
                    query = query,
                    page = 1, // Always fetch page 1 for refresh
                    perPage = ITEMS_PER_PAGE
                )
            } else {
                Logger.d("SearchViewModel", "Refreshing curated photos using repo")
                imageRepository.getCuratedPhotos(
                    page = 1, // Page might be ignored by impl, but pass 1
                    perPage = ITEMS_PER_PAGE
                )
            }

            when (result) {
                is PhotosResult.Success -> {
                    _photos.value = result.photos
                    currentPage = 1 // Reset to page 1
                    totalResults = result.totalResults
                    _canLoadMore.value = result.canLoadMore
                    _errorState.value = null

                    savedStateHandle[KEY_CURRENT_PAGE] = currentPage
                    savedStateHandle[KEY_TOTAL_RESULTS] = totalResults
                    savedStateHandle[KEY_CAN_LOAD_MORE] = _canLoadMore.value
                    savedStateHandle[KEY_PHOTOS_LIST_IDS] = _photos.value.map { it.id }

                    val type = if (_searchQuery.value.isNotBlank()) "search" else "curated"
                    Logger.d("SearchViewModel", "Refresh $type success via repo: ${result.photos.size} photos. Total: $totalResults.")
                }
                is PhotosResult.Error -> {
                    val type = if (_searchQuery.value.isNotBlank()) "refresh search" else "refresh curated"
                    handleRepositoryError(result, type)
                }
            }

            _isRefreshing.value = false
            _isResultsEmpty.value = _photos.value.isEmpty() && searchAttempted // Might need adjustment based on refresh logic
            Logger.d("SearchViewModel", "Refresh finished. isRefreshing set to false.")
        }
    }
}