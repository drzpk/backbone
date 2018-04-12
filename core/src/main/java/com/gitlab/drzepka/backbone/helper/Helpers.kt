@file:Suppress("unused")

package com.gitlab.drzepka.backbone.helper

import android.os.Handler
import android.os.Looper

/**
 * Runs given runnable (or lambda expression) in the Android UI thread.
 */
fun post(runnable: () -> Unit) {
    Handler(Looper.getMainLooper()).post(runnable)
}