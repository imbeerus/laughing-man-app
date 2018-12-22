package com.lockwood.laughingmanar

import android.app.Application
import com.lockwood.laughingmanar.extensions.DelegatesExt

class App : Application() {

    companion object {
        var instance: App by DelegatesExt.notNullSingleValue()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}