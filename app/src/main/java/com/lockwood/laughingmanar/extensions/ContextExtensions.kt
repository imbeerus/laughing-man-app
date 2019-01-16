package com.lockwood.laughingmanar.extensions

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import java.io.File

fun Context.color(res: Int): Int = ContextCompat.getColor(this, res)

fun Context.drawable(res: Int): Drawable? = ContextCompat.getDrawable(this, res)

fun Context.drawable(res: Int, colorRes: Int): Drawable? {
    val drawable = drawable(res)
    drawable?.let { DrawableCompat.setTint(it, color(colorRes)) }
    return drawable
}

fun Context.galleryAddPic(image: File) {
    Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also { mediaScanIntent ->
        mediaScanIntent.data = Uri.fromFile(image)
        sendBroadcast(mediaScanIntent)
    }
}

fun Context.galleryAddPic(imagePath: String) = galleryAddPic(File(imagePath))