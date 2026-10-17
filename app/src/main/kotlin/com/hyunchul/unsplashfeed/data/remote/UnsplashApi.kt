package com.hyunchul.unsplashfeed.data.remote

import com.hyunchul.unsplashfeed.data.remote.dto.UnsplashPhotoDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import javax.inject.Inject

class UnsplashApi @Inject constructor(
    private val client: HttpClient,
) {

    suspend fun getPhotos(
        page: Int = 1,
        perPage: Int = 20,
    ): UnsplashPhotoPageResponse {
        val response = client.get("photos") {
            parameter("page", page)
            parameter("per_page", perPage)
        }

        val photos = response.body<List<UnsplashPhotoDto>>()

        val linkHeader = response.headers
            .getAll(HttpHeaders.Link)
            ?.joinToString(",")

        return UnsplashPhotoPageResponse(
            photos = photos,
            hasNextPage = resolveHasNextPage(
                linkHeader = linkHeader,
                receivedCount = photos.size,
                requestedPerPage = perPage,
            )
        )
    }
}