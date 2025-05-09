package com.nguyenmoclam.pexelssample.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PexelsPhotoSrcDto(
    @SerialName("original")
    val original: String,
    @SerialName("large2x")
    val large2x: String,
    @SerialName("large")
    val large: String,
    @SerialName("medium")
    val medium: String,
    @SerialName("small")
    val small: String,
    @SerialName("portrait")
    val portrait: String,
    @SerialName("landscape")
    val landscape: String,
    @SerialName("tiny")
    val tiny: String
) 