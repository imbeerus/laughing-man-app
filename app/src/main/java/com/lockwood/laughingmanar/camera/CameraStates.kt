package com.lockwood.laughingmanar.camera

enum class CameraStates {
    STATE_PREVIEW,
    STATE_WAITING_LOCK,
    STATE_WAITING_PRECAPTURE,
    STATE_WAITING_NON_PRECAPTURE,
    STATE_PICTURE_TAKEN
}

enum class CameraFaces {
    CAMERA_BACK, CAMERA_FRONT;

    override fun toString(): String = ordinal.toString()
}

enum class CameraMode {
    CAMERA_PHOTO, CAMERA_VIDEO
}