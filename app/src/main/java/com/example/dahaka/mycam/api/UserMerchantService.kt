package com.example.dahaka.mycam.api

import com.example.dahaka.mycam.ui.ImageRequest
import io.reactivex.Single
import okhttp3.RequestBody

class UserMerchantService(networkClient: NetworkClient) {
    private val iCommon: ICommon = networkClient.create(ICommon::class.java)

    fun upload(url: String, req: ImageRequest.Data, file: RequestBody?) = iCommon.uploadFile(url, req, file)

}