package com.example.dahaka.mycam.api

import com.example.dahaka.mycam.ui.ImageRequest
import io.reactivex.Completable
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Url

private const val DATA = "data"
private const val FILE = "file\"; filename=\"name\""

interface ICommon {
    @Multipart
    @POST
    fun uploadFile(@Url url: String,
                   @Part(DATA) request: ImageRequest.Data,
                   @Part(FILE) file: RequestBody?): Completable
}