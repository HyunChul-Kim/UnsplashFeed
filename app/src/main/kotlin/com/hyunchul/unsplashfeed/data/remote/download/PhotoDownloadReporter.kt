package com.hyunchul.unsplashfeed.data.remote.download

import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.retry
import io.ktor.client.request.get
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.isSuccess
import java.io.IOException
import javax.inject.Inject

class PhotoDownloadReporter @Inject constructor(
    private val client: HttpClient,
) {

    suspend fun report(downloadLocation: String) {
        val url = Url(downloadLocation)
        require(
            url.protocol == URLProtocol.HTTPS &&
            url.host.equals("api.unsplash.com", ignoreCase = true)
        ) {
            "HTTPS Unsplash API download location is required."
        }

        val response = client.get(url) {
            expectSuccess = false

            retry { maxRetries = 0 }
        }

        if(!response.status.isSuccess()) {
            throw PhotoDownloadReportHttpException(response.status.value)
        }
    }
}

class PhotoDownloadReportHttpException(
    val statusCode: Int,
): IOException("Download reporting failed with HTTP $statusCode.")