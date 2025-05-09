package com.nguyenmoclam.pexelssample.ui.home

import android.util.Log
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.nguyenmoclam.pexelssample.data.mappers.toDomain
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
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

    private val mockPhotoSrcDto = PexelsPhotoSrcDto(original = "original_url", large2x = "large2x_url", large = "large_url", medium = "medium_url", small = "small_url", portrait = "portrait_url", landscape = "landscape_url", tiny = "tiny_url")
    private val mockPhotoDto1 = PexelsPhotoDto(id = 1, width = 100, height = 100, url = "url1", photographer = "Photographer 1", photographerUrl = "", photographerId = 1, avgColor = "", src = mockPhotoSrcDto, liked = false, alt = "Photo 1")
    private val mockPhotoDto2 = PexelsPhotoDto(id = 2, width = 100, height = 100, url = "url2", photographer = "Photographer 2", photographerUrl = "", photographerId = 2, avgColor = "", src = mockPhotoSrcDto, liked = false, alt = "Photo 2")
    private val mockPhotoDto3 = PexelsPhotoDto(id = 3, width = 100, height = 100, url = "url3", photographer = "Photographer 3", photographerUrl = "", photographerId = 3, avgColor = "", src = mockPhotoSrcDto, liked = false, alt = "Photo 3")

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
        val mockSuccessResponse = PexelsSearchResponseDto(photos = listOf(mockPhotoDto1), totalResults = 1, page = 1, perPage = 1, nextPage = null)
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

    @Test
    fun `photos are reset and updated correctly after onSearchClicked`() = runTest {
        val query = "nature"
        val photosPage1 = listOf(mockPhotoDto1)
        val responsePage1 = PexelsSearchResponseDto(
            photos = photosPage1,
            totalResults = 10,
            page = 1,
            perPage = 1,
            nextPage = "next_page_url"
        )

        coEvery { pexelsApiService.searchPhotos(query, 1, 20) } returns Response.success(responsePage1)

        // Set query
        viewModel.onQueryChanged(query)
        viewModel.onSearchClicked()

        advanceUntilIdle()

        viewModel.photos.test {
            assertThat(awaitItem()).isEqualTo(photosPage1.map { it.toDomain() })

            cancelAndConsumeRemainingEvents()
        }
    }
    @Test
    fun `canLoadMore is updated correctly after onSearchClicked`() = runTest {
        val query = "nature"
        val photosPage1 = listOf(mockPhotoDto1)
        val responsePage1 = PexelsSearchResponseDto(
            photos = photosPage1,
            totalResults = 10,
            page = 1,
            perPage = 1,
            nextPage = "next_page_url"
        )

        coEvery { pexelsApiService.searchPhotos(query, 1, 20) } returns Response.success(responsePage1)

        viewModel.onQueryChanged(query)
        viewModel.onSearchClicked()
        advanceUntilIdle()

        viewModel.canLoadMore.test {
            assertThat(awaitItem()).isTrue()
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `loadNextPage does nothing if isLoading is true`() = mainCoroutineRule.runBlockingTest {
        viewModel.onQueryChanged("test")

        val isLoadingField = SearchViewModel::class.java.getDeclaredField("_isLoading")
        isLoadingField.setAccessible(true)
        (isLoadingField.get(viewModel) as MutableStateFlow<Boolean>).value = true

        val canLoadMoreField = SearchViewModel::class.java.getDeclaredField("_canLoadMore")
        canLoadMoreField.setAccessible(true)
        (canLoadMoreField.get(viewModel) as MutableStateFlow<Boolean>).value = true

        viewModel.loadNextPage()
        advanceUntilIdle()

        coVerify(exactly = 0) { pexelsApiService.searchPhotos(any(), any(), any()) }
    }

    @Test
    fun `loadNextPage does nothing if canLoadMore is false`() = mainCoroutineRule.runBlockingTest {
        viewModel.onQueryChanged("test")

        val canLoadMoreField = SearchViewModel::class.java.getDeclaredField("_canLoadMore")
        canLoadMoreField.setAccessible(true)
        (canLoadMoreField.get(viewModel) as MutableStateFlow<Boolean>).value = false

        viewModel.loadNextPage()
        advanceUntilIdle()

        coVerify(exactly = 0) { pexelsApiService.searchPhotos(any(), any(), any()) }
    }

    @Test
    fun `loadNextPage increments currentPage, calls API, appends photos, updates canLoadMore`() = mainCoroutineRule.runBlockingTest {
        val query = "landscapes"
        val photosPage1 = listOf(mockPhotoDto1)
        val responsePage1 = PexelsSearchResponseDto(photos = photosPage1, totalResults = 40, page = 1, perPage = 1, nextPage = "next_page_url_page1")
        coEvery { pexelsApiService.searchPhotos(query, 1, 20) } returns Response.success(responsePage1)

        viewModel.onQueryChanged(query)
        viewModel.onSearchClicked()
        advanceUntilIdle()

        val photosPage2 = listOf(mockPhotoDto2)
        val responsePage2 = PexelsSearchResponseDto(photos = photosPage2, totalResults = 40, page = 2, perPage = 1, nextPage = null)
        coEvery { pexelsApiService.searchPhotos(query, 2, 20) } returns Response.success(responsePage2)

        viewModel.photos.test {
            viewModel.canLoadMore.test {
                assertThat(awaitItem()).isEqualTo(emptyList<com.nguyenmoclam.pexelssample.domain.model.Photo>())
                assertThat(awaitItem()).isEqualTo(photosPage1.map { it.toDomain() })
                assertThat(awaitItem()).isTrue()

                viewModel.loadNextPage()
                advanceUntilIdle()

                val currentPageField = SearchViewModel::class.java.getDeclaredField("currentPage")
                currentPageField.setAccessible(true)
                assertThat(currentPageField.getInt(viewModel)).isEqualTo(2)

                coVerify { pexelsApiService.searchPhotos(query = query, page = 2, perPage = 20) }
                assertThat(awaitItem()).isEqualTo(photosPage1.map { it.toDomain() } + photosPage2.map { it.toDomain() })
                assertThat(awaitItem()).isFalse()

                cancelAndConsumeRemainingEvents()
            }
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `loadNextPage handles API error and updates canLoadMore to false`() = mainCoroutineRule.runBlockingTest {
        val query = "error_case"
        val photosPage1 = listOf(mockPhotoDto1)
        val responsePage1 = PexelsSearchResponseDto(photos = photosPage1, totalResults = 2, page = 1, perPage = 1, nextPage = "next_page_url_exists")
        coEvery { pexelsApiService.searchPhotos(query, 1, 20) } returns Response.success(responsePage1)

        viewModel.onQueryChanged(query)
        viewModel.onSearchClicked()
        advanceUntilIdle()

        val errorResponseBody = """{"error":"Service Unavailable"}""".toResponseBody("application/json".toMediaType())
        coEvery { pexelsApiService.searchPhotos(query, 2, 20) } returns Response.error(503, errorResponseBody)

        viewModel.photos.test {
            viewModel.canLoadMore.test {
                assertThat(awaitItem()).isEqualTo(emptyList<com.nguyenmoclam.pexelssample.domain.model.Photo>())
                assertThat(awaitItem()).isEqualTo(photosPage1.map { it.toDomain() })
                assertThat(awaitItem()).isTrue()

                viewModel.loadNextPage()
                advanceUntilIdle()

                val currentPageField = SearchViewModel::class.java.getDeclaredField("currentPage")
                currentPageField.setAccessible(true)
                assertThat(currentPageField.getInt(viewModel)).isEqualTo(2)

                coVerify { pexelsApiService.searchPhotos(query = query, page = 2, perPage = 20) }
                expectNoEvents()
                assertThat(awaitItem()).isFalse()

                cancelAndConsumeRemainingEvents()
            }
            cancelAndConsumeRemainingEvents()
        }
    }
} 