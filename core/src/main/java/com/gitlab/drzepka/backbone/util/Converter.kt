@file:Suppress("unused", "UNCHECKED_CAST")

package com.gitlab.drzepka.backbone.util

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager

/**
 * Converts between various units.
 */
object Converter {

    private var metrics: DisplayMetrics? = null

    /**
     * Converts DP -> PX.
     */
    fun <T : Number> dpToPx(context: Context, dp: T): T {
        init(context)
        return (dp.toFloat() * (metrics!!.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)) as T
    }

    /**
     * Converts PX -> DP.
     */
    fun <T : Number> pxToDp(context: Context, px: T): T {
        init(context)
        return (px.toFloat() * (DisplayMetrics.DENSITY_DEFAULT.toFloat() / metrics!!.densityDpi)) as T
    }

    private fun init(context: Context) {
        if (metrics != null) return

        metrics = DisplayMetrics()
        val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        manager.defaultDisplay.getMetrics(metrics)
    }
}