package com.example.dahaka.mycam.ui.adapter

import android.content.Context
import android.support.v4.view.PagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.example.dahaka.mycam.R

class ImagePagerAdapter(val context: Context, images: MutableList<String>) : PagerAdapter() {
    private val imageList: MutableList<String> = ArrayList(images)

    override fun getCount(): Int {
        return imageList.size
    }

    override fun isViewFromObject(view: View, any: Any): Boolean {
        return view == any as ImageView
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val filePath = imageList[position]
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.pager_item, container, false) as ViewGroup
        val imageView = layout.findViewById<ImageView>(R.id.img)
        if (imageView.parent != null) {
            (imageView.parent as ViewGroup).removeView(imageView)
        }
        container.addView(imageView)
        Glide.with(context)
                .load(filePath)
                .into(imageView)
        return imageView
    }

    override fun destroyItem(container: ViewGroup, position: Int, any: Any) {
        container.removeView(any as? ImageView)
    }

    fun removeItem(position: Int) {
        imageList.remove(imageList[position])
        notifyDataSetChanged()
    }
}