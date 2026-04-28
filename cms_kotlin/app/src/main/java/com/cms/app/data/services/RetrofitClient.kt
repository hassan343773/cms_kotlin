package com.cms.app.data.services

import com.cms.app.utils.Constants
import com.cms.app.utils.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.Dns
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetAddress
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private var sessionManager: SessionManager? = null

    fun init(session: SessionManager) {
        sessionManager = session
    }

    private val authInterceptor = Interceptor { chain ->
        val token = runBlocking { sessionManager?.getToken() }

        val requestBuilder = chain.request().newBuilder()
            .addHeader("ngrok-skip-browser-warning", "true")
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")

        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        chain.proceed(requestBuilder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val googleDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            return try {
                InetAddress.getAllByName(hostname).toList()
            } catch (e: Exception) {
                Dns.SYSTEM.lookup(hostname)
            }
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .dns(googleDns)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}