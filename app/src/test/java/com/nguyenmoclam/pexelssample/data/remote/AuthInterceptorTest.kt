package com.nguyenmoclam.pexelssample.data.remote

import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AuthInterceptorTest {

    @Test
    fun `intercept should add correct Authorization header`() {
        val testApiKey = "test_api_key_123"
        val authInterceptor = AuthInterceptor(apiKey = testApiKey)

        val originalRequest = Request.Builder()
            .url("https://api.pexels.com/v1/test")
            .build()

        var interceptedRequest: Request? = null

        // Minimal mock for Interceptor.Chain
        val mockChain = object : Interceptor.Chain {
            override fun request(): Request = originalRequest

            override fun proceed(request: Request): Response {
                interceptedRequest = request
                // Return a dummy response, not strictly needed for this header test
                return Response.Builder()
                    .request(request)
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(okhttp3.ResponseBody.create(null, ""))
                    .build()
            }

            override fun connection(): Connection? = null // Added dummy implementation
            override fun call(): okhttp3.Call = OkHttpClient().newCall(originalRequest) // Dummy implementation
            override fun connectTimeoutMillis(): Int = 0
            override fun readTimeoutMillis(): Int = 0
            override fun writeTimeoutMillis(): Int = 0
            override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this
            override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this
            override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this
        }

        authInterceptor.intercept(mockChain)

        assertNotNull("Intercepted request should not be null", interceptedRequest)
        val authHeader = interceptedRequest!!.header("Authorization")
        assertNotNull("Authorization header should be present", authHeader)
        assertEquals("Authorization header should match the provided API key", testApiKey, authHeader)
    }
} 