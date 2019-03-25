package com.example.dahaka.mycam.api

object ApiConstants {
    const val BASE_URL_DEV = "https://uploadfiles.io/"
    const val BASE_URL_PROD = "https://uploadfiles.io/"

    object Common {
        const val UPLOAD = ""
    }
}

fun getBaseUrl(buildType: String): String {
    return when (buildType) {
        "release" -> ApiConstants.BASE_URL_PROD
        else -> ApiConstants.BASE_URL_DEV
    }
}