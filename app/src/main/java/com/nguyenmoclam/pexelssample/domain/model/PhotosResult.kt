package com.nguyenmoclam.pexelssample.domain.model

sealed class PhotosResult {
    data class Success(
        val photos: List<Photo>,
        val totalResults: Int,
        val canLoadMore: Boolean,
        val nextPageUrl: String?
    ) : PhotosResult()
    data class Error(val message: String, val isRetryable: Boolean) : PhotosResult()
} 