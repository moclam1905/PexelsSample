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
        val expectedHistory = fullHistory
        coEvery { mockDataStoreManager.searchHistoryFlow } returns flowOf(fullHistory)

        repository.getRecentSearches(limit).test {
            Assert.assertEquals(expectedHistory, awaitItem())
            awaitComplete()
        }

        verify { mockDataStoreManager.searchHistoryFlow }
    }

    @Test
    fun `addSearchTerm adds term to beginning when list is not full, respects MAX_HISTORY_SIZE`() = runTest {
        val newTermInput = "newTerm"
        val normalizedNewTerm = newTermInput.trim().lowercase()
        val currentTerms = List(SearchHistoryKeys.MAX_HISTORY_SIZE - 1) { "oldterm${it}" }
        coEvery { mockDataStoreManager.searchHistoryFlow } returns flowOf(currentTerms)
        coEvery { mockDataStoreManager.saveSearchHistory(any()) } coAnswers { call ->
            val savedList = call.invocation.args[0] as List<String>
            Assert.assertEquals("List size should be MAX_HISTORY_SIZE", SearchHistoryKeys.MAX_HISTORY_SIZE, savedList.size)
            Assert.assertEquals("First element should be the new term", normalizedNewTerm, savedList.first())
            if (currentTerms.isNotEmpty()) {
                Assert.assertTrue("Last term from original list should still be present", savedList.contains(currentTerms.last()))
            }
        }

        repository.addSearchTerm(newTermInput)

        coVerify { mockDataStoreManager.saveSearchHistory(any()) }
    }

    @Test
    fun `addSearchTerm adds term to beginning and evicts oldest when list is full`() = runTest {
        val newTermInput = "brandNewTerm"
        val normalizedNewTerm = newTermInput.trim().lowercase()
        val initialFullTerms = List(SearchHistoryKeys.MAX_HISTORY_SIZE) { "fullterm${it}" }
        val oldestTerm = initialFullTerms.last()

        coEvery { mockDataStoreManager.searchHistoryFlow } returns flowOf(initialFullTerms)
        coEvery { mockDataStoreManager.saveSearchHistory(any()) } coAnswers { call ->
            val savedList = call.invocation.args[0] as List<String>
            Assert.assertEquals("List size should remain MAX_HISTORY_SIZE", SearchHistoryKeys.MAX_HISTORY_SIZE, savedList.size)
            Assert.assertEquals("First element should be the new term", normalizedNewTerm, savedList.first())
            Assert.assertFalse("Oldest term ('${oldestTerm}') should be evicted", savedList.contains(oldestTerm))
            if (initialFullTerms.size > 1) {
                 Assert.assertTrue("Second oldest term from original list (now last) should be present", savedList.contains(initialFullTerms[SearchHistoryKeys.MAX_HISTORY_SIZE - 2]))
            }
        }

        repository.addSearchTerm(newTermInput)

        coVerify { mockDataStoreManager.saveSearchHistory(any()) }
    }

    @Test
    fun `addSearchTerm moves existing term to beginning`() = runTest {
        val existingTermInput = "Term1"
        val normalizedExistingTerm = existingTermInput.trim().lowercase()
        val initialHistory = listOf("term2", normalizedExistingTerm, "term3")
        coEvery { mockDataStoreManager.searchHistoryFlow } returns flowOf(listOf("term2", "term1", "term3"))
        
        coEvery { mockDataStoreManager.saveSearchHistory(any()) } coAnswers { call ->
            val savedList = call.invocation.args[0] as List<String>
            Assert.assertEquals("First element should be the moved term", normalizedExistingTerm, savedList.first())
            Assert.assertEquals("List size should not change", initialHistory.size, savedList.size)
            Assert.assertEquals("Moved term should appear only once", 1, savedList.count { it == normalizedExistingTerm })
        }

        repository.addSearchTerm(existingTermInput)

        coVerify { mockDataStoreManager.saveSearchHistory(any()) }
    }

    @Test
    fun `deleteSearchTerm removes term and saves`() = runTest {
        val termToDeleteInput = "Term1"
        val normalizedTermToDelete = termToDeleteInput.trim().lowercase()
        val initialHistory = listOf("term0", normalizedTermToDelete, "term2")
        val expectedHistoryAfterDelete = listOf("term0", "term2")
        coEvery { mockDataStoreManager.searchHistoryFlow } returns flowOf(initialHistory)

        repository.deleteSearchTerm(termToDeleteInput)

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