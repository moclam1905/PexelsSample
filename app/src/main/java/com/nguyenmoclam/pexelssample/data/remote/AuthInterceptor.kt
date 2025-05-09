package com.nguyenmoclam.pexelssample.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    @Named("PexelsApiKey") private val apiKey: String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("Authorization", apiKey)
            .build()
        return chain.proceed(request)
    }
} 