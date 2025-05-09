package com.nguyenmoclam.pexelssample.ui.home

import android.util.Log
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
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
import kotlinx.coroutines.test.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import retrofit2.Response
import java.io.IOException

@ExperimentalCoroutinesApi
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: SearchViewModel
    private lateinit var pexelsApiService: PexelsApiService

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        pexelsApiService = mockk()
        viewModel = SearchViewModel(pexelsApiService)
        mockkStatic(Log::class)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
    }

    @Test
    fun `onQueryChanged updates searchQuery StateFlow`() = runTest(testDispatcher) {
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
    fun `onSearchClicked with non-blank query calls apiService searchPhotos`() = runTest(testDispatcher) {
        val query = "nature"
        val mockResponse = PexelsSearchResponseDto(photos = emptyList(), totalResults = 0, page = 1, perPage = 20, nextPage = null)
        coEvery { pexelsApiService.searchPhotos(query, 1, 20) } returns Response.success(mockResponse)

        viewModel.onQueryChanged(query)
        viewModel.onSearchClicked()
        advanceUntilIdle()

        coVerify { pexelsApiService.searchPhotos(query = query, page = 1, perPage = 20) }
    }

    @Test
    fun `onSearchClicked with blank query does not call apiService`() = runTest(testDispatcher) {
        viewModel.onQueryChanged(" ")
        viewModel.onSearchClicked()
        advanceUntilIdle()

        coVerify(exactly = 0) { pexelsApiService.searchPhotos(any(), any(), any()) }
    }

    @Test
    fun `onSearchClicked success logs correct information`() = runTest(testDispatcher) {
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
    fun `onSearchClicked API error logs error information`() = runTest(testDispatcher) {
        val query = "dogs"
        val errorResponseBody = """{"error":"Forbidden"}""".toResponseBody("application/json".toMediaType())
        coEvery { pexelsApiService.searchPhotos(query, 1, 20) } returns Response.error(403, errorResponseBody)

        viewModel.onQueryChanged(query)
        viewModel.onSearchClicked()
        advanceUntilIdle()
    }

    @Test
    fun `onSearchClicked network IOException logs error information`() = runTest(testDispatcher) {
        val query = "birds"
        coEvery { pexelsApiService.searchPhotos(query, 1, 20) } throws IOException("Network error")

        viewModel.onQueryChanged(query)
        viewModel.onSearchClicked()
        advanceUntilIdle()
    }
} 