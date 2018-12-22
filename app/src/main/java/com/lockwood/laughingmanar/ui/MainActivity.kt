package com.lockwood.laughingmanar.ui

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.lockwood.laughingmanar.R
import com.lockwood.laughingmanar.extensions.replaceFragment
import com.lockwood.laughingmanar.ui.fragments.CameraFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        savedInstanceState ?: replaceFragment(R.id.container, CameraFragment.newInstance())
    }
}