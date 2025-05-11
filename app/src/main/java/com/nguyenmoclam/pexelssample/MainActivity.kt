package com.nguyenmoclam.pexelssample

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.nguyenmoclam.pexelssample.core.navigation.AppNavigation
import com.nguyenmoclam.pexelssample.ui.TempViewModel
import com.nguyenmoclam.pexelssample.ui.theme.PexelsSampleTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val tempViewModel: TempViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MainActivity", "TempViewModel instance: $tempViewModel")

        setContent {
            PexelsSampleTheme {
                val windowSizeClass = calculateWindowSizeClass(this)
                AppNavigation(windowSizeClass = windowSizeClass)
            }
        }
    }
}