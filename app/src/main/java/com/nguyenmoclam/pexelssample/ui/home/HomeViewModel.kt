package com.nguyenmoclam.pexelssample.ui.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {
    // Placeholder ViewModel logic can be added here later
    fun getGreeting(): String = "Hello from HomeViewModel!"
} 