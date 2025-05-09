package com.nguyenmoclam.pexelssample.ui.results

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
fun SearchResultsScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Search Results") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "This is the Search Results Screen.")
            Button(onClick = onNavigateBack) {
                Text(text = "Go Back")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SearchResultsScreenPreview() {
    PexelsSampleTheme {
        SearchResultsScreen(onNavigateBack = {})
    }
} 