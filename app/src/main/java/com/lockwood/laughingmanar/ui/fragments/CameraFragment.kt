package com.lockwood.laughingmanar.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.lockwood.laughingmanar.PIC_FILE_NAME
import com.lockwood.laughingmanar.R
import com.lockwood.laughingmanar.extensions.ctx
import org.jetbrains.anko.alert
import org.jetbrains.anko.find
import org.jetbrains.anko.toast
import org.jetbrains.anko.yesButton
import java.io.File

class CameraFragment : BaseFragment(), View.OnClickListener {

    override fun onCreateView(inflater: LayoutInflater,
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
        file = File(context?.getExternalFilesDir(null), PIC_FILE_NAME)
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (textureView.isAvailable) {
            openCamera(textureView.width, textureView.height)
        } else {
            textureView.surfaceTextureListener = surfaceTextureListener
        }
    }

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.capture -> lockFocus()
            R.id.info -> {
                view.ctx.alert(R.string.intro_message) {
                    yesButton { }
                }.show()
            }
            R.id.swap -> {
                view.ctx.toast("Swap camera")
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(): CameraFragment = CameraFragment()
    }
}