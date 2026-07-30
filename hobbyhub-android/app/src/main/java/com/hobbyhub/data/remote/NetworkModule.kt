package com.hobbyhub.data.remote

import android.content.Context
import com.hobbyhub.BuildConfig
import com.hobbyhub.data.local.UserSessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    // Base URL is now injected dynamically via BuildConfig
    private const val BASE_URL = BuildConfig.API_BASE_URL

    private var retrofit: Retrofit? = null

    fun getAuthApi(context: Context): AuthApi {
        if (retrofit == null) {
            val sessionManager = UserSessionManager(context)

            // Auth Interceptor for injecting JWT token
            val authInterceptor = Interceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                sessionManager.getJwtToken()?.let { token ->
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
                chain.proceed(requestBuilder.build())
            }

            // Logging Interceptor for debugging
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        return retrofit!!.create(AuthApi::class.java)
    }
}
