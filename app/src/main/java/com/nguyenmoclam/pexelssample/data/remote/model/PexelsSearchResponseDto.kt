package com.nguyenmoclam.pexelssample.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PexelsSearchResponseDto(
    @SerialName("page")
    val page: Int,
    @SerialName("per_page")
    val perPage: Int,
    @SerialName("photos")
    val photos: List<PexelsPhotoDto>,
    @SerialName("total_results")
    val totalResults: Int,
    @SerialName("next_page")
    val nextPage: String? = null // Explicitly provide default null
) 