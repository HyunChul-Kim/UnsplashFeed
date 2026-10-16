package com.hyunchul.unsplashfeed.domain.model

data class Photo(
    val id: String,
    val description: String?,
    val altDescription: String?,
    val width: Int,
    val height: Int,
    val thumbnailUrl: String,
    val regularUrl: String,
    val fullUrl: String,
    val photographerName: String,
    val photographerProfileUrl: String,
    val downloadLocation: String,
)