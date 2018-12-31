package com.lockwood.laughingmanar.extensions

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity

val AppCompatActivity.currentFragment
    get() = supportFragmentManager.fragments.first()

fun AppCompatActivity.replaceFragment(id: Int, fragment: Fragment) {
    supportFragmentManager
        .beginTransaction()
        .replace(id, fragment)
        .commit()
}

fun Activity.openFolder(folder: String) {
    val intent = Intent(Intent.ACTION_GET_CONTENT)
    val uri = Uri.parse(Environment.getExternalStorageDirectory().path + "/$folder/")
    intent.setDataAndType(uri, "text/csv")
    startActivity(Intent.createChooser(intent, "Open folder"))
}

fun Activity.openResFolder() = openFolder("Android/data/$packageName/files")