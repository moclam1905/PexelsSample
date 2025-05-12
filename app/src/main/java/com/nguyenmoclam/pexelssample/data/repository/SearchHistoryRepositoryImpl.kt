package com.nguyenmoclam.pexelssample.data.repository

import com.nguyenmoclam.pexelssample.data.local.datastore.SearchHistoryDataStoreManager
import com.nguyenmoclam.pexelssample.data.local.datastore.SearchHistoryKeys
import com.nguyenmoclam.pexelssample.domain.repository.SearchHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchHistoryRepositoryImpl @Inject constructor(
    private val dataStoreManager: SearchHistoryDataStoreManager
) : SearchHistoryRepository {

    override fun getRecentSearches(limit: Int): Flow<List<String>> {
        // As per story clarification, this method should return the full stored list
        // (which is already capped at MAX_HISTORY_SIZE by addSearchTerm).
        // The 'limit' parameter is effectively ignored here; the ViewModel will apply its own display limit.
        return dataStoreManager.searchHistoryFlow
    }

    override suspend fun addSearchTerm(term: String) {
        val normalizedTerm = term.trim().lowercase()
        if (normalizedTerm.isBlank()) {
            return
        }

        val currentHistory = dataStoreManager.searchHistoryFlow.firstOrNull() ?: emptyList()
        val mutableHistory = currentHistory.toMutableList()

        // Remove any existing instance of the normalizedTerm (case-insensitive)
        mutableHistory.removeAll { it.equals(normalizedTerm, ignoreCase = false) } // Story implies exact match for removal after normalization

        // Add the new normalized term to the beginning
        mutableHistory.add(0, normalizedTerm)

        // Trim the list if it exceeds MAX_HISTORY_SIZE
        val finalHistory = if (mutableHistory.size > SearchHistoryKeys.MAX_HISTORY_SIZE) {
            mutableHistory.take(SearchHistoryKeys.MAX_HISTORY_SIZE)
        } else {
            mutableHistory
        }
        dataStoreManager.saveSearchHistory(finalHistory)
    }

    override suspend fun deleteSearchTerm(term: String) {
        val normalizedTerm = term.trim().lowercase()
        if (normalizedTerm.isBlank()) {
            return // Cannot delete a blank term effectively, or could clear all if that was intent, but not specified
        }
        val currentHistory = dataStoreManager.searchHistoryFlow.firstOrNull() ?: emptyList()
        val mutableHistory = currentHistory.toMutableList()

        // Remove all instances of the normalized term (case-insensitive as per story for `deleteSearchTerm`)
        val removed = mutableHistory.removeAll { it.equals(normalizedTerm, ignoreCase = false) } // Story implies exact match after normalization

        if (removed) {
            dataStoreManager.saveSearchHistory(mutableHistory)
        }
    }

    override suspend fun clearSearchHistory() {
        dataStoreManager.saveSearchHistory(emptyList())
    }
} 