package com.lockwood.laughingmanar.mlkit


import android.graphics.Bitmap
import com.google.firebase.ml.common.FirebaseMLException
import java.nio.ByteBuffer

interface VisionImageProcessor {

    @Throws(FirebaseMLException::class)
    fun process(data: ByteBuffer, frameMetadata: FrameMetadata, graphicOverlay: GraphicOverlay)

    fun process(bitmap: Bitmap, graphicOverlay: GraphicOverlay)

    fun stop()
}