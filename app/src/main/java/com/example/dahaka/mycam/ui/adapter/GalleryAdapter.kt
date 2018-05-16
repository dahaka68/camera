package com.example.dahaka.mycam.ui.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.dahaka.mycam.R
import kotlinx.android.synthetic.main.gallery_item.view.*

class GalleryAdapter(val context: Context, photos: List<String> = emptyList(),
                     private val itemClickListener: GalleryAdapter.ItemClickListener)
    : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {
    private val photoList: MutableList<String> = ArrayList(photos)

    fun refreshList(list: List<String>) {
        photoList.clear()
        photoList.addAll(list)
        notifyDataSetChanged()
    }

    fun deleteItem(position: Int) {
        photoList.remove(photoList[position])
        notifyItemRemoved(position)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): GalleryAdapter.ViewHolder {
        return ViewHolder(LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.gallery_item, viewGroup, false), itemClickListener)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.bindItems(photoList[position])
    }

    override fun getItemCount(): Int {
        return photoList.size
    }

    class ViewHolder(val view: View, itemClickListener: GalleryAdapter.ItemClickListener) : RecyclerView.ViewHolder(view) {
        fun bindItems(photoPath: String) {
            Glide.with(itemView.context)
                    .load(photoPath)
                    .into(view.gallery_image)
        }

        init {
            itemView.setOnClickListener {
                itemClickListener.onItemClicked(this)
            }
            itemView.setOnLongClickListener {
                itemClickListener.onItemLongClicked(this)
                false
            }
        }
    }

    interface ItemClickListener {
        fun onItemClicked(viewHolder: ViewHolder)
        fun onItemLongClicked(viewHolder: ViewHolder)
    }
}