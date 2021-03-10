package com.shaon2016.dlibrealtimefacedetectionandeyeblinkingwith5pointfaciallandmark.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.DisplayMetrics
import androidx.camera.core.ImageProxy
import kotlin.math.roundToInt

object Helper {
    fun dpToPx(context: Context, dp: Int): Int {
        val displayMetrics: DisplayMetrics = context.resources.displayMetrics
        return (dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)).roundToInt()
    }

    fun ImageProxy.toBitmap(
        context: Context
    ): Bitmap? {
        val image = image ?: return null

        val rotationMatrix = Matrix()
        // if front camera is enable
        rotationMatrix.preScale(1.0f, -1.0f) // front camera in vertical

        rotationMatrix.postRotate(imageInfo.rotationDegrees.toFloat())


        val bitmap = Bitmap.createBitmap(
            width, height, Bitmap.Config.ARGB_8888
        )

        // Pass image to an image analyser
        YuvToRgbConverter(context).yuvToRgb(image, bitmap)

        // Create the Bitmap in the correct orientation
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            rotationMatrix,
            false
        )
    }
}