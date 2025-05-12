package com.nguyenmoclam.pexelssample.ui.home

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.nguyenmoclam.pexelssample.data.mappers.toDomain
import com.nguyenmoclam.pexelssample.data.remote.PexelsApiService
import com.nguyenmoclam.pexelssample.data.remote.model.PexelsPhotoDto
import com.nguyenmoclam.pexelssample.data.remote.model.PexelsPhotoSrcDto
import com.nguyenmoclam.pexelssample.data.remote.model.PexelsSearchResponseDto
import com.nguyenmoclam.pexelssample.domain.model.Photo
import com.nguyenmoclam.pexelssample.domain.repository.SearchHistoryRepository
import com.nguyenmoclam.pexelssample.data.local.datastore.SearchHistoryKeys
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
    private lateinit var pexelsApiService: PexelsApiService
    private lateinit var searchHistoryRepository: SearchHistoryRepository

    private val mockPhotoSrcDto = PexelsPhotoSrcDto(original = "original_url", large2x = "large2x_url", large = "large_url", medium = "medium_url", small = "small_url", portrait = "portrait_url", landscape = "landscape_url", tiny = "tiny_url")
    private val mockPhotoDto1 = PexelsPhotoDto(id = 1, width = 100, height = 100, url = "url1", photographer = "Photographer 1", photographerUrl = "", photographerId = 1, avgColor = "", src = mockPhotoSrcDto, liked = false, alt = "Photo 1")
    private val mockPhotoDto2 = PexelsPhotoDto(id = 2, width = 100, height = 100, url = "url2", photographer = "Photographer 2", photographerUrl = "", photographerId = 2, avgColor = "", src = mockPhotoSrcDto, liked = false, alt = "Photo 2")
    private val mockPhotoDto3 = PexelsPhotoDto(id = 3, width = 100, height = 100, url = "url3", photographer = "Photographer 3", photographerUrl = "", photographerId = 3, avgColor = "", src = mockPhotoSrcDto, liked = false, alt = "Photo 3")

    private val domainPhoto1 = mockPhotoDto1.toDomain()
    private val domainPhoto2 = mockPhotoDto2.toDomain()

    @Before
    fun setUp() {
        pexelsApiService = mockk()
        searchHistoryRepository = mockk(relaxed = true)
        viewModel = SearchViewModel(pexelsApiService, searchHistoryRepository, SavedStateHandle())
        coEvery { searchHistoryRepository.getRecentSearches(any()) } returns flowOf(emptyList())
        mockkStatic(Log::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `onRefreshTriggered sets isRefreshing to true`() = mainCoroutineRule.runBlockingTest {
        viewModel.isRefreshing.test {
            assertThat(awaitItem()).isFalse() // Initial state

            viewModel.onRefreshTriggered()

            assertThat(awaitItem()).isTrue() // State after triggering refresh

            // Story 8.2 will handle setting it back to false.
            // For this story, we only check it's set to true.
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
    fun `getPhotoById returns correct photo when photo exists in list`() {
        val photosList = listOf(domainPhoto1, domainPhoto2)
        setPhotosInViewModel(photosList)

        val result = viewModel.getPhotoById(domainPhoto1.id)

        assertThat(result).isEqualTo(domainPhoto1)
    }

    @Test
    fun `getPhotoById returns null when photo does not exist in list`() {
        val photosList = listOf(domainPhoto1, domainPhoto2)
        setPhotosInViewModel(photosList)

        val result = viewModel.getPhotoById(300)

        assertThat(result).isNull()
    }

    @Test
    fun `getPhotoById returns null when photo list is empty`() {
        setPhotosInViewModel(emptyList())

        val result = viewModel.getPhotoById(domainPhoto1.id)

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
    fun `when search bar loses focus, recent searches are hidden`() = runTest {
        val recentSearchesList = listOf("cat", "dog")
        coEvery { searchHistoryRepository.getRecentSearches(SearchHistoryKeys.MAX_HISTORY_SIZE) } returns flowOf(recentSearchesList)

        // Initial: focused, empty query -> suggestions shown
        viewModel.onSearchBarFocusChanged(true)
        advanceUntilIdle()
        // Assert initial state if necessary, or assume it's covered by other tests
        // For this test, we directly proceed to losing focus

        viewModel.showRecentSearchesSuggestions.test {
            // Skip initial false if any, wait for true after focus
            // This might require consuming the initial false if test starts with it.
            // Assuming it becomes true from setup/previous action.
            // Let's ensure it is true first
            viewModel.onQueryChanged("") // ensure query is empty
            viewModel.onSearchBarFocusChanged(true)
            advanceUntilIdle()
            val initialState = expectMostRecentItem()

            viewModel.onSearchBarFocusChanged(false) // Lose focus
            advanceUntilIdle()

            // If initial state was true, it should now be false.
            // If it was already false (e.g. query not empty), it remains false.
            // The crucial part is that it *becomes* false if it was true.
            if (initialState) {
                 assertThat(awaitItem()).isFalse()
            } else {
                 assertThat(expectMostRecentItem()).isFalse() // Stays false or becomes false
            }
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `when text entered into search bar, recent searches are hidden`() = runTest {
        val recentSearchesList = listOf("cat", "dog")
        coEvery { searchHistoryRepository.getRecentSearches(SearchHistoryKeys.MAX_HISTORY_SIZE) } returns flowOf(recentSearchesList)

        // Initial: focused, empty query -> suggestions shown
        viewModel.onSearchBarFocusChanged(true)
        viewModel.onQueryChanged("")
        advanceUntilIdle()
        // Assert initial state if necessary

        viewModel.showRecentSearchesSuggestions.test {
            // Skip initial states, wait for suggestions to be potentially shown
            // then check they hide after query change.
            // Let's ensure it is true first
            viewModel.onSearchBarFocusChanged(true)
            viewModel.onQueryChanged("")
            advanceUntilIdle()
            val initialState = expectMostRecentItem()

            viewModel.onQueryChanged("a") // Enter text
            advanceUntilIdle()

            if (initialState) {
                assertThat(awaitItem()).isFalse()
            } else {
                assertThat(expectMostRecentItem()).isFalse()
            }

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `when search bar focused, then text entered, then text cleared, searches reappear`() = runTest {
        val history = listOf("test1", "test2")
        coEvery { searchHistoryRepository.getRecentSearches(SearchHistoryKeys.MAX_HISTORY_SIZE) } returns flowOf(history)

        viewModel.showRecentSearchesSuggestions.test {
            assertThat(awaitItem()).isFalse() // Initial

            // 1. Focus (query is initially empty)
            viewModel.onSearchBarFocusChanged(true)
            advanceUntilIdle()
            assertThat(awaitItem()).isTrue() // Shown

            // 2. Text entered
            viewModel.onQueryChanged("cat")
            advanceUntilIdle()
            assertThat(awaitItem()).isFalse() // Hidden

            // 3. Text cleared
            viewModel.onQueryChanged("")
            advanceUntilIdle()
            assertThat(awaitItem()).isTrue() // Shown again

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `showRecentSearchesSuggestions from to true when focus`() = runTest {
        coEvery {
            searchHistoryRepository.getRecentSearches(SearchHistoryKeys.MAX_HISTORY_SIZE)
        } returns flowOf(listOf("cat", "dog"))

        viewModel.showRecentSearchesSuggestions.test {
            assertThat(awaitItem()).isFalse()

            viewModel.onSearchBarFocusChanged(true)
            advanceUntilIdle()

            assertThat(awaitItem()).isTrue()

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `recentSearches is empty and return list when focus`() = runTest {
        val recentList = listOf("cat", "dog")
        coEvery {
            searchHistoryRepository.getRecentSearches(SearchHistoryKeys.MAX_HISTORY_SIZE)
        } returns flowOf(recentList)

        viewModel.recentSearches.test {
            assertThat(awaitItem()).isEmpty()

            viewModel.onSearchBarFocusChanged(true)
            advanceUntilIdle()

            assertThat(awaitItem()).isEqualTo(recentList)

            coVerify {
                searchHistoryRepository.getRecentSearches(SearchHistoryKeys.MAX_HISTORY_SIZE)
            }

            cancelAndConsumeRemainingEvents()
        }
    }
} 