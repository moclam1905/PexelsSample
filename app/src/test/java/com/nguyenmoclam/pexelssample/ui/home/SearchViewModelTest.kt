package com.nguyenmoclam.pexelssample.ui.home

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.nguyenmoclam.pexelssample.data.local.datastore.SearchHistoryKeys
import com.nguyenmoclam.pexelssample.domain.model.Photo
import com.nguyenmoclam.pexelssample.domain.model.PhotoSrc
import com.nguyenmoclam.pexelssample.domain.model.PhotosResult
import com.nguyenmoclam.pexelssample.domain.repository.ImageRepository
import com.nguyenmoclam.pexelssample.domain.repository.SearchHistoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.lang.reflect.Field

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
    private lateinit var imageRepository: ImageRepository
    private lateinit var searchHistoryRepository: SearchHistoryRepository

    private val mockPhotoSrc = PhotoSrc(original = "original_url", large2x = "large2x_url", large = "large_url", medium = "medium_url", small = "small_url", portrait = "portrait_url", landscape = "landscape_url", tiny = "tiny_url")
    private val mockPhoto1 = Photo(id = 1, width = 100, height = 100, url = "url1", photographer = "Photographer 1", photographerUrl = "", photographerId = 1, avgColor = "", src = mockPhotoSrc, alt = "Photo 1")
    private val mockPhoto2 = Photo(id = 2, width = 100, height = 100, url = "url2", photographer = "Photographer 2", photographerUrl = "", photographerId = 2, avgColor = "", src = mockPhotoSrc, alt = "Photo 2")

    @Before
    fun setUp() {
        imageRepository = mockk()
        searchHistoryRepository = mockk(relaxed = true)
        viewModel = SearchViewModel(imageRepository, searchHistoryRepository, SavedStateHandle())
        coEvery { searchHistoryRepository.getRecentSearches(any()) } returns flowOf(emptyList())
        mockkStatic(Log::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `onRefreshTriggered sets isRefreshing to true`() = mainCoroutineRule.runBlockingTest {
        coEvery { imageRepository.getCuratedPhotos(1, 20) } returns PhotosResult.Success(emptyList(), 0, false, null)

        viewModel.isRefreshing.test {
            assertThat(awaitItem()).isFalse()

            viewModel.onRefreshTriggered()

            assertThat(awaitItem()).isTrue()
            advanceUntilIdle()
            assertThat(awaitItem()).isFalse()

            cancelAndConsumeRemainingEvents()
        }
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
    fun `onSearchClicked with non-blank query calls imageRepository searchPhotos`() = mainCoroutineRule.runBlockingTest {
        val query = "nature"
        val mockSuccessResult = PhotosResult.Success(photos = emptyList(), totalResults = 0, canLoadMore = false, nextPageUrl = null)
        coEvery { imageRepository.searchPhotos(query, 1, 20) } returns mockSuccessResult

        viewModel.onQueryChanged(query)
        viewModel.onSearchClicked()
        advanceUntilIdle()

        coVerify { imageRepository.searchPhotos(query = query, page = 1, perPage = 20) }
    }

    @Test
    fun `onSearchClicked with blank query does not call imageRepository`() = mainCoroutineRule.runBlockingTest {
        viewModel.onQueryChanged(" ")
        viewModel.onSearchClicked()
        advanceUntilIdle()

        coVerify(exactly = 0) { imageRepository.searchPhotos(any(), any(), any()) }
    }

    @Test
    fun `onSearchClicked with non-blank query success, isLoading is true then false`() = mainCoroutineRule.runBlockingTest {
        val query = "nature"
        val mockSuccessResult = PhotosResult.Success(emptyList(), 0, false, null)
        coEvery { imageRepository.searchPhotos(query, 1, 20) } returns mockSuccessResult

        viewModel.onQueryChanged(query)

        viewModel.isLoading.test {
            assertThat(awaitItem()).isEqualTo(false)

            viewModel.onSearchClicked()

            assertThat(awaitItem()).isEqualTo(true)
            assertThat(awaitItem()).isEqualTo(false)

            cancelAndConsumeRemainingEvents()
        }
        coVerify { imageRepository.searchPhotos(query, 1, 20) }
    }

    @Test
    fun `onSearchClicked with non-blank query failure, isLoading is true then false`() = mainCoroutineRule.runBlockingTest {
        val query = "error"
        val mockErrorResult = PhotosResult.Error("Repo error", true)
        coEvery { imageRepository.searchPhotos(query, 1, 20) } returns mockErrorResult

        viewModel.onQueryChanged(query)

        viewModel.isLoading.test {
            assertThat(awaitItem()).isEqualTo(false)

            viewModel.onSearchClicked()

            assertThat(awaitItem()).isEqualTo(true)
            assertThat(awaitItem()).isEqualTo(false)

            cancelAndConsumeRemainingEvents()
        }
        coVerify { imageRepository.searchPhotos(query, 1, 20) }
    }

    @Test
    fun `onSearchClicked with blank query, isLoading remains false and no repo call`() = mainCoroutineRule.runBlockingTest {
        val query = " "
        viewModel.onQueryChanged(query)

        viewModel.isLoading.test {
            assertThat(awaitItem()).isEqualTo(false)

            viewModel.onSearchClicked()

            expectNoEvents()
        }
        coVerify(exactly = 0) { imageRepository.searchPhotos(any(), any(), any()) }
    }

    @Test
    fun `photos are reset and updated correctly after onSearchClicked`() = runTest {
        val query = "nature"
        val photosPage1 = listOf(mockPhoto1)
        val successResult = PhotosResult.Success(
            photos = photosPage1,
            totalResults = 10,
            canLoadMore = true,
            null
        )

        coEvery { imageRepository.searchPhotos(query, 1, 20) } returns successResult

        viewModel.onQueryChanged(query)
        viewModel.onSearchClicked()
        advanceUntilIdle()

        viewModel.photos.test {
            assertThat(awaitItem()).isEqualTo(photosPage1)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `canLoadMore is updated correctly after onSearchClicked`() = runTest {
        val query = "nature"
        val successResult = PhotosResult.Success(
            photos = listOf(mockPhoto1),
            totalResults = 10,
            canLoadMore = true,
            null
        )
        coEvery { imageRepository.searchPhotos(query, 1, 20) } returns successResult

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
        isLoadingField.isAccessible = true
        (isLoadingField.get(viewModel) as MutableStateFlow<Boolean>).value = true

        val canLoadMoreField = SearchViewModel::class.java.getDeclaredField("_canLoadMore")
        canLoadMoreField.isAccessible = true
        (canLoadMoreField.get(viewModel) as MutableStateFlow<Boolean>).value = true

        viewModel.loadNextPage()
        advanceUntilIdle()

        coVerify(exactly = 0) { imageRepository.searchPhotos(any(), any(), any()) }
    }

    @Test
    fun `loadNextPage does nothing if canLoadMore is false`() = mainCoroutineRule.runBlockingTest {
        viewModel.onQueryChanged("test")

        val canLoadMoreField = SearchViewModel::class.java.getDeclaredField("_canLoadMore")
        canLoadMoreField.isAccessible = true
        (canLoadMoreField.get(viewModel) as MutableStateFlow<Boolean>).value = false

        viewModel.loadNextPage()
        advanceUntilIdle()

        coVerify(exactly = 0) { imageRepository.searchPhotos(any(), any(), any()) }
    }

    @Test
    fun `getPhotoById returns correct photo when photo exists in list`() {
        val photosList = listOf(mockPhoto1, mockPhoto2)
        setPhotosInViewModel(photosList)

        val result = viewModel.getPhotoById(mockPhoto1.id)

        assertThat(result).isEqualTo(mockPhoto1)
    }

    @Test
    fun `getPhotoById returns null when photo does not exist in list`() {
        val photosList = listOf(mockPhoto1, mockPhoto2)
        setPhotosInViewModel(photosList)

        val result = viewModel.getPhotoById(300)

        assertThat(result).isNull()
    }

    @Test
    fun `getPhotoById returns null when photo list is empty`() {
        setPhotosInViewModel(emptyList())

        val result = viewModel.getPhotoById(mockPhoto1.id)

        assertThat(result).isNull()
    }

    private fun setPhotosInViewModel(photos: List<Photo>) {
        try {
            val field: Field = SearchViewModel::class.java.getDeclaredField("_photos")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val mutableStateFlow = field.get(viewModel) as MutableStateFlow<List<Photo>>
            mutableStateFlow.value = photos
        } catch (e: Exception) {
            throw RuntimeException("Failed to set _photos field in SearchViewModel via reflection", e)
        }
    }

    @Test
    fun `when search bar focused, then text entered, then text cleared, searches reappear`() = runTest {
        val history = listOf("test1", "test2")
        coEvery { searchHistoryRepository.getRecentSearches(SearchHistoryKeys.MAX_HISTORY_SIZE) } returns flowOf(history)

        viewModel.showRecentSearchesSuggestions.test {
            assertThat(awaitItem()).isFalse()

            viewModel.onQueryChanged("")
            viewModel.onSearchBarFocusChanged(true)
            advanceUntilIdle()
            assertThat(awaitItem()).isTrue()

            viewModel.onQueryChanged("cat")
            advanceUntilIdle()
            assertThat(awaitItem()).isFalse()

            viewModel.onQueryChanged("")
            advanceUntilIdle()
            assertThat(awaitItem()).isTrue()

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `showRecentSearchesSuggestions transitions correctly on focus and query changes`() = runTest {
        val recentSearchesList = listOf("cat", "dog")
        coEvery { searchHistoryRepository.getRecentSearches(SearchHistoryKeys.MAX_HISTORY_SIZE) } returns flowOf(recentSearchesList)

        viewModel.showRecentSearchesSuggestions.test {
            assertThat(awaitItem()).isFalse()

            viewModel.onQueryChanged("")
            viewModel.onSearchBarFocusChanged(true)
            advanceUntilIdle()
            assertThat(awaitItem()).isTrue()

            viewModel.onSearchBarFocusChanged(false)
            advanceUntilIdle()
            assertThat(awaitItem()).isFalse()

            viewModel.onSearchBarFocusChanged(true)
            advanceUntilIdle()
            assertThat(awaitItem()).isTrue()

            viewModel.onQueryChanged("test")
            advanceUntilIdle()
            assertThat(awaitItem()).isFalse()

            viewModel.onQueryChanged("")
            advanceUntilIdle()
            assertThat(awaitItem()).isTrue()

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `recentSearches updates when search bar focused with empty query`() = runTest {
        val recentList = listOf("cat", "dog")
        coEvery { searchHistoryRepository.getRecentSearches(SearchHistoryKeys.MAX_HISTORY_SIZE) } returns flowOf(emptyList())

        viewModel.recentSearches.test {
            assertThat(awaitItem()).isEmpty()

            coEvery { searchHistoryRepository.getRecentSearches(SearchHistoryKeys.MAX_HISTORY_SIZE) } returns flowOf(recentList)

            viewModel.onQueryChanged("")
            viewModel.onSearchBarFocusChanged(true)
            advanceUntilIdle()

            assertThat(awaitItem()).isEqualTo(recentList)

            coVerify { searchHistoryRepository.getRecentSearches(SearchHistoryKeys.MAX_HISTORY_SIZE) }
            cancelAndConsumeRemainingEvents()
        }
    }
} 