package com.gitlab.drzepka.backbone.util

import android.annotation.TargetApi
import android.app.Activity
import android.app.IntentService
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.util.DisplayMetrics
import android.view.WindowManager
import com.gitlab.drzepka.backbone.activity.ImageCropperActivity
import com.gitlab.drzepka.backbone.component.ActivityComponent
import com.gitlab.drzepka.backbone.helper.post
import com.gitlab.drzepka.backbone.payload.NotificationPayload
import com.gitlab.drzepka.backbone.util.ScreenShooter.Companion.canTakeScreenshot
import eu.chainfire.libsuperuser.Shell
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.concurrent.thread

/**
 * This class' purpose is to facilitate taking screenshots.
 *
 * **Note:** before using this class you should check whether a device meets the requirements by invoking the
 * [canTakeScreenshot] method.
 */
class ScreenShooter(
        /** Owner of this instance of class. */
        private val activityComponent: ActivityComponent,
        /**
         * A notification info that will be used to build and show notification. Positive and negative button texts must be provided.
         * Pass `null` if a screenshot should be taken instantly after calling the [shoot] method.
         */
        private val notificationPayload: NotificationPayload?,
        /** A listener for ScreenShooter events. */
        listener: OnScreenshotReadyListener,
        /** Whether to prefer taking screenshot using root method over media projection. */
        private val preferRoot: Boolean = true,
        /** Whether to run editor before returning an image. */
        private val useEditor: Boolean = true,
        /** Whether this class should return bitmap or bitmap URI to a listener. */
        private val returnBitmap: Boolean = true,
        /** Delay between a screenshot request and capture (used to wait for notification bar to collapse). */
        private val delay: Long = 500L
) {

    private val listenerId: Int = ReferenceCenter.registerReference(listener)

    /**
     * Shows notification that will be used to take a screenshot (if no notification data has been given, screenshot
     * will be taken immediately). Pass either fragment or activity instance, depending
     * on where you're calling this method from. This method  requires the `onActivityResult` call to be redirected
     * here (see the [onActivityResult] method).
     */
    fun shoot() {
        // Device doesn't meet the requirements
        if (!canTakeScreenshot())
            return

        fun mediaProjection() {
            if (API.lollipop()) {
                val manager = activityComponent.context!!.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                val intent = manager.createScreenCaptureIntent()
                activityComponent.startActivityForResult(intent, REQUEST_MEDIA_PROJECTION)
            }
        }

        if (preferRoot) {
            if (Shell.SU.available())
                rootMethod()
            else
                mediaProjection()
        } else {
            if (API.lollipop())
                mediaProjection()
            else
                rootMethod()
        }
    }

    /**
     * Call this code from your activity's or frament's `onActivityResult` method in order for this class to work.
     */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_MEDIA_PROJECTION && resultCode == Activity.RESULT_OK)
            mediaProjectionMethod(resultCode, data!!)
    }

    private fun getIntents(code: Int, data: Intent?): Array<Intent> {
        fun attachData(intent: Intent) {
            intent.putExtra(EXTRA_REFERENCE_ID, listenerId)
            intent.putExtra(EXTRA_RETURN_BITMAP, returnBitmap)
            intent.putExtra(EXTRA_USE_EDITOR, useEditor)
            intent.putExtra(EXTRA_DELAY, delay)
        }

        val shootIntent = Intent(activityComponent.context, ScreenShooterService::class.java)
        attachData(shootIntent)
        if (data != null) {
            // Media projection mode
            shootIntent.action = ACTION_SCREENSHOT_MEDIA_PROJECTION
            shootIntent.putExtra(EXTRA_CODE, code)
            shootIntent.putExtra(EXTRA_DATA, data)
        } else {
            // Root method
            shootIntent.action = ACTION_SCREENSHOT_ROOT
        }

        val cancelIntent = Intent(activityComponent.context, ScreenShooterService::class.java)
        cancelIntent.action = ACTION_SCREENSHOT_CANCEL
        attachData(cancelIntent)

        val ret = ArrayList<Intent>()
        ret.add(shootIntent)
        ret.add(cancelIntent)
        return ret.toTypedArray()
    }

    private fun showNotification(code: Int, data: Intent?) {
        if (notificationPayload == null)
            return

        val intents = getIntents(code, data)

        val shootPendingIntent = PendingIntent.getService(activityComponent.context, 1, intents[0], PendingIntent.FLAG_ONE_SHOT)
        val cancelPendingIntent = PendingIntent.getService(activityComponent.context, 2, intents[1], PendingIntent.FLAG_ONE_SHOT)

        val builder = NotificationCompat.Builder(notificationPayload.context, notificationPayload.channel)
                .setContentTitle(notificationPayload.title.getText(activityComponent.context!!))
                .setStyle(NotificationCompat.BigTextStyle().bigText(notificationPayload.content.getText(activityComponent.context!!)))
                .setAutoCancel(false)
                .addAction(android.R.color.transparent, notificationPayload.positiveActionText?.getText(activityComponent.context!!)
                        ?: "", shootPendingIntent)
                .addAction(android.R.color.transparent, notificationPayload.negativeActionText?.getText(activityComponent.context!!)
                        ?: "", cancelPendingIntent)

        if (notificationPayload.icon != 0)
            builder.setSmallIcon(notificationPayload.icon)
        else
            builder.setSmallIcon(android.R.mipmap.sym_def_app_icon)

        val manager = NotificationManagerCompat.from(activityComponent.context)
        manager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun rootMethod() {
        if (notificationPayload == null) {
            // Launch screenshot routine directly
            val intent = getIntents(0, null)[0]
            activityComponent.context?.startService(intent)
        } else
            showNotification(0, null)
    }

    private fun mediaProjectionMethod(code: Int, data: Intent) {
        if (notificationPayload == null) {
            // Launch screenshot routine directly
            val intent = getIntents(code, data)[0]
            activityComponent.context?.startService(intent)
        } else
            showNotification(code, data)
    }

    /**
     * The listener associated with the ScreenShooter.
     */
    interface OnScreenshotReadyListener {
        /**
         * Called when the screenshot is ready. Returns either a [bitmap] or [bitmapUri], depending on the
         * ScreenShooter settings.
         * @see ScreenShooter.returnBitmap
         */
        fun onScreenshotReady(bitmap: Bitmap? = null, bitmapUri: Uri? = null)

        /**
         * Called when a notification or image editing is cancelled.
         */
        fun onScreenshotCanceled()
    }

    class ScreenShooterService : IntentService("ScreenShooterService") {
        private var referenceId = 0
        private var returnBitmap = true
        private var useEditor = false
        private var delay = 0L

        override fun onHandleIntent(intent: Intent?) {
            if (intent == null)
                return

            referenceId = intent.getIntExtra(EXTRA_REFERENCE_ID, 0)
            returnBitmap = intent.getBooleanExtra(EXTRA_RETURN_BITMAP, true)
            useEditor = intent.getBooleanExtra(EXTRA_USE_EDITOR, false)
            delay = intent.getLongExtra(EXTRA_DELAY, 0)

            when (intent.action) {
                ACTION_SCREENSHOT_MEDIA_PROJECTION -> {
                    val code = intent.getIntExtra(EXTRA_CODE, 0)
                    val data = intent.getParcelableExtra<Intent>(EXTRA_DATA)
                    Handler(Looper.getMainLooper()).postDelayed({ screenshotByMediaProjection(code, data) }, delay)
                    dismissNotification(this)
                }
                ACTION_SCREENSHOT_ROOT -> {
                    screenshotByRoot()
                    dismissNotification(this)
                }
                ACTION_SCREENSHOT_CANCEL -> {
                    dismissNotification(this)
                    returnCancel()
                }
                ACTION_RETURN_IMAGE -> returnImage(
                        if (intent.hasExtra(EXTRA_IMAGE_URI)) Uri.parse(intent.getStringExtra(EXTRA_IMAGE_URI)) else null)
            }
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        private fun screenshotByMediaProjection(code: Int, data: Intent) {
            val windowManager = applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            windowManager.defaultDisplay.getRealMetrics(metrics)

            val size = Point()
            windowManager.defaultDisplay.getRealSize(size)

            val imageReader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2)

            val manager = applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val projection = manager.getMediaProjection(code, data) ?: return
            val display = projection.createVirtualDisplay(
                    "screen-shooter-display",
                    metrics.widthPixels,
                    metrics.heightPixels,
                    metrics.densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC or DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY,
                    imageReader.surface,
                    null,
                    null)

            imageReader.setOnImageAvailableListener({
                // Compute image parameters
                val image = it.acquireLatestImage()
                val plane = image.planes[0]
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = (rowStride - pixelStride * metrics.widthPixels) / pixelStride

                // Create the bitmap
                val bitmap = Bitmap.createBitmap(metrics.widthPixels + rowPadding, metrics.heightPixels, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(plane.buffer)

                // Finish up
                image.close()
                projection.stop()
                display.release()
                it.setOnImageAvailableListener(null, null)
                if (useEditor) {
                    val uri = saveBitmap(bitmap)
                    editBitmap(uri)
                } else
                    returnImage(bitmap)
            }, null)
        }

        private fun screenshotByRoot() {
            Thread(Runnable {
                try {
                    // Create new file
                    val screenshotFile = File(applicationContext.cacheDir, "screenshot.png")

                    // Get the security context of the 'files' directory.
                    var contextCommand = ""
                    val files = Shell.SH.run("ls -laZ " + applicationContext.cacheDir)
                    if (files.size > 0) {
                        var line = arrayOf<String>()
                        for (candidate in files) {
                            if (!candidate.toLowerCase().contains("total") && !candidate.toLowerCase().contains("screenshot")) {
                                line = candidate.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                                break
                            }
                        }

                        for (candidate in line) {
                            if (candidate.toLowerCase().contains("object") || candidate.toLowerCase().contains("u:")) {
                                contextCommand = "chcon " + candidate + " " + screenshotFile.path
                                break
                            }
                        }
                    }

                    // Take a screenshot
                    Thread.sleep(delay)
                    Shell.SU.run(arrayOf("screencap -p " + screenshotFile.path, "chmod 777 " + screenshotFile.path, contextCommand))

                    val stream = FileInputStream(screenshotFile)
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        if (useEditor) {
                            val uri = saveBitmap(bitmap)
                            editBitmap(uri)
                        } else
                            returnImage(bitmap)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    returnCancel()
                }
            }).start()
        }

        private fun saveBitmap(bitmap: Bitmap): Uri {
            val tmpFile = File.createTempFile("screen_shooter", null, applicationContext.cacheDir)
            val stream = FileOutputStream(tmpFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()
            return Uri.fromFile(tmpFile)
        }

        private fun editBitmap(bitmapUri: Uri) {
            val intent = Intent(applicationContext, ImageCropperActivity::class.java)
            intent.putExtra(EXTRA_REFERENCE_ID, referenceId)
            intent.putExtra(EXTRA_RETURN_BITMAP, returnBitmap)
            intent.putExtra(EXTRA_IMAGE_URI, bitmapUri.toString())
            startActivity(intent)
        }

        private fun returnImage(bitmap: Bitmap) {
            val listener = ReferenceCenter.getReference(referenceId) as OnScreenshotReadyListener? ?: return
            val uri = if (!returnBitmap) saveBitmap(bitmap) else null
            post {
                if (uri != null)
                    listener.onScreenshotReady(null, uri)
                else
                    listener.onScreenshotReady(bitmap)
            }
        }

        private fun returnImage(bitmapUri: Uri?) {
            if (bitmapUri != null) {
                // Bitmap was edited successfully
                if (returnBitmap) {
                    thread {
                        try {
                            val stream = contentResolver.openInputStream(bitmapUri)
                            val bitmap = BitmapFactory.decodeStream(stream)
                            stream.close()
                            post { returnImage(bitmap) }
                        } catch (exception: Exception) {
                            exception.printStackTrace()
                            post { returnCancel() }
                        }
                    }
                } else {
                    val listener = ReferenceCenter.getReference(referenceId) as OnScreenshotReadyListener?
                    listener?.onScreenshotReady(null, bitmapUri)
                }

            } else
                returnCancel()
        }

        private fun returnCancel() {
            val listener = ReferenceCenter.getReference(referenceId) as OnScreenshotReadyListener? ?: return
            post { listener.onScreenshotCanceled() }
        }
    }

    companion object {
        /**
         * Determines whether it is possible to take a screenshot using this class. At least one requirement must
         * be meet. The requirements are as follows:
         * * at lease Android Lollipop (API 21)
         * * root access
         */
        fun canTakeScreenshot(): Boolean = API.lollipop() or Shell.SU.available()

        private fun dismissNotification(context: Context) {
            val manager = NotificationManagerCompat.from(context)
            manager.cancel(NOTIFICATION_ID)
        }

        /** Code for media projection request generated in this class. */
        const val REQUEST_MEDIA_PROJECTION = 10000
        /** ID of a notification with screenshot request. */
        const val NOTIFICATION_ID = 1100

        const val EXTRA_REFERENCE_ID = "ScreenShooter.EXTRA_REFERENCE_ID"
        const val EXTRA_RETURN_BITMAP = "ScreenShooter.EXTRA_RETURN_BITMAP"
        const val EXTRA_IMAGE_URI = "ScreenShooter.EXTRA_IMAGE_URI"
        private const val EXTRA_USE_EDITOR = "ScreenShooter.EXTRA_USER_EDITOR"
        private const val EXTRA_DATA = "ScreenShooter.EXTRA_DATA"
        private const val EXTRA_CODE = "ScreenShooter.EXTRA_CODE"
        private const val EXTRA_DELAY = "ScreenShooter.EXTRA_DELAY"

        const val ACTION_RETURN_IMAGE = "ScreenShooter.ACTION_RETURN_IMAGE"
        const val ACTION_SCREENSHOT_CANCEL = "ScreenShooter.ACTION_SCREENSHOT_CANCEL"
        private const val ACTION_SCREENSHOT_ROOT = "ScreenShooter.ACTION_SCREENSHOT_ROOT"
        private const val ACTION_SCREENSHOT_MEDIA_PROJECTION = "ScreenShooter.ACTION_SCREENSHOT_MEDIA_PROJECTION"
    }
}