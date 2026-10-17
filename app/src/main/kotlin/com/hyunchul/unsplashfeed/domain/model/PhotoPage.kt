package com.hyunchul.unsplashfeed.domain.model

data class PhotoPage(
    val photos: List<Photo>,
    val hasNextPage: Boolean,
)