package com.hyunchul.unsplashfeed.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.hyunchul.unsplashfeed.domain.model.FeedPhotoItem
import com.hyunchul.unsplashfeed.domain.model.PhotoPage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class PhotoPagingSource(
    private val loadPage: suspend (page: Int, perPage: Int) -> PhotoPage,
): PagingSource<Int, FeedPhotoItem>() {

    override suspend fun load(
        params: LoadParams<Int>,
    ): LoadResult<Int, FeedPhotoItem> {
        return try {
            currentCoroutineContext().ensureActive()
            if(invalid) return LoadResult.Invalid()
            check(params !is LoadParams.Prepend) {
                "Forward paging only."
            }

            val page = params.key ?: 1
            require(page > 0)

            val response = loadPage(page, PAGE_SIZE)
            val items = response.photos.mapIndexed { indexInPage, photo ->
                FeedPhotoItem(
                    entryKey = "feed:$page:$indexInPage:${photo.id}",
                    photo = photo,
                )
            }

            val nextKey = if(response.photos.isNotEmpty() && response.hasNextPage) {
                check(page < Int.MAX_VALUE) {
                    "Page key overflow."
                }
                page + 1
            } else {
                null
            }

            currentCoroutineContext().ensureActive()
            if(invalid) return LoadResult.Invalid()

            LoadResult.Page(
                data = items,
                prevKey = null,
                nextKey = nextKey,
            )
        } catch(e: CancellationException) {
            throw e
        } catch(e: Exception) {
            currentCoroutineContext().ensureActive()
            if(invalid) LoadResult.Invalid() else LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, FeedPhotoItem>): Int? = null

    companion object {
        const val PAGE_SIZE = 20
    }
}