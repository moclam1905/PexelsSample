package com.nguyenmoclam.pexelssample.ui.home

import android.util.Log
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.nguyenmoclam.pexelssample.data.remote.PexelsApiService
import com.nguyenmoclam.pexelssample.data.remote.model.PexelsPhotoDto
import com.nguyenmoclam.pexelssample.data.remote.model.PexelsPhotoSrcDto
import com.nguyenmoclam.pexelssample.data.remote.model.PexelsSearchResponseDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import retrofit2.Response
import java.io.IOException

@ExperimentalCoroutinesApi
class MainCoroutineRule(
    val testDispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()
) : TestWatcher(), TestCoroutineScope by TestCoroutineScope(testDispatcher) {

    override fun starting(description: Description?) {
        super.starting(description)
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description?) {
        super.finished(description)
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    fun runBlockingTest(block: suspend TestCoroutineScope.() -> Unit) =
        kotlinx.coroutines.test.runBlockingTest(this.testDispatcher, block)
}

@ExperimentalCoroutinesApi
class SearchViewModelTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var viewModel: SearchViewModel
    private lateinit var pexelsApiService: PexelsApiService

    @Before
    fun setUp() {
        pexelsApiService = mockk()
        viewModel = SearchViewModel(pexelsApiService)
        mockkStatic(Log::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `onQueryChanged updates searchQuery StateFlow`() = mainCoroutineRule.runBlockingTest {
        viewModel.searchQuery.test {
            assertThat(awaitItem()).isEqualTo("")

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
    fun `onSearchClicked with non-blank query calls apiService searchPhotos`() = mainCoroutineRule.runBlockingTest {
        val query = "nature"
        val mockResponse = PexelsSearchResponseDto(photos = emptyList(), totalResults = 0, page = 1, perPage = 20, nextPage = null)
        coEvery { pexelsApiService.searchPhotos(query, 1, 20) } returns Response.success(mockResponse)

        viewModel.onQueryChanged(query)
        viewModel.onSearchClicked()
        advanceUntilIdle()

        coVerify { pexelsApiService.searchPhotos(query = query, page = 1, perPage = 20) }
    }

    @Test
    fun `onSearchClicked with blank query does not call apiService`() = mainCoroutineRule.runBlockingTest {
        viewModel.onQueryChanged(" ")
        viewModel.onSearchClicked()
        advanceUntilIdle()

        coVerify(exactly = 0) { pexelsApiService.searchPhotos(any(), any(), any()) }
    }

    @Test
    fun `onSearchClicked success logs correct information`() = mainCoroutineRule.runBlockingTest {
        val query = "cats"
        val mockPhotoSrcDto = mockk<PexelsPhotoSrcDto>()
        val photos = listOf(
            PexelsPhotoDto(id = 1, width = 100, height = 100, url = "", photographer = "", photographerUrl = "", photographerId = 1, avgColor = "", src = mockPhotoSrcDto, liked = false, alt = "")
        )
        val mockSuccessResponse = PexelsSearchResponseDto(photos = photos, totalResults = 1, page = 1, perPage = 1, nextPage = null)
        coEvery { pexelsApiService.searchPhotos(query, 1, 20) } returns Response.success(mockSuccessResponse)

        viewModel.onQueryChanged(query)
        viewModel.onSearchClicked()
        advanceUntilIdle()
    }

    @Test
    fun `onSearchClicked API error logs error information`() = mainCoroutineRule.runBlockingTest {
        val query = "dogs"
        val errorResponseBody = """{"error":"Forbidden"}""".toResponseBody("application/json".toMediaType())
        coEvery { pexelsApiService.searchPhotos(query, 1, 20) } returns Response.error(403, errorResponseBody)

        viewModel.onQueryChanged(query)
        viewModel.onSearchClicked()
        advanceUntilIdle()
    }

    @Test
    fun `onSearchClicked network IOException logs error information`() = mainCoroutineRule.runBlockingTest {
        val query = "birds"
        coEvery { pexelsApiService.searchPhotos(query, 1, 20) } throws IOException("Network error")

        viewModel.onQueryChanged(query)
        viewModel.onSearchClicked()
        advanceUntilIdle()
    }

    @Test
    fun `onSearchClicked with non-blank query success, isLoading is true then false`() = mainCoroutineRule.runBlockingTest {
        val query = "nature"
        val mockApiResponse = mockk<PexelsSearchResponseDto>(relaxed = true)
        coEvery { pexelsApiService.searchPhotos(query, 1, 20) } returns Response.success(mockApiResponse)

        viewModel.onQueryChanged(query)

        viewModel.isLoading.test {
            assertThat(awaitItem()).isEqualTo(false)

            viewModel.onSearchClicked()

            assertThat(awaitItem()).isEqualTo(true)
            assertThat(awaitItem()).isEqualTo(false)

            cancelAndConsumeRemainingEvents()
        }

        coVerify { pexelsApiService.searchPhotos(query, 1, 20) }
    }

    @Test
    fun `onSearchClicked with non-blank query failure, isLoading is true then false`() = mainCoroutineRule.runBlockingTest {
        val query = "error"
        val exception = RuntimeException("Network error")
        coEvery { pexelsApiService.searchPhotos(query, 1, 20) } throws exception

        viewModel.onQueryChanged(query)

        viewModel.isLoading.test {
            assertThat(awaitItem()).isEqualTo(false)

            viewModel.onSearchClicked()

            assertThat(awaitItem()).isEqualTo(true)
            assertThat(awaitItem()).isEqualTo(false)

            cancelAndConsumeRemainingEvents()
        }

        coVerify { pexelsApiService.searchPhotos(query, 1, 20) }
    }

    @Test
    fun `onSearchClicked with blank query, isLoading remains false and no API call`() = mainCoroutineRule.runBlockingTest {
        val query = " "
        viewModel.onQueryChanged(query)

        viewModel.isLoading.test {
            assertThat(awaitItem()).isEqualTo(false)

            viewModel.onSearchClicked()

            expectNoEvents()
        }
        coVerify(exactly = 0) { pexelsApiService.searchPhotos(any(), any(), any()) }
    }
} 