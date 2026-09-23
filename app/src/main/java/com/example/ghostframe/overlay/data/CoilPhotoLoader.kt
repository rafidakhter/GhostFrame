package com.example.ghostframe.overlay.data

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import coil3.ImageLoader
import coil3.asDrawable
import coil3.request.ImageRequest
import coil3.size.Scale

class CoilPhotoLoader(
    context: Context,
    private val imageLoader: ImageLoader
) : PhotoLoader {

    private val appContext = context.applicationContext

    override fun load(
        uri: Uri,
        width: Int,
        height: Int,
        onSuccess: (Drawable) -> Unit,
        onError: (Throwable) -> Unit
    ): PhotoLoad {
        val request = ImageRequest.Builder(appContext)
            .data(uri)
            .size(width.coerceAtLeast(1), height.coerceAtLeast(1))
            .scale(Scale.FIT)
            .target(
                onSuccess = { image ->
                    // Coil applies EXIF orientation during decoding. Crop coordinates
                    // therefore use this oriented drawable, not raw file dimensions.
                    onSuccess(image.asDrawable(appContext.resources))
                }
            )
            .listener(
                onError = { _, result ->
                    onError(result.throwable)
                }
            )
            .build()

        val disposable = imageLoader.enqueue(request)

        return PhotoLoad { disposable.dispose() }
    }
}