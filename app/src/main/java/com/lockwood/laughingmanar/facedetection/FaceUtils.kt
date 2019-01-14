package com.lockwood.laughingmanar.facedetection

import android.graphics.*
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector
import com.lockwood.laughingmanar.App
import com.lockwood.laughingmanar.extensions.*
import com.lockwood.laughingmanar.mlkit.BitmapUtils
import com.lockwood.laughingmanar.mlkit.BitmapUtils.translateX
import com.lockwood.laughingmanar.mlkit.BitmapUtils.translateY
import com.lockwood.laughingmanar.mlkit.CameraSource

object FaceUtils {

    const val TAG = "FaceUtils"

    private val boxPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 1.0f
    }

    fun detectFacesAndOverlayImage(
        picture: Bitmap,
        overlayType: OverlayType,
        isFacingFront: Boolean,
        onResult: (Bitmap) -> Unit
    ) {
        val faceDetector: FirebaseVisionFaceDetector = FirebaseVision.getInstance().visionFaceDetector
        // Initialize result bitmap to original picture
        var resultBitmap = if (isFacingFront) {
            BitmapUtils.rotateBitmap(picture, 180, CameraSource.CAMERA_FACING_FRONT)
        } else {
            picture
        }
        val detectFaces = faceDetector.detectInImage(FirebaseVisionImage.fromBitmap(picture))
        detectFaces.addOnSuccessListener {
            it.forEach { face ->
                // Add the faceBitmap to the proper position in the original image
                resultBitmap = addBitmapToFace(resultBitmap, face, overlayType, isFacingFront)
            }
            onResult(resultBitmap)
        }
    }

    private fun addBitmapToFace(
        originBitmap: Bitmap,
        face: FirebaseVisionFace,
        overlayType: OverlayType,
        isFacingFront: Boolean
    ): Bitmap {
        val context = App.instance.applicationContext
        val res = App.instance.resources
        // Initialize the results bitmap to be a mutable copy of the original image
        val resultBitmap = Bitmap.createBitmap(originBitmap.width, originBitmap.height, originBitmap.config)
        val scaleFactor = 1.2f
        val resId = overlayType.resId

        // Create the canvas and draw the bitmaps to it
        val canvas = Canvas(resultBitmap)
        canvas.drawBitmap(originBitmap, 0f, 0f, null)
        when (overlayType) {
            OverlayType.STATIC_PNG -> {
                // static image
                var overlayBitmap: Bitmap = BitmapFactory.decodeResource(res, resId)
                val xOffset = face.width * scaleFactor
                val yOffset = face.height * scaleFactor
                // Scale the face
                overlayBitmap = Bitmap.createScaledBitmap(overlayBitmap, xOffset.toInt(), yOffset.toInt(), false)
                val x = translateX(face.centerX, 1.0f, canvas.width.toFloat(), isFacingFront)
                val y = translateY(face.centerY, 1.0f)
                val left = x - xOffset / 2
                val top = y - yOffset / 2
                // Draw it
                canvas.drawBitmap(overlayBitmap, left, top, null)
            }
            OverlayType.STATIC_SVG -> {
                val drawable = context.drawable(resId)
                val left = 0
                val top = 0
                val right = 100
                val bottom = 100
                drawable?.let {
                    it.setBounds(left, top, right, bottom)
                    it.draw(canvas)
                }
            }
            OverlayType.ANIMATED_GIF -> {
            }
            OverlayType.ANIMATED_SVG -> {
            }
        }
        return resultBitmap
    }

}