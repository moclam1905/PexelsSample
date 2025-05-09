package com.nguyenmoclam.pexelssample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.nguyenmoclam.pexelssample.core.navigation.AppNavigation
import com.nguyenmoclam.pexelssample.ui.theme.PexelsSampleTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PexelsSampleTheme {
                AppNavigation()
            }
        }
    }
}