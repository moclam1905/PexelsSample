package com.nguyenmoclam.pexelssample.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.nguyenmoclam.pexelssample.ui.theme.PexelsSampleTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigateToSearchResults: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Home") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Welcome to Home Screen!")
            Button(onClick = onNavigateToSearchResults) {
                Text(text = "Go to Search Results")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    PexelsSampleTheme {
        HomeScreen(onNavigateToSearchResults = {})
    }
}