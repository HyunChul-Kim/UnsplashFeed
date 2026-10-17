package com.hyunchul.unsplashfeed.data.remote

import com.hyunchul.unsplashfeed.data.remote.dto.UnsplashPhotoDto

data class UnsplashPhotoPageResponse(
    val photos: List<UnsplashPhotoDto>,
    val hasNextPage: Boolean,
)