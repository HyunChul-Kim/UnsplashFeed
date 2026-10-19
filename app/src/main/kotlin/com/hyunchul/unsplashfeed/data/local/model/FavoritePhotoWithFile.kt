package com.hyunchul.unsplashfeed.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.hyunchul.unsplashfeed.data.local.entity.FavoritePhotoEntity
import com.hyunchul.unsplashfeed.data.local.entity.PhotoFileEntity

data class FavoritePhotoWithFile(
    @Embedded
    val favorite: FavoritePhotoEntity,
    @Relation(parentColumn = "photoId", entityColumn = "photoId")
    val file: PhotoFileEntity,
)