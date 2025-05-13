package com.nguyenmoclam.pexelssample.data.repository

import com.nguyenmoclam.pexelssample.data.mappers.toDomain
import com.nguyenmoclam.pexelssample.data.remote.PexelsApiService
import com.nguyenmoclam.pexelssample.domain.model.PhotosResult
import com.nguyenmoclam.pexelssample.domain.repository.ImageRepository
import com.nguyenmoclam.pexelssample.logger.Logger
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageRepositoryImpl @Inject constructor(
    private val apiService: PexelsApiService
) : ImageRepository {

    override suspend fun searchPhotos(query: String, page: Int, perPage: Int): PhotosResult {
        return try {
            val response = apiService.searchPhotos(query = query, page = page, perPage = perPage)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                PhotosResult.Success(
                    photos = body.photos.map { it.toDomain() },
                    totalResults = body.totalResults,
                    canLoadMore = body.nextPage != null,
                    nextPageUrl = body.nextPage
                )
            } else {
                val errorMsg = "API Error ${response.code()}: ${response.message()} - ${response.errorBody()?.string()}"
                Logger.e("ImageRepositoryImpl", errorMsg)
                // Basic error mapping, can be refined
                val userMessage = when(response.code()) {
                     401, 403 -> "Authentication error."
                     429 -> "Rate limit exceeded. Please try again later."
                     in 500..599 -> "Server error (${response.code()}). Please try again later."
                     else -> "Failed to search photos (Error ${response.code()})."
                }
                PhotosResult.Error(message = userMessage, isRetryable = response.code() >= 500 || response.code() == 429)
            }
        } catch (e: IOException) {
            Logger.e("ImageRepositoryImpl", "Network error searching photos: ${e.message}", e)
            PhotosResult.Error(message = "Network error. Please check connection.", isRetryable = true)
        } catch (e: Exception) {
            Logger.e("ImageRepositoryImpl", "Unknown error searching photos: ${e.message}", e)
            PhotosResult.Error(message = "An unexpected error occurred.", isRetryable = true)
        }
    }

    override suspend fun getCuratedPhotos(page: Int, perPage: Int): PhotosResult {
        return try {
            // Using getCuratedPhotosTest which only takes perPage, ignoring the 'page' parameter for now.
            val response = apiService.getCuratedPhotos(page = page, perPage = perPage)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                PhotosResult.Success(
                    photos = body.photos.map { it.toDomain() },
                    totalResults = body.totalResults,
                    canLoadMore = body.nextPage != null,
                    nextPageUrl = body.nextPage
                )
            } else {
                val errorMsg = "API Error ${response.code()}: ${response.message()} - ${response.errorBody()?.string()}"
                Logger.e("ImageRepositoryImpl", errorMsg)
                 // Basic error mapping, can be refined
                val userMessage = when(response.code()) {
                     401, 403 -> "Authentication error."
                     429 -> "Rate limit exceeded. Please try again later."
                     in 500..599 -> "Server error (${response.code()}). Please try again later."
                     else -> "Failed to get curated photos (Error ${response.code()})."
                }
                PhotosResult.Error(message = userMessage, isRetryable = response.code() >= 500 || response.code() == 429)
            }
        } catch (e: IOException) {
            Logger.e("ImageRepositoryImpl", "Network error getting curated photos: ${e.message}", e)
            PhotosResult.Error(message = "Network error. Please check connection.", isRetryable = true)
        } catch (e: Exception) {
            Logger.e("ImageRepositoryImpl", "Unknown error getting curated photos: ${e.message}", e)
            PhotosResult.Error(message = "An unexpected error occurred.", isRetryable = true)
        }
    }

    override suspend fun getPhotosFromUrl(url: String): PhotosResult {
        return try {
            val response = apiService.getPhotosFromUrl(url = url)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                PhotosResult.Success(
                    photos = body.photos.map { it.toDomain() },
                    totalResults = body.totalResults,
                    canLoadMore = body.nextPage != null,
                    nextPageUrl = body.nextPage
                )
            } else {
                val errorMsg = "API Error ${response.code()}: ${response.message()} - ${response.errorBody()?.string()}"
                Logger.e("ImageRepositoryImpl", errorMsg)
                val userMessage = when(response.code()) {
                    401, 403 -> "Authentication error."
                    404 -> "Resource not found (invalid URL?)"
                    429 -> "Rate limit exceeded. Please try again later."
                    in 500..599 -> "Server error (${response.code()}). Please try again later."
                    else -> "Failed to get photos from URL (Error ${response.code()})."
                }
                PhotosResult.Error(message = userMessage, isRetryable = response.code() >= 500 || response.code() == 429 || response.code() == 404) // Cân nhắc retry 404?
            }
        } catch (e: IOException) {
            Logger.e("ImageRepositoryImpl", "Network error getting photos from URL: ${e.message}", e)
            PhotosResult.Error(message = "Network error. Please check connection.", isRetryable = true)
        } catch (e: Exception) {
            Logger.e("ImageRepositoryImpl", "Unknown error getting photos from URL: ${e.message}", e)
            PhotosResult.Error(message = "An unexpected error occurred.", isRetryable = true)
        }
    }

} 