package com.lockwood.laughingmanar.facedetection

import android.graphics.Bitmap
import android.graphics.Canvas
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions

object FaceUtils {

    const val TAG = "FaceUtils"

    private val faceDetector: FirebaseVisionFaceDetector
        get() {
            val options = FirebaseVisionFaceDetectorOptions.Builder()
                .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                .build()
            return FirebaseVision.getInstance().getVisionFaceDetector(options)
        }

    fun detectFacesandOverlayImage(originalBitmap: Bitmap, imageType: ImageType, isFacingFront: Boolean): Bitmap {
        var resultBitmap = originalBitmap
        val faces = faceDetector.detectInImage(FirebaseVisionImage.fromBitmap(resultBitmap))
        faces.addOnSuccessListener {
            it.forEach { face ->
                // Initialize the results bitmap to be a mutable copy of the original image
                resultBitmap = Bitmap.createBitmap(originalBitmap.width, originalBitmap.height, originalBitmap.config)
                // Init the canvas for draw the bitmaps to it
                val canvas = Canvas(resultBitmap)
            }
        }
        faceDetector.close()
        return resultBitmap
    }

}