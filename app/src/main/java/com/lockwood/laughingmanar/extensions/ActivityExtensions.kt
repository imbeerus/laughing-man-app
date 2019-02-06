package com.lockwood.laughingmanar.extensions

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.net.Uri
import android.os.Environment

fun AppCompatActivity.openFolder(folder: String) {
    val intent = Intent(Intent.ACTION_GET_CONTENT)
    val uri = Uri.parse(Environment.getExternalStorageDirectory().path + "/$folder/")
    intent.setDataAndType(uri, "text/csv")
    startActivity(Intent.createChooser(intent, "Open folder"))
}

fun AppCompatActivity.openResFolder() = openFolder("Android/data/$packageName/files")