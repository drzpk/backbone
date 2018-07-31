package com.gitlab.drzepka.backbone.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.Button
import com.gitlab.drzepka.backbone.R
import com.gitlab.drzepka.backbone.util.ScreenShooter
import com.theartofdev.edmodo.cropper.CropImageView

/**
 * An activity used to crop images created by [com.gitlab.drzepka.backbone.util.ScreenShooter]. It utilizes the
 * [Android-Image-Cropper](https://github.com/ArthurHub/Android-Image-Cropper) library.
 *
 * This activity is designed to work in conjunction with the [ScreenShooter] class and shouldn't be used directly.
 */
class ImageCropperActivity : AppCompatActivity() {

    private lateinit var imageView: CropImageView
    private lateinit var fileUri: Uri
    private var referenceId: Int = 0
    private var returnBitmap: Boolean = true

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_cropper)
        imageView = findViewById(R.id.image_cropper_image)

        // Inflate the custom toolbar
        val toolbarView = findViewById<Toolbar>(R.id.activity_image_cropper_toolbar)
        toolbarView.findViewById<Button>(R.id.image_cropper_toolbar_discard).setOnClickListener {
            discardImage()
        }

        // Apply the custom layout to the toolbar
        setSupportActionBar(toolbarView)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        //supportActionBar?.setDisplayShowCustomEnabled(true)
        //supportActionBar?.customView = toolbarVie

        if (!intent.hasExtra(ScreenShooter.EXTRA_IMAGE_URI))
            throw IllegalStateException("Image URI must be provided")
        fileUri = Uri.parse(intent.getStringExtra(ScreenShooter.EXTRA_IMAGE_URI))
        referenceId = intent.getIntExtra(ScreenShooter.EXTRA_REFERENCE_ID, 0)
        returnBitmap = intent.getBooleanExtra(ScreenShooter.EXTRA_RETURN_BITMAP, true)

        imageView.setImageUriAsync(fileUri)
        imageView.setOnSetImageUriCompleteListener({ _, _, error ->
            if (error == null) {
                // Image was loaded successfully
                toolbarView.findViewById<Button>(R.id.image_cropper_toolbar_save).setOnClickListener {
                    saveImage()
                }
                toolbarView.findViewById<View>(R.id.image_cropper_toolbar_rotate_left).setOnClickListener {
                    rotateImage(false)
                }
                toolbarView.findViewById<View>(R.id.image_cropper_toolbar_rotate_right).setOnClickListener {
                    rotateImage(true)
                }
            } else {
                // An error occurred
                error.printStackTrace()
                finish(false)
            }
        })
    }

    private fun saveImage() {
        imageView.setOnCropImageCompleteListener({ _, _ ->
            finish(true)
        })
        imageView.saveCroppedImageAsync(fileUri, Bitmap.CompressFormat.PNG, 100)
    }

    private fun rotateImage(clockwise: Boolean) {
        imageView.rotateImage(90 * if (clockwise) 1 else -1)
    }

    private fun discardImage() {
        setResult(RESULT_CANCELED)
        finish(false)
    }

    private fun finish(success: Boolean) {
        val intent = Intent(this, ScreenShooter.ScreenShooterService::class.java)
        intent.putExtra(ScreenShooter.EXTRA_REFERENCE_ID, referenceId)
        intent.putExtra(ScreenShooter.EXTRA_RETURN_BITMAP, returnBitmap)
        if (success) {
            intent.action = ScreenShooter.ACTION_RETURN_IMAGE
            intent.putExtra(ScreenShooter.EXTRA_IMAGE_URI, fileUri.toString())
        } else
            intent.action = ScreenShooter.ACTION_SCREENSHOT_CANCEL

        startService(intent)
        finish()
    }
}