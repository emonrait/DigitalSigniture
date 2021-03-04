package com.example.digitalsigniture

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.github.gcacace.signaturepad.views.SignaturePad
import java.io.*

class MainActivity : AppCompatActivity() {
    private val REQUEST_EXTERNAL_STORAGE = 1
    private val PERMISSIONS_STORAGE = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private var mSignaturePad: SignaturePad? = null
    private var mClearButton: Button? = null
    private var mSaveButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        verifyStoragePermissions(this)
        setContentView(R.layout.activity_main)

        mSignaturePad = findViewById<View>(R.id.signature_pad) as SignaturePad
        mSignaturePad?.setOnSignedListener(object : SignaturePad.OnSignedListener {
            override fun onStartSigning() {
                //Toast.makeText(this@MainActivity, "OnStartSigning", Toast.LENGTH_SHORT).show()
            }

            override fun onSigned() {
                mSaveButton!!.isEnabled = true
                mClearButton!!.isEnabled = true
            }

            override fun onClear() {
                mSaveButton!!.isEnabled = false
                mClearButton!!.isEnabled = false
            }
        })

        mClearButton = findViewById<View>(R.id.clear_button) as Button
        mSaveButton = findViewById<View>(R.id.save_button) as Button

        mClearButton?.setOnClickListener(View.OnClickListener { mSignaturePad?.clear() })

        mSaveButton?.setOnClickListener(View.OnClickListener {
            val signatureBitmap = mSignaturePad?.getSignatureBitmap()
            if (signatureBitmap?.let { it1 -> addJpgSignatureToGallery(it1) } == true) {
                Toast.makeText(
                        this@MainActivity,
                        "Signature saved into the Gallery",
                        Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                        this@MainActivity,
                        "Unable to store the signature",
                        Toast.LENGTH_SHORT
                ).show()
            }/*
            if (addSvgSignatureToGallery(mSignaturePad?.getSignatureSvg())) {
                Toast.makeText(
                        this@MainActivity,
                        "SVG Signature saved into the Gallery",
                        Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                        this@MainActivity,
                        "Unable to store the SVG signature",
                        Toast.LENGTH_SHORT
                ).show()
            }*/
        })
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String?>, grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_EXTERNAL_STORAGE -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.size <= 0
                        || grantResults[0] != PackageManager.PERMISSION_GRANTED
                ) {
                    Toast.makeText(
                            this@MainActivity,
                            "Cannot write images to external storage",
                            Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun getAlbumStorageDir(albumName: String?): File? {
        // Get the directory for the user's public pictures directory.

        val file = File(
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES
                ), albumName
        )
        if (!file.mkdirs()) {
            Log.e("SignaturePad", "Directory not created")
        }
        return file
    }

    @Throws(IOException::class)
    fun saveBitmapToJPG(bitmap: Bitmap, photo: File?) {
        val newBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(newBitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        val stream: OutputStream = FileOutputStream(photo)
        newBitmap.compress(Bitmap.CompressFormat.PNG, 80, stream)
        stream.close()
    }

    fun addJpgSignatureToGallery(signature: Bitmap): Boolean {
        var result = false
        try {
            val photo = File(
                    getAlbumStorageDir("SignaturePad"),
                    String.format("Signature_%d.jpg", System.currentTimeMillis())
            )
            saveBitmapToJPG(signature, photo)
            scanMediaFile(photo)
            result = true
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return result
    }

    private fun scanMediaFile(photo: File) {
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        val contentUri = Uri.fromFile(photo)
        mediaScanIntent.data = contentUri
        this@MainActivity.sendBroadcast(mediaScanIntent)
    }

    fun addSvgSignatureToGallery(signatureSvg: String?): Boolean {
        var result = false
        try {
            val svgFile = File(
                    getAlbumStorageDir("SignaturePad"),
                    String.format("Signature_%d.svg", System.currentTimeMillis())
            )
            val stream: OutputStream = FileOutputStream(svgFile)
            val writer = OutputStreamWriter(stream)
            writer.write(signatureSvg)
            writer.close()
            stream.flush()
            stream.close()
            scanMediaFile(svgFile)
            result = true
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return result
    }

    /**
     * Checks if the app has permission to write to device storage
     *
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity the activity from which permissions are checked
     */
    fun verifyStoragePermissions(activity: Activity?) {
        // Check if we have write permission
        val permission = ActivityCompat.checkSelfPermission(
                activity!!,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            )
        }
    }
}
