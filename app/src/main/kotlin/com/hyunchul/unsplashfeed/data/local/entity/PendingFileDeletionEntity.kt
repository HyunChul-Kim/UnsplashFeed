package com.hyunchul.unsplashfeed.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_file_deletions")
data class PendingFileDeletionEntity(
    @PrimaryKey
    val relativePath: String,
)