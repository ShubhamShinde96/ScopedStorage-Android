package com.shubham.scopedstorage_android.data

import android.net.Uri

data class SharedImage(
    val id: Long,
    val name: String,
    val width: Int,
    val height: Int,
    val contentUri: Uri
)