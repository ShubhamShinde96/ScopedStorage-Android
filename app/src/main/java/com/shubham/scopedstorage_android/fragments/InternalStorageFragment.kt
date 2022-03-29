package com.shubham.scopedstorage_android.fragments

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.shubham.scopedstorage_android.R
import com.shubham.scopedstorage_android.adapter.InternalStorageImageAdapter
import com.shubham.scopedstorage_android.data.InternalImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*


class InternalStorageFragment : Fragment() {

    private lateinit var activity: Activity

    private lateinit var adapterInternalStorageImages: InternalStorageImageAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is Activity) {
            activity = context
        }
    }

    private lateinit var captureImageFab: FloatingActionButton
    private lateinit var rvInternalStorage: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_internal_storage, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        captureImageFab = view.findViewById(R.id.captureImageFlab)
        rvInternalStorage = view.findViewById(R.id.rvInternalStorage)

        adapterInternalStorageImages = InternalStorageImageAdapter {

            lifecycleScope.launch {

                val isDeletionSuccessful = deleteImageFromInternalStorage(it.name)

                if (isDeletionSuccessful) {

                    loadImagesFromInternalStorageIntoRecyclerView()
                    Toast.makeText(
                        activity,
                        "Image successfully deleted.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {

                    Toast.makeText(
                        activity,
                        "Unable to delete the image!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        val takePhoto = registerForActivityResult(
            ActivityResultContracts.TakePicturePreview()
        ) {

            lifecycleScope.launch {

                savePhotoToInternalStorage(it)
                loadImagesFromInternalStorageIntoRecyclerView()
            }
        }

        captureImageFab.setOnClickListener {
            takePhoto.launch()
        }

        setupInternalStorageRecyclerview()
        loadImagesFromInternalStorageIntoRecyclerView()
    }

    private fun setupInternalStorageRecyclerview() = rvInternalStorage.apply {

        adapter = adapterInternalStorageImages
        layoutManager = GridLayoutManager(activity, 4)
    }

    private suspend fun loadImagesFromInternalStorage(): List<InternalImage> {

        return withContext(Dispatchers.IO) {

            val files = activity.filesDir.listFiles()

            files?.filter { it.isFile && it.canRead() && it.name.endsWith(".jpeg") }?.map {
                val bytes = it.readBytes()
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                InternalImage(it.name, bmp)
            } ?: listOf()
        }
    }

    private fun loadImagesFromInternalStorageIntoRecyclerView() {

        lifecycleScope.launch {

            val imagesList = loadImagesFromInternalStorage()
            adapterInternalStorageImages.differ.submitList(imagesList)
        }
    }

    private suspend fun savePhotoToInternalStorage(bmp: Bitmap?): Boolean {

        return withContext(Dispatchers.IO) {

            try {

                bmp?.let {

                    activity.openFileOutput(
                        "${UUID.randomUUID()}.jpeg",
                        AppCompatActivity.MODE_PRIVATE
                    ).use { fileOutputStram ->

                        if (!bmp.compress(Bitmap.CompressFormat.JPEG, 95, fileOutputStram)) {
                            throw IOException("Couldn't save the image!")
                        }
                        true
                    }
                } ?: false

            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private suspend fun deleteImageFromInternalStorage(name: String): Boolean {

        return withContext(Dispatchers.IO) {

            try {

                activity.deleteFile(name)

            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }


}