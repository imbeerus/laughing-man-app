package com.lockwood.laughingmanar.ui

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Camera
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.GestureDetectorCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.CompoundButton
import com.google.android.gms.common.annotation.KeepName
import com.google.firebase.ml.common.FirebaseMLException
import com.lockwood.laughingmanar.R
import com.lockwood.laughingmanar.extensions.drawable
import com.lockwood.laughingmanar.extensions.openResFolder
import com.lockwood.laughingmanar.facedetection.FaceDetectionProcessor
import com.lockwood.laughingmanar.mlkit.CameraSource
import kotlinx.android.synthetic.main.activity_camera.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.toast
import java.io.IOException

@KeepName
class CameraActivity : AppCompatActivity(), View.OnClickListener, GestureDetector.OnGestureListener,
    ActivityCompat.OnRequestPermissionsResultCallback {

    private var cameraSource: CameraSource? = null
    private var selectedModel = FACE_DETECTION
    private var selectedMode = CameraSource.CaptureMode.PHOTO_MODE_CAPTURE

    private lateinit var gestureDetector: GestureDetectorCompat

    private val requiredPermissions: Array<String?>
        get() {
            return try {
                val info = this.packageManager
                    .getPackageInfo(this.packageName, PackageManager.GET_PERMISSIONS)
                val ps = info.requestedPermissions
                if (ps != null && ps.isNotEmpty()) {
                    ps
                } else {
                    arrayOfNulls(0)
                }
            } catch (e: Exception) {
                arrayOfNulls(0)
            }
        }

    private val swapCheckedChangeListener =
        CompoundButton.OnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "Set facing")
            cameraSource?.let {
                if (isChecked) {
                    it.setFacing(CameraSource.CAMERA_FACING_FRONT)
                } else {
                    it.setFacing(CameraSource.CAMERA_FACING_BACK)
                }
            }
            cameraPreview?.stop()
            startCameraSource()
        }

    private val captureCheckedChangeListener =
        CompoundButton.OnCheckedChangeListener { _, isChecked ->
            when (selectedMode) {
                CameraSource.CaptureMode.PHOTO_MODE_CAPTURE -> {
                    captureButton.background = drawable(R.drawable.ic_start)
                }
                CameraSource.CaptureMode.VIDEO_MODE_END -> {
                    captureButton.background = drawable(R.drawable.ic_stop)
                    selectedMode = CameraSource.CaptureMode.VIDEO_MODE_START
                }
                CameraSource.CaptureMode.VIDEO_MODE_START -> {
                    captureButton.background = drawable(R.drawable.ic_start)
                    selectedMode = CameraSource.CaptureMode.VIDEO_MODE_END
                }
            }
        }


    private fun changCameraMode() {
        // changCameraMode()
        Log.d(TAG, "changCameraMode: $selectedMode")
        selectedMode = if (selectedMode == CameraSource.CaptureMode.PHOTO_MODE_CAPTURE) {
            CameraSource.CaptureMode.VIDEO_MODE_END
        } else {
            CameraSource.CaptureMode.PHOTO_MODE_CAPTURE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        setContentView(R.layout.activity_camera)

        if (cameraPreview == null) {
            Log.d(TAG, "Preview is null")
        }

        if (faceOverlay == null) {
            Log.d(TAG, "graphicOverlay is null")
        }

        gestureDetector = GestureDetectorCompat(this, this)

        val facingSwitch = facingSwitch
        facingSwitch.setOnCheckedChangeListener(swapCheckedChangeListener)
        // Hide the toggle button if there is only 1 camera
        if (Camera.getNumberOfCameras() == 1) {
            facingSwitch.visibility = View.GONE
        }

        captureButton.setOnCheckedChangeListener(captureCheckedChangeListener)
        infoButton.setOnClickListener(this)

        if (allPermissionsGranted()) {
            createCameraSource(selectedModel)
        } else {
            getRuntimePermissions()
        }
    }

    private fun createCameraSource(model: String) {
        // If there's no existing cameraSource, create one.
        if (cameraSource == null) {
            cameraSource = CameraSource(this, faceOverlay)
        }

        try {
            cameraSource?.setMachineLearningFrameProcessor(FaceDetectionProcessor())
        } catch (e: FirebaseMLException) {
            Log.e(TAG, "can not create camera source: $model")
        }
    }

    private fun startCameraSource() {
        cameraSource?.let {
            try {
                if (cameraPreview == null) {
                    Log.d(TAG, "resume: Preview is null")
                }
                if (faceOverlay == null) {
                    Log.d(TAG, "resume: graphOverlay is null")
                }
                cameraPreview?.start(cameraSource!!, faceOverlay)
            } catch (e: IOException) {
                Log.e(TAG, "Unable to start camera source.", e)
                cameraSource?.release()
                cameraSource = null
            }
        }
    }

    public override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        startCameraSource()
    }

    override fun onPause() {
        super.onPause()
        cameraPreview?.stop()
    }

    public override fun onDestroy() {
        super.onDestroy()
        cameraSource?.release()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.infoButton -> {
                alert(R.string.intro_message) {
                    positiveButton("Results") { openResFolder() }
                }.show()
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return if (gestureDetector.onTouchEvent(event)) {
            true
        } else {
            super.onTouchEvent(event)
        }
    }

    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        val deltaY = Math.abs(e1.y - e2.y)
        if ((deltaY >= MIN_SWIPE_DISTANCE_Y) && (deltaY <= MAX_SWIPE_DISTANCE_Y)) {
//         TODO:  cameraSource.swapCamera()
        }
        return true
    }

    override fun onLongPress(e: MotionEvent?) {
        changCameraMode()
    }

    override fun onShowPress(e: MotionEvent?) {}

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean = true

    override fun onSingleTapUp(e: MotionEvent?): Boolean = true

    override fun onDown(e: MotionEvent?): Boolean = true

    private fun allPermissionsGranted(): Boolean {
        for (permission in requiredPermissions) {
            if (!isPermissionGranted(this, permission!!)) {
                return false
            }
        }
        return true
    }

    private fun getRuntimePermissions() {
        val allNeededPermissions = arrayListOf<String>()
        for (permission in requiredPermissions) {
            if (!isPermissionGranted(this, permission!!)) {
                allNeededPermissions.add(permission)
            }
        }

        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                this, allNeededPermissions.toTypedArray(), PERMISSION_REQUESTS
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.i(TAG, "Permission granted!")
        if (allPermissionsGranted()) {
            createCameraSource(selectedModel)
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        private const val FACE_DETECTION = "Face Detection"
        private const val TAG = "CameraActivity"
        private const val PERMISSION_REQUESTS = 1

        private const val MIN_SWIPE_DISTANCE_Y = 100
        private const val MAX_SWIPE_DISTANCE_Y = 1000

        private fun isPermissionGranted(context: Context, permission: String): Boolean {
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Permission granted: $permission")
                return true
            }
            Log.i(TAG, "Permission NOT granted: $permission")
            return false
        }
    }
}