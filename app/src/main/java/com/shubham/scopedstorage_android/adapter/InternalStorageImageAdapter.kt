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
import com.shubham.scopedstorage_android.data.InternalImage

class InternalStorageImageAdapter(
    val onImageClick: (InternalImage) -> Unit
): RecyclerView.Adapter<InternalStorageImageAdapter.ImageViewHolder>() {

    private val callback = object: DiffUtil.ItemCallback<InternalImage>() {
        override fun areItemsTheSame(oldItem: InternalImage, newItem: InternalImage): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: InternalImage, newItem: InternalImage): Boolean {
            return oldItem.name == newItem.name && oldItem.bmp.sameAs(newItem.bmp)
        }
    }

    val differ = AsyncListDiffer(this, callback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {

        val layoutInflater: LayoutInflater = LayoutInflater.from(parent.context)
        val listItem = layoutInflater.inflate(R.layout.item_photo, parent, false)

        return ImageViewHolder(listItem)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {

        holder.bind(differ.currentList[position])

    }

    override fun getItemCount(): Int = differ.currentList.size

    inner class ImageViewHolder(val view: View): RecyclerView.ViewHolder(view) {

        val image: ImageView = view.findViewById(R.id.ivPhoto)

        fun bind(internalImage: InternalImage) {

            Glide.with(image.context)
                .load(internalImage.bmp)
                .placeholder(R.drawable.img_placeholder)
                .apply(RequestOptions().override(180, 180))
                .centerCrop()
                .into(image)

            image.setOnLongClickListener {

                onImageClick(differ.currentList[adapterPosition])
                true
            }

        }
    }

}