package com.lockwood.laughingmanar.mlkit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import com.lockwood.laughingmanar.mlkit.GraphicOverlay.Graphic

/** Draw camera image to background.  */
class CameraImageGraphic(overlay: GraphicOverlay, private val bitmap: Bitmap) : Graphic(overlay) {

    override fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, null, Rect(0, 0, canvas.width, canvas.height), null)
    }
}