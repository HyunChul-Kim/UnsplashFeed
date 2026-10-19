package com.hyunchul.unsplashfeed.data.remote.download

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout

fun createPhotoDownloadClient(
    engine: HttpClientEngine? = null,
): HttpClient {
    val configure: HttpClientConfig<*>.() -> Unit = {
        expectSuccess = false
        followRedirects = false

        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 30_000
        }
    }

    return if(engine == null) {
        HttpClient(OkHttp, configure)
    } else {
        HttpClient(engine, configure)
    }
}