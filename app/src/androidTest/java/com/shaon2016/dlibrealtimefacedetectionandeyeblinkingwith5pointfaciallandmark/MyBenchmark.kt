package com.shaon2016.dlibrealtimefacedetectionandeyeblinkingwith5pointfaciallandmark

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getExternalCacheDirs
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shaon2016.dlibrealtimefacedetectionandeyeblinkingwith5pointfaciallandmark.util.FileUtil
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File


@RunWith(AndroidJUnit4::class)
class MyBenchmark {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @get:Rule
    val benchmarkRule = BenchmarkRule()

//    @get:Rule
//    val mRuntimePermissionRule: GrantPermissionRule =
//        GrantPermissionRule.grant(
//            Manifest.permission.WRITE_EXTERNAL_STORAGE
//        )

    @Test
    fun benchmarkSomeWork() = benchmarkRule.measureRepeated {
        doSomeWork()
    }

    private fun doSomeWork() {
//        MyPermissionRequester.request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
//        var grantResult = ActivityCompat.checkSelfPermission(
//           context,
//            Manifest.permission.WRITE_EXTERNAL_STORAGE
//        )
//        Assert.assertEquals(PackageManager.PERMISSION_GRANTED, grantResult)
//        grantResult = ActivityCompat.checkSelfPermission(
//            context,
//            Manifest.permission.WRITE_EXTERNAL_STORAGE
//        )
//        Assert.assertEquals(PackageManager.PERMISSION_GRANTED, grantResult)
//
//
//        val modelPath =
//            "${
//                getExternalCacheDirs(
//                    context,
//                ).first()
//            }/shape_predictor_68_face_landmarks.dat"
//        if (!File(modelPath).exists())
//            FileUtil.copyFileFromAsset(
//                context, "shape_predictor_68_face_landmarks.dat",
//                modelPath
//            )
//
//        Native.loadModel(modelPath)
//
//        val b = BitmapFactory.decodeResource(context.resources, R.drawable.man)
//
//        val bmp = b.copy(Bitmap.Config.ARGB_8888, true)
//
//        val width = bmp.width
//        val height = bmp.height
//
//        val pixels = IntArray(width * height)
//        b.getPixels(pixels, 0, width, 0, 0, width, height)
//
//        Log.d("DATATAG", Native.detectLandmark(pixels, width, height).toString())
    }
}