package com.lockwood.laughingmanar.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import com.lockwood.laughingmanar.R
import com.lockwood.laughingmanar.REQUEST_CAMERA_PERMISSION
import com.lockwood.laughingmanar.camera.CameraSource
import com.lockwood.laughingmanar.extensions.color
import com.lockwood.laughingmanar.extensions.ctx
import com.lockwood.laughingmanar.extensions.openResFolder
import com.lockwood.laughingmanar.ui.components.AutoFitTextureView
import org.jetbrains.anko.alert
import org.jetbrains.anko.cancelButton
import org.jetbrains.anko.find
import org.jetbrains.anko.okButton

class CameraFragment : Fragment(), View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private lateinit var cameraSource: CameraSource
    private lateinit var textureView: AutoFitTextureView

    private val buttons = listOf(R.id.capture, R.id.info, R.id.swap)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.frag_camera, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = with(view) {
        buttons.forEach { id ->
            val button = find<ImageButton>(id)
            button.setOnClickListener(this@CameraFragment)
            button.setColorFilter(ctx.color(R.color.blue_grey_100), PorterDuff.Mode.SRC_IN)
        }
        textureView = find(R.id.texture)
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

    override fun onResume() {
        super.onResume()
        cameraSource.update(ctx, textureView)
        cameraSource.startBackgroundThread()
        cameraSource.openCameraIfAvailable()
    }

    override fun onPause() {
        cameraSource.closeCamera()
        cameraSource.stopBackgroundThread()
        super.onPause()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.capture -> cameraSource.lockFocus()
            R.id.info -> {
                view.ctx.alert(R.string.intro_message) {
                    positiveButton("Results") { activity?.openResFolder() }
                }.show()
            }
            R.id.swap -> cameraSource.swapCamera()
        }
    }

    private fun showPermissionAlert() {
        ctx.alert(R.string.request_permission) {
            okButton {
                parentFragment?.requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
            }
            cancelButton { parentFragment?.activity?.finish() }
        }
    }

    private fun requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            showPermissionAlert()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.size != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                showPermissionAlert()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            cameraSource = CameraSource.getInstance(ctx, textureView)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(): CameraFragment = CameraFragment()
    }
}