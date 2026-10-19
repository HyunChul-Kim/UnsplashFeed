package com.hyunchul.unsplashfeed.feature.feed

import com.hyunchul.unsplashfeed.data.local.entity.PhotoFileState

data class FeedFavoriteItem(
    val requestId: String,
    val fileState: PhotoFileState,
)

data class FeedFavoriteUiState(
    val loaded: Boolean = false,
    val items: Map<String, FeedFavoriteItem> = emptyMap(),
)