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
    private var lastAction: (() -> Unit)? = null // Added for retry mechanism

    fun onQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onSearchClicked() {
        if (_searchQuery.value.isNotBlank()) {
            Logger.d("SearchViewModel", "Search initiated for: ${_searchQuery.value}")
            // Store the action for potential retry
            lastAction = { performSearchInternal() }
            performSearchInternal()
        } else {
            Logger.d("SearchViewModel", "Search query is empty.")
        }
    }

    private fun performSearchInternal() {
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
                    // Enhanced error handling based on HTTP status codes
                    _errorState.value = when (response.code()) {
                        401, 403 -> UserFacingError(message = "Authentication error. Please check configuration.", isRetryable = false)
                        429 -> UserFacingError(message = "Too many requests. Please try again in an hour.", isRetryable = false)
                        in 500..599 -> UserFacingError(message = "Pexels.com seems to be unavailable. Please try again later.", isRetryable = true)
                        in 400..499 -> UserFacingError(message = "Invalid request (Error ${response.code()}). Please try modifying your search.", isRetryable = true)
                        else -> UserFacingError(message = "An unknown error occurred (Error ${response.code()}). Please try again.", isRetryable = true)
                    }
                    _isResultsEmpty.value = false
                    _navigateToResults.value = true
                    _isLoading.value = false // Ensure isLoading is false on error
                }
            } catch (e: java.io.IOException) { // Specific catch for network errors
                Logger.e("SearchViewModel", "Network error during search: ${e.message}", e)
                _errorState.value = UserFacingError(message = "No internet connection. Please check your connection and try again.", isRetryable = true)
                _isResultsEmpty.value = false
                _navigateToResults.value = true
                _isLoading.value = false // Ensure isLoading is false on error
            } catch (e: Exception) { // General catch for other errors
                Logger.e("SearchViewModel", "Other error during search: ${e.message}", e)
                _errorState.value = UserFacingError(message = "An unexpected error occurred: ${e.localizedMessage ?: "Unknown"}", isRetryable = true)
                _isResultsEmpty.value = false
                _navigateToResults.value = true
                _isLoading.value = false // Ensure isLoading is false on error
            } finally {
                _isLoading.value = false
                // After loading is complete, determine if results are empty.
                // This should only be true if there was no error and the photos list is empty.
                if (_errorState.value == null) { // No error occurred during the fetch
                    _isResultsEmpty.value = _photos.value.isEmpty() && searchAttempted
                } else {
                    // If an error occurred, it's not an "empty result" scenario,
                    // it's an "error" scenario. So, isResultsEmpty should be false.
                    _isResultsEmpty.value = false
                }
            }
        }
    }

    fun loadNextPage() {
        if (_isLoading.value || _isLoadingMore.value || !_canLoadMore.value) {
            Logger.d("SearchViewModel", "loadNextPage: Condition not met. isLoading: ${_isLoading.value}, isLoadingMore: ${_isLoadingMore.value}, canLoadMore: ${_canLoadMore.value}")
            return
        }
        // Store the action for potential retry
        lastAction = { performLoadNextPageInternal() }
        performLoadNextPageInternal()
    }

    private fun performLoadNextPageInternal() {
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
                    // Enhanced error handling based on HTTP status codes
                    _errorState.value = when (response.code()) {
                        401, 403 -> UserFacingError(message = "Authentication error. Please check configuration.", isRetryable = false)
                        429 -> UserFacingError(message = "Too many requests. Please try again in an hour.", isRetryable = false)
                        in 500..599 -> UserFacingError(message = "Pexels.com seems to be unavailable. Please try again later.", isRetryable = true)
                        in 400..499 -> UserFacingError(message = "Invalid request (Error ${response.code()}). Please try modifying your search.", isRetryable = true)
                        else -> UserFacingError(message = "An unknown error occurred (Error ${response.code()}). Please try again.", isRetryable = true)
                    }
                    _canLoadMore.value = false // Stop further pagination attempts on error
                    _isLoadingMore.value = false // Ensure isLoadingMore is false on error
                }
            } catch (e: java.io.IOException) { // Specific catch for network errors
                Logger.e("SearchViewModel", "Network error during pagination (Page $currentPage): ${e.message}", e)
                _errorState.value = UserFacingError(message = "No internet connection. Please check your connection and try again.", isRetryable = true)
                _canLoadMore.value = false // Stop further pagination attempts on error
                _isLoadingMore.value = false // Ensure isLoadingMore is false on error
            } catch (e: Exception) { // General catch for other errors
                Logger.e("SearchViewModel", "Other error during pagination (Page $currentPage): ${e.message}", e)
                _errorState.value = UserFacingError(message = "An unexpected error occurred: ${e.localizedMessage ?: "Unknown"}", isRetryable = true)
                _canLoadMore.value = false // Stop further pagination attempts on error
                _isLoadingMore.value = false // Ensure isLoadingMore is false on error
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun onNavigationComplete() {
        _navigateToResults.value = false
    }

    fun getPhotoById(id: Int): Photo? = _photos.value.find { it.id == id }
    
    fun retryLastFailedOperation() {
        _errorState.value = null // Clear the error before retrying
        Logger.d("SearchViewModel", "retryLastFailedOperation: Attempting to execute last stored action.")
        lastAction?.invoke()
    }
} 