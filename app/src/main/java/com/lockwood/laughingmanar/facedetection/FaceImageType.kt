package com.lockwood.laughingmanar.facedetection

import com.lockwood.laughingmanar.R

enum class ImageType(val resId: Int, val scaleFactory: Float) {
    STATIC_PNG(R.drawable.face_static, 3.5f),
    STATIC_SVG(R.drawable.face_svg, 2.0f),
    ANIMATED_GIF(R.drawable.face_animated, 1.5f),
    ANIMATED_SVG(R.drawable.face_svg, 1.5f)
}