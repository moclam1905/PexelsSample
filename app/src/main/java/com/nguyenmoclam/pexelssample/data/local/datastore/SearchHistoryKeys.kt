package com.nguyenmoclam.pexelssample.data.local.datastore

import androidx.datastore.preferences.core.stringPreferencesKey

object SearchHistoryKeys {
    val SEARCH_HISTORY_TERMS_JSON_LIST = stringPreferencesKey("search_history_terms_json_list")
    const val MAX_HISTORY_SIZE = 10 // Define max history size
} 