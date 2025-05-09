package com.nguyenmoclam.pexelssample.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nguyenmoclam.pexelssample.data.local.dao.PhotoDao
import com.nguyenmoclam.pexelssample.data.local.model.CachedPhotoEntity

@Database(entities = [CachedPhotoEntity::class], version = 1, exportSchema = false)
abstract class PexelsRoomDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao
} 