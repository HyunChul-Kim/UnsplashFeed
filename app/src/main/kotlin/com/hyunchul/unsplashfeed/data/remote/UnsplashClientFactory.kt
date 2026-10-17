package com.hyunchul.unsplashfeed.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun createUnsplashClient(
    accessKey: String,
    engine: HttpClientEngine = OkHttp.create(),
): HttpClient {
    require(accessKey.isNotBlank())

    return HttpClient(engine) {
        expectSuccess = true

        defaultRequest {
            url("https://api.unsplash.com/")
            header(
                "Accept-Version",
                "v1"
            )
            header(
                HttpHeaders.Authorization,
                "Client-ID $accessKey",
            )
        }

        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                }
            )
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 15_000
        }
    }
}