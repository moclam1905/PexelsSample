package com.nguyenmoclam.pexelssample.data.mappers

import com.nguyenmoclam.pexelssample.data.remote.model.PexelsPhotoDto
import com.nguyenmoclam.pexelssample.data.remote.model.PexelsPhotoSrcDto
import com.nguyenmoclam.pexelssample.domain.model.Photo
import com.nguyenmoclam.pexelssample.domain.model.PhotoSrc

fun PexelsPhotoSrcDto.toDomain(): PhotoSrc {
    return PhotoSrc(
        original = this.original,
        large2x = this.large2x,
        large = this.large,
        medium = this.medium,
        small = this.small,
        portrait = this.portrait,
        landscape = this.landscape,
        tiny = this.tiny
    )
}

fun PexelsPhotoDto.toDomain(): Photo {
    return Photo(
        id = this.id,
        width = this.width,
        height = this.height,
        url = this.url,
        photographer = this.photographer,
        photographerUrl = this.photographerUrl,
        photographerId = this.photographerId,
        avgColor = this.avgColor,
        src = this.src.toDomain(),
        alt = this.alt ?: "" // Handle nullable alt, defaulting to empty string
    )
} 