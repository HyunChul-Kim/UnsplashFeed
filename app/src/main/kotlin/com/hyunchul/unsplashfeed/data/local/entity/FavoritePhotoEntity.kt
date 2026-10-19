package com.hyunchul.unsplashfeed.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_photos")
data class FavoritePhotoEntity(
    @PrimaryKey
    val photoId: String,
    val photographerName: String,
    val photographerProfileImageUrl: String? = null,
    val description: String?,
    val altDescription: String?,
    val createdAt: Long,
)