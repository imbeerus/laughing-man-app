package com.lockwood.laughingmanar.facedetection

import android.graphics.*
import android.graphics.Paint.Style
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.lockwood.laughingmanar.extensions.*
import com.lockwood.laughingmanar.mlkit.GraphicOverlay

class FaceGraphic(
    overlay: GraphicOverlay,
    private val firebaseVisionFace: FirebaseVisionFace?,
    private val facing: Int
) : GraphicOverlay.Graphic(overlay) {

    private val selectedImageType = ImageType.STATIC_PNG

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
        val scaleFactory = selectedImageType.scaleFactory
        val resId = selectedImageType.resId
        when (selectedImageType) {
            ImageType.STATIC_PNG -> {
                // static image
                var faceBitmap: Bitmap = BitmapFactory.decodeResource(appContext.resources, resId)
                val xOffset = (face.width * scaleFactory).toInt()
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
            ImageType.STATIC_SVG -> {
                val drawable = appContext.drawable(resId)
                val x = translateX(face.centerX)
                val y = translateY(face.centerY)
                val xOffset = face.width * scaleFactory
                val yOffset = face.height * scaleFactory
                val left = (x - xOffset).toInt()
                val top = (y - yOffset).toInt()
                val right = (x + xOffset).toInt()
                val bottom = (y + yOffset).toInt()
                drawable?.let {
                    it.setBounds(left, top, right, bottom)
                    it.draw(canvas)
                }
            }
            ImageType.ANIMATED_GIF -> {

            }
            ImageType.ANIMATED_SVG -> {

            }
        }
    }

    companion object {
        private const val BOX_STROKE_WIDTH = 1.0f
        private const val GRAPHIC_SCALE_FACTOR = 1.5f
    }
}