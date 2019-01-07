package com.lockwood.laughingmanar.camera

import android.content.Context
import android.media.Image
import android.util.Log
import com.lockwood.laughingmanar.extensions.TAG
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object SaveUtils {
    private const val BASE_FILE_NAME = "result-"
    private const val DATE_FORMAT = "yyyy-MM-dd-HH:mm:ss"

    const val FORMAT_PIC_FILE_NAME = ".jpg"
    const val FORMAT_VIDEO_FILE_NAME = ".mp4"

    @JvmStatic
    fun makeFile(ctx: Context, format: String): File {
        val fileName = getFilePath(format)
        return File(ctx.getExternalFilesDir(null), fileName)
    }

    @JvmStatic
    fun getFilePath(format: String): String {
        val df = SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH)
        val date = df.format(Calendar.getInstance().time) // current time
        return "$BASE_FILE_NAME$date$format"
    }
}

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