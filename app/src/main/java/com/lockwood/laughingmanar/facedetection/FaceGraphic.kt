package com.lockwood.laughingmanar.facedetection

import android.graphics.*
import android.graphics.Paint.Style
import android.support.v4.content.ContextCompat
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

    private val selectedImageType = ImageType.STATIC_SVG

    private val boxPaint = Paint().apply {
        color = Color.WHITE
        style = Style.STROKE
        strokeWidth = BOX_STROKE_WIDTH
    }

    override fun draw(canvas: Canvas) {
        val face = firebaseVisionFace ?: return
//        drawFaceBorder(face_svg, canvas)
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

    // TODO: add select model
    private fun drawFaceImage(
        face: FirebaseVisionFace,
        canvas: Canvas
    ) {
        when (selectedImageType) {
            ImageType.STATIC_PNG -> {
                // static image
                var faceBitmap: Bitmap =
                    BitmapFactory.decodeResource(applicationContext.resources, R.drawable.face_static)
                val xOffset = (face.width * selectedImageType.scaleFactory).toInt()
                val yOffset = (face.height * selectedImageType.scaleFactory).toInt()
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
                val drawable = ContextCompat.getDrawable(applicationContext, R.drawable.face_svg)
                val x = translateX(face.centerX)
                val y = translateY(face.centerY)
                val xOffset = face.width * selectedImageType.scaleFactory
                val yOffset = face.height * selectedImageType.scaleFactory
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

    enum class ImageType(val scaleFactory: Float) {
        STATIC_PNG(3.5f),
        STATIC_SVG(2.0f),
        ANIMATED_GIF(GRAPHIC_SCALE_FACTOR),
        ANIMATED_SVG(GRAPHIC_SCALE_FACTOR)
    }

    companion object {
        private const val BOX_STROKE_WIDTH = 1.0f
        private const val GRAPHIC_SCALE_FACTOR = 1.5f
    }
}