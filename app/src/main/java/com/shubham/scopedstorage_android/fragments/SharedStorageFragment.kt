package com.shubham.scopedstorage_android.fragments

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.shubham.scopedstorage_android.R
import com.shubham.scopedstorage_android.adapter.SharedStorageImageAdapter
import com.shubham.scopedstorage_android.data.SharedImage
import com.shubham.scopedstorage_android.util.sdk29AndUp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*

class SharedStorageFragment : Fragment() {

    private lateinit var activity: Activity

    private lateinit var adapterSharedStorageImages: SharedStorageImageAdapter

    private var readPermissionGranted = false
    private var writePermissionGranted = false

    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is Activity) {
            activity = context
        }
    }

    private lateinit var captureImageFab: FloatingActionButton
    private lateinit var rvSharedStorage: RecyclerView

    private lateinit var contentObserver: ContentObserver

    private lateinit var intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>

    private var deletedImageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_shared_storage, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        captureImageFab = view.findViewById(R.id.captureImageFlab)
        rvSharedStorage = view.findViewById(R.id.rvSharedStorage)

        adapterSharedStorageImages = SharedStorageImageAdapter {

            lifecycleScope.launch {
                deletePhotoFromExternalStorage(it.contentUri)
                deletedImageUri = it.contentUri
            }
        }

        setupExternalStorageRecyclerview()
        initContentObserver()

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
                loadImagesFromExternalStorageIntoRecyclerView()
            } else {
                Toast
                    .makeText(
                        activity,
                        "Can't read files, please grant the read permission",
                        Toast.LENGTH_LONG
                    ).show()
            }
        }

        updateOrRequestPermission()

        val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) {

            lifecycleScope.launch {

                val isSavedSuccessfully = if (writePermissionGranted) {
                    savePhotoToExternalStorage(UUID.randomUUID().toString(), it)
                } else {
                    false
                }

                if (isSavedSuccessfully) {
                    Toast.makeText(
                        activity,
                        "Photo saved successfully.",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    Toast.makeText(activity, "Failed to save photo!", Toast.LENGTH_SHORT)
                        .show()
                }

            }
        }

        captureImageFab.setOnClickListener {
            takePhoto.launch()
        }

        if (readPermissionGranted) {
            Log.d("PHOTO_LOG", "readPErmissionGranted == true")
            loadImagesFromExternalStorageIntoRecyclerView()
        }

        intentSenderLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) {

            if (it.resultCode == RESULT_OK) {

                Toast.makeText(activity, "Photo deleted successfully.", Toast.LENGTH_LONG).show()

                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {

                    lifecycleScope.launch {
                        deletePhotoFromExternalStorage(deletedImageUri ?: return@launch)
                    }
                }
            } else {

                Toast.makeText(activity, "Photo couldn't be deleted!", Toast.LENGTH_LONG).show()
            }
        }

    }

    private fun updateOrRequestPermission() {

        val hasReadPermission = ContextCompat.checkSelfPermission(
            activity,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        val hasWritePermission = ContextCompat.checkSelfPermission(
            activity,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        val minSdk29 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        readPermissionGranted = hasReadPermission
        writePermissionGranted = hasWritePermission || minSdk29

        val permissionsToRequest = mutableListOf<String>()

        if (!readPermissionGranted) {
            permissionsToRequest.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (!writePermissionGranted) {
            permissionsToRequest.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun setupExternalStorageRecyclerview() = rvSharedStorage.apply {

        adapter = adapterSharedStorageImages
//        layoutManager = StaggeredGridLayoutManager(3, RecyclerView.VERTICAL)
        layoutManager = GridLayoutManager(activity, 4)
    }

    private fun loadImagesFromExternalStorageIntoRecyclerView() {

        lifecycleScope.launch(Dispatchers.IO) {

            val sharedImageList = loadPhotosFromExternalStorage()

            withContext(Dispatchers.Main) {
                adapterSharedStorageImages.differ.submitList(sharedImageList)
            }

            Log.d("PHOTO_LOG", "photoList.size(): ${sharedImageList.size}")
        }
    }

    private suspend fun loadPhotosFromExternalStorage(): List<SharedImage> {

        return withContext(Dispatchers.IO) {

            val collection = sdk29AndUp {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } ?: MediaStore.Images.Media.EXTERNAL_CONTENT_URI

            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
            )

            val photos = mutableListOf<SharedImage>()

            activity.contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DISPLAY_NAME} ASC"
            )?.use { cursor ->

                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val displayNameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

                while (cursor.moveToNext()) {

                    val id = cursor.getLong(idColumn)
                    val displayName = cursor.getString(displayNameColumn)
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    photos.add(SharedImage(id, displayName, width, height, contentUri))
                }

                photos.toList()
            } ?: listOf()

        }
    }

    private suspend fun savePhotoToExternalStorage(displayName: String, bmp: Bitmap?): Boolean {

        bmp?.let {

            return withContext(Dispatchers.IO) {

                val imageCollection = sdk29AndUp {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } ?: MediaStore.Images.Media.EXTERNAL_CONTENT_URI

                // Store MetaData using ContentValues(), so that other apps can use this data
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.jpeg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.WIDTH, bmp.width)
                    put(MediaStore.Images.Media.HEIGHT, bmp.height)
                }

                try {

                    activity.contentResolver.insert(imageCollection, contentValues)?.also { uri ->

                        activity.contentResolver.openOutputStream(uri).use { outputStream ->

                            if (!bmp.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) {
                                throw IOException("Couldn't save the image!")
                            }
                        }
                    } ?: throw IOException("Couldn't create the MediaStore entry!")

                    true

                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }
        } ?: return false
    }

    private fun initContentObserver() {

        contentObserver = object : ContentObserver(null) {

            override fun onChange(selfChange: Boolean) {

                if (readPermissionGranted) {
                    loadImagesFromExternalStorageIntoRecyclerView()
                }
            }
        }

        activity.contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver
        )
    }

    override fun onDestroy() {
        activity.contentResolver.unregisterContentObserver(contentObserver)
        super.onDestroy()
    }


    private suspend fun deletePhotoFromExternalStorage(imageUri: Uri) {

        withContext(Dispatchers.IO) {

            try {

                activity.contentResolver.delete(imageUri, null, null)

            } catch (e: SecurityException) {
                e.printStackTrace()

                val intentSender = when {

                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                        MediaStore.createDeleteRequest(
                            activity.contentResolver,
                            listOf(imageUri)
                        ).intentSender
                    }

                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {

                        val recoverableSecutiryException = e as? RecoverableSecurityException
                        recoverableSecutiryException?.userAction?.actionIntent?.intentSender
                    }

                    else -> null
                }

                intentSender?.let { intentSender ->
                    intentSenderLauncher.launch (
                        IntentSenderRequest.Builder(intentSender).build()
                    )
                }
            }
        }
    }

}