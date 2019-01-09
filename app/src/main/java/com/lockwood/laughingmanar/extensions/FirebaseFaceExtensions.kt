package com.lockwood.laughingmanar.extensions

import com.google.firebase.ml.vision.face.FirebaseVisionFace

val FirebaseVisionFace.centerX: Float
    get() = boundingBox.centerX().toFloat()

val FirebaseVisionFace.centerY: Float
    get() = boundingBox.centerY().toFloat()

val FirebaseVisionFace.width: Float
    get() = boundingBox.width().toFloat()

val FirebaseVisionFace.height: Float
    get() = boundingBox.height().toFloat()