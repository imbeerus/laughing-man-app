package com.lockwood.laughingmanar.extensions

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat

fun Context.color(res: Int): Int = ContextCompat.getColor(this, res)

fun Context.drawable(res: Int): Drawable? = ContextCompat.getDrawable(this, res)

fun Context.drawable(res: Int, colorRes: Int): Drawable? {
    val drawable = drawable(res)
    drawable?.let { DrawableCompat.setTint(it, color(colorRes)) }
    return drawable
}