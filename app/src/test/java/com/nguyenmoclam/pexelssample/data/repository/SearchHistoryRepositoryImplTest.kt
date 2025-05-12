package com.nguyenmoclam.pexelssample.data.repository

import app.cash.turbine.test
import com.nguyenmoclam.pexelssample.data.local.datastore.SearchHistoryDataStoreManager
import com.nguyenmoclam.pexelssample.data.local.datastore.SearchHistoryKeys
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SearchHistoryRepositoryImplTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @RelaxedMockK
    lateinit var mockDataStoreManager: SearchHistoryDataStoreManager

    private lateinit var repository: SearchHistoryRepositoryImpl

    @Before
    fun setUp() {
        repository = SearchHistoryRepositoryImpl(mockDataStoreManager)
    }

    @Test
    fun `getRecentSearches returns limited list from dataStoreManager`() = runTest {
        val fullHistory = listOf("term1", "term2", "term3")
        val limit = 2
        val expectedHistory = listOf("term1", "term2")
        coEvery { mockDataStoreManager.searchHistoryFlow } returns flowOf(fullHistory)

        repository.getRecentSearches(limit).test {
            Assert.assertEquals(expectedHistory, awaitItem())
            awaitComplete()
        }

        verify { mockDataStoreManager.searchHistoryFlow }
    }

    @Test
    fun `addSearchTerm adds term to beginning when list is not full, respects MAX_HISTORY_SIZE`() = runTest {
        val newTerm = "newTerm"
        val currentTerms = List(SearchHistoryKeys.MAX_HISTORY_SIZE - 1) { "oldTerm$it" }
        coEvery { mockDataStoreManager.searchHistoryFlow } returns flowOf(currentTerms)
        coEvery { mockDataStoreManager.saveSearchHistory(any()) } coAnswers { call ->
            val savedList = call.invocation.args[0] as List<String>
            Assert.assertEquals(SearchHistoryKeys.MAX_HISTORY_SIZE, savedList.size)
            Assert.assertEquals(newTerm, savedList.first())
            if (currentTerms.isNotEmpty()) {
                Assert.assertTrue("Last term from original list should still be present", savedList.contains(currentTerms.last()))
            }
        }

        repository.addSearchTerm(newTerm)

        coVerify { mockDataStoreManager.saveSearchHistory(any()) }
    }

    @Test
    fun `addSearchTerm adds term to beginning and evicts oldest when list is full`() = runTest {
        val newTerm = "brandNewTerm"
        val initialFullTerms = List(SearchHistoryKeys.MAX_HISTORY_SIZE) { "fullTerm$it" }
        val oldestTerm = initialFullTerms.last()

        coEvery { mockDataStoreManager.searchHistoryFlow } returns flowOf(initialFullTerms)
        coEvery { mockDataStoreManager.saveSearchHistory(any()) } coAnswers { call ->
            val savedList = call.invocation.args[0] as List<String>
            Assert.assertEquals(SearchHistoryKeys.MAX_HISTORY_SIZE, savedList.size)
            Assert.assertEquals(newTerm, savedList.first())
            Assert.assertFalse("Oldest term should be evicted", savedList.contains(oldestTerm))
            if (initialFullTerms.size > 1) {
                 Assert.assertTrue("Second oldest term (now last) should be present", savedList.contains(initialFullTerms[SearchHistoryKeys.MAX_HISTORY_SIZE - 2]))
            }
        }

        repository.addSearchTerm(newTerm)

        coVerify { mockDataStoreManager.saveSearchHistory(any()) }
    }

    @Test
    fun `addSearchTerm moves existing term to beginning`() = runTest {
        val existingTerm = "term1"
        val initialHistory = listOf("term2", existingTerm, "term3")
        coEvery { mockDataStoreManager.searchHistoryFlow } returns flowOf(initialHistory)
        coEvery { mockDataStoreManager.saveSearchHistory(any()) } coAnswers { call ->
            val savedList = call.invocation.args[0] as List<String>
            Assert.assertEquals(existingTerm, savedList.first())
            Assert.assertEquals(initialHistory.size, savedList.size)
        }

        repository.addSearchTerm(existingTerm)

        coVerify { mockDataStoreManager.saveSearchHistory(any()) }
    }

    @Test
    fun `deleteSearchTerm removes term and saves`() = runTest {
        val termToDelete = "term1"
        val initialHistory = listOf("term0", termToDelete, "term2")
        val expectedHistoryAfterDelete = listOf("term0", "term2")
        coEvery { mockDataStoreManager.searchHistoryFlow } returns flowOf(initialHistory)

        repository.deleteSearchTerm(termToDelete)

        coVerify { mockDataStoreManager.saveSearchHistory(expectedHistoryAfterDelete) }
    }

    @Test
    fun `deleteSearchTerm does nothing if term not found`() = runTest {
        val termToDelete = "nonExistentTerm"
        val initialHistory = listOf("term0", "term1")
        coEvery { mockDataStoreManager.searchHistoryFlow } returns flowOf(initialHistory)

        repository.deleteSearchTerm(termToDelete)

        coVerify(exactly = 0) { mockDataStoreManager.saveSearchHistory(any()) }
    }

    @Test
    fun `clearSearchHistory saves empty list`() = runTest {
        coEvery { mockDataStoreManager.searchHistoryFlow } returns flowOf(listOf("term1", "term2"))

        repository.clearSearchHistory()

        coVerify { mockDataStoreManager.saveSearchHistory(emptyList()) }
    }
} 