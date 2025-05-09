package com.nguyenmoclam.pexelssample.data.local.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_photos")
data class CachedPhotoEntity(
    @PrimaryKey val id: Int,
    val width: Int,
    val height: Int,
    val url: String,
    val photographer: String,
    val photographerUrl: String,
    val photographerId: Int,
    @Embedded(prefix = "src_")
    val src: CachedPhotoSrcEntity,
    val liked: Boolean,
    val alt: String? // Added alt text as it's a common and useful field
) 