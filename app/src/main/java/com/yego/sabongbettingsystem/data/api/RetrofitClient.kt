package com.yego.sabongbettingsystem.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // For emulator: use 10.0.2.2 (special alias for host machine)
    // For physical device: use 192.168.1.10 (your actual IP)
    const val BASE_URL = "http://192.168.1.10:8000/api/"

    private val jsonHeaderInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .addHeader("Connection", "close") // Try to disable persistent connections if they are hanging
            .build()
        chain.proceed(request)
    }

    private val logging = HttpLoggingInterceptor().apply {
        // Change to HEADERS for large payloads to avoid overhead and memory issues in Logcat
        level = HttpLoggingInterceptor.Level.HEADERS
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(jsonHeaderInterceptor)
        .addInterceptor(logging)
        .connectTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
