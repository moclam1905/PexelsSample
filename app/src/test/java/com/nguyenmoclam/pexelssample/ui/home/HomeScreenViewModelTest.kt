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
        // ViewModel will be initialized here, triggering init block
    }

    @After
    fun tearDown() {
        // Clean up if needed
    }

    @Test
    fun `init loads initial photos successfully`() = runTest {
        // Arrange
        val dummyPhotos = listOf(createDummyPhoto(1), createDummyPhoto(2))
        val successResult = PhotosResult.Success(photos = dummyPhotos, totalResults = 2, canLoadMore = false)
        coEvery { imageRepository.getCuratedPhotos(page = 1, perPage = any()) } returns successResult

        // Act
        viewModel = HomeScreenViewModel(imageRepository) // Initialize ViewModel AFTER mocking

        // Assert
        // runTest ensures coroutines launched in viewModelScope complete before assertions
        assertThat(viewModel.isLoadingInitial.value).isFalse() // Check final loading state
        assertThat(viewModel.photos.value).isEqualTo(dummyPhotos) // Check final photos state

        coVerify(exactly = 1) { imageRepository.getCuratedPhotos(page = 1, perPage = any()) }
    }

    @Test
    fun `init handles repository error during initial load`() = runTest {
        // Arrange
        val errorResult = PhotosResult.Error(message = "Network Error", isRetryable = true)
        coEvery { imageRepository.getCuratedPhotos(page = 1, perPage = any()) } returns errorResult

        // Act
        viewModel = HomeScreenViewModel(imageRepository)

        // Assert
        assertThat(viewModel.isLoadingInitial.value).isFalse()
        assertThat(viewModel.photos.value).isEmpty() // Photos should remain/be empty on error

        coVerify(exactly = 1) { imageRepository.getCuratedPhotos(page = 1, perPage = any()) }
        // Assert error state handling here once implemented in ViewModel (Story 10.6)
    }

    @Test
    fun `init handles exception during initial load`() = runTest {
        // Arrange
        val exception = RuntimeException("Something went wrong")
        coEvery { imageRepository.getCuratedPhotos(page = 1, perPage = any()) } throws exception

        // Act
        viewModel = HomeScreenViewModel(imageRepository)

        // Assert
        assertThat(viewModel.isLoadingInitial.value).isFalse()
        assertThat(viewModel.photos.value).isEmpty() // Photos should remain/be empty on exception

        coVerify(exactly = 1) { imageRepository.getCuratedPhotos(page = 1, perPage = any()) }
        // Assert error state handling here once implemented in ViewModel (Story 10.6)
    }

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