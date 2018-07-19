package com.gitlab.drzepka.backbone.component

import android.app.Activity
import android.app.Fragment
import android.content.Context
import android.content.Intent

class ActivityComponent {
    val activity: Activity?
    val fragment: Fragment?

    val context: Context?
    get() {
        return activity ?: fragment?.activity
    }

    constructor(activity: Activity) {
        this.activity = activity
        this.fragment = null
    }

    constructor(fragment: Fragment) {
        this.activity = null
        this.fragment = fragment
    }

    fun startActivityForResult(intent: Intent, requestCode: Int) {
        if (activity != null)
            activity.startActivityForResult(intent, requestCode)
        else
            fragment!!.startActivityForResult(intent, requestCode)
    }
}