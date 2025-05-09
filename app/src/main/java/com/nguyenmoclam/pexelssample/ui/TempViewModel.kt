package com.nguyenmoclam.pexelssample.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nguyenmoclam.pexelssample.data.remote.PexelsApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TempViewModel @Inject constructor(
    private val pexelsApiService: PexelsApiService
) : ViewModel() {

    private val TAG = "TempViewModel"

    fun makeTestApiCall() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Making API call...")
                val response = pexelsApiService.getCuratedPhotosTest(perPage = 1)
                if (response.isSuccessful) {
                    Log.d(TAG, "API Call Successful! Code: ${response.code()}")
                } else {
                    Log.e(TAG, "API Call Failed! Code: ${response.code()}")
                    Log.e(TAG, "Error Body: ${response.errorBody()?.string()}")
                    Log.e(TAG, "Response Headers: ${response.headers()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "API Call Exception: ${e.message}", e)
            }
        }
    }
} 