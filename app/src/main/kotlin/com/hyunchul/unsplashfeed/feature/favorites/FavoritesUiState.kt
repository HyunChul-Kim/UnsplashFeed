package com.hyunchul.unsplashfeed.feature.favorites

import com.hyunchul.unsplashfeed.data.local.entity.PhotoFileState
import java.io.File

data class FavoritePhotoUiItem(
    val photoId: String,
    val requestId: String,
    val photographerName: String,
    val imageDescription: String?,
    val fileState: PhotoFileState,
    val localImage: File?,
) {
    val canDisplayLocalImage: Boolean
        get() = fileState == PhotoFileState.READY && localImage != null
}

data class FavoritesUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val showOnlyCanDisplayLocalImage: Boolean = true,
    val items: List<FavoritePhotoUiItem> = emptyList(),
)