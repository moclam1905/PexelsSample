package com.nguyenmoclam.pexelssample.data.repository

import com.nguyenmoclam.pexelssample.data.local.datastore.SearchHistoryDataStoreManager
import com.nguyenmoclam.pexelssample.data.local.datastore.SearchHistoryKeys.MAX_HISTORY_SIZE
import com.nguyenmoclam.pexelssample.domain.repository.SearchHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchHistoryRepositoryImpl @Inject constructor(
    private val dataStoreManager: SearchHistoryDataStoreManager
) : SearchHistoryRepository {

    override fun getRecentSearches(limit: Int): Flow<List<String>> {
        return dataStoreManager.searchHistoryFlow.map { history ->
            history.take(limit)
        }
    }

    override suspend fun addSearchTerm(term: String) {
        val currentHistory = dataStoreManager.searchHistoryFlow.first().toMutableList()
        currentHistory.remove(term) // Remove if exists to add it to the top
        currentHistory.add(0, term) // Add to the beginning
        val updatedHistory = currentHistory.take(MAX_HISTORY_SIZE) // Enforce max size
        dataStoreManager.saveSearchHistory(updatedHistory)
    }

    override suspend fun deleteSearchTerm(term: String) {
        val currentHistory = dataStoreManager.searchHistoryFlow.first().toMutableList()
        if (currentHistory.remove(term)) {
            dataStoreManager.saveSearchHistory(currentHistory)
        }
    }

    override suspend fun clearSearchHistory() {
        dataStoreManager.saveSearchHistory(emptyList())
    }
} 