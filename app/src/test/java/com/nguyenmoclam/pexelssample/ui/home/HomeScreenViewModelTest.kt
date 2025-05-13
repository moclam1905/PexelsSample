package com.nguyenmoclam.pexelssample.ui.home

import com.google.common.truth.Truth.assertThat
import com.nguyenmoclam.pexelssample.domain.model.Photo
import com.nguyenmoclam.pexelssample.domain.model.PhotoSrc
import com.nguyenmoclam.pexelssample.domain.model.PhotosResult
import com.nguyenmoclam.pexelssample.domain.repository.ImageRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class HomeScreenViewModelTest {

    // Rule to swap the main dispatcher with a test dispatcher
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: HomeScreenViewModel
    private val imageRepository: ImageRepository = mockk()

    // Helper function to create dummy photos
    private fun createDummyPhoto(id: Int): Photo = Photo(
        id = id,
        width = 100,
        height = 150,
        url = "url_$id",
        photographer = "Photographer $id",
        photographerUrl = "photographer_url_$id",
        photographerId = id.toLong(),
        avgColor = "#FFFFFF",
        src = PhotoSrc(
            original = "original_$id",
            large2x = "large2x_$id",
            large = "large_$id",
            medium = "medium_$id",
            small = "small_$id",
            portrait = "portrait_$id",
            landscape = "landscape_$id",
            tiny = "tiny_$id"
        ),
        alt = "Alt text $id"
    )

    @Before
    fun setUp() {
        // Provide the mocked repository before each test
        // ViewModel will be initialized in each test *after* mocking specific responses
    }

    @After
    fun tearDown() {
        // Clean up if needed
    }

    @Test
    fun `init - loads initial photos successfully - sets Content state`() = runTest {
        // Arrange
        val dummyPhotos = listOf(createDummyPhoto(1), createDummyPhoto(2))
        val successResult = PhotosResult.Success(photos = dummyPhotos, totalResults = 2, canLoadMore = false, nextPageUrl = "next_page_url")
        coEvery { imageRepository.getCuratedPhotos(page = 1, perPage = ITEMS_PER_PAGE) } returns successResult

        // Act
        viewModel = HomeScreenViewModel(imageRepository) // Initialize ViewModel AFTER mocking
        val finalState = viewModel.uiState.value // Get the state after init

        // Assert
        assertThat(finalState).isInstanceOf(HomeScreenUiState.Content::class.java)
        val contentState = finalState as HomeScreenUiState.Content
        assertThat(contentState.photos).isEqualTo(dummyPhotos)
        assertThat(viewModel.nextPageUrl.value).isEqualTo("next_page_url") // Check if next page URL is set

        coVerify(exactly = 1) { imageRepository.getCuratedPhotos(page = 1, perPage = ITEMS_PER_PAGE) }
    }

    @Test
    fun `init - loads initial photos successfully with empty list - sets Empty state`() = runTest {
        // Arrange
        val emptyPhotos = emptyList<Photo>()
        val successResult = PhotosResult.Success(photos = emptyPhotos, totalResults = 0, canLoadMore = false, nextPageUrl = null)
        coEvery { imageRepository.getCuratedPhotos(page = 1, perPage = ITEMS_PER_PAGE) } returns successResult

        // Act
        viewModel = HomeScreenViewModel(imageRepository)
        val finalState = viewModel.uiState.value

        // Assert
        assertThat(finalState).isEqualTo(HomeScreenUiState.Empty)
        assertThat(viewModel.nextPageUrl.value).isNull() // Should be null if empty

        coVerify(exactly = 1) { imageRepository.getCuratedPhotos(page = 1, perPage = ITEMS_PER_PAGE) }
    }

    @Test
    fun `init - handles repository error during initial load - sets Error state`() = runTest {
        // Arrange
        val errorMessage = "Network Error"
        val errorResult = PhotosResult.Error(message = errorMessage, isRetryable = true)
        coEvery { imageRepository.getCuratedPhotos(page = 1, perPage = ITEMS_PER_PAGE) } returns errorResult

        // Act
        viewModel = HomeScreenViewModel(imageRepository)
        val finalState = viewModel.uiState.value

        // Assert
        assertThat(finalState).isInstanceOf(HomeScreenUiState.Error::class.java)
        val errorState = finalState as HomeScreenUiState.Error
        assertThat(errorState.errorDetails.message).contains(errorMessage)
        assertThat(errorState.errorDetails.isRetryable).isTrue()
        assertThat(viewModel.nextPageUrl.value).isNull()

        coVerify(exactly = 1) { imageRepository.getCuratedPhotos(page = 1, perPage = ITEMS_PER_PAGE) }
    }

    @Test
    fun `init - handles exception during initial load - sets Error state`() = runTest {
        // Arrange
        val exceptionMessage = "Something went wrong"
        val exception = RuntimeException(exceptionMessage)
        coEvery { imageRepository.getCuratedPhotos(page = 1, perPage = ITEMS_PER_PAGE) } throws exception

        // Act
        viewModel = HomeScreenViewModel(imageRepository)
        val finalState = viewModel.uiState.value

        // Assert
        assertThat(finalState).isInstanceOf(HomeScreenUiState.Error::class.java)
        val errorState = finalState as HomeScreenUiState.Error
        assertThat(errorState.errorDetails.message).contains("unexpected error") // Check generic message
        assertThat(errorState.errorDetails.isRetryable).isTrue()
        assertThat(viewModel.nextPageUrl.value).isNull()

        coVerify(exactly = 1) { imageRepository.getCuratedPhotos(page = 1, perPage = ITEMS_PER_PAGE) }
    }

    @Test
    fun `fetchInitialPhotos called from Error state - retries successfully`() = runTest {
        // Arrange: Setup initial error state
        val errorMessage = "Initial Network Error"
        val errorResult = PhotosResult.Error(message = errorMessage, isRetryable = true)
        coEvery { imageRepository.getCuratedPhotos(page = 1, perPage = ITEMS_PER_PAGE) } returns errorResult
        viewModel = HomeScreenViewModel(imageRepository)
        assertThat(viewModel.uiState.value).isInstanceOf(HomeScreenUiState.Error::class.java)

        // Arrange: Mock successful retry response
        val retryPhotos = listOf(createDummyPhoto(3))
        val successRetryResult = PhotosResult.Success(photos = retryPhotos, totalResults = 1, canLoadMore = false, nextPageUrl = "retry_next_page")
        coEvery { imageRepository.getCuratedPhotos(page = 1, perPage = ITEMS_PER_PAGE) } returns successRetryResult

        // Act: Call retry function
        viewModel.fetchInitialPhotos()
        val finalState = viewModel.uiState.value

        // Assert: Check final state is Content
        assertThat(finalState).isInstanceOf(HomeScreenUiState.Content::class.java)
        val contentState = finalState as HomeScreenUiState.Content
        assertThat(contentState.photos).isEqualTo(retryPhotos)
        assertThat(viewModel.nextPageUrl.value).isEqualTo("retry_next_page")

        // Verify repository called twice (initial load + retry)
        coVerify(exactly = 2) { imageRepository.getCuratedPhotos(page = 1, perPage = ITEMS_PER_PAGE) }
    }

    @Test
    fun `onManualRefreshTriggered - success - sets Loading then Content state`() = runTest {
        // Arrange: Setup initial Content state (optional, could start from any non-loading state)
        val initialPhotos = listOf(createDummyPhoto(1))
        val initialSuccess = PhotosResult.Success(initialPhotos, 1, false, "initial_url")
        coEvery { imageRepository.getCuratedPhotos(page = 1, perPage = ITEMS_PER_PAGE) } returns initialSuccess
        viewModel = HomeScreenViewModel(imageRepository)
        assertThat(viewModel.uiState.value).isInstanceOf(HomeScreenUiState.Content::class.java)

        // Arrange: Mock successful refresh response
        val refreshedPhotos = listOf(createDummyPhoto(10), createDummyPhoto(11))
        val refreshSuccess = PhotosResult.Success(refreshedPhotos, 2, false, "refresh_url")
        // Make sure the mock is set for the *next* call
        coEvery { imageRepository.getCuratedPhotos(page = 1, perPage = ITEMS_PER_PAGE) } returns refreshSuccess

        // Act
        viewModel.onManualRefreshTriggered()
        // Note: State might briefly be InitialLoading during the refresh
        val finalState = viewModel.uiState.value // Get state after refresh completes

        // Assert
        assertThat(viewModel.isRefreshingManual.value).isFalse() // Refreshing flag should be reset
        assertThat(finalState).isInstanceOf(HomeScreenUiState.Content::class.java)
        val contentState = finalState as HomeScreenUiState.Content
        assertThat(contentState.photos).isEqualTo(refreshedPhotos)
        assertThat(viewModel.nextPageUrl.value).isEqualTo("refresh_url")

        coVerify(exactly = 2) { imageRepository.getCuratedPhotos(page = 1, perPage = ITEMS_PER_PAGE) } // Initial + Refresh
    }

    @Test
    fun `onManualRefreshTriggered - empty result - sets Loading then Empty state`() = runTest {
        // Arrange: Start with Content
        val initialPhotos = listOf(createDummyPhoto(1))
        val initialSuccess = PhotosResult.Success(initialPhotos, 1, false, "initial_url")
        coEvery { imageRepository.getCuratedPhotos(page = 1, perPage = ITEMS_PER_PAGE) } returns initialSuccess
        viewModel = HomeScreenViewModel(imageRepository)
        assertThat(viewModel.uiState.value).isInstanceOf(HomeScreenUiState.Content::class.java)

        // Arrange: Mock empty refresh response
        val emptyResult = PhotosResult.Success(emptyList(), 0, false, null)
        coEvery { imageRepository.getCuratedPhotos(page = 1, perPage = ITEMS_PER_PAGE) } returns emptyResult

        // Act
        viewModel.onManualRefreshTriggered()
        val finalState = viewModel.uiState.value

        // Assert
        assertThat(viewModel.isRefreshingManual.value).isFalse()
        assertThat(finalState).isEqualTo(HomeScreenUiState.Empty)
        assertThat(viewModel.nextPageUrl.value).isNull()

        coVerify(exactly = 2) { imageRepository.getCuratedPhotos(page = 1, perPage = ITEMS_PER_PAGE) }
    }

    @Test
    fun `onManualRefreshTriggered - error - sets Loading then Error state`() = runTest {
        // Arrange: Start with Content
        val initialPhotos = listOf(createDummyPhoto(1))
        val initialSuccess = PhotosResult.Success(initialPhotos, 1, false, "initial_url")
        coEvery { imageRepository.getCuratedPhotos(page = 1, perPage = ITEMS_PER_PAGE) } returns initialSuccess
        viewModel = HomeScreenViewModel(imageRepository)
        assertThat(viewModel.uiState.value).isInstanceOf(HomeScreenUiState.Content::class.java)

        // Arrange: Mock error refresh response
        val errorMsg = "Refresh Failed"
        val errorResult = PhotosResult.Error(errorMsg, true)
        coEvery { imageRepository.getCuratedPhotos(page = 1, perPage = ITEMS_PER_PAGE) } returns errorResult

        // Act
        viewModel.onManualRefreshTriggered()
        val finalState = viewModel.uiState.value

        // Assert
        assertThat(viewModel.isRefreshingManual.value).isFalse()
        assertThat(finalState).isInstanceOf(HomeScreenUiState.Error::class.java)
        val errorState = finalState as HomeScreenUiState.Error
        assertThat(errorState.errorDetails.message).contains(errorMsg)
        assertThat(errorState.errorDetails.isRetryable).isTrue()
        assertThat(viewModel.nextPageUrl.value).isNull()

        coVerify(exactly = 2) { imageRepository.getCuratedPhotos(page = 1, perPage = ITEMS_PER_PAGE) }
    }

    // TODO: Add tests for loadMorePhotos behavior with HomeScreenUiState.Content if needed

}

// Reusable rule for setting the main dispatcher
@ExperimentalCoroutinesApi
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : org.junit.rules.TestWatcher() {
    override fun starting(description: org.junit.runner.Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: org.junit.runner.Description) {
        Dispatchers.resetMain()
    }
} 