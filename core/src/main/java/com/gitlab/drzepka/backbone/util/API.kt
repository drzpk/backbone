@file:Suppress("unused")

package com.gitlab.drzepka.backbone.util

import android.os.Build

/**
 * Checks if current API level is greater or equal.
 */
object API {
    /** Is API level >= 17. */
    fun jellybeanMR1(): Boolean = check(Build.VERSION_CODES.JELLY_BEAN_MR1)

    /** Is API level >= 18. */
    fun jellybeanMR2(): Boolean = check(Build.VERSION_CODES.JELLY_BEAN_MR2)

    /** Is API level >= 19. */
    fun kitkat(): Boolean = check(Build.VERSION_CODES.KITKAT)

    /** Is API level >= 21. */
    fun lollipop(): Boolean = check(Build.VERSION_CODES.LOLLIPOP)

    /** Is API level >= 22. */
    fun lollipopMR2(): Boolean = check(Build.VERSION_CODES.LOLLIPOP_MR1)

    /** Is API level >= 23. */
    fun marshmallow(): Boolean = check(Build.VERSION_CODES.M)

    /** Is API level >= 24. */
    fun nougat(): Boolean = check(Build.VERSION_CODES.N)

    /** Is API level >= 25. */
    fun nougatMR1(): Boolean = check(Build.VERSION_CODES.N_MR1)

    /** Is API level >= 26. */
    fun oreo(): Boolean = check(Build.VERSION_CODES.O)

    private fun check(api: Int): Boolean = Build.VERSION.SDK_INT >= api
}