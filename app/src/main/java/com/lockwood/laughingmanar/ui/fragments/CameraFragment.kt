package com.lockwood.laughingmanar.ui.fragments

import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.lockwood.laughingmanar.R
import org.jetbrains.anko.alert
import org.jetbrains.anko.toast
import org.jetbrains.anko.yesButton

class CameraFragment : Fragment(), View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.frag_camera, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.capture).setOnClickListener(this)
        view.findViewById<View>(R.id.info).setOnClickListener(this)
        view.findViewById<View>(R.id.swap).setOnClickListener(this)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.capture -> lockFocus()
            R.id.info -> {
                activity?.alert(R.string.intro_message){
                    yesButton { }
                }?.show()
            }
            R.id.swap -> {
                activity?.toast("Swap camera")
            }
        }
    }

    private fun lockFocus() {
        activity?.toast("Lock Focus")
    }

    companion object {
        @JvmStatic
        fun newInstance(): CameraFragment = CameraFragment()
    }
}