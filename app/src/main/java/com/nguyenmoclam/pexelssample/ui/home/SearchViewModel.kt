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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _photos = MutableStateFlow<List<Photo>>(emptyList())
    val photos: StateFlow<List<Photo>> = _photos.asStateFlow()

    private val _navigateToResults = MutableStateFlow(false)
    val navigateToResults: StateFlow<Boolean> = _navigateToResults.asStateFlow()

    fun onQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onSearchClicked() {
        if (_searchQuery.value.isNotBlank()) {
            Logger.d("SearchViewModel", "Search initiated for: ${_searchQuery.value}")
            viewModelScope.launch {
                _photos.value = emptyList()
                _navigateToResults.value = false
                _isLoading.value = true
                try {
                    val response = pexelsApiService.searchPhotos(
                        query = _searchQuery.value,
                        page = 1,
                        perPage = 20
                    )
                    if (response.isSuccessful && response.body() != null) {
                        val responseBody = response.body()!!
                        Logger.d("SearchViewModel", "API Success: Received ${responseBody.photos.size} photos. Total results: ${responseBody.totalResults}")
                        val mappedPhotos = responseBody.photos.map { it.toDomain() }
                        _photos.value = mappedPhotos
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

    fun onNavigationComplete() {
        _navigateToResults.value = false
    }
} 