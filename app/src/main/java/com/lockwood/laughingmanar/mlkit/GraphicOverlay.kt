package com.lockwood.laughingmanar.mlkit

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import com.google.android.gms.vision.CameraSource
import com.lockwood.laughingmanar.extensions.ctx
import java.util.*

class GraphicOverlay(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val lock = Any()
    private var facing = CameraSource.CAMERA_FACING_BACK
    private val graphics = ArrayList<Graphic>()

    var widthScaleFactor = 1.0f
    var heightScaleFactor = 1.0f
    var previewWidth: Int = 0
    var previewHeight: Int = 0

    abstract class Graphic(private val overlay: GraphicOverlay) {

        val appContext: Context
            get() = overlay.ctx.applicationContext

        abstract fun draw(canvas: Canvas)

        fun translateX(x: Float): Float = with(overlay) {
            return@with BitmapUtils.translateX(
                x,
                widthScaleFactor,
                width.toFloat(),
                facing == CameraSource.CAMERA_FACING_FRONT
            )
        }

        fun translateY(y: Float): Float = BitmapUtils.translateY(y, overlay.heightScaleFactor)

        fun postInvalidate() {
            overlay.postInvalidate()
        }
    }

    fun clear() {
        synchronized(lock) {
            graphics.clear()
        }
        postInvalidate()
    }

    fun add(graphic: Graphic) {
        synchronized(lock) {
            graphics.add(graphic)
        }
    }

    fun remove(graphic: Graphic) {
        synchronized(lock) {
            graphics.remove(graphic)
        }
        postInvalidate()
    }

    fun setCameraInfo(previewWidth: Int, previewHeight: Int, facing: Int) {
        synchronized(lock) {
            this.previewWidth = previewWidth
            this.previewHeight = previewHeight
            this.facing = facing
        }
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        synchronized(lock) {
            if (previewWidth != 0 && previewHeight != 0) {
                widthScaleFactor = (width / previewWidth).toFloat()
                heightScaleFactor = (height / previewHeight).toFloat()
            }

            for (graphic in graphics) {
                graphic.draw(canvas)
            }
        }
    }
}