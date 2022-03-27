package com.shubham.scopedstorage_android

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.shubham.scopedstorage_android.adapter.InternalStorageImageAdapter
import com.shubham.scopedstorage_android.adapter.SharedStorageImageAdapter
import com.shubham.scopedstorage_android.data.InternalImage
import com.shubham.scopedstorage_android.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapterInternalStorageImages: InternalStorageImageAdapter
    private lateinit var adapterSharedStorageImages: SharedStorageImageAdapter

    private var readPermissionGranted = false
    private var writePermissionGranted = false

    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)


        adapterInternalStorageImages = InternalStorageImageAdapter {

            lifecycleScope.launch {

                val isDeletionSuccessful = deleteImageFromInternalStorage(it.name)

                if (isDeletionSuccessful) {

                    loadImagesFromInternalStorageIntoRecyclerView()
                    Toast.makeText(
                        this@MainActivity,
                        "Image successfully deleted.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {

                    Toast.makeText(
                        this@MainActivity,
                        "Unable to delete the image!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        adapterSharedStorageImages = SharedStorageImageAdapter {

        }

        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            readPermissionGranted =
                permissions[android.Manifest.permission.READ_EXTERNAL_STORAGE]
                    ?: readPermissionGranted

            writePermissionGranted =
                permissions[android.Manifest.permission.WRITE_EXTERNAL_STORAGE]
                    ?: writePermissionGranted

            if (readPermissionGranted) {

            } else {
                Toast
                    .makeText(
                        this@MainActivity,
                        "Can't read files, please grant the read permission",
                        Toast.LENGTH_LONG
                    ).show()
            }
        }

        val takePhoto = registerForActivityResult(
            ActivityResultContracts.TakePicturePreview()
        ) {

            lifecycleScope.launch {

                val isPrivate = binding.switchPrivate.isChecked

                when {
                    isPrivate -> savePhotoToInternalStorage(it)

                }

                if (isPrivate) {
                    loadImagesFromInternalStorageIntoRecyclerView()
                }

            }
        }

        binding.btnTakePhoto.setOnClickListener {
            takePhoto.launch()
        }

        setupInternalStorageRecyclerview()
        loadImagesFromInternalStorageIntoRecyclerView()
    }



// * * * * * * * * * * * * * * * * * * * * * * * * INTERNAL STORAGE * * * * * * * * * * * * * * * * * * * * * * * * * * *

    private fun setupInternalStorageRecyclerview() = binding.rvPrivatePhotos.apply {

        adapter = adapterInternalStorageImages
        layoutManager = StaggeredGridLayoutManager(3, RecyclerView.VERTICAL)
    }

    private suspend fun loadImagesFromInternalStorage(): List<InternalImage> {

        return withContext(Dispatchers.IO) {

            val files = filesDir.listFiles()

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

                    openFileOutput(
                        "${UUID.randomUUID()}.jpeg",
                        MODE_PRIVATE
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

                deleteFile(name)

            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    }
}



































