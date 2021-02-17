package com.shaon2016.dlibrealtimefacedetectionandeyeblinkingwith5pointfaciallandmark

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat

class ImageWithLandmark(context: Context, attrs: AttributeSet) :
    AppCompatImageView(context, attrs) {
    // Stroke & paint.
    private val WIDTH = 2f
    private var mStrokeWidth = 2
    private var mStrokePaint: Paint
    private val mRenderMatrix = Matrix()
    private val rPaint = Paint()

    private var landmarks: LongArray? = null

    init {
        val density = getContext()
            .resources.displayMetrics.density

        mStrokeWidth = (density * WIDTH).toInt()

        mStrokePaint = Paint()
        mStrokePaint.color = ContextCompat.getColor(getContext(), R.color.purple_200)

        rPaint.color = Color.rgb(255, 160, 0)
        rPaint.style = Paint.Style.STROKE
        rPaint.strokeWidth = 5f

    }

    override fun onDrawForeground(canvas: Canvas?) {
        super.onDrawForeground(canvas)

        landmarks?.let {
            for (i in landmarks!!.indices step 2) {
                val x = landmarks!![i]
                val y = landmarks!![i + 1]

                canvas?.drawCircle(x.toFloat(), y.toFloat(), 8f, mStrokePaint)

                Log.d("DATATAG", i.toString())
            }

        }
    }

    fun setLendmarks(landmarks: LongArray) {
        this.landmarks = landmarks
    }


}