package com.nguyenmoclam.pexelssample.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nguyenmoclam.pexelssample.domain.model.Photo
import com.nguyenmoclam.pexelssample.ui.model.UserFacingError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val KEY_PHOTO_ID_ARG = "photoId" // Renamed to avoid conflict if used elsewhere
private const val KEY_SCALE = "image_detail_scale"
private const val KEY_OFFSET_X = "image_detail_offset_x"
private const val KEY_OFFSET_Y = "image_detail_offset_y"

const val DEFAULT_MIN_SCALE = 1.0f
const val DEFAULT_MAX_SCALE = 3.0f

// UiState now focuses on the photo passed in and its transform state
data class ImageDetailUiState(
    val photo: Photo? = null,
    val isLoading: Boolean = false, // Could mean "looking up photo in passed ViewModels"
    val error: UserFacingError? = null,   // Could mean "photo not found"
    val scale: Float = DEFAULT_MIN_SCALE,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
)

@HiltViewModel
class ImageDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImageDetailUiState(
        scale = savedStateHandle.get<Float>(KEY_SCALE) ?: DEFAULT_MIN_SCALE,
        offsetX = savedStateHandle.get<Float>(KEY_OFFSET_X) ?: 0f,
        offsetY = savedStateHandle.get<Float>(KEY_OFFSET_Y) ?: 0f
    ))
    val uiState: StateFlow<ImageDetailUiState> = _uiState.asStateFlow()

    // Expose individual transform states for simplicity in the Composable's LaunchedEffects
    val scale: StateFlow<Float> = MutableStateFlow(_uiState.value.scale).also { flow ->
        viewModelScope.launch { flow.collect { savedStateHandle[KEY_SCALE] = it } }
    }.asStateFlow()
    val offsetX: StateFlow<Float> = MutableStateFlow(_uiState.value.offsetX).also { flow ->
        viewModelScope.launch { flow.collect { savedStateHandle[KEY_OFFSET_X] = it } }
    }.asStateFlow()
    val offsetY: StateFlow<Float> = MutableStateFlow(_uiState.value.offsetY).also { flow ->
        viewModelScope.launch { flow.collect { savedStateHandle[KEY_OFFSET_Y] = it } }
    }.asStateFlow()

    // Expose photo directly for easier observation in the Composable if needed, though UiState is preferred
    val photo: StateFlow<Photo?> = MutableStateFlow<Photo?>(null).also { photoFlow ->
        viewModelScope.launch {
            _uiState.collect { state ->
                if (photoFlow.value != state.photo) {
                    photoFlow.value = state.photo
                }
            }
        }
    }.asStateFlow()

    val minScale: Float = DEFAULT_MIN_SCALE
    val maxScale: Float = DEFAULT_MAX_SCALE

    fun setPhotoDetails(newPhoto: Photo?) {
        // If newPhoto is null, it means it wasn't found by the calling screen
        if (newPhoto == null && _uiState.value.photo == null) {
             _uiState.update { it.copy(
                photo = null, 
                isLoading = false, 
                error = UserFacingError(message = "Photo details not found.", isRetryable = true)
            ) }
        } else if (_uiState.value.photo?.id != newPhoto?.id) {
            _uiState.update { it.copy(photo = newPhoto, isLoading = false, error = null) }
        } else if (newPhoto != null && _uiState.value.photo == null) {
            // Case where photo was null (e.g. error state) and now we get a valid photo
            _uiState.update { it.copy(photo = newPhoto, isLoading = false, error = null) }
        }
        // If newPhoto is the same as current, do nothing to avoid state churn
    }
    
    // Call this when starting to look for photo in other ViewModels
    fun signalLoadingPhoto() {
        _uiState.update { it.copy(isLoading = true, error = null, photo = null) }
    }

    fun updateTransform(newScale: Float, newOffsetX: Float, newOffsetY: Float) {
        val clampedScale = newScale.coerceIn(minScale, maxScale)
        _uiState.update {
            it.copy(scale = clampedScale, offsetX = newOffsetX, offsetY = newOffsetY)
        }
        (scale as? MutableStateFlow)?.value = clampedScale
        (offsetX as? MutableStateFlow)?.value = newOffsetX
        (offsetY as? MutableStateFlow)?.value = newOffsetY
    }

    fun onDoubleTap(currentScale: Float, currentOffsetX: Float, currentOffsetY: Float, targetScaleValue: Float, targetOffsetXValue: Float, targetOffsetYValue: Float) {
        val finalScale: Float
        val finalOffsetX: Float
        val finalOffsetY: Float
        if (kotlin.math.abs(currentScale - minScale) < 0.01f) {
            finalScale = targetScaleValue.coerceIn(minScale, maxScale)
            finalOffsetX = targetOffsetXValue
            finalOffsetY = targetOffsetYValue
        } else {
            finalScale = minScale
            finalOffsetX = 0f
            finalOffsetY = 0f
        }
         _uiState.update { it.copy(scale = finalScale, offsetX = finalOffsetX, offsetY = finalOffsetY) }
         (scale as? MutableStateFlow)?.value = finalScale
         (offsetX as? MutableStateFlow)?.value = finalOffsetX
         (offsetY as? MutableStateFlow)?.value = finalOffsetY
    }

    fun resetTransform() {
        _uiState.update { it.copy(scale = minScale, offsetX = 0f, offsetY = 0f) }
        (scale as? MutableStateFlow)?.value = minScale
        (offsetX as? MutableStateFlow)?.value = 0f
        (offsetY as? MutableStateFlow)?.value = 0f
    }
} 