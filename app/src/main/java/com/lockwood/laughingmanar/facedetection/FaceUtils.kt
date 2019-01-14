package com.lockwood.laughingmanar.facedetection

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector
import com.lockwood.laughingmanar.App
import com.lockwood.laughingmanar.extensions.*
import com.lockwood.laughingmanar.mlkit.BitmapUtils
import com.lockwood.laughingmanar.mlkit.CameraSource

object FaceUtils {

    const val TAG = "FaceUtils"

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
                resultBitmap = addBitmapToFace(resultBitmap, face, overlayType)
            }
            onResult(resultBitmap)
        }
    }

    private fun addBitmapToFace(
        originBitmap: Bitmap,
        face: FirebaseVisionFace,
        overlayType: OverlayType
    ): Bitmap {
        val context = App.instance.applicationContext
        val res = App.instance.resources
        // Initialize the results bitmap to be a mutable copy of the original image
        val resultBitmap = Bitmap.createBitmap(originBitmap.width, originBitmap.height, originBitmap.config)
        val scaleFactor = 1.25f
        val resId = overlayType.resId

        // Create the canvas and draw the bitmaps to it
        val canvas = Canvas(resultBitmap)
        canvas.drawBitmap(originBitmap, 0f, 0f, null)
        when (overlayType) {
            OverlayType.STATIC_PNG -> {
                var overlayBitmap = BitmapFactory.decodeResource(res, resId)
                // Determine the size of the overlay to match the width of the face and preserve aspect ratio
                val newOverlayWidth = (face.width * scaleFactor).toInt()
                val newOverlayHeight = (face.height * newOverlayWidth / face.width * scaleFactor).toInt()
                // Scale the overlay
                overlayBitmap = Bitmap.createScaledBitmap(overlayBitmap, newOverlayWidth, newOverlayHeight, false)
                // Determine the overlay position so it best lines up with the face
                val left = face.x + face.width / 2 - overlayBitmap.width / 2
                val top = face.y + face.height / 2 - overlayBitmap.height / 2
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