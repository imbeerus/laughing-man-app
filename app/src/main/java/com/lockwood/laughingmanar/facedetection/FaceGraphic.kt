package com.lockwood.laughingmanar.facedetection

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Style
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.lockwood.laughingmanar.extensions.centerX
import com.lockwood.laughingmanar.extensions.centerY
import com.lockwood.laughingmanar.extensions.height
import com.lockwood.laughingmanar.extensions.width
import com.lockwood.laughingmanar.mlkit.GraphicOverlay


/**
 * Graphic instance for rendering face position, orientation, and landmarks within an associated
 * graphic overlay view.
 */
class FaceGraphic(
    overlay: GraphicOverlay,
    private val firebaseVisionFace: FirebaseVisionFace?,
    private val facing: Int
) : GraphicOverlay.Graphic(overlay) {

    private val boxPaint = Paint().apply {
        color = Color.WHITE
        style = Style.STROKE
        strokeWidth = BOX_STROKE_WIDTH
    }

    override fun draw(canvas: Canvas) {
        val face = firebaseVisionFace ?: return

        // Draws a circle at the position of the detected face, with the face's track id below.
        val x = translateX(face.centerX)
        val y = translateY(face.centerY)
        val xOffset = face.width * GRAPHIC_SCALE_FACTOR
        val yOffset = face.height * GRAPHIC_SCALE_FACTOR
        val left = x - xOffset
        val top = y - yOffset
        val right = x + xOffset
        val bottom = y + yOffset
        canvas.drawRect(left, top, right, bottom, boxPaint)
    }

    companion object {
        private const val BOX_STROKE_WIDTH = 1.0f
        private const val GRAPHIC_SCALE_FACTOR = 1.5f
    }
}