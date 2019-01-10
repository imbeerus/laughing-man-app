package com.lockwood.laughingmanar.facedetection

import android.graphics.*
import android.graphics.Paint.Style
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.lockwood.laughingmanar.R
import com.lockwood.laughingmanar.extensions.centerX
import com.lockwood.laughingmanar.extensions.centerY
import com.lockwood.laughingmanar.extensions.height
import com.lockwood.laughingmanar.extensions.width
import com.lockwood.laughingmanar.mlkit.GraphicOverlay

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
//        drawFaceBorder(face, canvas)
        drawFaceImage(face, canvas)
    }

    private fun drawFaceBorder(
        face: FirebaseVisionFace,
        canvas: Canvas
    ) {
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

    private fun drawFaceImage(
        face: FirebaseVisionFace,
        canvas: Canvas
    ) {
        var faceBitmap: Bitmap = BitmapFactory.decodeResource(applicationContext.resources, R.drawable.face)
        val scaleFactory = FACE_SCALE_FACTOR
        val xOffset = (face.width * scaleFactory).toInt()
//        val yOffset = (face.height * xOffset / face.width * scaleFactory).toInt()
        val yOffset = (face.height * scaleFactory).toInt()

        // Scale the face
        faceBitmap = Bitmap.createScaledBitmap(faceBitmap, xOffset, yOffset, false)
        val x = translateX(face.centerX)
        val y = translateY(face.centerY)
        val left = x - xOffset / 2
        val top = y - yOffset / 2
        // Draw it
        canvas.drawBitmap(faceBitmap, left, top, null)
    }

    companion object {
        private const val BOX_STROKE_WIDTH = 1.0f
        private const val GRAPHIC_SCALE_FACTOR = 1.5f
        private const val FACE_SCALE_FACTOR = 3.5f
    }
}