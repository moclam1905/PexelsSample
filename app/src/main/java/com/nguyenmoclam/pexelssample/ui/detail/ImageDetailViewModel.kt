package com.nguyenmoclam.pexelssample.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nguyenmoclam.pexelssample.domain.model.Photo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val KEY_PHOTO_ID_ARG = "photoId" // Renamed to avoid conflict if used elsewhere
private const val KEY_SCALE = "image_detail_scale"
private const val KEY_OFFSET_X = "image_detail_offset_x"
private const val KEY_OFFSET_Y = "image_detail_offset_y"

const val DEFAULT_MIN_SCALE = 1.0f
const val DEFAULT_MAX_SCALE = 3.0f

@HiltViewModel
class ImageDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _photo = MutableStateFlow<Photo?>(null)
    val photo: StateFlow<Photo?> = _photo.asStateFlow()

    // Store photoId from nav args if needed for other purposes, not for direct loading here.
    private val photoIdFromArgs: Int? = savedStateHandle.get<Int>(KEY_PHOTO_ID_ARG)

    private val _scale = MutableStateFlow(savedStateHandle.get<Float>(KEY_SCALE) ?: DEFAULT_MIN_SCALE)
    val scale: StateFlow<Float> = _scale.asStateFlow()

    private val _offsetX = MutableStateFlow(savedStateHandle.get<Float>(KEY_OFFSET_X) ?: 0f)
    val offsetX: StateFlow<Float> = _offsetX.asStateFlow()

    private val _offsetY = MutableStateFlow(savedStateHandle.get<Float>(KEY_OFFSET_Y) ?: 0f)
    val offsetY: StateFlow<Float> = _offsetY.asStateFlow()

    val minScale: Float = DEFAULT_MIN_SCALE
    val maxScale: Float = DEFAULT_MAX_SCALE

    init {
        // Photo is now set via setPhotoDetails by the Composable

        viewModelScope.launch {
            scale.collect { savedStateHandle[KEY_SCALE] = it }
        }
        viewModelScope.launch {
            offsetX.collect { savedStateHandle[KEY_OFFSET_X] = it }
        }
        viewModelScope.launch {
            offsetY.collect { savedStateHandle[KEY_OFFSET_Y] = it }
        }
    }

    fun setPhotoDetails(newPhoto: Photo?) {
        _photo.value = newPhoto
        // When a new photo is set, we might want to reset the transform state
        // unless the state being restored from SavedStateHandle is for this specific photo.
        // For now, if a new photo is explicitly set, reset transform.
        // This could be more sophisticated by associating saved state with photoId.
        if (newPhoto != null) { // Only reset if it's a new valid photo, not if it's cleared
            val currentSavedScale = savedStateHandle.get<Float>(KEY_SCALE)
            // If there's no saved scale, it implies a fresh state for this VM instance for this photo,
            // or it's a different photo than the one state was saved for.
            // This logic is tricky. For now, always resetting on explicit setPhotoDetails might be too aggressive
            // if setPhotoDetails is called with the same photo after a configuration change.
            // Let's assume that if setPhotoDetails is called, it's either the initial photo
            // or a *new* photo. If it's the initial photo, the scale/offsets from SavedStateHandle should apply.
            // The problem is SavedStateHandle is not inherently tied to a specific photoId within this VM easily.

            // Simplification: if setPhotoDetails called, and current scale IS default, then it's fine.
            // If scale is NOT default, and a new photo comes, we SHOULD reset.
            // This needs a robust way to check if saved state belongs to 'newPhoto'.
            // For this iteration, let's not auto-reset here. The Composable's LaunchedEffect(photo, layout) handles reset.
        }
    }

    fun updateTransform(newScale: Float, newOffsetX: Float, newOffsetY: Float) {
        _scale.value = newScale.coerceIn(minScale, maxScale)
        _offsetX.value = newOffsetX
        _offsetY.value = newOffsetY
    }

    fun onDoubleTap(currentScale: Float, currentOffsetX: Float, currentOffsetY: Float, targetScaleValue: Float, targetOffsetXValue: Float, targetOffsetYValue: Float) {
        if (kotlin.math.abs(currentScale - minScale) < 0.01f) {
            _scale.value = targetScaleValue.coerceIn(minScale, maxScale)
            _offsetX.value = targetOffsetXValue
            _offsetY.value = targetOffsetYValue
        } else {
            _scale.value = minScale
            _offsetX.value = 0f
            _offsetY.value = 0f
        }
    }

    fun resetTransform() {
        _scale.value = minScale
        _offsetX.value = 0f
        _offsetY.value = 0f
    }
} 