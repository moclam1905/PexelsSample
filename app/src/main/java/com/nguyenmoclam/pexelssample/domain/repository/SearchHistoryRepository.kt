package com.nguyenmoclam.pexelssample.domain.repository

import kotlinx.coroutines.flow.Flow

interface SearchHistoryRepository {
    fun getRecentSearches(limit: Int): Flow<List<String>>
    suspend fun addSearchTerm(term: String)
    suspend fun deleteSearchTerm(term: String)
    suspend fun clearSearchHistory()
} 