package com.shubham.scopedstorage_android.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.shubham.scopedstorage_android.R
import com.shubham.scopedstorage_android.data.SharedImage


class SharedStorageImageAdapter(
    val onImageClick: (SharedImage) -> Unit
) : RecyclerView.Adapter<SharedStorageImageAdapter.ImageViewHolder>() {

    private val callback = object : DiffUtil.ItemCallback<SharedImage>() {
        override fun areItemsTheSame(oldItem: SharedImage, newItem: SharedImage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SharedImage, newItem: SharedImage): Boolean {
            return oldItem.name == newItem.name && oldItem.contentUri == newItem.contentUri
        }
    }

    val differ = AsyncListDiffer(this, callback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {

        /*val layoutInflater = LayoutInflater.from(parent.context)

        val binding: ItemPhotoBinding = DataBindingUtil.inflate(layoutInflater, R.layout.item_photo, parent, false)
//        val binding = ItemPhotoBinding.inflate(layoutInflater, parent, false)

        return ImageViewHolder(binding)*/

        val layoutInflater: LayoutInflater = LayoutInflater.from(parent.context)
        val listItem = layoutInflater.inflate(R.layout.item_photo, parent, false)

        return ImageViewHolder(listItem)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {

        holder.bind(differ.currentList[position])

    }
    override fun getItemCount(): Int = differ.currentList.size

    inner class ImageViewHolder(val view: View) :
        RecyclerView.ViewHolder(view) {

        val image = view.findViewById<ImageView>(R.id.ivPhoto)

        fun bind(sharedImage: SharedImage) {

            Glide.with(image.context)
                .load(differ.currentList[adapterPosition].contentUri)
                .placeholder(R.drawable.img_placeholder)
                .apply(RequestOptions().override(180, 180))
                .into(image)

            image.setOnLongClickListener {

                onImageClick(differ.currentList[adapterPosition])
                true
            }

        }
    }


}