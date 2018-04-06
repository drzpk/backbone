@file:Suppress("unused")

package com.gitlab.drzepka.backbone.permission

import android.support.annotation.StringRes

/**
 * Permission bundle is a class containg all information that is needed for permission management. It's used
 * in a [PermissionManager].
 *
 * **Note:** don't forget to define all needed permissions in the Android manifest.
 */
class PermissionBundle {

    val rationale: Int?
    val rationaleStr: String?

    val rationaleTitle: Int?
    val rationaleTitleStr: String?

    val permissions: Array<out String>

    /** Set whether this bundle should display a rationale after permissions were denied. */
    var shouldDisplayRationale = true

    /**
     * Creates a new permission bundle.
     * @param [rationale] A message that will be shown when a permission is rejected.
     * @param [rationaleTitle] Custom title for this rationale. Overrides a title defined in PermissionManager.
     * @param [permissions] A list of permissions requested in this bundle.
     */
    constructor(@StringRes rationale: Int, @StringRes rationaleTitle: Int? = null, vararg permissions: String) {
        this.rationale = rationale
        this.rationaleStr = null
        this.rationaleTitle = rationaleTitle
        this.rationaleTitleStr = null
        this.permissions = permissions
    }

    /**
     * Creates a new permission bundle.
     * @param [rationale] A message that will be shown when a permission is rejected.
     * @param [rationaleTitle] Custom title for this rationale. Overrides a title defined in PermissionManager.
     * @param [permissions] A list of permissions requested in this bundle.
     */
    constructor(rationale: String, rationaleTitle: String, vararg permissions: String) {
        this.rationale = null
        this.rationaleStr = rationale
        this.rationaleTitle = null
        this.rationaleTitleStr = rationaleTitle
        this.permissions = permissions
    }
}