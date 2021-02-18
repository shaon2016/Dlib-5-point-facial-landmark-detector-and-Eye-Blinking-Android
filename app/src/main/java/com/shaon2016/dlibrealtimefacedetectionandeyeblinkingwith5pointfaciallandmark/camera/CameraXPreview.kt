package com.shaon2016.dlibrealtimefacedetectionandeyeblinkingwith5pointfaciallandmark.camera

import android.content.Context
import android.hardware.display.DisplayManager
import android.net.Uri
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.OrientationEventListener
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.shaon2016.dlibrealtimefacedetectionandeyeblinkingwith5pointfaciallandmark.util.FileUtil
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraXPreview(context: Context, attrs: AttributeSet) : SurfaceView(context, attrs),
    SurfaceHolder.Callback, LifecycleOwner {
    private lateinit var lifecycleRegistry: LifecycleRegistry

    private val TAG = "CameraXPreview"

    var previewWidth = 0
    var previewHeight = 0
    var displayRotation = 0

    private var captureImageUri: Uri? = null

    // CameraX
    private var imageCapture: ImageCapture? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_FRONT
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var viewFinder: PreviewView
    private var displayId: Int = -1
    private var camera: Camera? = null

    private val displayManager by lazy {
        context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    /**
     * We need a display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit


        override fun onDisplayChanged(displayId: Int) {
            if (displayId == this@CameraXPreview.displayId) {
                Log.d(TAG, "Rotation changed: ${display.rotation}")
                imageCapture?.targetRotation = display.rotation
            }
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        lifecycleRegistry = LifecycleRegistry(this)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Every time the orientation of device changes, update rotation for use cases
        displayManager.registerDisplayListener(displayListener, null)

        setupCamera()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        orientationEventListener?.disable()
        orientationEventListener = null

        cameraExecutor.shutdown()
        displayManager.unregisterDisplayListener(displayListener)

        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            cameraProvider = cameraProviderFuture.get()

            // Select lensFacing depending on the available cameras
            lensFacing = when {
                hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
                else -> throw IllegalStateException("front camera are unavailable")
            }

            // Build and bind the camera use cases
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(context))


    }

    private var orientationEventListener: OrientationEventListener? = null

    private fun bindCameraUseCases() {
        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { display.getRealMetrics(it) }

        // Preview
        val preview = configurePreviewUseCase()

        imageCapture = configureImageCapture()

        orientationEventListener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                // Monitors orientation values to determine the target rotation value
                val rotation = when (orientation) {
                    in 45..134 -> Surface.ROTATION_270
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }

                imageCapture?.targetRotation = rotation
            }
        }
        orientationEventListener?.enable()


        // Select front camera as a default
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll()

            // Bind use cases to camera
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture
            )

        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun configureImageCapture() = ImageCapture.Builder()
        .setTargetRotation(display.rotation)
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
        .build()

    private fun configurePreviewUseCase() = Preview.Builder()
        .setTargetRotation(display.rotation)
        .build()


//        .also {
//            it.setSurfaceProvider(viewFinder.surfaceProvider)
//        }

    /** Returns true if the device has an available front camera. False otherwise */
    private fun hasFrontCamera() = cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        val photoFile = FileUtil.getImageOutputDirectory(context)

        // Setup image capture metadata
        val metadata = ImageCapture.Metadata().apply {

            // Mirror image when using the front camera
            isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
        }
        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
            .setMetadata(metadata)
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {

                    captureImageUri = Uri.fromFile(photoFile)

                    captureImageUri?.let {

                    }
                }
            })
    }

    companion object {
        const val portrait = 90
        const val landleft = 0
        const val landright = 180
    }

    override fun getLifecycle() = lifecycleRegistry

}