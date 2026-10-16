package com.hyunchul.unsplashfeed.data.remote

import com.hyunchul.unsplashfeed.data.remote.dto.UnsplashPhotoDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class UnsplashApi(
    private val client: HttpClient,
) {

    suspend fun getPhotos(
        page: Int = 1,
        perPage: Int = 20,
    ): List<UnsplashPhotoDto> {
        return client.get("photos") {
            parameter("page", page)
            parameter("per_page", perPage)
        }.body()
    }
}