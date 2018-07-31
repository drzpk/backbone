package com.gitlab.drzepka.backbone.sample

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import com.gitlab.drzepka.backbone.component.ActivityComponent
import com.gitlab.drzepka.backbone.component.TextComponent
import com.gitlab.drzepka.backbone.payload.NotificationPayload
import com.gitlab.drzepka.backbone.util.API
import com.gitlab.drzepka.backbone.util.ScreenShooter

class ScreenShooterActivity : AppCompatActivity(), ScreenShooter.OnScreenshotReadyListener {

    private lateinit var imageView: ImageView
    private lateinit var screenShooter: ScreenShooter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen_shooter)
        imageView = findViewById(R.id.activity_screenshooter_image)

        if (API.oreo()) {
            val channel = NotificationChannel("basic_channel", "channel", NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val payload = NotificationPayload(
                this,
                TextComponent("Take a screenshot"),
                TextComponent("Press one of the buttons to take a screenshot or another to dismiss this notification"),
                channel = "basic_channel",
                positiveActionText = TextComponent("Shoot"),
                negativeActionText = TextComponent("cancel")
        )
        screenShooter = ScreenShooter(ActivityComponent(this), payload, this)
    }

    @Suppress("UNUSED_PARAMETER")
    fun takeScreenshot(view: View?) {
        screenShooter.shoot()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        screenShooter.onActivityResult(requestCode, resultCode, data)
    }

    override fun onScreenshotReady(bitmap: Bitmap?, bitmapUri: Uri?) {
        Toast.makeText(this, "Screenshot has been taken", Toast.LENGTH_LONG).show()
        imageView.setImageBitmap(bitmap)
    }

    override fun onScreenshotCanceled() {
        Toast.makeText(this, "Screenshot cancelled", Toast.LENGTH_LONG).show()
    }
}