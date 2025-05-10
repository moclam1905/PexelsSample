package com.nguyenmoclam.pexelssample.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nguyenmoclam.pexelssample.data.mappers.toDomain
import com.nguyenmoclam.pexelssample.data.remote.PexelsApiService
import com.nguyenmoclam.pexelssample.data.remote.model.PexelsSearchResponseDto
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
    private val pexelsApiService: PexelsApiService
) : ViewModel() {

    private val ITEMS_PER_PAGE = 20

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _photos = MutableStateFlow<List<Photo>>(emptyList())
    val photos: StateFlow<List<Photo>> = _photos.asStateFlow()

    private val _navigateToResults = MutableStateFlow(false)
    val navigateToResults: StateFlow<Boolean> = _navigateToResults.asStateFlow()

    private var currentPage = 1
    private var totalResults = 0
    private val _canLoadMore = MutableStateFlow(false)
    val canLoadMore: StateFlow<Boolean> = _canLoadMore.asStateFlow()

    private val _isResultsEmpty = MutableStateFlow(false)
    val isResultsEmpty: StateFlow<Boolean> = _isResultsEmpty.asStateFlow()

    private val _errorState = MutableStateFlow<UserFacingError?>(null)
    val errorState: StateFlow<UserFacingError?> = _errorState.asStateFlow()

    private var searchAttempted = false

    fun onQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onSearchClicked() {
        if (_searchQuery.value.isNotBlank()) {
            Logger.d("SearchViewModel", "Search initiated for: ${_searchQuery.value}")
            _errorState.value = null // Clear previous errors
            currentPage = 1
            _photos.value = emptyList()
            totalResults = 0
            _canLoadMore.value = false
            _navigateToResults.value = false
            _isLoadingMore.value = false
            searchAttempted = true
            _isResultsEmpty.value = false // Reset at the start of a new search

            viewModelScope.launch {
                _isLoading.value = true
                var response: retrofit2.Response<PexelsSearchResponseDto>? = null
                try {
                    response = pexelsApiService.searchPhotos(
                        query = _searchQuery.value,
                        page = currentPage,
                        perPage = ITEMS_PER_PAGE
                    )
                    if (response.isSuccessful && response.body() != null) {
                        val responseBody = response.body()!!
                        Logger.d("SearchViewModel", "API Success: Received ${responseBody.photos.size} photos. Total results: ${responseBody.totalResults}")
                        val mappedPhotos = responseBody.photos.map { it.toDomain() }
                        _photos.value = mappedPhotos
                        totalResults = responseBody.totalResults
                        _canLoadMore.value = responseBody.nextPage != null
                        _errorState.value = null // Clear error on success
                        _navigateToResults.value = true
                    } else {
                        Logger.e("SearchViewModel", "API Error: ${response.code()} - ${response.message()}. Body: ${response.errorBody()?.string()}")
                        _errorState.value = UserFacingError(message = "Could not load images. Please try again.", isRetryable = true)
                        _isResultsEmpty.value = false
                        _navigateToResults.value = false
                    }
                } catch (e: Exception) {
                    Logger.e("SearchViewModel", "Network or other error: ${e.message}", e)
                    _errorState.value = UserFacingError(message = "Could not load images. Please check your connection.", isRetryable = true)
                    _isResultsEmpty.value = false
                    _navigateToResults.value = false
                } finally {
                    _isLoading.value = false
                    if (searchAttempted && response?.isSuccessful == true && response?.body() != null) {
                        _isResultsEmpty.value = _photos.value.isEmpty()
                    } else {
                        _isResultsEmpty.value = false
                    }
                }
            }
        } else {
            Logger.d("SearchViewModel", "Search query is empty.")
        }
    }

    fun loadNextPage() {
        if (_isLoading.value || _isLoadingMore.value || !_canLoadMore.value) {
            Logger.d("SearchViewModel", "loadNextPage: Condition not met. isLoading: ${_isLoading.value}, isLoadingMore: ${_isLoadingMore.value}, canLoadMore: ${_canLoadMore.value}")
            return
        }
        _errorState.value = null // Clear previous errors before attempting to load more
        _isLoadingMore.value = true
        Logger.d("SearchViewModel", "loadNextPage: Loading page ${currentPage + 1}")
        viewModelScope.launch {
            currentPage++
            try {
                val response = pexelsApiService.searchPhotos(
                    query = _searchQuery.value,
                    page = currentPage,
                    perPage = ITEMS_PER_PAGE
                )
                if (response.isSuccessful && response.body() != null) {
                    val responseBody = response.body()!!
                    Logger.d("SearchViewModel", "API Success (Page $currentPage): Received ${responseBody.photos.size} new photos.")
                    val mappedNewPhotos = responseBody.photos.map { it.toDomain() }
                    _photos.value = _photos.value + mappedNewPhotos
                    _canLoadMore.value = responseBody.nextPage != null
                    _errorState.value = null // Clear error on success
                } else {
                    Logger.e("SearchViewModel", "API Error (Page $currentPage): ${response.code()} - ${response.message()}. Body: ${response.errorBody()?.string()}")
                    _errorState.value = UserFacingError(message = "Could not load more images. Please try again.", isRetryable = true)
                    _canLoadMore.value = false // Stop further pagination attempts on error
                }
            } catch (e: Exception) {
                Logger.e("SearchViewModel", "Network or other error (Page $currentPage): ${e.message}", e)
                _errorState.value = UserFacingError(message = "Could not load more images. Please check your connection.", isRetryable = true)
                _canLoadMore.value = false // Stop further pagination attempts on error
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun onNavigationComplete() {
        _navigateToResults.value = false
    }

    fun getPhotoById(id: Int): Photo? = _photos.value.find { it.id == id }
    
    // Placeholder for retry logic as per story refinement.
    // For now, SearchResultsScreen will call onSearchClicked or loadNextPage directly.
    // This function can be expanded later if more complex retry logic is needed.
    fun retryLastFailedOperation() {
        // Determine what failed (initial search or pagination)
        // For now, if there's an error, and we have a search query, let's assume initial search failed.
        // This is a simplification.
        if (_errorState.value != null && _searchQuery.value.isNotBlank()) {
            Logger.d("SearchViewModel", "Retrying search for: ${_searchQuery.value}")
            onSearchClicked()
        } else if (_errorState.value != null) {
            // If no query, but error, maybe it was a pagination error
            // This part needs more robust logic to know if it was pagination
            // For now, let's try to load next page if canLoadMore was true before error
            // Or, if photos exist, it implies it might be a pagination error.
             if (_photos.value.isNotEmpty()) { // A simple heuristic
                 Logger.d("SearchViewModel", "Retrying loadNextPage")
                 loadNextPage()
             }
        }
    }
} 