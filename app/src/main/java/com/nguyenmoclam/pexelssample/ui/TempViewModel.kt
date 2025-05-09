package com.nguyenmoclam.pexelssample.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import com.nguyenmoclam.pexelssample.data.local.dao.PhotoDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TempViewModel @Inject constructor(
    private val photoDao: PhotoDao
) : ViewModel() {

    init {
        if (photoDao != null) {
            Log.d("TempViewModel", "PhotoDao successfully injected: $photoDao")
        } else {
            Log.e("TempViewModel", "PhotoDao injection failed!")
        }
    }
} 