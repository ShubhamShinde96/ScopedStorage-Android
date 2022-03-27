package com.shubham.scopedstorage_android.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.shubham.scopedstorage_android.R
import com.shubham.scopedstorage_android.data.SharedImage
import com.shubham.scopedstorage_android.databinding.ItemPhotoBinding

class SharedStorageImageAdapter(
    val onImageClick: (SharedImage) -> Unit
): RecyclerView.Adapter<SharedStorageImageAdapter.ImageViewHolder>() {

    private val callback = object: DiffUtil.ItemCallback<SharedImage>() {
        override fun areItemsTheSame(oldItem: SharedImage, newItem: SharedImage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SharedImage, newItem: SharedImage): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, callback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {

        val layoutInflater = LayoutInflater.from(parent.context)

        val binding: ItemPhotoBinding = DataBindingUtil.inflate(layoutInflater, R.layout.item_photo, parent, false)

        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {

        holder.bind(differ.currentList[position])

    }

    override fun getItemCount(): Int = differ.currentList.size

    inner class ImageViewHolder(private val binding: ItemPhotoBinding): RecyclerView.ViewHolder(binding.root) {

        fun bind(sharedImage: SharedImage) {

            binding.ivPhoto.setImageURI(sharedImage.contentUri)

            val aspectRatio = sharedImage.width.toFloat() / sharedImage.height.toFloat()
            ConstraintSet().apply {
                clone(binding.constraint)
                setDimensionRatio(binding.ivPhoto.id, aspectRatio.toString())
                applyTo(binding.constraint)
            }

            binding.ivPhoto.setOnLongClickListener {

                onImageClick(differ.currentList[adapterPosition])
                true
            }

        }
    }



}