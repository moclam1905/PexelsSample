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

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val pexelsApiService: PexelsApiService
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun onQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onSearchClicked() {
        if (_searchQuery.value.isNotBlank()) {
            Log.d("SearchViewModel", "Search initiated for: ${_searchQuery.value}")
            viewModelScope.launch {
                try {
                    val response = pexelsApiService.searchPhotos(
                        query = _searchQuery.value,
                        page = 1,
                        perPage = 20
                    )
                    if (response.isSuccessful && response.body() != null) {
                        val responseBody = response.body()!!
                        Log.d("SearchViewModel", "API Success: Received ${responseBody.photos.size} photos. Total results: ${responseBody.totalResults}")
                    } else {
                        Log.e("SearchViewModel", "API Error: ${response.code()} - ${response.message()}. Body: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e("SearchViewModel", "Network or other error: ${e.message}", e)
                }
            }
        } else {
            Log.d("SearchViewModel", "Search query is empty.")
        }
    }
} 