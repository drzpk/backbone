package com.gitlab.drzepka.backbone.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.annotation.DrawableRes

class BitmapComponent {
    private val bitmapRes: Int?
    private val bitmap: Bitmap?

    constructor(@DrawableRes bitmapRes: Int) {
        this.bitmapRes = bitmapRes
        this.bitmap = null
    }

    constructor(bitmap: Bitmap) {
        this.bitmapRes = null
        this.bitmap = bitmap
    }

    fun getBitmap(context: Context): Bitmap {
        return bitmap ?: BitmapFactory.decodeResource(context.resources, bitmapRes!!)
    }
}