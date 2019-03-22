package com.example.dahaka.mycam.ui

import java.io.File

class ImageRequest(var data: Data,
                   var file: File? = null) {

    data class Data(var title: String? = null)
}