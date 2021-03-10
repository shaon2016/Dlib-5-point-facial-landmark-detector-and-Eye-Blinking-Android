package com.shaon2016.dlibrealtimefacedetectionandeyeblinkingwith5pointfaciallandmark.camera

import android.content.Context
import android.graphics.Matrix
import android.hardware.camera2.*
import android.hardware.display.DisplayManager
import android.net.Uri
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.OrientationEventListener
import android.view.Surface
import android.widget.ImageView
import androidx.camera.core.*
import androidx.camera.core.impl.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import com.shaon2016.dlibrealtimefacedetectionandeyeblinkingwith5pointfaciallandmark.FaceLandmarkOverlay
import com.shaon2016.dlibrealtimefacedetectionandeyeblinkingwith5pointfaciallandmark.Native
import com.shaon2016.dlibrealtimefacedetectionandeyeblinkingwith5pointfaciallandmark.OverlayView
import com.shaon2016.dlibrealtimefacedetectionandeyeblinkingwith5pointfaciallandmark.R
import com.shaon2016.dlibrealtimefacedetectionandeyeblinkingwith5pointfaciallandmark.Recognition
import com.shaon2016.dlibrealtimefacedetectionandeyeblinkingwith5pointfaciallandmark.util.Helper
import com.shaon2016.dlibrealtimefacedetectionandeyeblinkingwith5pointfaciallandmark.util.Helper.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

typealias RecognitionListener = (recognition: List<Recognition>) -> Unit

class CameraXManager(
    private val context: Context,
    private val viewFinder: PreviewView,
    private val mOverlayView: FaceLandmarkOverlay
) : LifecycleOwner {

    private val TAG = "CameraXManager"
    private var lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)

    // CameraX
    private var captureImageUri: Uri? = null
    private var imageCapture: ImageCapture? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_FRONT
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private lateinit var cameraProvider: ProcessCameraProvider
    private var displayId: Int = -1
    private var imageAnalyzer: ImageAnalysis? = null
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
            if (displayId == this@CameraXManager.displayId) {
                Log.d(TAG, "Rotation changed: ${viewFinder.display.rotation}")
                imageCapture?.targetRotation = viewFinder.display.rotation
                imageAnalyzer?.targetRotation = viewFinder.display.rotation
            }
        }
    }

    private val mFaceDetectionMatrix: Matrix? = null

    /*private val mCaptureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult
        ) {
            process(partialResult)
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            process(result)

        }

        private fun process(result: CaptureResult) {
            val mode = result.get(CaptureResult.STATISTICS_FACE_DETECT_MODE)
            val faces = result.get(CaptureResult.STATISTICS_FACES)

            Log.i("Test", "faces : " + "${faces?.size ?: 0}" + " , mode : " + mode)

            if (faces != null && mode != null) {
                if (faces.isNotEmpty()) {
                    for (i in faces.indices) {
                        if (faces[i].score > 50) {
                            Log.i("Test", "faces : " + faces.size + " , mode : " + mode)
                            val left = faces[i].bounds.left
                            val top = faces[i].bounds.top
                            val right = faces[i].bounds.right
                            val bottom = faces[i].bounds.bottom
                            //float points[] = {(float)left, (float)top, (float)right, (float)bottom};
                            val uRect = Rect(left, top, right, bottom)
                            val rectF = RectF(uRect)
                            mFaceDetectionMatrix?.mapRect(rectF)

                            rectF.round(uRect)

                            Log.i("Test", "Activity rect$i bounds: $uRect")
                            (context as Activity).runOnUiThread(Runnable {
                                mOverlayView.rect = uRect
                                mOverlayView.requestLayout()
                            })
                            break
                        }
                    }
                }
            }
        }

    }*/


    init {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED

        // Every time the orientation of device changes, update rotation for use cases
        displayManager.registerDisplayListener(displayListener, null)

    }


    fun setupCamera() {
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
        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)

        // Preview
        val preview = configurePreviewUseCase(screenAspectRatio)

        imageCapture = configureImageCapture(screenAspectRatio)

        imageAnalyzer = configureImageAnalyzer(screenAspectRatio)

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
                this, cameraSelector, preview, imageCapture, imageAnalyzer
            )

            analyzeImage()
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }

    }

    private fun analyzeImage() {
    }

    private fun configureImageAnalyzer(screenAspectRatio: Int): ImageAnalysis {
        return ImageAnalysis.Builder()
//            .setTargetAspectRatio(screenAspectRatio)
            .setTargetResolution(Size(viewFinder.width, Helper.dpToPx(context, 300)))
            .setTargetRotation(viewFinder.display.rotation)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, ImageAnalyzer(context) {

                })
            }
    }
    inner class ImageAnalyzer(
        private val ctx: Context,
        val recognitionListener: RecognitionListener
    ) :
        ImageAnalysis.Analyzer {

//        private val cascadeClassifier = CascadeClassifier(C.modelPath(ctx))
//        private val faceDetections = MatOfRect()

        override fun analyze(image: ImageProxy) {
            val items = mutableListOf<Recognition>()

//            val matForBitmap = Mat()
////            Utils.bitmapToMat(imageProxyToBitmap(ctx, image), matForBitmap)
//            val toBitmap = image.toBitmap(
//                ctx,
//                needToFlipForFrontCamera,
//                imageInFrontCameraVertical
//            )
//            Utils.bitmapToMat(
//                toBitmap, matForBitmap
//            )
//            cascadeClassifier.detectMultiScale(matForBitmap, faceDetections)
//
//            // Drawing boxes
//            if (faceDetections.toArray().isNotEmpty()) {
//                val rect = faceDetections.toArray()[0]
//
//                Log.d(
//                    TAG,
//                    "Rect: X: ${rect.x}, Y: ${rect.y}, Width: ${rect.width}, Height: ${rect.height}"
//                )
//
//                val rect1 = Rect(
//                    rect.x,
//                    rect.y,
//                    rect.x + rect.width,
//                    rect.y + rect.height
//                )
//                viewFaceOverlay.setFace(rect1)
////
//
//                lifecycleScope.launch(Dispatchers.Main) {
//                    iv.setImageBitmap(
//                        Bitmap.createBitmap(
//                            toBitmap!!,
//                            rect.x,
//                            rect.x,
//                            rect.width,
//                            rect.width,
//
//                            )
//                    )
//                }
//            } else {
//                viewFaceOverlay.setFace(Rect())
//            }

            val toBitmap = image.toBitmap(ctx)


            toBitmap?.let {
                val width = toBitmap.width
                val height = toBitmap.height
                Log.d("DATATAG", "Image proxy ${Helper.dpToPx(ctx, height)}")

                val pixels = IntArray(width * height)
                toBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

                // Detect landmark
                val landmarks = Native.detectLandmark(pixels, width, height)

                lifecycleScope.launch(Dispatchers.Main) {

                    landmarks?.let {
                        mOverlayView.setLendmarks(landmarks)
                    }

                }
            }



            image.close()
        }
    }

    private fun configureImageCapture(screenAspectRatio: Int) = ImageCapture.Builder()
        .setTargetAspectRatio(screenAspectRatio)
        .setTargetRotation(viewFinder.display.rotation)
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
        .build()

    private fun configurePreviewUseCase(screenAspectRatio: Int) = Preview.Builder()
        /*.also {
            *//**
             * Code for camera 2 capture callback
             * *//*
            val previewExtender = Camera2Interop.Extender(it)

            previewExtender.setSessionCaptureCallback(mCaptureCallback)

            previewExtender.setCaptureRequestOption(
                CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                CameraMetadata.STATISTICS_FACE_DETECT_MODE_FULL
            )
        }*/
//        .setTargetAspectRatio(screenAspectRatio)
        .setTargetResolution(Size(viewFinder.width, Helper.dpToPx(context, 300)))
        .setTargetRotation(viewFinder.display.rotation)
        .build()
        .also {
            it.setSurfaceProvider(viewFinder.surfaceProvider)
        }

    /** Returns true if the device has an available front camera. False otherwise */
    private fun hasFrontCamera() = cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)

    /*private fun takePhoto() {
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
    }*/

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    override fun getLifecycle() = lifecycleRegistry

    fun destroyed() {
        orientationEventListener?.disable()
        orientationEventListener = null

        cameraExecutor.shutdown()
        displayManager.unregisterDisplayListener(displayListener)

        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    fun onResume() {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        setupCamera()
    }


    companion object {
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }
}