package com.nguyenmoclam.pexelssample.domain.repository

import com.nguyenmoclam.pexelssample.domain.model.PhotosResult

interface ImageRepository {
    suspend fun searchPhotos(query: String, page: Int, perPage: Int): PhotosResult
    suspend fun getCuratedPhotos(page: Int, perPage: Int): PhotosResult
    suspend fun getPhotosFromUrl(url: String): PhotosResult
} 