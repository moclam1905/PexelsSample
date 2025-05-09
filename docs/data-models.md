
# PexelsSample Data Models

This document defines the structure of data used within the PexelsSample application, including domain models, API Data Transfer Objects (DTOs), and local database entities. These models are crucial for API interaction (Story 1.4), domain logic, and local caching.

## 1. Domain Models

These are the clean representations of the core entities used throughout the application, particularly in the `domain` and `ui` layers. They are independent of any specific data source (API or database) structure.

### Photo (Domain Model)

Represents a single photo with its essential attributes.

```kotlin
package com.nguyenmoclam.pexelssample.domain.model

// Located in: app/src/main/java/com/nguyenmoclam/pexelssample/domain/model/Photo.kt
data class Photo(
    val id: Int,                           // Unique identifier for the photo
    val width: Int,                        // Width of the photo in pixels
    val height: Int,                       // Height of the photo in pixels
    val url: String,                       // URL to the Pexels page for the photo
    val photographer: String,              // Name of the photographer
    val photographerUrl: String,           // URL to the photographer's Pexels profile
    val photographerId: Int,               // ID of the photographer
    val avgColor: String,                  // Average color of the photo (hex string, e.g., "#RRGGBB")
    val src: PhotoSrc,                     // Object containing various image versions/sizes
    val alt: String                        // Alternative text for the photo (description)
    // val liked: Boolean, // 'liked' field is mentioned in Pexels docs, but might not be used in MVP
)
```

### PhotoSrc (Domain Model)

Represents the different available sizes/versions of a photo.

```kotlin
package com.nguyenmoclam.pexelssample.domain.model

// Located in: app/src/main/java/com/nguyenmoclam/pexelssample/domain/model/PhotoSrc.kt
data class PhotoSrc(
    val original: String,                  // URL to the original, full-resolution image
    val large2x: String,                   // URL to a large version (e.g., for high-DPI displays)
    val large: String,                     // URL to a large version
    val medium: String,                    // URL to a medium version (good for thumbnails or moderate views)
    val small: String,                     // URL to a small version
    val portrait: String,                  // URL to a portrait-oriented version
    val landscape: String,                 // URL to a landscape-oriented version
    val tiny: String                       // URL to a very small version (e.g., for placeholders)
)
```

## 2. API Data Transfer Objects (DTOs)

These data classes directly map to the JSON structure of the Pexels API responses. They are used in the `data/remote` layer and are annotated for `kotlinx.serialization`. (Story 1.3, Story 1.4)

### PexelsPhotoDto

Maps to a single photo object in the Pexels API response.

```kotlin
package com.nguyenmoclam.pexelssample.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Located in: app/src/main/java/com/nguyenmoclam/pexelssample/data/remote/model/PexelsPhotoDto.kt
@Serializable
data class PexelsPhotoDto(
    val id: Int,
    val width: Int,
    val height: Int,
    val url: String,
    val photographer: String,
    @SerialName("photographer_url")
    val photographerUrl: String,
    @SerialName("photographer_id")
    val photographerId: Int,
    @SerialName("avg_color")
    val avgColor: String,
    val src: PexelsPhotoSrcDto,
    val liked: Boolean, // Pexels API includes this
    val alt: String? // Alt text can sometimes be null or missing from API
)
```

### PexelsPhotoSrcDto

Maps to the `src` object within a Pexels photo object.

```kotlin
package com.nguyenmoclam.pexelssample.data.remote.model

import kotlinx.serialization.Serializable

// Located in: app/src/main/java/com/nguyenmoclam/pexelssample/data/remote/model/PexelsPhotoSrcDto.kt
@Serializable
data class PexelsPhotoSrcDto(
    val original: String,
    val large2x: String,
    val large: String,
    val medium: String,
    val small: String,
    val portrait: String,
    val landscape: String,
    val tiny: String
)
```

### PexelsSearchResponseDto

Maps to the overall response structure for both `/search` and `/curated` Pexels API endpoints.

```kotlin
package com.nguyenmoclam.pexelssample.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Located in: app/src/main/java/com/nguyenmoclam/pexelssample/data/remote/model/PexelsSearchResponseDto.kt
@Serializable
data class PexelsSearchResponseDto(
    val photos: List<PexelsPhotoDto>,
    val page: Int,
    @SerialName("per_page")
    val perPage: Int,
    @SerialName("total_results")
    val totalResults: Int,
    @SerialName("next_page")
    val nextPage: String? = null // URL for the next page, can be null
)
```

**Mappers:** Mapper functions will be created in the `data/mappers/` package to convert `PexelsPhotoDto` to `Photo` (Domain Model) and `PexelsPhotoSrcDto` to `PhotoSrc` (Domain Model). This ensures the domain layer remains decoupled from the API's specific structure. For example:

```kotlin
// In app/src/main/java/com/nguyenmoclam/pexelssample/data/mappers/PhotoMappers.kt
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
        alt = this.alt ?: "" // Provide a default for null alt text
    )
}

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
```

## 3. Local Cache Entities (Room)

These data classes are annotated for use with the Room persistence library and represent the structure of data stored in the local SQLite database. For MVP, these can be very similar or identical to the domain models to simplify caching. We will cache `Photo` metadata. (Story 1.6)

### CachedPhotoEntity

Represents a photo stored in the Room database.

```kotlin
package com.nguyenmoclam.pexelssample.data.local.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

// Located in: app/src/main/java/com/nguyenmoclam/pexelssample/data/local/model/CachedPhotoEntity.kt
@Entity(tableName = "cached_photos")
data class CachedPhotoEntity(
    @PrimaryKey val id: Int,
    val width: Int,
    val height: Int,
    val url: String,
    val photographer: String,
    val photographerUrl: String,
    val photographerId: Int,
    val avgColor: String,
    @Embedded(prefix = "src_") // Prefix to avoid column name clashes if PhotoSrcEntity fields are common
    val src: CachedPhotoSrcEntity,
    val alt: String,
    val lastAccessed: Long = System.currentTimeMillis() // For potential cache eviction strategy
)
```

### CachedPhotoSrcEntity

Represents the photo source URLs, embedded within `CachedPhotoEntity`.

```kotlin
package com.nguyenmoclam.pexelssample.data.local.model

// Located in: app/src/main/java/com/nguyenmoclam/pexelssample/data/local/model/CachedPhotoSrcEntity.kt
// This class might not need @Entity if only used as @Embedded
data class CachedPhotoSrcEntity(
    val original: String,
    val large2x: String,
    val large: String,
    val medium: String,
    val small: String,
    val portrait: String,
    val landscape: String,
    val tiny: String
)
```

**Mappers for Cache Entities:** Similar to API DTOs, mappers will be needed to convert between `Photo` (Domain Model) and `CachedPhotoEntity`.

```kotlin
// In app/src/main/java/com/nguyenmoclam/pexelssample/data/mappers/CacheMappers.kt (example)
fun Photo.toEntity(): CachedPhotoEntity {
    return CachedPhotoEntity(
        id = this.id,
        width = this.width,
        height = this.height,
        // ... other fields ...
        src = this.src.toEntity(),
        alt = this.alt
    )
}

fun PhotoSrc.toEntity(): CachedPhotoSrcEntity {
    return CachedPhotoSrcEntity(
        original = this.original,
        // ... other fields ...
    )
}

fun CachedPhotoEntity.toDomain(): Photo {
    return Photo(
        id = this.id,
        width = this.width,
        height = this.height,
        // ... other fields ...
        src = this.src.toDomain(),
        alt = this.alt
    )
}

fun CachedPhotoSrcEntity.toDomain(): PhotoSrc {
    return PhotoSrc(
        original = this.original,
        // ... other fields ...
    )
}
```

*(Note: The `liked` field from the Pexels API is included in the DTO but commented out in the Domain Model as its direct use in the MVP is not specified. It can be easily added if needed. The `alt` text from the API can be null, so the DTO reflects this, and the mapper to the domain model provides a non-null default.)*

## Change Log

| Change        | Date       | Version | Description                                     | Author     |
| :------------ | :--------- | :------ | :---------------------------------------------- | :--------- |
| Initial draft | 2025-05-08 | 0.1     | Initial draft defining Domain, DTO, and Cache models. | Architect AI |

