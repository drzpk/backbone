package com.gitlab.drzepka.backbone.payload

import android.content.Context
import android.support.annotation.DrawableRes
import com.gitlab.drzepka.backbone.component.BitmapComponent
import com.gitlab.drzepka.backbone.component.TextComponent

/**
 * Contains data that will be used to build notification, when it needs to be built by external tools.
 */
class NotificationPayload(
        context: Context,
        /** Title of the notification. */
        val title: TextComponent,
        /** Content of the notification. */
        val content: TextComponent,
        /** Small icon of the notification. */
        @DrawableRes val icon: Int = 0,
        /** Channel ID of the notification. Required on Android 8 or higher. Ignored on lower APIs. */
        val channel: String,
        /** Text of the positive button that will appear below notification. */
        val positiveActionText: TextComponent? = null,
        /** Text of the neutral button that will appear below notification. */
        val neutralActionText: TextComponent? = null,
        /** Text of the negative button that will appear below notification. */
        val negativeActionText: TextComponent? = null,
        /** Large icon that will be shown with notification. */
        val largeIcon: BitmapComponent? = null
) : BasePayload(context)