package com.hyunchul.unsplashfeed.feature.detail

import androidx.core.net.toUri

fun detailImageUrl(
    rawUrl: String,
    displayWidthPx: Int,
    originalWidth: Int?,
): String {
    val requestedWidth = displayWidthPx.coerceAtLeast(1)
    val width = originalWidth
        ?.takeIf { it > 0 }
        ?.let { minOf(it, requestedWidth) }
        ?: requestedWidth

    return rawUrl.toUri()
        .buildUpon()
        .appendQueryParameter("w", width.toString())
        .appendQueryParameter("fit", "max")
        .appendQueryParameter("fm", "jpg")
        .appendQueryParameter("q", "80")
        .build()
        .toString()
}