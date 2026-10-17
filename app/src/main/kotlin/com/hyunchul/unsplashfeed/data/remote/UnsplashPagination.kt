package com.hyunchul.unsplashfeed.data.remote

internal fun resolveHasNextPage(
    linkHeader: String?,
    receivedCount: Int,
    requestedPerPage: Int,
): Boolean {
    if(linkHeader.isNullOrBlank()) {
        return receivedCount >= requestedPerPage
    }

    return linkHeader.split(',').any { link ->
        link.substringAfter('>', "")
            .split(';')
            .any { parameter ->
                val pair = parameter.split('=', limit = 2)

                pair.size == 2 &&
                        pair[0].trim().equals("rel", true) &&
                        pair[1].trim('"').equals("next", true)
            }
    }
}