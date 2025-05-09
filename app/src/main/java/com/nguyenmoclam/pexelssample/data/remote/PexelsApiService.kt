package com.nguyenmoclam.pexelssample.data.remote

import com.nguyenmoclam.pexelssample.data.remote.model.PexelsSearchResponseDto
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface PexelsApiService {

    @GET("curated")
    suspend fun getCuratedPhotosTest(
        @Query("per_page") perPage: Int = 1
    ): Response<PexelsSearchResponseDto> // Or ResponseBody as per story, JsonObject is more specific for kotlinx.serialization
} 