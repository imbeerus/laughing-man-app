package com.lockwood.laughingmanar.ui

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import com.lockwood.laughingmanar.R
import com.lockwood.laughingmanar.extensions.currentFragment
import com.lockwood.laughingmanar.extensions.replaceFragment
import com.lockwood.laughingmanar.ui.fragments.CameraFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        savedInstanceState ?: replaceFragment(R.id.container, CameraFragment.newInstance())
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            (currentFragment as CameraFragment).onKeyDown(keyCode, event)
        }
        return super.onKeyDown(keyCode, event)
    }
}