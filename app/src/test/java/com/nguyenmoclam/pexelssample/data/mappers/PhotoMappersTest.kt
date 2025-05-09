package com.nguyenmoclam.pexelssample.data.mappers

import com.nguyenmoclam.pexelssample.data.remote.model.PexelsPhotoDto
import com.nguyenmoclam.pexelssample.data.remote.model.PexelsPhotoSrcDto
import org.junit.Assert.*
import org.junit.Test

class PhotoMappersTest {

    @Test
    fun `PexelsPhotoSrcDto toDomain mapping works correctly`() {
        val pexelsPhotoSrcDto = PexelsPhotoSrcDto(
            original = "original_url",
            large2x = "large2x_url",
            large = "large_url",
            medium = "medium_url",
            small = "small_url",
            portrait = "portrait_url",
            landscape = "landscape_url",
            tiny = "tiny_url"
        )

        val photoSrc = pexelsPhotoSrcDto.toDomain()

        assertEquals("original_url", photoSrc.original)
        assertEquals("large2x_url", photoSrc.large2x)
        assertEquals("large_url", photoSrc.large)
        assertEquals("medium_url", photoSrc.medium)
        assertEquals("small_url", photoSrc.small)
        assertEquals("portrait_url", photoSrc.portrait)
        assertEquals("landscape_url", photoSrc.landscape)
        assertEquals("tiny_url", photoSrc.tiny)
    }

    @Test
    fun `PexelsPhotoDto toDomain mapping works correctly with non-null alt`() {
        val pexelsPhotoSrcDto = PexelsPhotoSrcDto("o", "l2x", "l", "m", "s", "p", "ls", "t")
        val pexelsPhotoDto = PexelsPhotoDto(
            id = 1,
            width = 100,
            height = 200,
            url = "photo_url",
            photographer = "Photographer Name",
            photographerUrl = "photographer_url",
            photographerId = 10,
            avgColor = "#FFFFFF",
            src = pexelsPhotoSrcDto,
            liked = false,
            alt = "A beautiful photo"
        )

        val photo = pexelsPhotoDto.toDomain()

        assertEquals(1, photo.id)
        assertEquals(100, photo.width)
        assertEquals(200, photo.height)
        assertEquals("photo_url", photo.url)
        assertEquals("Photographer Name", photo.photographer)
        assertEquals("photographer_url", photo.photographerUrl)
        assertEquals(10, photo.photographerId)
        assertEquals("#FFFFFF", photo.avgColor)
        assertEquals(pexelsPhotoSrcDto.original, photo.src.original) // Basic check for src mapping
        assertEquals("A beautiful photo", photo.alt)
    }

    @Test
    fun `PexelsPhotoDto toDomain mapping handles null alt correctly`() {
        val pexelsPhotoSrcDto = PexelsPhotoSrcDto("o", "l2x", "l", "m", "s", "p", "ls", "t")
        val pexelsPhotoDto = PexelsPhotoDto(
            id = 2,
            width = 300,
            height = 400,
            url = "photo_url2",
            photographer = "Another Photographer",
            photographerUrl = "photographer_url2",
            photographerId = 20,
            avgColor = "#000000",
            src = pexelsPhotoSrcDto,
            liked = true,
            alt = null // Null alt text
        )

        val photo = pexelsPhotoDto.toDomain()

        assertEquals("", photo.alt) // Should default to empty string
    }
} 