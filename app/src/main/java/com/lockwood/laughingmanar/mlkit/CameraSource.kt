package com.lockwood.laughingmanar.mlkit

import android.Manifest
import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.os.Environment
import androidx.annotation.RequiresPermission
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.WindowManager
import com.google.android.gms.common.images.Size
import com.lockwood.laughingmanar.extensions.galleryAddPic
import com.lockwood.laughingmanar.extensions.toast
import com.lockwood.laughingmanar.facedetection.FaceUtils
import com.lockwood.laughingmanar.facedetection.OverlayType
import java.io.*
import java.lang.Thread.State
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("MissingPermission")
open class CameraSource(
    private var activity: AppCompatActivity,
    private val graphicOverlay: GraphicOverlay
) {

    private var camera: Camera? = null
    private var rotation: Int = 0
    private var isBusy: Boolean = false
    var previewSize: Size? = null

    var cameraFacing = CAMERA_FACING_BACK

    private val mPicture = Camera.PictureCallback { data, camera ->
        val pictureFile: File = getOutputMediaFile(MEDIA_TYPE_IMAGE) ?: run {
            Log.d(TAG, ("Error creating media file, check storage permissions"))
            return@PictureCallback
        }

        val isFacingFront = cameraFacing == CameraSource.CAMERA_FACING_FRONT
        val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
        FaceUtils.detectFacesAndOverlayImage(bitmap, OverlayType.STATIC_PNG, isFacingFront) { resultBitmap ->
            val stream = ByteArrayOutputStream()
            resultBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            val resultByteArray = stream.toByteArray()
            resultBitmap.recycle()

            try {
                val fos = FileOutputStream(pictureFile)
                fos.write(resultByteArray)
                fos.close()
                activity.galleryAddPic(pictureFile)
            } catch (e: FileNotFoundException) {
                Log.d(TAG, "File not found: ${e.message}")
            } catch (e: IOException) {
                Log.d(TAG, "Error accessing file: ${e.message}")
            }
            isBusy = false
            camera.startPreview()
        }
    }

    fun capture() {
        if (!isBusy) {
            isBusy = true
            camera?.takePicture(null, null, mPicture)
        }
    }

    private fun getOutputMediaFile(type: Int): File? {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        val mediaStorageDir =
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), FOLDER_NAME)
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        mediaStorageDir.apply {
            if (!exists()) {
                if (!mkdirs()) {
                    Log.d(TAG, "failed to create directory $FOLDER_NAME")
                    return null
                }
            }
        }

        // Create a media file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
        return when (type) {
            MEDIA_TYPE_IMAGE -> {
                val path = "${mediaStorageDir.path}${File.separator}IMG_$timeStamp.jpg"
                activity.toast("photo saved to $path")
                File(path)
            }
            MEDIA_TYPE_VIDEO -> {
                val path = "${mediaStorageDir.path}${File.separator}VID_$timeStamp.mp4"
                activity.toast("video saved to $path")
                File(path)
            }
            else -> null
        }
    }

    // These values may be requested by the caller.  Due to hardware limitations, we may need to
    // select close, but not exactly the same values for these.
    private val requestedFps = 30.0f
    private val requestedPreviewWidth = 480
    private val requestedPreviewHeight = 360
    private val requestedAutoFocus = true

    // These instances need to be held onto to avoid GC of their underlying resources.  Even though
    // these aren't used outside of the method that creates them, they still must have hard
    // references maintained to them.
    private var dummySurfaceTexture: SurfaceTexture? = null

    // True if a SurfaceTexture is being used for the preview, false if a SurfaceHolder is being
    // used for the preview.  We want to be compatible back to Gingerbread, but SurfaceTexture
    // wasn't introduced until Honeycomb.  Since the interface cannot use a SurfaceTexture, if the
    // developer wants to display a preview we must use a SurfaceHolder.  If the developer doesn't
    // want to display a preview we use a SurfaceTexture if we are running at least Honeycomb.
    private var usingSurfaceTexture: Boolean = false

    private var processingThread: Thread? = null

    private val processingRunnable: FrameProcessingRunnable

    private val processorLock = Any()
    // @GuardedBy("processorLock")
    private var frameProcessor: VisionImageProcessor? = null

    private val bytesToByteBuffer = IdentityHashMap<ByteArray, ByteBuffer>()

    init {
        graphicOverlay.clear()
        processingRunnable = FrameProcessingRunnable()

        if (Camera.getNumberOfCameras() == 1) {
            val cameraInfo = CameraInfo()
            Camera.getCameraInfo(0, cameraInfo)
            cameraFacing = cameraInfo.facing
        }
    }

    // ==============================================================================================
    // Public
    // ==============================================================================================

    fun release() {
        synchronized(processorLock) {
            stop()
            processingRunnable.release()
            cleanScreen()

            if (frameProcessor != null) {
                frameProcessor!!.stop()
            }
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.CAMERA)
    @Synchronized
    @Throws(IOException::class)
    fun start(): CameraSource {
        if (camera != null) {
            return this
        }

        camera = createCamera()
        dummySurfaceTexture = SurfaceTexture(DUMMY_TEXTURE_NAME)
        camera!!.setPreviewTexture(dummySurfaceTexture)
        usingSurfaceTexture = true
        camera!!.startPreview()

        processingThread = Thread(processingRunnable)
        processingRunnable.setActive(true)
        processingThread!!.start()
        return this
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    @Synchronized
    @Throws(IOException::class)
    fun start(surfaceHolder: SurfaceHolder): CameraSource {
        if (camera != null) {
            return this
        }

        camera = createCamera()
        camera!!.setPreviewDisplay(surfaceHolder)
        camera!!.startPreview()

        processingThread = Thread(processingRunnable)
        processingRunnable.setActive(true)
        processingThread!!.start()

        usingSurfaceTexture = false
        return this
    }

    @Synchronized
    fun stop() {
        processingRunnable.setActive(false)
        if (processingThread != null) {
            try {
                // Wait for the thread to complete to ensure that we can't have multiple threads
                // executing at the same time (i.e., which would happen if we called start too
                // quickly after stop).
                processingThread!!.join()
            } catch (e: InterruptedException) {
                Log.d(TAG, "Frame processing thread interrupted on release.")
            }

            processingThread = null
        }

        if (camera != null) {
            camera!!.stopPreview()
            camera!!.setPreviewCallbackWithBuffer(null)
            try {
                if (usingSurfaceTexture) {
                    camera!!.setPreviewTexture(null)
                } else {
                    camera!!.setPreviewDisplay(null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear camera preview: $e")
            }

            camera!!.release()
            camera = null
        }

        // Release the reference to any image buffers, since these will no longer be in use.
        bytesToByteBuffer.clear()
    }

    @Synchronized
    fun setFacing(facing: Int) {
        if (facing != CAMERA_FACING_BACK && facing != CAMERA_FACING_FRONT) {
            throw IllegalArgumentException("Invalid camera: $facing")
        }
        this.cameraFacing = facing
    }

    @SuppressLint("InlinedApi")
    @Throws(IOException::class)
    private fun createCamera(): Camera {
        val requestedCameraId = getIdForRequestedCamera(cameraFacing)
        if (requestedCameraId == -1) {
            throw IOException("Could not find requested camera.")
        }
        val camera = Camera.open(requestedCameraId)
        val parameters = camera.parameters
        // TODO: add support to select picture resolution
        val highRes = parameters.supportedPictureSizes[2]

        val sizePair = selectSizePair(camera, requestedPreviewWidth, requestedPreviewHeight)
            ?: throw IOException("Could not find suitable preview size.")
        previewSize = sizePair.previewSize()

        val previewFpsRange = selectPreviewFpsRange(camera, requestedFps)
            ?: throw IOException("Could not find suitable preview frames per second range.")

        parameters.setPictureSize(highRes.width, highRes.height)
        parameters.setPreviewSize(previewSize!!.width, previewSize!!.height)
        parameters.setPreviewFpsRange(
            previewFpsRange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
            previewFpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]
        )
        parameters.previewFormat = ImageFormat.NV21
        parameters.jpegQuality = 100

        setRotation(camera, parameters, requestedCameraId)

        if (requestedAutoFocus) {
            if (parameters
                    .supportedFocusModes
                    .contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)
            ) {
                parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
            } else {
                Log.i(TAG, "Camera auto focus is not supported on this device.")
            }
        }

        camera.parameters = parameters

        // Four frame buffers are needed for working with the camera:
        //
        //   one for the frame that is currently being executed upon in doing detection
        //   one for the next pending frame to process immediately upon completing detection
        //   two for the frames that the camera uses to populate future preview images
        //
        // Through trial and error it appears that two free buffers, in addition to the two buffers
        // used in this code, are needed for the camera to work properly.  Perhaps the camera has
        // one thread for acquiring images, and another thread for calling into user code.  If only
        // three buffers are used, then the camera will spew thousands of warning messages when
        // detection takes a non-trivial amount of time.
        camera.setPreviewCallbackWithBuffer(CameraPreviewCallback())
        camera.addCallbackBuffer(createPreviewBuffer(previewSize!!))
        camera.addCallbackBuffer(createPreviewBuffer(previewSize!!))
        camera.addCallbackBuffer(createPreviewBuffer(previewSize!!))
        camera.addCallbackBuffer(createPreviewBuffer(previewSize!!))

        return camera
    }

    private class SizePair internal constructor(
        previewSize: android.hardware.Camera.Size,
        pictureSize: android.hardware.Camera.Size?
    ) {
        private val preview: Size = Size(previewSize.width, previewSize.height)
        private var picture: Size? = null

        init {
            if (pictureSize != null) {
                picture = Size(pictureSize.width, pictureSize.height)
            }
        }

        internal fun previewSize(): Size {
            return preview
        }
    }

    private fun setRotation(camera: Camera, parameters: Camera.Parameters, cameraId: Int) {
        val windowManager = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        var degrees = 0
        val rotation = windowManager.defaultDisplay.rotation
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
            else -> Log.e(TAG, "Bad rotation value: $rotation")
        }

        val cameraInfo = CameraInfo()
        Camera.getCameraInfo(cameraId, cameraInfo)

        val angle: Int
        val displayAngle: Int
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            angle = (cameraInfo.orientation + degrees) % 360
            displayAngle = (360 - angle) % 360 // compensate for it being mirrored
        } else { // back-facing
            angle = (cameraInfo.orientation - degrees + 360) % 360
            displayAngle = angle
        }

        // This corresponds to the rotation constants.
        this.rotation = angle / 90

        camera.setDisplayOrientation(displayAngle)
        parameters.setRotation(angle)
    }

    @SuppressLint("InlinedApi")
    private fun createPreviewBuffer(previewSize: Size): ByteArray {
        val bitsPerPixel = ImageFormat.getBitsPerPixel(ImageFormat.NV21)
        val sizeInBits = previewSize.height.toLong() * previewSize.width.toLong() * bitsPerPixel.toLong()
        val bufferSize = Math.ceil(sizeInBits / 8.0).toInt() + 1

        // Creating the byte array this way and wrapping it, as opposed to using .allocate(),
        // should guarantee that there will be an array to work with.
        val byteArray = ByteArray(bufferSize)
        val buffer = ByteBuffer.wrap(byteArray)
        if (!buffer.hasArray() || buffer.array() != byteArray) {
            // I don't think that this will ever happen.  But if it does, then we wouldn't be
            // passing the preview content to the underlying detector later.
            throw IllegalStateException("Failed to create valid buffer for camera source.")
        }

        bytesToByteBuffer[byteArray] = buffer
        return byteArray
    }

    // ==============================================================================================
    // Frame processing
    // ==============================================================================================

    private inner class CameraPreviewCallback : Camera.PreviewCallback {
        override fun onPreviewFrame(data: ByteArray, camera: Camera) {
            processingRunnable.setNextFrame(data, camera)
        }
    }

    fun setMachineLearningFrameProcessor(processor: VisionImageProcessor) {
        synchronized(processorLock) {
            cleanScreen()
            if (frameProcessor != null) {
                frameProcessor!!.stop()
            }
            frameProcessor = processor
        }
    }

    private inner class FrameProcessingRunnable internal constructor() : Runnable {

        // This lock guards all of the member variables below.
        private val lock = Object()
        private var active = true

        // These pending variables hold the state associated with the new frame awaiting processing.
        private var pendingFrameData: ByteBuffer? = null

        @SuppressLint("Assert")
        internal fun release() {
            assert(processingThread!!.state == State.TERMINATED)
        }

        internal fun setActive(active: Boolean) {
            synchronized(lock) {
                this.active = active
                lock.notifyAll()
            }
        }

        internal fun setNextFrame(data: ByteArray, camera: Camera) {
            synchronized(lock) {
                if (pendingFrameData != null) {
                    camera.addCallbackBuffer(pendingFrameData!!.array())
                    pendingFrameData = null
                }

                if (!bytesToByteBuffer.containsKey(data)) {
                    Log.d(
                        TAG,
                        "Skipping frame. Could not find ByteBuffer associated with the image data from the camera."
                    )
                    return
                }

                pendingFrameData = bytesToByteBuffer[data]

                // Notify the processor thread if it is waiting on the next frame (see below).
                lock.notifyAll()
            }
        }

        @SuppressLint("InlinedApi")
        override fun run() {
            var data: ByteBuffer

            while (true) {
                synchronized(lock) {
                    while (active && pendingFrameData == null) {
                        try {
                            // Wait for the next frame to be received from the camera, since we
                            // don't have it yet.
                            lock.wait()
                        } catch (e: InterruptedException) {
                            Log.d(TAG, "Frame processing loop terminated.", e)
                            return
                        }

                    }

                    if (!active) {
                        // Exit the loop once this camera source is stopped or released.  We check
                        // this here, immediately after the wait() above, to handle the case where
                        // setActive(false) had been called, triggering the termination of this
                        // loop.
                        return
                    }

                    // Hold onto the frame data locally, so that we can use this for detection
                    // below.  We need to clear pendingFrameData to ensure that this buffer isn't
                    // recycled back to the camera before we are done using that data.
                    data = pendingFrameData!!
                    pendingFrameData = null
                }

                // The code below needs to run outside of synchronization, because this will allow
                // the camera to add pending frame(s) while we are running detection on the current
                // frame.

                try {
                    synchronized(processorLock) {
                        Log.d(TAG, "Process an image")
                        frameProcessor!!.process(
                            data,
                            FrameMetadata.Builder()
                                .setWidth(previewSize!!.width)
                                .setHeight(previewSize!!.height)
                                .setRotation(rotation)
                                .setCameraFacing(cameraFacing)
                                .build(),
                            graphicOverlay
                        )
                    }
                } catch (t: Throwable) {
                    Log.e(TAG, "Exception thrown from receiver.", t)
                } finally {
                    camera!!.addCallbackBuffer(data.array())
                }
            }
        }
    }

    private fun cleanScreen() {
        graphicOverlay.clear()
    }

    companion object {

        private const val MEDIA_TYPE_IMAGE = 1
        private const val MEDIA_TYPE_VIDEO = 2

        @SuppressLint("InlinedApi")
        val CAMERA_FACING_BACK = CameraInfo.CAMERA_FACING_BACK

        @SuppressLint("InlinedApi")
        val CAMERA_FACING_FRONT = CameraInfo.CAMERA_FACING_FRONT

        private const val TAG = "CameraSource"
        private const val FOLDER_NAME = "TheLaughingMan"

        private const val DUMMY_TEXTURE_NAME = 100

        private const val ASPECT_RATIO_TOLERANCE = 0.01f

        private fun getIdForRequestedCamera(facing: Int): Int {
            val cameraInfo = CameraInfo()
            for (i in 0 until Camera.getNumberOfCameras()) {
                Camera.getCameraInfo(i, cameraInfo)
                if (cameraInfo.facing == facing) {
                    return i
                }
            }
            return -1
        }

        private fun selectSizePair(camera: Camera, desiredWidth: Int, desiredHeight: Int): SizePair? {
            val validPreviewSizes = generateValidPreviewSizeList(camera)

            // The method for selecting the best size is to minimize the sum of the differences between
            // the desired values and the actual values for width and height.  This is certainly not the
            // only way to select the best size, but it provides a decent tradeoff between using the
            // closest aspect ratio vs. using the closest pixel area.
            var selectedPair: SizePair? = null
            var minDiff = Integer.MAX_VALUE
            for (sizePair in validPreviewSizes) {
                val size = sizePair.previewSize()
                val diff = Math.abs(size.width - desiredWidth) + Math.abs(size.height - desiredHeight)
                if (diff < minDiff) {
                    selectedPair = sizePair
                    minDiff = diff
                }
            }

            return selectedPair
        }

        private fun generateValidPreviewSizeList(camera: Camera): List<SizePair> {
            val parameters = camera.parameters
            val supportedPreviewSizes = parameters.supportedPreviewSizes
            val supportedPictureSizes = parameters.supportedPictureSizes
            val validPreviewSizes = ArrayList<SizePair>()
            for (previewSize in supportedPreviewSizes) {
                val previewAspectRatio = previewSize.width.toFloat() / previewSize.height.toFloat()

                // By looping through the picture sizes in order, we favor the higher resolutions.
                // We choose the highest resolution in order to support taking the full resolution
                // picture later.
                for (pictureSize in supportedPictureSizes) {
                    val pictureAspectRatio = pictureSize.width.toFloat() / pictureSize.height.toFloat()
                    if (Math.abs(previewAspectRatio - pictureAspectRatio) < ASPECT_RATIO_TOLERANCE) {
                        validPreviewSizes.add(SizePair(previewSize, pictureSize))
                        break
                    }
                }
            }

            // If there are no picture sizes with the same aspect ratio as any preview sizes, allow all
            // of the preview sizes and hope that the camera can handle it.  Probably unlikely, but we
            // still account for it.
            if (validPreviewSizes.size == 0) {
                Log.w(TAG, "No preview sizes have a corresponding same-aspect-ratio picture size")
                for (previewSize in supportedPreviewSizes) {
                    // The null picture size will let us know that we shouldn't set a picture size.
                    validPreviewSizes.add(SizePair(previewSize, null))
                }
            }

            return validPreviewSizes
        }

        @SuppressLint("InlinedApi")
        private fun selectPreviewFpsRange(camera: Camera, desiredPreviewFps: Float): IntArray? {
            // The camera API uses integers scaled by a factor of 1000 instead of floating-point frame
            // rates.
            val desiredPreviewFpsScaled = (desiredPreviewFps * 1000.0f).toInt()

            // The method for selecting the best range is to minimize the sum of the differences between
            // the desired value and the upper and lower bounds of the range.  This may select a range
            // that the desired value is outside of, but this is often preferred.  For example, if the
            // desired frame rate is 29.97, the range (30, 30) is probably more desirable than the
            // range (15, 30).
            var selectedFpsRange: IntArray? = null
            var minDiff = Integer.MAX_VALUE
            val previewFpsRangeList = camera.parameters.supportedPreviewFpsRange
            for (range in previewFpsRangeList) {
                val deltaMin = desiredPreviewFpsScaled - range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX]
                val deltaMax = desiredPreviewFpsScaled - range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]
                val diff = Math.abs(deltaMin) + Math.abs(deltaMax)
                if (diff < minDiff) {
                    selectedFpsRange = range
                    minDiff = diff
                }
            }
            return selectedFpsRange
        }
    }

}