package com.lockwood.laughingmanar.ui

import android.os.Bundle
import com.lockwood.laughingmanar.R
import com.lockwood.laughingmanar.ui.fragments.CameraFragment

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            replaceFragment(CameraFragment())
        }
    }
}
