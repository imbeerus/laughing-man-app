package com.lockwood.laughingmanar.ui

import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.view.GestureDetectorCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.CompoundButton
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

abstract class BaseActivity : AppCompatActivity(),
    View.OnClickListener, GestureDetector.OnGestureListener, ActivityCompat.OnRequestPermissionsResultCallback {

    protected lateinit var gestureDetector: GestureDetectorCompat

    protected var cameraSource: CameraSource? = null
    protected var selectedModel = FACE_DETECTION
    protected var selectedMode = CameraSource.CaptureMode.PHOTO_MODE_CAPTURE

    protected val requiredPermissions: Array<String?>
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

    protected val captureCheckedChangeListener =
        CompoundButton.OnCheckedChangeListener { _, _ ->
            captureWithMode(selectedMode)
        }

    protected val swapCheckedChangeListener =
        CompoundButton.OnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "Set facing")
            swapCamera(isChecked)
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            captureWithMode(selectedMode)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        val deltaY = Math.abs(e1.y - e2.y)
        if ((deltaY >= MIN_SWIPE_DISTANCE_Y) && (deltaY <= MAX_SWIPE_DISTANCE_Y)) {
            val isChecked = !captureButton.isChecked
            captureButton.isChecked = isChecked
            swapCamera(isChecked)
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

    private fun captureWithMode(mode: CameraSource.CaptureMode) {
        when (mode) {
            CameraSource.CaptureMode.PHOTO_MODE_CAPTURE -> {
                toast("capture")
                captureButton.background = drawable(R.drawable.ic_start)
            }
            CameraSource.CaptureMode.VIDEO_MODE_END -> {
                toast("start record")
                captureButton.background = drawable(R.drawable.ic_stop)
                selectedMode = CameraSource.CaptureMode.VIDEO_MODE_START
            }
            CameraSource.CaptureMode.VIDEO_MODE_START -> {
                toast("end record")
                captureButton.background = drawable(R.drawable.ic_start)
                selectedMode = CameraSource.CaptureMode.VIDEO_MODE_END
            }
        }
    }

    private fun swapCamera(isChecked: Boolean) {
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

    private fun changCameraMode() {
        // changCameraMode()
        Log.d(TAG, "changCameraMode: $selectedMode")
        selectedMode = if (selectedMode == CameraSource.CaptureMode.PHOTO_MODE_CAPTURE) {
            CameraSource.CaptureMode.VIDEO_MODE_END
        } else {
            CameraSource.CaptureMode.PHOTO_MODE_CAPTURE
        }
    }

    protected fun createCameraSource(model: String) {
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

    companion object {
        private const val FACE_DETECTION = "Face Detection"
        private const val TAG = "BaseActivity"

        private const val MIN_SWIPE_DISTANCE_Y = 100
        private const val MAX_SWIPE_DISTANCE_Y = 1000
    }
}