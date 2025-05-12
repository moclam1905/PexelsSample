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

// Define items per page here or retrieve from a config if needed
private const val ITEMS_PER_PAGE = 20 // Example value, adjust as needed

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val imageRepository: ImageRepository
) : ViewModel() {

    private val _photos = MutableStateFlow<List<Photo>>(emptyList())
    val photos: StateFlow<List<Photo>> = _photos.asStateFlow()

    private val _isLoadingInitial = MutableStateFlow(false)
    val isLoadingInitial: StateFlow<Boolean> = _isLoadingInitial.asStateFlow()

    // Error state can be added later as per Story 10.6
    // private val _errorState = MutableStateFlow<String?>(null)
    // val errorState: StateFlow<String?> = _errorState.asStateFlow()

    init {
        fetchInitialPhotos()
    }

    private fun fetchInitialPhotos() {
        if (_isLoadingInitial.value) return // Prevent concurrent initial fetches

        viewModelScope.launch {
            _isLoadingInitial.value = true
            // _errorState.value = null // Reset error on new fetch attempt
            try {
                // Page 1 for initial load
                when (val result = imageRepository.getCuratedPhotos(page = 1, perPage = ITEMS_PER_PAGE)) {
                    is PhotosResult.Success -> {
                        _photos.value = result.photos
                        // Handle totalResults and canLoadMore if needed for pagination later
                    }
                    is PhotosResult.Error -> {
                        Logger.e("HomeScreenViewModel", "Error fetching initial photos: ${result.message}")
                        // _errorState.value = result.message // Update error state
                        _photos.value = emptyList() // Clear photos on error? Or keep stale data? Cleared for now.
                    }
                }
            } catch (e: Exception) {
                // Catch any unexpected exceptions from the repository layer upwards
                Logger.e("HomeScreenViewModel", "Unexpected error fetching initial photos: ${e.message}", e)
                // _errorState.value = "An unexpected error occurred."
                _photos.value = emptyList()
            } finally {
                _isLoadingInitial.value = false
            }
        }
    }

    // Potential future methods for pagination (e.g., loadNextPage), refresh, etc.
} 