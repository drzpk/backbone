@file:Suppress("unused")

package com.gitlab.drzepka.backbone.helper

import android.content.Context
import android.graphics.PorterDuff
import android.support.annotation.IdRes
import android.view.Menu
import android.view.View
import com.gitlab.drzepka.backbone.util.API

/**
 * Shows a view.
 */
fun View.show() {
    this.visibility = View.VISIBLE
}

/**
 * Hides a view.
 */
fun View.hide() {
    this.visibility = View.INVISIBLE
}

/**
 * Makes a view gone.
 */
fun View.gone() {
    this.visibility = View.GONE
}

/**
 * Sets tint to the menu item with given id.
 */
fun Menu.setTint(@IdRes id: Int, tint: Int) {
    val item = this.findItem(id)
    item.icon.setColorFilter(tint, PorterDuff.Mode.SRC_ATOP)
}

/**
 * Sets tint loaded from resources to the menu item with given id.
 */
@Suppress("DEPRECATION")
fun Menu.setTint(@IdRes id: Int, tintRes: Int, context: Context) {
    val color = if (API.marshmallow())
        context.resources.getColor(tintRes, context.theme)
    else
        context.resources.getColor(tintRes)
    this.setTint(id, color)
}