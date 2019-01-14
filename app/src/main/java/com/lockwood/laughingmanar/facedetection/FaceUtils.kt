package com.lockwood.laughingmanar.facedetection

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector

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
        var resultBitmap = picture
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
        // Initialize the results bitmap to be a mutable copy of the original image
        val resultBitmap = Bitmap.createBitmap(originBitmap.width, originBitmap.height, originBitmap.config)
        var scaleFactor = overlayType.scaleFactory
        var resId = overlayType.resId

        val idPaint = Paint()
        idPaint.color = Color.WHITE
        idPaint.textSize = 30.0f

        val canvas = Canvas(resultBitmap)
        canvas.drawBitmap(originBitmap, 0.0f, 0.0f, null)
        canvas.drawText("TEST", 100.0f, 100.0f, idPaint)

//        var emojiBitmap = emojiBitmap
//
//        // Initialize the results bitmap to be a mutable copy of the original image
//        val resultBitmap = Bitmap.createBitmap(
//            originBitmap.width,
//            originBitmap.height, originBitmap.config
//        )
//
//        // Scale the emoji so it looks better on the face
//        val scaleFactor = EMOJI_SCALE_FACTOR
//
//        // Determine the size of the emoji to match the width of the face and preserve aspect ratio
//        val newEmojiWidth = (face.getWidth() * scaleFactor) as Int
//        val newEmojiHeight = (emojiBitmap.height * newEmojiWidth / emojiBitmap.width * scaleFactor).toInt()
//
//
//        // Scale the emoji
//        emojiBitmap = Bitmap.createScaledBitmap(emojiBitmap, newEmojiWidth, newEmojiHeight, false)
//
//        // Determine the emoji position so it best lines up with the face
//        val emojiPositionX = face.getPosition().x + face.getWidth() / 2 - emojiBitmap.width / 2
//        val emojiPositionY = face.getPosition().y + face.getHeight() / 2 - emojiBitmap.height / 3
//
//        // Create the canvas and draw the bitmaps to it
//        val canvas = Canvas(resultBitmap)
//        canvas.drawBitmap(originBitmap, 0f, 0f, null)
//        canvas.drawBitmap(emojiBitmap, emojiPositionX, emojiPositionY, null)
//
//        return resultBitmap
        return originBitmap
    }

}