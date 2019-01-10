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
    private var previewWidth: Int = 0
    private var widthScaleFactor = 1.0f
    private var previewHeight: Int = 0
    private var heightScaleFactor = 1.0f
    private var facing = CameraSource.CAMERA_FACING_BACK
    private val graphics = ArrayList<Graphic>()

    abstract class Graphic(private val overlay: GraphicOverlay) {

        val applicationContext: Context
            get() = overlay.ctx.applicationContext

        abstract fun draw(canvas: Canvas)

        fun scaleX(horizontal: Float): Float {
            return horizontal * overlay.widthScaleFactor
        }

        fun scaleY(vertical: Float): Float {
            return vertical * overlay.heightScaleFactor
        }

        fun translateX(x: Float): Float {
            return if (overlay.facing == CameraSource.CAMERA_FACING_FRONT) {
                overlay.width - scaleX(x)
            } else {
                scaleX(x)
            }
        }

        fun translateY(y: Float): Float {
            return scaleY(y)
        }

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
                widthScaleFactor = width.toFloat() / previewWidth.toFloat()
                heightScaleFactor = height.toFloat() / previewHeight.toFloat()
            }

            for (graphic in graphics) {
                graphic.draw(canvas)
            }
        }
    }
}