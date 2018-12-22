package com.lockwood.laughingmanar.camera

import android.media.Image
import android.util.Log
import com.lockwood.laughingmanar.extensions.TAG
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

internal class ImageSaver(private val image: Image, private val file: File) : Runnable {

    override fun run() {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        var output: FileOutputStream? = null
        try {
            output = FileOutputStream(file).apply {
                write(bytes)
            }
        } catch (e: IOException) {
            Log.e(TAG, e.toString())
        } finally {
            image.close()
            output?.let {
                try {
                    it.close()
                } catch (e: IOException) {
                    Log.e(TAG, e.toString())
                }
            }
        }
    }
}