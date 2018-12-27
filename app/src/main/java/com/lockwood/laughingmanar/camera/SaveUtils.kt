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
    private const val BASE_PIC_FILE_NAME = "result-"
    private const val FORMAT_PIC_FILE_NAME = ".jpg"
    private const val DATE_FORMAT = "yyyy-MM-dd-HH:mm:ss"

    @JvmStatic
    fun makeFile(ctx: Context): File {
        val df = SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH)
        val date = df.format(Calendar.getInstance().time) // current time
        val fileName = "$BASE_PIC_FILE_NAME$date$FORMAT_PIC_FILE_NAME"
        return File(ctx.getExternalFilesDir(null), fileName)
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