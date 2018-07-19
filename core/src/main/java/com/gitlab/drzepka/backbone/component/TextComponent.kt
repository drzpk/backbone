package com.gitlab.drzepka.backbone.component

import android.content.Context
import android.support.annotation.StringRes

/**
 * Unifies Android strings: can take either string id or string itself and returns only string.
 */
class TextComponent {
    private val stringRes: Int?
    private val string: String?

    constructor(@StringRes stringRes: Int) {
        this.stringRes = stringRes
        this.string = null
    }

    constructor(string: String) {
        this.stringRes = null
        this.string = string
    }

    fun getText(context: Context): String {
        return string ?: context.getString(stringRes!!)
    }
}