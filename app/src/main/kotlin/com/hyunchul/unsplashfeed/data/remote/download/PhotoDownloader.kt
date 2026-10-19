package com.hyunchul.unsplashfeed.data.remote.download

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.util.Locale

class PhotoDownloader(
    private val client: HttpClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    suspend fun download(
        imageUrl: String,
        output: OutputStream,
    ) {
        val url = Url(imageUrl)
        require(url.protocol == URLProtocol.HTTPS && url.host.isNotBlank()) {
            "imageUrl must be absolute HTTPS URL."
        }

        withContext(ioDispatcher) {
            client.prepareGet(imageUrl) {
                header(HttpHeaders.Accept, "image/jpeg,image/png,image/webp")
            }.execute { response ->
                response.status.isSuccess()
                if(response.status != HttpStatusCode.OK) {
                    throw PhotoDownloadHttpException(response.status.value)
                }

                val contentType = response.contentType() ?: throw IOException("Unsupported or missing photo Content-Type.")
                if(SUPPORTED_CONTENT_TYPES.none { type -> contentType.match(type) }) {
                    throw IOException("Unsupported or missing photo Content-Type.")
                }

                val contentEncoding = response.headers[HttpHeaders.ContentEncoding]
                if(
                    !contentEncoding.isNullOrBlank() &&
                    !contentEncoding.trim().equals("identity", true)
                ) {
                    throw IOException("Encoded HTTP photo bodies are not supported.")
                }

                val declaredLength = response.headers[HttpHeaders.ContentLength]?.let { value ->
                    value.toLongOrNull()?.takeIf { it >= 0 } ?: throw IOException("Invalid photo Content-Length")
                }

                val channel = response.bodyAsChannel()
                val buffer = ByteArray(BUFFER_BYTES)
                var received = 0L

                while(true) {
                    currentCoroutineContext().ensureActive()
                    val count = channel.readAvailable(buffer)
                    if(count < 0) break
                    if(count == 0) continue

                    output.write(buffer, 0, count)
                    received += count
                }

                channel.closedCause?.let { throw it }
                currentCoroutineContext().ensureActive()
                if(received == 0L) {
                    throw IOException("Empty photo response")
                }
                if(declaredLength != null && received != declaredLength) {
                    throw IOException("Incomplete photo response.")
                }
            }
        }
    }

    companion object {
        private const val BUFFER_BYTES = 8 * 1024
        private val SUPPORTED_CONTENT_TYPES = listOf(
            ContentType.Image.JPEG,
            ContentType.Image.PNG,
            ContentType.Image.WEBP,
        )
    }
}

class PhotoDownloadHttpException(
    val statusCode: Int,
): IOException("Photo download failed with HTTP $statusCode")