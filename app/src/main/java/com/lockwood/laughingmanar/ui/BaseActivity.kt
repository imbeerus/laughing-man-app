package com.lockwood.laughingmanar.ui

import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import com.lockwood.laughingmanar.R

abstract class BaseActivity : AppCompatActivity() {

    fun replaceFragment(fragmentToReplace: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, fragmentToReplace)
            .commit()
    }
}