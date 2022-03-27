package com.shubham.scopedstorage_android.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.shubham.scopedstorage_android.R
import com.shubham.scopedstorage_android.data.InternalImage
import com.shubham.scopedstorage_android.databinding.ItemPhotoBinding

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

        val layoutInflater = LayoutInflater.from(parent.context)

        val binding: ItemPhotoBinding = DataBindingUtil.inflate(layoutInflater, R.layout.item_photo, parent, false)

        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {

        holder.bind(differ.currentList[position])

    }

    override fun getItemCount(): Int = differ.currentList.size

    inner class ImageViewHolder(private val binding: ItemPhotoBinding): RecyclerView.ViewHolder(binding.root) {

        fun bind(internalImage: InternalImage) {

            binding.ivPhoto.setImageBitmap(internalImage.bmp)

            val aspectRatio = internalImage.bmp.width.toFloat() / internalImage.bmp.height.toFloat()
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