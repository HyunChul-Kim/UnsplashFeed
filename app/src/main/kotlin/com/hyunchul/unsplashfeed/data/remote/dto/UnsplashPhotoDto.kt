package com.hyunchul.unsplashfeed.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UnsplashPhotoDto(
    val id: String,
    val width: Int,
    val height: Int,
    val description: String? = null,
    @SerialName("alt_description")
    val altDescription: String? = null,
    val user: UnsplashUserDto,
    val urls: UnsplashPhotoUrlsDto,
    val links: UnsplashPhotoLinksDto,
)

@Serializable
data class UnsplashUserDto(
    val name: String,
    val username: String,
    @SerialName("profile_image")
    val profileImage: UnsplashUserProfileImageDto,
)

@Serializable
data class UnsplashUserProfileImageDto(
    val small: String,
    val medium: String,
    val large: String,
)

@Serializable
data class UnsplashPhotoUrlsDto(
    val thumb: String,
    val small: String,
    val regular: String,
    val full: String,
    val raw: String,
)

@Serializable
data class UnsplashPhotoLinksDto(
    @SerialName("download_location")
    val downloadLocation: String,
)
