package com.shaon2016.dlibrealtimefacedetectionandeyeblinkingwith5pointfaciallandmark

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.shaon2016.dlibrealtimefacedetectionandeyeblinkingwith5pointfaciallandmark.util.FileUtil
import java.io.ByteArrayOutputStream
import java.io.File

/** The request code for requesting [Manifest.permission.READ_EXTERNAL_STORAGE] permission. */
private const val PERMISSIONS_REQUEST = 0x1045

class MainActivity : AppCompatActivity() {
    private val pb = findViewById<ProgressBar>(R.id.pb)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pb.visibility = View.VISIBLE
        loadDlibModel()
        pb.visibility = View.GONE

        if (havePermission()) {
            loadFragment(CameraXFragment.newInstance())
        } else {
            requestPermissions()
        }
    }

    private fun loadDlibModel() {
        val modelPath =
            "${getExternalFilesDirs(Environment.DIRECTORY_DOCUMENTS).first()}/shape_predictor_68_face_landmarks.dat"
        if (!File(modelPath).exists())
            FileUtil.copyFileFromAsset(
                this, "shape_predictor_68_face_landmarks.dat",
                modelPath
            )

        Native.loadModel(modelPath)
    }

    // Permission Sections
    private fun havePermission() =
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED)
        } else {
            (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED)
        }

    /**
     * Convenience method to request [Manifest.permission.READ_EXTERNAL_STORAGE] permission.
     */
    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (!havePermission()) {
                val permissions = arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
                )
                ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST)
            }
        } else {
            if (!havePermission()) {
                val permissions = Manifest.permission.CAMERA

                ActivityCompat.requestPermissions(this, arrayOf(permissions), PERMISSIONS_REQUEST)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSIONS_REQUEST -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && havePermission()) {
                    loadFragment(CameraXFragment.newInstance())
                } else {
                    // If we weren't granted the permission, check to see if we should show
                    // rationale for the permission.
                    showDialogToAcceptPermissions()
                }
                return
            }
        }

    }

    private fun showDialogToAcceptPermissions() {
        showPermissionRationalDialog("You need to allow access to view and capture image")
    }

    private fun showPermissionRationalDialog(msg: String) {
        AlertDialog.Builder(this)
            .setMessage(msg)
            .setPositiveButton(
                "OK"
            ) { dialog, which ->
                goToSettings()
            }
            .setNegativeButton("Cancel") { dialog, which ->
                onBackPressed()
            }
            .create()
            .show()
    }

    private val startSettingsForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (havePermission()) {
                loadFragment(CameraXFragment.newInstance())
            } else finish()
        }

    private fun loadFragment(frag: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, frag)
            .commit()
    }

    private fun goToSettings() {
        val intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        startSettingsForResult.launch(intent)
    }

    companion object {
        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }

    fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)

        return stream.toByteArray()
    }

}