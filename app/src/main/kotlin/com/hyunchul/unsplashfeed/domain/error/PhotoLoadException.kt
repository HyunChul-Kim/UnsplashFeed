package com.hyunchul.unsplashfeed.domain.error

sealed class PhotoLoadException(
    message: String,
    cause: Throwable,
): Exception(message, cause) {

    class Unauthorized(cause: Throwable): PhotoLoadException("Unauthorized", cause)

    class RateLimited(cause: Throwable): PhotoLoadException("Rate limit exceeded", cause)

    class Network(cause: Throwable): PhotoLoadException("Network error", cause)

    class HttpError(
        val statusCode: Int,
        cause: Throwable,
    ): PhotoLoadException("HTTP $statusCode", cause)

    class Unexpected(cause: Throwable): PhotoLoadException("Unexpected error", cause)
}