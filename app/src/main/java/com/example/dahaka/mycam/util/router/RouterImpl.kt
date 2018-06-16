package com.example.dahaka.mycam.util.router

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import android.support.customtabs.CustomTabsIntent
import com.example.dahaka.mycam.R
import com.example.dahaka.mycam.ui.barcode.BarcodeCaptureActivity
import com.example.dahaka.mycam.ui.camera.CameraActivity
import com.example.dahaka.mycam.ui.detail.DetailActivity
import com.example.dahaka.mycam.ui.faceDetector.FaceDetectorActivity
import com.example.dahaka.mycam.ui.fragment.DeletePhotoDialogFragment
import com.example.dahaka.mycam.ui.gallery.GalleryActivity
import com.example.dahaka.mycam.util.BASE_URL

class RouterImpl(val context: Context) : Router {

    //I used activity in the parameters instead of the context in the router's constructor
    // since the old devices require a flag: FLAG_ACTIVITY_NEW_TASK

    override fun openCameraScreen(activity: Activity) {
        activity.startActivity(Intent(activity, CameraActivity::class.java))
    }

    override fun openBarcodeScreen(activity: Activity) {
        activity.startActivity(Intent(activity, BarcodeCaptureActivity::class.java))
    }

    override fun openGalleryScreen(activity: Activity) {
        activity.startActivity(Intent(activity, GalleryActivity::class.java))
    }

    override fun openDetailScreen(activity: Activity, position: Int) {
        activity.startActivity(Intent(activity, DetailActivity::class.java)
                .putExtra(DetailActivity.FILE_POSITION, position))
    }

    override fun openDetailScreen(activity: Activity, file: String) {
        activity.startActivity(Intent(activity, DetailActivity::class.java)
                .putExtra(DetailActivity.FILE_PATH, file))
    }

    override fun openBrowserScreen(activity: Activity, url: String) {
        CustomTabsIntent.Builder().build().launchUrl(activity, Uri.parse(BASE_URL + url))
    }

    override fun startDeleteDialog(manager: android.support.v4.app.FragmentManager) {
        val dialogFragment = DeletePhotoDialogFragment.newInstance()
        dialogFragment.show(manager, context.getString(R.string.delete))
    }

    override fun startFaceScreen(file: String) {
        context.startActivity(Intent(context, FaceDetectorActivity::class.java)
                .apply {
                    addFlags(FLAG_ACTIVITY_NEW_TASK)
                    putExtra(DetailActivity.FACE_FILE_PATH, file)
                })
    }
}