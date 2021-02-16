package com.shaon2016.dlibrealtimefacedetectionandeyeblinkingwith5pointfaciallandmark

import android.graphics.YuvImage


/**
 * Native:  act as an interface between Kotlin and C++
 * Created by Luca on 12/04/2018.
 */
object Native {
    /**
     * analise the raw captured frame from camera to find the face landmarks
     */
//    fun analiseFrame(
//        yuv: ByteArray,
//        rotation: Int,
//        width: Int,
//        height: Int,
//        region: Rect
//    ): LongArray {
//        return detectLandmarks(
//            yuv, rotation, width, height,
//            region.left, region.top, region.right, region.bottom
//        )
//    }

    /**
     * load the specified landmark model (for dlib)
     */
    external fun loadModel(path: String?)
//    external fun setImageFormat(format: Int)
//    external fun detectLandmarks(
//        yuv: ByteArray, rotation: Int, width: Int, height: Int,
//        left: Int, top: Int, right: Int, bottom: Int
//    ): LongArray

    external fun detectLandmarkARGB(argb: IntArray, width: Int, height: Int): Int


}