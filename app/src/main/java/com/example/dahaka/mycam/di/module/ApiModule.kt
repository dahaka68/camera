package com.example.dahaka.mycam.di.module

import com.example.dahaka.mycam.BuildConfig
import com.example.dahaka.mycam.api.NetworkClient
import com.example.dahaka.mycam.api.UserMerchantService
import com.example.dahaka.mycam.api.getBaseUrl
import com.example.dahaka.mycam.util.UtcDateTypeAdapter
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module.module
import retrofit2.CallAdapter
import retrofit2.Converter
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import java.util.concurrent.TimeUnit

val apiModule = module {

    factory { provideGson() }
    factory { provideConverterFactory(get()) }
    factory { provideAdapterFactory() }
    factory { provideHttpClient(get()) }
    factory { provideHttpLoggingInterceptor() }

    single { UserMerchantService(provideClientService(get(), get(), get())) }
}

private fun provideClientService(
        converterFactory: Converter.Factory,
        httpClient: OkHttpClient,
        adapterFactory: CallAdapter.Factory): NetworkClient {

    val baseUrl: String = getBaseUrl(BuildConfig.BUILD_TYPE)
    return NetworkClient(baseUrl, converterFactory, httpClient, adapterFactory)
}

private fun provideHttpClient(interceptor: Interceptor): OkHttpClient {
    return OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(interceptor)
            .build()
}

private fun provideHttpLoggingInterceptor(): Interceptor {
    val interceptor = HttpLoggingInterceptor()
    interceptor.level = HttpLoggingInterceptor.Level.BODY
    return interceptor
}

private fun provideAdapterFactory(): CallAdapter.Factory = RxJava2CallAdapterFactory.create()

private fun provideGson(): Gson {
    return GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY)
            .registerTypeAdapter(Date::class.java, UtcDateTypeAdapter())
            .create()
}

private fun provideConverterFactory(gson: Gson): Converter.Factory {
    return GsonConverterFactory.create(gson)
}