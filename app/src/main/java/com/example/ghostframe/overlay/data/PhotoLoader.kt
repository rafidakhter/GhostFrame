package com.example.ghostframe.overlay.data

import android.graphics.drawable.Drawable
import android.net.Uri

interface PhotoLoader {
    fun load(
        uri: Uri,
        width: Int,
        height: Int,
        onSuccess: (Drawable) -> Unit,
        onError: (Throwable) -> Unit
    ): PhotoLoad
}

fun interface PhotoLoad {
    fun cancel()
}