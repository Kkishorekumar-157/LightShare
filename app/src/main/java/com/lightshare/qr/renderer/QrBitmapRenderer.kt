package com.lightshare.qr.renderer

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.lightshare.qr.generator.QrPixels

object QrBitmapRenderer {
    fun toImageBitmap(q: QrPixels): ImageBitmap =
        Bitmap.createBitmap(q.argb, q.size, q.size, Bitmap.Config.ARGB_8888).asImageBitmap()
}
