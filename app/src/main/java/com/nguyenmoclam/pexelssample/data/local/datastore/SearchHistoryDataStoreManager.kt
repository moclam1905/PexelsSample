package com.nguyenmoclam.pexelssample.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.nguyenmoclam.pexelssample.data.local.datastore.SearchHistoryKeys.SEARCH_HISTORY_TERMS_JSON_LIST
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.searchHistoryDataStore: DataStore<Preferences> by preferencesDataStore(name = "search_history_prefs")

@Singleton
class SearchHistoryDataStoreManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {

    val searchHistoryFlow: Flow<List<String>> = context.searchHistoryDataStore.data
        .catch {
            if (it is IOException) {
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map {
            val jsonString = it[SEARCH_HISTORY_TERMS_JSON_LIST] ?: "[]"
            try {
                json.decodeFromString<List<String>>(jsonString)
            } catch (e: Exception) {
                // Handle deserialization error, e.g. log and return empty list
                // For now, returning empty list if data is malformed
                emptyList<String>()
            }
        }

    suspend fun saveSearchHistory(terms: List<String>) {
        context.searchHistoryDataStore.edit { preferences ->
            val jsonString = json.encodeToString(terms)
            preferences[SEARCH_HISTORY_TERMS_JSON_LIST] = jsonString
        }
    }
} 