package com.nguyenmoclam.pexelssample.di

import com.nguyenmoclam.pexelssample.data.repository.ImageRepositoryImpl
import com.nguyenmoclam.pexelssample.data.repository.SearchHistoryRepositoryImpl
import com.nguyenmoclam.pexelssample.domain.repository.ImageRepository
import com.nguyenmoclam.pexelssample.domain.repository.SearchHistoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSearchHistoryRepository(
        searchHistoryRepositoryImpl: SearchHistoryRepositoryImpl
    ): SearchHistoryRepository

    @Binds
    @Singleton
    abstract fun bindImageRepository(
        imageRepositoryImpl: ImageRepositoryImpl
    ): ImageRepository
} 