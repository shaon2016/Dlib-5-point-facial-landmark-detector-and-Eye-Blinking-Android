package com.shaon2016.dlibrealtimefacedetectionandeyeblinkingwith5pointfaciallandmark

import android.os.Bundle
import android.view.*
import android.widget.RelativeLayout
import androidx.camera.view.PreviewView
import androidx.fragment.app.Fragment
import com.shaon2016.dlibrealtimefacedetectionandeyeblinkingwith5pointfaciallandmark.camera.CameraXManager


class CameraXFragment : Fragment() {
    private lateinit var viewFinder: PreviewView
    private lateinit var overlayView: OverlayView

    private val cm by lazy {
        CameraXManager(requireContext(), viewFinder, overlayView)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_camera_x, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val container = view as RelativeLayout
        viewFinder = container.findViewById(R.id.viewFinder)
        overlayView = container.findViewById(R.id.overlay_view)

        viewFinder.post {
            cm.setupCamera()
        }
    }

    override fun onResume() {
        super.onResume()

        cm.onResume()
    }


    override fun onDestroy() {
        super.onDestroy()

        cm.destroyed()
    }


    companion object {

        @JvmStatic
        fun newInstance() = CameraXFragment()

    }
}