package com.nguyenmoclam.pexelssample.data.remote

import com.nguyenmoclam.pexelssample.data.remote.model.PexelsSearchResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface PexelsApiService {

    @GET("curated")
    suspend fun getCuratedPhotos(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): Response<PexelsSearchResponseDto>

    @GET
    suspend fun getPhotosFromUrl(
        @Url url: String
    ): Response<PexelsSearchResponseDto>

    @GET("search")
    suspend fun searchPhotos(
        @Query("query") query: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): Response<PexelsSearchResponseDto>
} 