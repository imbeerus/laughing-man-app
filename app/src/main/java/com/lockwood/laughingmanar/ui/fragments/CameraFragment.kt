package com.lockwood.laughingmanar.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.view.GestureDetectorCompat
import android.view.*
import android.widget.ImageButton
import com.lockwood.laughingmanar.R
import com.lockwood.laughingmanar.REQUEST_CAMERA_PERMISSION
import com.lockwood.laughingmanar.REQUEST_VIDEO_PERMISSIONS
import com.lockwood.laughingmanar.VIDEO_PERMISSIONS
import com.lockwood.laughingmanar.camera.CameraSource
import com.lockwood.laughingmanar.extensions.ctx
import com.lockwood.laughingmanar.extensions.openResFolder
import com.lockwood.laughingmanar.ui.components.AutoFitTextureView
import org.jetbrains.anko.alert
import org.jetbrains.anko.cancelButton
import org.jetbrains.anko.find
import org.jetbrains.anko.okButton

class CameraFragment : Fragment(), View.OnClickListener, View.OnTouchListener,
    ActivityCompat.OnRequestPermissionsResultCallback {

    private lateinit var cameraSource: CameraSource
    private lateinit var textureView: AutoFitTextureView
    private lateinit var captureButton: ImageButton

    private val buttons = listOf(R.id.capture, R.id.info, R.id.swap)
    private val gestureDetector = GestureDetectorCompat(ctx, object : GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) {
            changCameraMode()
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            val deltaY = Math.abs(e1.y - e2.y)
            if ((deltaY >= MIN_SWIPE_DISTANCE_Y) && (deltaY <= MAX_SWIPE_DISTANCE_Y)) {
                cameraSource.swapCamera()
                return true
            }
            return super.onFling(e1, e2, velocityX, velocityY)
        }

        override fun onDown(e: MotionEvent?): Boolean {
            return true
        }
    })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.frag_camera, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = with(view) {
        buttons.forEach { id -> find<View>(id).setOnClickListener(this@CameraFragment) }
        captureButton = find(R.id.capture)
        textureView = find(R.id.texture)
        textureView.setOnTouchListener(this@CameraFragment)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val permission = ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission()
            return
        }
        cameraSource = CameraSource.getInstance(ctx, textureView)
    }

    override fun onResume() = with(cameraSource) {
        super.onResume()
        update(ctx, textureView)
        startBackgroundThread()
        openCameraIfAvailable()
    }

    override fun onPause() = with(cameraSource) {
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.capture -> {
                if (cameraSource.isVideoMode()) {
                    handleVideo()
                } else {
                    cameraSource.lockFocus()
                }
            }
            R.id.info -> {
                view.ctx.alert(R.string.intro_message) {
                    positiveButton("Results") { activity?.openResFolder() }
                }.show()
            }
            R.id.swap -> cameraSource.swapCamera()
        }
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        v.performClick()
        return gestureDetector.onTouchEvent(event)
    }

    fun onKeyDown(keyCode: Int, event: KeyEvent) {
        cameraSource.lockFocus()
    }

    private fun handleVideo() {
        if (cameraSource.isRecordingVideo()) {
            captureButton.setImageResource(R.drawable.ic_start)
            cameraSource.stopRecordingVideo()
        } else {
            captureButton.setImageResource(R.drawable.ic_stop)
            cameraSource.startRecordingVideo()
        }
    }

    private fun changCameraMode() = with(cameraSource) {
        changCameraMode()
        if (isVideoMode()) {
            captureButton.setImageResource(R.drawable.ic_start)
        } else {
            captureButton.setImageResource(R.drawable.ic_shutter)
        }
    }

    private fun shouldShowRequestPermissionRationale(permissions: Array<String>) =
        permissions.any { shouldShowRequestPermissionRationale(it) }

    private fun showCameraPermissionAlert() {
        ctx.alert(R.string.permission_request) {
            okButton { parentFragment?.requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION) }
            cancelButton { parentFragment?.activity?.finish() }
        }
    }

    private fun requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            showCameraPermissionAlert()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        }
    }

    private fun requestVideoPermissions() {
        if (shouldShowRequestPermissionRationale(VIDEO_PERMISSIONS)) {
            ctx.alert(R.string.permission_request) {
                okButton { parentFragment?.requestPermissions(VIDEO_PERMISSIONS, REQUEST_VIDEO_PERMISSIONS) }
                cancelButton { parentFragment?.activity?.finish() }
            }
        } else {
            requestPermissions(VIDEO_PERMISSIONS, REQUEST_VIDEO_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.size != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                showCameraPermissionAlert()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            cameraSource = CameraSource.getInstance(ctx, textureView)
        }
    }

    companion object {
        private const val MIN_SWIPE_DISTANCE_Y = 100
        private const val MAX_SWIPE_DISTANCE_Y = 1000

        @JvmStatic
        fun newInstance(): CameraFragment = CameraFragment()
    }
}