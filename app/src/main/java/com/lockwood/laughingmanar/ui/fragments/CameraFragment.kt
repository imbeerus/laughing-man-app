package com.lockwood.laughingmanar.ui.fragments

import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.lockwood.laughingmanar.R

class CameraFragment : Fragment(), View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.frag_camera, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.picture).setOnClickListener(this)
        view.findViewById<View>(R.id.info).setOnClickListener(this)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.picture -> lockFocus()
            R.id.info -> {
                activity?.let {
                    AlertDialog.Builder(it)
                        .setMessage(R.string.intro_message)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
            }
        }
    }

    private fun lockFocus() {

    }

    companion object {
        @JvmStatic
        fun newInstance(): CameraFragment = CameraFragment()
    }
}