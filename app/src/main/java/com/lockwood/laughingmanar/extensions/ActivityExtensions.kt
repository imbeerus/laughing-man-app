package com.lockwood.laughingmanar.extensions

import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity

fun AppCompatActivity.replaceFragment(id: Int, fragment: Fragment) {
    supportFragmentManager
        .beginTransaction()
        .replace(id, fragment)
        .commit()
}