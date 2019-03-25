package com.example.dahaka.mycam.api

import okhttp3.OkHttpClient
import retrofit2.CallAdapter
import retrofit2.Converter
import retrofit2.Retrofit

class NetworkClient(
    baseUrl: String,
    converterFactory: Converter.Factory,
    client: OkHttpClient,
    adapterFactory: CallAdapter.Factory
) {

    private val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addCallAdapterFactory(adapterFactory)
            .addConverterFactory(converterFactory)
            .build()

    fun <T> create(apiInterfaceClass: Class<T>): T {
        return retrofit.create(apiInterfaceClass)
    }
}