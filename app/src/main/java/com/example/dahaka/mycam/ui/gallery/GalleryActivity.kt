package com.example.dahaka.mycam.ui.gallery

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.os.Environment
import android.support.transition.ChangeTransform
import android.support.transition.TransitionManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnimationUtils
import com.example.dahaka.mycam.R
import com.example.dahaka.mycam.ui.adapter.GalleryAdapter
import com.example.dahaka.mycam.ui.fragment.DeletePhotoDialogFragment
import com.example.dahaka.mycam.ui.viewModel.GalleryViewModel
import com.example.dahaka.mycam.util.APP_NAME
import kotlinx.android.synthetic.main.activity_gallery.*
import kotlinx.android.synthetic.main.toolbar.*
import org.koin.android.architecture.ext.viewModel
import java.io.File

class GalleryActivity : AppCompatActivity(), GalleryAdapter.ItemClickListener, DeletePhotoDialogFragment.OkListener {
    private val galleryViewModel by viewModel<GalleryViewModel>()
    private val photosList = mutableListOf<String>()
    private val layoutManager by lazy { GridLayoutManager(applicationContext, defValue) }
    private val adapter: GalleryAdapter by lazy { GalleryAdapter(this, photosList, this) }
    private val defValue = 3
    private var isRotated = false
    private var deletePosition = 0
    private val changeTransform = ChangeTransform().apply {
        duration = 200
        interpolator = AccelerateInterpolator()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)
        prepareListForAdapter()
        initAdapter(recycler, defValue)
        galleryViewModel.getSpanCount().observe(this, Observer { count ->
            galleryViewModel.setRvPosition(layoutManager.findFirstVisibleItemPosition())
            reloadAnimationForSpans()
            initAdapter(recycler, count)
        })
        galleryViewModel.getFilesQuantity().observe(this, Observer { files ->
            if (files == 0) {
                noPhoto.visibility = View.VISIBLE
            } else if (files != 0 && noPhoto.visibility == View.VISIBLE) {
                noPhoto.visibility = View.GONE
            }
            prepareListForAdapter()
            adapter.refreshList(photosList)
        })
        changeSpanCount.setOnClickListener {
            TransitionManager.beginDelayedTransition(toolbarContainer, changeTransform)
            galleryViewModel.changeSpanCount()
            toggleRotation(spanImage)
        }
        fab.setOnClickListener { galleryViewModel.startCameraActivity(this) }
        qrCode.setOnClickListener { galleryViewModel.startBarcodeActivity(this) }
        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && fab.visibility == View.VISIBLE) {
                    fab.hide()
                } else if (dy < 0 && fab.visibility != View.VISIBLE) {
                    fab.show()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        galleryViewModel.refreshGalleryItemsCount()
    }

    private fun toggleRotation(v: View) {
        if (isRotated) {
            v.rotation = 0.0f
            isRotated = false
        } else {
            v.rotation = 90.0f
            isRotated = true
        }
    }

    private fun prepareListForAdapter() {
        photosList.clear()
        photosList.addAll(getListOfPictures())
    }

    private fun initAdapter(recyclerView: RecyclerView, count: Int?) {
        recyclerView.setHasFixedSize(true)
        layoutManager.spanCount = count ?: defValue
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
        recyclerView.layoutManager.scrollToPosition(galleryViewModel.getRvPosition())
    }

    override fun onItemClicked(viewHolder: GalleryAdapter.ViewHolder) {
        galleryViewModel.startDetailActivity(this, viewHolder.adapterPosition)
    }

    override fun onItemLongClicked(viewHolder: GalleryAdapter.ViewHolder) {
        deletePosition = viewHolder.adapterPosition
        galleryViewModel.startDeleteDialog(this.supportFragmentManager)
    }

    override fun onOkButtonClicked() {
        val file = File(photosList[deletePosition])
        file.delete()
        photosList.removeAt(deletePosition)
        adapter.deleteItem(deletePosition)
    }

    private fun reloadAnimationForSpans() {
        recycler.layoutAnimation = AnimationUtils.loadLayoutAnimation(this, R.anim.grid_layout_animation)
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
}