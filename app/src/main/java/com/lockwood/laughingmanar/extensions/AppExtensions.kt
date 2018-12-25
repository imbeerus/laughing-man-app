package com.lockwood.laughingmanar.extensions

import com.lockwood.laughingmanar.App
import com.lockwood.laughingmanar.R


val isRTL: Boolean
    get() = App.instance.resources.getBoolean(R.bool.is_right_to_left)

val packageName: String
    get() = App.instance.packageName