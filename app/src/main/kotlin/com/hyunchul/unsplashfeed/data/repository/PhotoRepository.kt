package com.hyunchul.unsplashfeed.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.hyunchul.unsplashfeed.data.paging.PhotoPagingSource
import com.hyunchul.unsplashfeed.data.remote.UnsplashApi
import com.hyunchul.unsplashfeed.data.remote.mapper.toPhoto
import com.hyunchul.unsplashfeed.domain.error.PhotoLoadException
import com.hyunchul.unsplashfeed.domain.model.FeedPhotoItem
import com.hyunchul.unsplashfeed.domain.model.Photo
import com.hyunchul.unsplashfeed.domain.model.PhotoPage
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.io.IOException
import javax.inject.Inject

class PhotoRepository @Inject constructor(
    private val api: UnsplashApi,
) {

    suspend fun getPhoto(
        photoId: String,
    ): Photo {
        require(photoId.isNotBlank())
        return withPhotoLoadErrors {
            api.getPhoto(photoId).toPhoto()
        }
    }

    suspend fun getPhotos(
        page: Int = 1,
        perPage: Int = 20,
    ): List<Photo> {
        return getPhotoPage(page, perPage).photos
    }

    suspend fun getPhotoPage(
        page: Int = 1,
        perPage: Int = 20,
    ): PhotoPage {
        require(page > 0)
        require(perPage in 1..30)

        return withPhotoLoadErrors {
            val response = api.getPhotos(
                page = page,
                perPage = perPage,
            )

            PhotoPage(
                photos = response.photos.map { it.toPhoto() },
                hasNextPage = response.hasNextPage,
            )
        }
    }

    fun getPagedPhotos(): Flow<PagingData<FeedPhotoItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = PhotoPagingSource.PAGE_SIZE,
                initialLoadSize = PhotoPagingSource.PAGE_SIZE,
                prefetchDistance = 5,
                enablePlaceholders = false,
                maxSize = PagingConfig.MAX_SIZE_UNBOUNDED,
            ),
            initialKey = 1,
            pagingSourceFactory = {
                PhotoPagingSource(
                    loadPage = ::getPhotoPage,
                )
            },
        ).flow
    }

    private suspend fun <T> withPhotoLoadErrors(
        block: suspend () -> T,
    ): T {
        return try {
            block()
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
