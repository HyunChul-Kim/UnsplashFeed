package com.hyunchul.unsplashfeed.data.repository

import com.hyunchul.unsplashfeed.data.remote.UnsplashApi
import com.hyunchul.unsplashfeed.data.remote.mapper.toPhoto
import com.hyunchul.unsplashfeed.domain.error.PhotoLoadException
import com.hyunchul.unsplashfeed.domain.model.Photo
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException

class PhotoRepository(
    private val api: UnsplashApi,
) {

    suspend fun getPhotos(
        page: Int = 1,
        perPage: Int = 20,
    ): List<Photo> {
        require(page > 0)
        require(perPage in 1..30)

        return try {
            api.getPhotos(
                page = page,
                perPage = perPage,
            ).map { it.toPhoto() }
        } catch(e: CancellationException) {
            throw e
        } catch(e: ResponseException) {
            throw when(e.response.status) {
                HttpStatusCode.Unauthorized -> PhotoLoadException.Unauthorized(e)
                HttpStatusCode.TooManyRequests -> PhotoLoadException.RateLimited(e)
                else -> {
                    PhotoLoadException.HttpError(
                        statusCode = e.response.status.value,
                        cause = e,
                    )
                }
            }
        } catch(e: IOException) {
            throw PhotoLoadException.Network(e)
        } catch(e: Exception) {
            throw PhotoLoadException.Unexpected(e)
        }
    }
}
