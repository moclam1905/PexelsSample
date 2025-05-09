package com.nguyenmoclam.pexelssample.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nguyenmoclam.pexelssample.data.remote.PexelsApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import com.nguyenmoclam.pexelssample.logger.Logger
import com.nguyenmoclam.pexelssample.domain.model.Photo
import com.nguyenmoclam.pexelssample.data.mappers.*

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val pexelsApiService: PexelsApiService
) : ViewModel() {

    private val ITEMS_PER_PAGE = 20

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _photos = MutableStateFlow<List<Photo>>(emptyList())
    val photos: StateFlow<List<Photo>> = _photos.asStateFlow()

    private val _navigateToResults = MutableStateFlow(false)
    val navigateToResults: StateFlow<Boolean> = _navigateToResults.asStateFlow()

    private var currentPage = 1
    private var totalResults = 0
    private val _canLoadMore = MutableStateFlow(false)
    val canLoadMore: StateFlow<Boolean> = _canLoadMore.asStateFlow()

    fun onQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onSearchClicked() {
        if (_searchQuery.value.isNotBlank()) {
            Logger.d("SearchViewModel", "Search initiated for: ${_searchQuery.value}")
            currentPage = 1
            _photos.value = emptyList()
            totalResults = 0
            _canLoadMore.value = false
            _navigateToResults.value = false

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
                        Logger.d("SearchViewModel", "API Success: Received ${responseBody.photos.size} photos. Total results: ${responseBody.totalResults}")
                        val mappedPhotos = responseBody.photos.map { it.toDomain() }
                        _photos.value = mappedPhotos
                        totalResults = responseBody.totalResults
                        _canLoadMore.value = responseBody.nextPage != null
                        if (mappedPhotos.isNotEmpty()) {
                            _navigateToResults.value = true
                        }
                    } else {
                        Logger.e("SearchViewModel", "API Error: ${response.code()} - ${response.message()}. Body: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Logger.e("SearchViewModel", "Network or other error: ${e.message}", e)
                } finally {
                    _isLoading.value = false
                }
            }
        } else {
            Logger.d("SearchViewModel", "Search query is empty.")
        }
    }

    fun loadNextPage() {
        if (_isLoading.value || !_canLoadMore.value) {
            Logger.d("SearchViewModel", "loadNextPage: Condition not met. isLoading: ${_isLoading.value}, canLoadMore: ${_canLoadMore.value}")
            return
        }

        Logger.d("SearchViewModel", "loadNextPage: Loading page ${currentPage + 1}")
        viewModelScope.launch {
            _isLoading.value = true
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
                } else {
                    Logger.e("SearchViewModel", "API Error (Page $currentPage): ${response.code()} - ${response.message()}. Body: ${response.errorBody()?.string()}")
                    _canLoadMore.value = false
                }
            } catch (e: Exception) {
                Logger.e("SearchViewModel", "Network or other error (Page $currentPage): ${e.message}", e)
                _canLoadMore.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onNavigationComplete() {
        _navigateToResults.value = false
    }
} 