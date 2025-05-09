package com.nguyenmoclam.pexelssample.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.nguyenmoclam.pexelssample.BuildConfig
import com.nguyenmoclam.pexelssample.data.remote.AuthInterceptor
import com.nguyenmoclam.pexelssample.data.remote.PexelsApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val PEXELS_API_BASE_URL = "https://api.pexels.com/v1/"

    @Provides
    @Singleton
    @Named("PexelsApiKey")
    fun providePexelsApiKey(): String {
        return BuildConfig.PEXELS_API_KEY
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(@Named("PexelsApiKey") apiKey: String): AuthInterceptor {
        return AuthInterceptor(apiKey)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json { ignoreUnknownKeys = true }
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(PEXELS_API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun providePexelsApiService(retrofit: Retrofit): PexelsApiService {
        return retrofit.create(PexelsApiService::class.java)
    }
} 