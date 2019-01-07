package com.lockwood.laughingmanar.camera

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.view.WindowManager
import com.lockwood.laughingmanar.R
import com.lockwood.laughingmanar.extensions.TAG
import com.lockwood.laughingmanar.model.SingletonHolder
import com.lockwood.laughingmanar.ui.components.AutoFitTextureView
import org.jetbrains.anko.alert
import org.jetbrains.anko.okButton
import org.jetbrains.anko.toast
import java.io.File
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

class CameraSource private constructor(
    private var activity: Activity,
    private var textureView: AutoFitTextureView
) {
    var cameraDevice: CameraDevice? = null

    private lateinit var previewRequestBuilder: CaptureRequest.Builder
    private lateinit var previewRequest: CaptureRequest
    private lateinit var previewSize: Size
    private lateinit var file: File

    private val windowManager: WindowManager = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val cameraOpenCloseLock = Semaphore(1)

    private var cameraFace: CameraFaces = CameraFaces.CAMERA_BACK
    private var currentMode: CameraMode = CameraMode.MODE_PHOTO
    private var state = CameraStates.STATE_PREVIEW
    private var flashSupported = false
    private var sensorOrientation = 0

    private var captureSession: CameraCaptureSession? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private var imageReader: ImageReader? = null

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(t: SurfaceTexture, w: Int, h: Int) = openCamera(w, h)
        override fun onSurfaceTextureSizeChanged(t: SurfaceTexture, w: Int, h: Int) = configureTransform(w, h)
        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture) = true
        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
    }

    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
        private fun process(result: CaptureResult) {
            when (state) {
                CameraStates.STATE_PREVIEW -> Unit // Do nothing when the camera preview is working normally.
                CameraStates.STATE_WAITING_LOCK -> capturePicture(result)
                CameraStates.STATE_WAITING_PRECAPTURE -> {
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE || aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        state = CameraStates.STATE_WAITING_NON_PRECAPTURE
                    }
                }
                CameraStates.STATE_WAITING_NON_PRECAPTURE -> {
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        state = CameraStates.STATE_PICTURE_TAKEN
                        captureStillPicture()
                    }
                }
                else -> return
            }
        }

        private fun capturePicture(result: CaptureResult) {
            val afState = result.get(CaptureResult.CONTROL_AF_STATE)
            if (afState == null) {
                captureStillPicture()
            } else if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED || afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                    state = CameraStates.STATE_PICTURE_TAKEN
                    captureStillPicture()
                } else {
                    runPrecaptureSequence()
                }
            }
        }

        override fun onCaptureProgressed(s: CameraCaptureSession, r: CaptureRequest, result: CaptureResult) =
            process(result)

        override fun onCaptureCompleted(s: CameraCaptureSession, r: CaptureRequest, result: TotalCaptureResult) =
            process(result)
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraOpenCloseLock.release()
            cameraDevice = camera
            createCameraPreviewSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraOpenCloseLock.release()
            camera.close()
            cameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            onDisconnected(cameraDevice)
            (activity as AppCompatActivity).finish()
        }
    }

    private val onImageAvailableListener = ImageReader.OnImageAvailableListener {
        file = SaveUtils.makeFile(activity)
        backgroundHandler?.post(ImageSaver(it.acquireNextImage(), file))
    }

    fun update(newContext: Activity, newTextureView: AutoFitTextureView) {
        activity = newContext
        textureView = newTextureView
    }

    fun openCameraIfAvailable() {
        if (textureView.isAvailable) {
            openCamera(textureView.width, textureView.height)
        } else {
            textureView.surfaceTextureListener = surfaceTextureListener
        }
    }

    fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
            imageReader?.close()
            imageReader = null
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            cameraOpenCloseLock.release()
        }
    }

    fun swapCamera() {
        if (cameraFace == CameraFaces.CAMERA_FRONT) {
            cameraFace = CameraFaces.CAMERA_BACK
        } else if (cameraFace == CameraFaces.CAMERA_BACK) {
            cameraFace = CameraFaces.CAMERA_FRONT
        }
        closeCamera()
        openCameraIfAvailable()
    }

    fun lockFocus() {
        try {
            // This is how to tell the camera to lock focus.
            previewRequestBuilder.set(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_START
            )
            // Tell #captureCallback to wait for the lock.
            state = CameraStates.STATE_WAITING_LOCK
            captureSession?.capture(previewRequestBuilder.build(), captureCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }
    }

    fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread?.looper)
    }

    fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, e.toString())
        }
    }

    fun isCurrentCameraFront(): Boolean = cameraFace == CameraFaces.CAMERA_FRONT

    fun isCurrentCameraBack(): Boolean = cameraFace == CameraFaces.CAMERA_BACK

    @SuppressLint("MissingPermission")
    private fun openCamera(width: Int, height: Int) {
        setUpCameraOutputs(width, height)
        configureTransform(width, height)
        val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            // Wait for camera to open - 2.5 seconds is sufficient
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            manager.openCamera(cameraFace.toString(), stateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.", e)
        }
    }

    private fun setUpCameraOutputs(width: Int, height: Int) {
        val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val characteristics = manager.getCameraCharacteristics(cameraFace.toString())
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            // For still image captures, we use the largest available size.
            val largest = Collections.max(
                Arrays.asList(*map!!.getOutputSizes(ImageFormat.JPEG)),
                CompareSizesByArea()
            )
            imageReader = ImageReader.newInstance(largest.width, largest.height, ImageFormat.JPEG, 2).apply {
                setOnImageAvailableListener(onImageAvailableListener, backgroundHandler)
            }
            // Find out if we need to swap dimension to get the preview size relative to sensor
            // coordinate.
            val displayRotation = windowManager.defaultDisplay.rotation

            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
            val swappedDimensions = areDimensionsSwapped(displayRotation)

            val displaySize = Point()
            windowManager.defaultDisplay.getSize(displaySize)
            val rotatedPreviewWidth = if (swappedDimensions) height else width
            val rotatedPreviewHeight = if (swappedDimensions) width else height
            var maxPreviewWidth = if (swappedDimensions) displaySize.y else displaySize.x
            var maxPreviewHeight = if (swappedDimensions) displaySize.x else displaySize.y

            if (maxPreviewWidth > MAX_PREVIEW_WIDTH) maxPreviewWidth = MAX_PREVIEW_WIDTH
            if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) maxPreviewHeight = MAX_PREVIEW_HEIGHT

            previewSize = chooseOptimalSize(
                map.getOutputSizes(SurfaceTexture::class.java),
                rotatedPreviewWidth, rotatedPreviewHeight,
                maxPreviewWidth, maxPreviewHeight,
                largest
            )

            // We fit the aspect ratio of TextureView to the size of preview we picked.
            if (activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                textureView.setAspectRatio(previewSize.width, previewSize.height)
            } else {
                textureView.setAspectRatio(previewSize.height, previewSize.width)
            }
            flashSupported = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        } catch (e: NullPointerException) {
            activity.alert(R.string.camera_error) {
                okButton { (ctx as AppCompatActivity).finish() }
            }
        }
    }

    private fun areDimensionsSwapped(displayRotation: Int): Boolean {
        var swappedDimensions = false
        when (displayRotation) {
            Surface.ROTATION_0, Surface.ROTATION_180 -> {
                if (sensorOrientation == 90 || sensorOrientation == 270) {
                    swappedDimensions = true
                }
            }
            Surface.ROTATION_90, Surface.ROTATION_270 -> {
                if (sensorOrientation == 0 || sensorOrientation == 180) {
                    swappedDimensions = true
                }
            }
            else -> {
                Log.e(TAG, "Display rotation is invalid: $displayRotation")
            }
        }
        return swappedDimensions
    }

    private fun createCameraPreviewSession() {
        try {
            val texture = textureView.surfaceTexture
            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(previewSize.width, previewSize.height)
            // This is the output Surface we need to start preview.
            val surface = Surface(texture)
            // We set up a CaptureRequest.Builder with the output Surface.
            previewRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder.addTarget(surface)
            // Here, we create a CameraCaptureSession for camera preview.
            cameraDevice?.createCaptureSession(
                Arrays.asList(surface, imageReader?.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        // The camera is already closed
                        if (cameraDevice == null) return
                        // When the session is ready, we start displaying the preview.
                        captureSession = cameraCaptureSession
                        try {
                            // Auto focus should be continuous for camera preview.
                            previewRequestBuilder.set(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                            )
                            // Flash is automatically enabled when necessary.
                            setAutoFlash(previewRequestBuilder)
                            // Finally, we start displaying the camera preview.
                            previewRequest = previewRequestBuilder.build()
                            captureSession?.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler)
                        } catch (e: CameraAccessException) {
                            Log.e(TAG, e.toString())
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        activity.toast("Failed")
                    }
                }, null
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }

    }

    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        val rotation = windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, previewSize.height.toFloat(), previewSize.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            val scale = Math.max(
                viewHeight.toFloat() / previewSize.height,
                viewWidth.toFloat() / previewSize.width
            )
            with(matrix) {
                setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
                postScale(scale, scale, centerX, centerY)
                postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
            }
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }
        textureView.setTransform(matrix)
    }

    private fun runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            previewRequestBuilder.set(
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START
            )
            // Tell #captureCallback to wait for the precapture sequence to be set.
            state = CameraStates.STATE_WAITING_PRECAPTURE
            captureSession?.capture(previewRequestBuilder.build(), captureCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }

    }

    private fun captureStillPicture() {
        try {
            if (cameraDevice == null) return
            val rotation = windowManager.defaultDisplay.rotation

            // This is the CaptureRequest.Builder that we use to take a picture.
            val captureBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)?.apply {
                addTarget(imageReader!!.surface)

                // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
                // We have to take that into account and rotate JPEG properly.
                // For devices with orientation of 90, we return our mapping from ORIENTATIONS.
                // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
                set(
                    CaptureRequest.JPEG_ORIENTATION,
                    (ORIENTATIONS.get(rotation) + sensorOrientation + 270) % 360
                )

                // Use the same AE and AF modes as the preview.
                set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
            }?.also { setAutoFlash(it) }

            val captureCallback = object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    activity.toast("Saved: $file")
                    Log.d(TAG, file.toString())
                    unlockFocus()
                }
            }

            captureSession?.apply {
                stopRepeating()
                abortCaptures()
                capture(captureBuilder!!.build(), captureCallback, null)
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }
    }

    private fun unlockFocus() {
        try {
            // Reset the auto-focus trigger
            previewRequestBuilder.set(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_CANCEL
            )
            setAutoFlash(previewRequestBuilder)
            captureSession?.capture(
                previewRequestBuilder.build(), captureCallback,
                backgroundHandler
            )
            // After this, the camera will go back to the normal state of preview.
            state = CameraStates.STATE_PREVIEW
            captureSession?.setRepeatingRequest(
                previewRequest, captureCallback,
                backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }

    }

    private fun setAutoFlash(requestBuilder: CaptureRequest.Builder) {
        if (flashSupported) {
            requestBuilder.set(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
            )
        }
    }

    fun changCameraMode() {
        if (currentMode == CameraMode.MODE_PHOTO) {
            currentMode = CameraMode.MODE_VIDEO
            activity.toast("Video mode")
        } else {
            // TODO: don't change mode if recording
            currentMode = CameraMode.MODE_PHOTO
            activity.toast("Photo mode")
        }
    }

    companion object : SingletonHolder<CameraSource, Activity, AutoFitTextureView>(::CameraSource) {
        private const val MAX_PREVIEW_WIDTH = 1920
        private const val MAX_PREVIEW_HEIGHT = 1080

        private val ORIENTATIONS = SparseIntArray()

        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)
            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }

        @JvmStatic
        private fun chooseOptimalSize(
            choices: Array<Size>,
            textureViewWidth: Int,
            textureViewHeight: Int,
            maxWidth: Int,
            maxHeight: Int,
            aspectRatio: Size
        ): Size {
            // Collect the supported resolutions that are at least as big as the preview Surface
            val bigEnough = ArrayList<Size>()
            // Collect the supported resolutions that are smaller than the preview Surface
            val notBigEnough = ArrayList<Size>()
            val w = aspectRatio.width
            val h = aspectRatio.height
            for (option in choices) {
                if (option.width <= maxWidth && option.height <= maxHeight &&
                    option.height == option.width * h / w
                ) {
                    if (option.width >= textureViewWidth && option.height >= textureViewHeight) {
                        bigEnough.add(option)
                    } else {
                        notBigEnough.add(option)
                    }
                }
            }

            // Pick the smallest of those big enough. If there is no one big enough, pick the
            // largest of those not big enough.
            return when {
                bigEnough.size > 0 -> Collections.min(bigEnough, CompareSizesByArea())
                notBigEnough.size > 0 -> Collections.max(notBigEnough, CompareSizesByArea())
                else -> {
                    Log.e(TAG, "Couldn't find any suitable preview size")
                    choices[0]
                }
            }
        }
    }
}