package com.nguyenmoclam.pexelssample.di

import android.content.Context
import androidx.room.Room
import com.nguyenmoclam.pexelssample.data.local.PexelsRoomDatabase
import com.nguyenmoclam.pexelssample.data.local.dao.PhotoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_NAME = "pexels_database"

    @Provides
    @Singleton
    fun providePexelsRoomDatabase(@ApplicationContext appContext: Context): PexelsRoomDatabase {
        return Room.databaseBuilder(
            appContext,
            PexelsRoomDatabase::class.java,
            DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun providePhotoDao(database: PexelsRoomDatabase): PhotoDao {
        return database.photoDao()
    }
} 