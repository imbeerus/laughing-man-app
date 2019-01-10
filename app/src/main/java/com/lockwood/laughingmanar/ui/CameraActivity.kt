package com.lockwood.laughingmanar.ui

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Camera
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.CompoundButton
import com.google.android.gms.common.annotation.KeepName
import com.google.firebase.ml.common.FirebaseMLException
import com.lockwood.laughingmanar.R
import com.lockwood.laughingmanar.facedetection.FaceDetectionProcessor
import com.lockwood.laughingmanar.mlkit.CameraSource
import kotlinx.android.synthetic.main.activity_camera.*
import java.io.IOException

@KeepName
class CameraActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    private var cameraSource: CameraSource? = null
    private var selectedModel = FACE_DETECTION

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
        CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
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
        CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            cameraPreview
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

        val facingSwitch = facingSwitch
        facingSwitch.setOnCheckedChangeListener(swapCheckedChangeListener)
        // Hide the toggle button if there is only 1 camera
        if (Camera.getNumberOfCameras() == 1) {
            facingSwitch.visibility = View.GONE
        }

        captureButton.setOnCheckedChangeListener(captureCheckedChangeListener)

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