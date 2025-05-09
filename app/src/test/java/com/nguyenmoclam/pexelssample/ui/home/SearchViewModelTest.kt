package com.nguyenmoclam.pexelssample.ui.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.nguyenmoclam.pexelssample.util.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import android.util.Log // Import for Log.d
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify

@ExperimentalCoroutinesApi
class SearchViewModelTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var viewModel: SearchViewModel

    @Before
    fun setUp() {
        viewModel = SearchViewModel()
        mockkStatic(Log::class) // Mock Android's Log class
    }

    // No tearDown needed to unmockkStatic(Log::class) if tests are independent enough
    // or if using @After per test, but for this scope it might be okay.
    // Consider unmockkStatic(Log::class) in an @After method if tests interfere.

    @Test
    fun `onQueryChanged updates searchQuery StateFlow`() = runTest {
        viewModel.searchQuery.test {
            assertThat(awaitItem()).isEqualTo("") // Initial value

            viewModel.onQueryChanged("cats")
            assertThat(awaitItem()).isEqualTo("cats")

            viewModel.onQueryChanged("dogs and cats")
            assertThat(awaitItem()).isEqualTo("dogs and cats")

            viewModel.onQueryChanged("")
            assertThat(awaitItem()).isEqualTo("")

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onSearchClicked with non-blank query logs search initiated`() = runTest {
        val testQuery = "TEST"
        viewModel.onQueryChanged(testQuery)
        viewModel.onSearchClicked()
        // Verify Log.d was called with the correct message
        verify { Log.d("SearchViewModel", "Search initiated for: $testQuery") }
    }

    @Test
    fun `onSearchClicked with blank query logs query empty`() = runTest {
        viewModel.onQueryChanged("   ") // Query with only spaces
        viewModel.onSearchClicked()
        // Verify Log.d was called with the correct message
        verify { Log.d("SearchViewModel", "Search query is empty.") }
    }

    @Test
    fun `onSearchClicked with empty query logs query empty`() = runTest {
        viewModel.onQueryChanged("") // Empty query
        viewModel.onSearchClicked()
        // Verify Log.d was called with the correct message
        verify { Log.d("SearchViewModel", "Search query is empty.") }
        // unmockkStatic(Log::class) // If unmocking per test or after all tests in class
    }
} 