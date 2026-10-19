package com.hyunchul.unsplashfeed.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photo_files",
    foreignKeys = [
        ForeignKey(
            entity = FavoritePhotoEntity::class,
            parentColumns = ["photoId"],
            childColumns = ["photoId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["requestId"], unique = true),
        Index(value = ["localImageRelativePath"], unique = true),
    ],
)
data class PhotoFileEntity(
    @PrimaryKey
    val photoId: String,
    val requestId: String,
    val imageUrl: String?,
    val state: PhotoFileState,
    val localImageRelativePath: String? = null,
    val downloadLocation: String? = null,
    val failureCode: String? = null,
)