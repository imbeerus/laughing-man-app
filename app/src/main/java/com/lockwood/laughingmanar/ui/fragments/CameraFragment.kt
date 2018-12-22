package com.lockwood.laughingmanar.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.lockwood.laughingmanar.R
import com.lockwood.laughingmanar.REQUEST_CAMERA_PERMISSION
import com.lockwood.laughingmanar.camera.CurrentCameraManager
import com.lockwood.laughingmanar.extensions.ctx
import com.lockwood.laughingmanar.extensions.requestCameraPermission
import com.lockwood.laughingmanar.ui.components.AutoFitTextureView
import org.jetbrains.anko.alert
import org.jetbrains.anko.find
import org.jetbrains.anko.okButton

class CameraFragment : Fragment(), View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private lateinit var cameraManager: CurrentCameraManager
    private lateinit var textureView: AutoFitTextureView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.frag_camera, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.find<View>(R.id.capture).setOnClickListener(this)
        view.find<View>(R.id.info).setOnClickListener(this)
        view.find<View>(R.id.swap).setOnClickListener(this)
        textureView = view.find(R.id.texture)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val permission = ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission()
            return
        }
        cameraManager = CurrentCameraManager.getInstance(ctx, textureView)
    }

    override fun onResume() {
        super.onResume()
        cameraManager.startBackgroundThread()
        cameraManager.openCameraIfAvailable()
    }

    override fun onPause() {
        cameraManager.closeCamera()
        cameraManager.stopBackgroundThread()
        super.onPause()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.capture -> cameraManager.lockFocus()
            R.id.info -> {
                view.ctx.alert(R.string.intro_message) {
                    okButton { }
                }.show()
            }
            R.id.swap -> cameraManager.swapCamera()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.size != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                //  TODO: ErrorDialog
                //  ErrorDialog.newInstance(getString(R.string.request_permission))
                //  .show(childFragmentManager, FRAGMENT_DIALOG)
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            cameraManager = CurrentCameraManager.getInstance(ctx, textureView)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(): CameraFragment = CameraFragment()
    }
}