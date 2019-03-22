package com.example.dahaka.mycam.ui.detail

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.support.annotation.RequiresApi
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import com.example.dahaka.mycam.R
import com.example.dahaka.mycam.ui.ImageRequest
import com.example.dahaka.mycam.ui.adapter.ImagePagerAdapter
import com.example.dahaka.mycam.ui.fragment.DeletePhotoDialogFragment
import com.example.dahaka.mycam.ui.viewModel.DetailViewModel
import com.example.dahaka.mycam.util.APP_NAME
import com.example.dahaka.mycam.util.DepthPageTransformer
import kotlinx.android.synthetic.main.activity_detail.*
import kotlinx.android.synthetic.main.detail_activity_toolbar.*
import okhttp3.MediaType
import okhttp3.RequestBody
import org.koin.android.architecture.ext.viewModel
import java.io.File

private const val IMAGE_JPEG = "image/jpeg"

class DetailActivity : AppCompatActivity(), DeletePhotoDialogFragment.OkListener {
    private val viewPager by lazy { findViewById<ViewPager>(R.id.viewpager) }
    private val adapter by lazy { ImagePagerAdapter(this, photosList) }
    private val detailViewModel by viewModel<DetailViewModel>()
    private val photosList = mutableListOf<String>()
    private val filePosition by lazy { intent.getIntExtra(FILE_POSITION, 0) }
    private val filePath by lazy { intent.getStringExtra(FILE_PATH) }
    private var viewPagerPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            setStatusBarTranslucent(true)
        }
//        prepareListForAdapter()
        viewPager.adapter = adapter
        viewPager.setPageTransformer(true, DepthPageTransformer())
//        getItemPosition(filePosition, filePath)
        getImage()
        share.setOnClickListener {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // temp permission for receiving app to read this file
                setDataAndType(Uri.parse(photosList[filePosition]), contentResolver.getType(Uri.parse(photosList[filePosition])))
                putExtra(Intent.EXTRA_STREAM, Uri.parse(photosList[viewPagerPosition]))
                type = getString(R.string.jpg_type)
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.choose_app)))
        }
        delete.setOnClickListener {
            detailViewModel.startDeleteDialog(this.supportFragmentManager)
        }
        viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                viewPagerPosition = position
            }
        })
        face.setOnClickListener {
            detailViewModel.startFaceActivity(photosList[viewPager.currentItem])
            progressbar.visibility = View.VISIBLE
        }
        retry.setOnClickListener{onBackPressed()}
        done.setOnClickListener {  }
    }

    private fun getItemPosition(position: Int, path: String?) {
        if (path != null && path != "") {
            for (file in photosList) {
                if (file == path) {
                    viewPager.currentItem = photosList.indexOf(file)
                }
            }
        } else {
            viewPager.currentItem = position
        }
    }

    private fun getImage() {
        val f = externalCacheDir
        val file = File(f, "image.jpg")
//        Glide.with(this)
//                .load(file)
//                .into(detail_image)
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        detail_image.setImageBitmap(bitmap)
    }

    private fun uploadImage(file : File){
            val req = ImageRequest(ImageRequest.Data("hello"), file)
            var body: RequestBody = RequestBody.create(MediaType.parse(IMAGE_JPEG), it)
                if (imagePath == null || TextUtils.isEmpty(imagePath)) {
                    req.data.apply {
                        this.file = filePath
                        media_type = mediaType
                    }
                }
                req.file?.let {
                    body =
                }
                addDisposable(userMerchantService.editPost(req.data, body)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribe({ result -> onSuccess(isVideo, result) },
                                { error ->
                                    error
                                }
                        ))
    }

    override fun onOkButtonClicked() {
        val file = File(photosList[viewPagerPosition])
        file.delete()
        adapter.removeItem(viewPagerPosition)
        photosList.removeAt(viewPagerPosition)
        viewPager.adapter = adapter
        viewPager.currentItem = viewPagerPosition
    }

    private fun prepareListForAdapter() {
        photosList.clear()
        photosList.addAll(getListOfPictures())
    }

    private fun getListOfPictures(): List<String> {
        val list = mutableListOf<String>()
        val f = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), APP_NAME)
        val file = f.listFiles()
        file.sortByDescending { it }
        file.forEach { list.add(it.path) }
        return list
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun setStatusBarTranslucent(makeTranslucent: Boolean) {
        if (makeTranslucent) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
    }

    override fun onResume() {
        super.onResume()
        progressbar.visibility = View.GONE
    }

    override fun onBackPressed() {
        super.onBackPressed()
        externalCacheDir.delete()
    }

    override fun onStop() {
        super.onStop()
        progressbar.visibility = View.GONE
        externalCacheDir.delete()
    }

    companion object {
        const val FILE_PATH = "path"
        const val FACE_FILE_PATH = "face"
        const val FILE_POSITION = "viewPagerPosition"
    }
}