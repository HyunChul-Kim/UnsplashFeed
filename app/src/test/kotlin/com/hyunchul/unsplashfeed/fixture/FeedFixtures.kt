package com.hyunchul.unsplashfeed.fixture

import com.hyunchul.unsplashfeed.domain.model.Photo
import com.hyunchul.unsplashfeed.domain.model.PhotoPage

object FeedFixtures {

    fun photo(id: String) = Photo(
        id = id,
        description = null,
        altDescription = null,
        width = 500,
        height = 400,
        thumbnailUrl = "https://example.com/$id/small",
        regularUrl = "https://example.com/$id/regular",
        fullUrl = "https://example.com/$id/full",
        rawUrl = "https://example.com/$id/raw",
        photographerName = "Test User",
        photographerProfileUrl = "https://example.com/user",
        downloadLocation = "https://example.com/$id/download",
    )

    fun itemsWithDuplicates(): List<PhotoPage> {
        val pageSize = 20
        val totalCount = 400
        val duplicateCount = 21
        val duplicateInterval = 18
        val duplicateSourceOffset = 7

        val photos = MutableList(totalCount - duplicateCount) { index ->
            photo("p${index.toString().padStart(10, '0')}")
        }

        repeat(duplicateCount) { index ->
            val insertIndex = pageSize + index * duplicateInterval
            photos.add(insertIndex, photos[insertIndex - duplicateSourceOffset])
        }

        val pages = photos.chunked(pageSize)

        return pages.mapIndexed { index, photos ->
            PhotoPage(
                photos = photos,
                hasNextPage = index < pages.lastIndex
            )
        }
    }
}