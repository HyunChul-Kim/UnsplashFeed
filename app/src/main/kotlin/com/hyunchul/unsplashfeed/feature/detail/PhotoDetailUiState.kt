package com.hyunchul.unsplashfeed.feature.detail

import com.hyunchul.unsplashfeed.data.local.entity.PhotoFileState
import com.hyunchul.unsplashfeed.domain.model.Photo

sealed interface PhotoDetailContent {
    data object Loading: PhotoDetailContent
    data class Success(val photo: Photo): PhotoDetailContent
    data class Error(val failure: Exception): PhotoDetailContent
}

data class PhotoDetailFavoriteState(
    val loaded: Boolean = false,
    val loadFailed: Boolean = false,
    val isFavorite: Boolean = false,
    val requestId: String? = null,
    val fileState: PhotoFileState? = null,
)

data class PhotoDetailUiState(
    val content: PhotoDetailContent = PhotoDetailContent.Loading,
    val favorite: PhotoDetailFavoriteState = PhotoDetailFavoriteState(),
)