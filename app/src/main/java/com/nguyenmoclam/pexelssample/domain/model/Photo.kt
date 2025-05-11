package com.nguyenmoclam.pexelssample.domain.model

data class Photo(
    val id: Int,
    val width: Int,
    val height: Int,
    val url: String,
    val photographer: String,
    val photographerUrl: String,
    val photographerId: Long,
    val avgColor: String,
    val src: PhotoSrc,
    val alt: String // Non-nullable, to be handled by mapper
) 