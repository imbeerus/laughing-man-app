package com.lockwood.laughingmanar.extensions

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Environment

fun Activity.openFolder(folder: String) {
    val intent = Intent(Intent.ACTION_GET_CONTENT)
    val uri = Uri.parse(Environment.getExternalStorageDirectory().path + "/$folder/")
    intent.setDataAndType(uri, "text/csv")
    startActivity(Intent.createChooser(intent, "Open folder"))
}

fun Activity.openResFolder() = openFolder("Android/data/$packageName/files")