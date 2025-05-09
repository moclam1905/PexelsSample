package com.nguyenmoclam.pexelssample.ui.home

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
// import androidx.compose.material3.Button // Will remove this if not used elsewhere
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nguyenmoclam.pexelssample.ui.theme.PexelsSampleTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    // onNavigateToSearchResults: () -> Unit, // Commenting out as per story, search happens on this screen
    viewModel: HomeViewModel = hiltViewModel()
) {
    val searchQuery = remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Pexels Image Search") }) // Updated title
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp), // Added overall padding for the content
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top // Align content to the top
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery.value,
                    onValueChange = { searchQuery.value = it },
                    placeholder = { Text("Search for images...") },
                    label = { Text("Search") },
                    modifier = Modifier.weight(1f), // TextField takes remaining space
                    singleLine = true,
                    leadingIcon = { // Optional leading icon as mentioned in story
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search Icon"
                        )
                    }
                )
                IconButton(onClick = {
                    // Story 2.2 will connect this to ViewModel
                    Log.d("HomeScreen", "Search button clicked with query: ${searchQuery.value}")
                }) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search button"
                    )
                }
            }

            // Placeholder for search results or other content, can be added later
            // Text(text = "Welcome to Home Screen!") // Removed old placeholder
            // Text(text = viewModel.getGreeting()) // Removed old placeholder
            // Button(onClick = onNavigateToSearchResults) { // Removed old button
            // Text(text = "Go to Search Results")
            // }
        }
    }
    // Log.d("HomeScreen", "ViewModel Greeting: ${viewModel.getGreeting()}") // Commented out as greeting is removed
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    PexelsSampleTheme {
        HomeScreen() // Removed onNavigateToSearchResults as it's not used now
    }
}