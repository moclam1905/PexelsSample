package com.nguyenmoclam.pexelssample.data.local.model

// No annotations needed here as it's an embedded class
data class CachedPhotoSrcEntity(
    val original: String,
    val large: String,
    val medium: String,
    val small: String,
    val portrait: String,
    val tiny: String
) 