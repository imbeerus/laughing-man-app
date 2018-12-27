package com.lockwood.laughingmanar.extensions

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity

val Fragment.ctx: FragmentActivity
    get() {
        activity?.let {
            return it
        }
    }