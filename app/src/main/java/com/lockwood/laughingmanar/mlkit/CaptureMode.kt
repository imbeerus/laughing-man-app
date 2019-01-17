package com.lockwood.laughingmanar.mlkit

enum class CaptureMode(private val str: String) {
    VIDEO_MODE_START("VIDEO MODE START"),
    VIDEO_MODE_END("VIDEO MODE END"),
    PHOTO_MODE_CAPTURE("PHOTO MODE CAPTURE");

    override fun toString(): String = str
}