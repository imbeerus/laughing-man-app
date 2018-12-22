package com.lockwood.laughingmanar.extensions

import android.Manifest
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import com.lockwood.laughingmanar.REQUEST_CAMERA_PERMISSION

val Fragment.ctx: FragmentActivity
    get() {
        activity?.let {
            return it
        }
    }

fun Fragment.requestCameraPermission() {
    if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
        //  TODO: ConfirmationDialog
        //  ConfirmationDialog().show(childFragmentManager, FRAGMENT_DIALOG)
    } else {
        requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
    }
}