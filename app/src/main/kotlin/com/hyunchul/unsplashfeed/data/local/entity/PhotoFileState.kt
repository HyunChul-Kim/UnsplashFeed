package com.hyunchul.unsplashfeed.data.local.entity

import androidx.room.TypeConverter

enum class PhotoFileState {
    PENDING,
    READY,
    FAILED,
}

class PhotoFileStateConverters {
    @TypeConverter
    fun encode(value: PhotoFileState): String = value.name

    @TypeConverter
    fun decode(value: String): PhotoFileState = PhotoFileState.valueOf(value)
}