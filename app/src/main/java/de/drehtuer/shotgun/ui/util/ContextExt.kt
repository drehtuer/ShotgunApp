package de.drehtuer.shotgun.ui.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Walks up the context chain to the hosting [Activity]. Compose hands out a
 * context that may be wrapped several times, so the cast alone is not safe.
 */
internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
