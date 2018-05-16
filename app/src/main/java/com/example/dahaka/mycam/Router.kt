package com.example.dahaka.mycam

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import android.support.customtabs.CustomTabsIntent
import com.example.dahaka.mycam.ui.activity.*
import com.example.dahaka.mycam.ui.fragment.DeletePhotoDialogFragment
import com.example.dahaka.mycam.util.BASE_URL

class Router(val context: Context) {

    //I used activity in the parameters instead of the context in the router's constructor
    // since the old devices require a flag: FLAG_ACTIVITY_NEW_TASK

    fun openCameraScreen(activity: Activity) {
        activity.startActivity(Intent(activity, CameraActivity::class.java))
    }

    fun openBarcodeScreen(activity: Activity) {
        activity.startActivity(Intent(activity, BarcodeCaptureActivity::class.java))
    }

    fun openGalleryScreen(activity: Activity) {
        activity.startActivity(Intent(activity, GalleryActivity::class.java))
    }

    fun openDetailScreen(activity: Activity, position: Int) {
        activity.startActivity(Intent(activity, DetailActivity::class.java)
                .putExtra(DetailActivity.FILE_POSITION, position))
    }

    fun openDetailScreen(activity: Activity, file: String) {
        activity.startActivity(Intent(activity, DetailActivity::class.java)
                .putExtra(DetailActivity.FILE_PATH, file))
    }

    fun openBrowserScreen(activity: Activity, url: String) {
        CustomTabsIntent.Builder().build().launchUrl(activity, Uri.parse(BASE_URL + url))
    }

    fun startDeleteDialog(manager: android.support.v4.app.FragmentManager) {
        val dialogFragment = DeletePhotoDialogFragment.newInstance()
        dialogFragment.show(manager, context.getString(R.string.delete))
    }
    fun startFaceScreen(file: String){
        context.startActivity(Intent(context, FaceDetectorActivity::class.java)
                .apply {
                    addFlags(FLAG_ACTIVITY_NEW_TASK)
                    putExtra(DetailActivity.FACE_FILE_PATH, file)
                })
    }
}