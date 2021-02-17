package com.shaon2016.dlibrealtimefacedetectionandeyeblinkingwith5pointfaciallandmark

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.YuvImage
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.shaon2016.dlibrealtimefacedetectionandeyeblinkingwith5pointfaciallandmark.util.FileUtil
import java.io.ByteArrayOutputStream
import java.io.File

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Example of a call to a native method
//        findViewById<TextView>(R.id.sample_text).text = stringFromJNI()

        val modelPath =
            "${getExternalFilesDirs(Environment.DIRECTORY_DOCUMENTS).first()}/shape_predictor_5_face_landmarks.dat"
        if (!File(modelPath).exists())
            FileUtil.copyFileFromAsset(
                this, "shape_predictor_5_face_landmarks.dat",
                modelPath
            )

        Native.loadModel(modelPath)

        val b = BitmapFactory.decodeResource(resources, R.drawable.man)
        findViewById<ImageView>(R.id.iv).setImageBitmap(b)

        val bmp = b.copy(Bitmap.Config.ARGB_8888, true)

        val width = bmp.width
        val height = bmp.height
        val pixels = IntArray(width * height)
        bmp.getPixels(pixels, 0, width, 0, 0, width, height)

        // Detect landmark

        Log.d("DATATAG", Native.detectLandmarkARGB(pixels, width, height).toString())
    }

    companion object {
        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }

    fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)

        return stream.toByteArray()
    }

}