package com.hyunchul.unsplashfeed.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hyunchul.unsplashfeed.data.local.dao.FavoritePhotoDao
import com.hyunchul.unsplashfeed.data.local.entity.FavoritePhotoEntity
import com.hyunchul.unsplashfeed.data.local.entity.PendingFileDeletionEntity
import com.hyunchul.unsplashfeed.data.local.entity.PhotoFileEntity
import com.hyunchul.unsplashfeed.data.local.entity.PhotoFileStateConverters

@Database(
    entities = [
        FavoritePhotoEntity::class,
        PhotoFileEntity::class,
        PendingFileDeletionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(PhotoFileStateConverters::class)
abstract class UnsplashDatabase: RoomDatabase() {

    abstract fun favoritePhotoDao(): FavoritePhotoDao
}