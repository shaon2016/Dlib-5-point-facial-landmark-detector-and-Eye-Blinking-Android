package com.shaon2016.dlibrealtimefacedetectionandeyeblinkingwith5pointfaciallandmark.util

import android.R.attr.path
import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream


object FileUtil {
    fun copyFileFromAsset(context: Context, assetFileName: String, outputPath: String) {
        Log.d("DATATAG", "Called")
        val assetManager = context.assets
        try {
            val `in` = assetManager.open(assetFileName)
            val out = FileOutputStream(File(outputPath))
            val buffer = ByteArray(1024)
            var read: Int = `in`.read(buffer)
            while (read != -1) {
                out.write(buffer, 0, read)
                read = `in`.read(buffer)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}