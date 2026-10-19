package com.hyunchul.unsplashfeed.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.hyunchul.unsplashfeed.data.local.entity.FavoritePhotoEntity
import com.hyunchul.unsplashfeed.data.local.entity.PendingFileDeletionEntity
import com.hyunchul.unsplashfeed.data.local.entity.PhotoFileEntity
import com.hyunchul.unsplashfeed.data.local.entity.PhotoFileState
import com.hyunchul.unsplashfeed.data.local.model.FavoritePhotoWithFile
import kotlinx.coroutines.flow.Flow

@Dao
abstract class FavoritePhotoDao {

    @Query("""
        SELECT *
        FROM favorite_photos
        ORDER BY createdAt DESC, photoId ASC
    """)
    abstract fun observeAll(): Flow<List<FavoritePhotoEntity>>

    @Query("""
        SELECT photoId
        FROM favorite_photos
        ORDER BY photoId ASC
    """)
    abstract fun observePhotoIds(): Flow<List<String>>

    @Transaction
    @Query("""
       SELECT * 
       FROM favorite_photos
       ORDER BY createdAt DESC, photoId ASC
    """)
    abstract fun observeWithFiles(): Flow<List<FavoritePhotoWithFile>>

    @Transaction
    @Query("""
        SELECT f.* FROM favorite_photos AS f
        INNER JOIN photo_files AS p ON p.photoId = f.photoId
        WHERE p.state = 'READY' AND p.localImageRelativePath IS NOT NULL
        ORDER BY f.createdAt DESC, f.photoId ASC
    """)
    abstract fun observeReadyPhotos(): Flow<List<FavoritePhotoWithFile>>

    @Query("""
        SELECT *
        FROM favorite_photos
        WHERE photoId = :photoId
    """)
    abstract suspend fun findById(
        photoId: String,
    ): FavoritePhotoEntity?

    @Query("""
        SELECT *
        FROM photo_files
        WHERE photoId = :photoId
    """)
    abstract suspend fun findFile(
        photoId: String,
    ): PhotoFileEntity?

    @Query("""
        SELECT *
        FROM photo_files
        WHERE state = 'PENDING'
        ORDER BY photoId ASC
    """)
    abstract suspend fun pendingDownloads(): List<PhotoFileEntity>

    @Query("""
        SELECT *
        FROM pending_file_deletions
        ORDER BY relativePath ASC
    """)
    abstract suspend fun pendingFileDeletions(): List<PendingFileDeletionEntity>

    @Query("""
        SELECT EXISTS(
            SELECT 1
            FROM photo_files
            WHERE localImageRelativePath = :relativePath
        )
    """)
    abstract suspend fun isReferenced(
        relativePath: String,
    ): Boolean

    @Query("""
        DELETE FROM pending_file_deletions
        WHERE relativePath = :relativePath
    """)
    abstract suspend fun acknowledgeFileDeleted(
        relativePath: String,
    ): Int

    @Query("""
        SELECT EXISTS(
            SELECT 1
            FROM pending_file_deletions
            WHERE relativePath = :relativePath
        )
    """)
    protected abstract suspend fun isQueuedForDeletion(
        relativePath: String,
    ): Boolean

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertFavorite(
        favorite: FavoritePhotoEntity,
    )

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertFile(
        file: PhotoFileEntity,
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertDeletion(
        file: PendingFileDeletionEntity,
    )

    @Query("""
        DELETE FROM favorite_photos
        WHERE photoId = :photoId
    """)
    protected abstract suspend fun deleteFavoriteRow(
        photoId: String
    ): Int

    @Query("""
        UPDATE photo_files
        SET state = 'READY', localImageRelativePath = :newPath, failureCode = NULL
        WHERE photoId = :photoId AND requestId = :requestId AND state = 'PENDING'
    """)
    protected abstract suspend fun markReady(
        photoId: String,
        requestId: String,
        newPath: String,
    ): Int

    @Query("""
        UPDATE photo_files
        SET state = 'FAILED', failureCode = :failureCode
        WHERE photoId = :photoId AND requestId = :requestId AND state = 'PENDING'
    """)
    abstract suspend fun markFailed(
        photoId: String,
        requestId: String,
        failureCode: String,
    ): Int

    @Query("""
        UPDATE photo_files
        SET requestId = :newRequestId, state = 'PENDING', localImageRelativePath = NULL, failureCode = NULL
        WHERE photoId = :photoId AND requestId = :expectedRequestId AND state = 'FAILED'
    """)
    protected abstract suspend fun resetFailed(
        photoId: String,
        expectedRequestId: String,
        newRequestId: String,
    ): Int

    @Transaction
    open suspend fun addFavorite(
        favorite: FavoritePhotoEntity,
        imageUrl: String,
        newRequestId: String,
        downloadLocation: String? = null,
    ): PhotoFileEntity {
        require(favorite.photoId.isNotBlank())
        findById(favorite.photoId)?.let {
            return checkNotNull(findFile(favorite.photoId))
        }

        require(imageUrl.isNotBlank())
        require(newRequestId.isNotBlank())

        insertFavorite(favorite)

        val file = PhotoFileEntity(
            photoId = favorite.photoId,
            requestId = newRequestId,
            imageUrl = imageUrl,
            state = PhotoFileState.PENDING,
            downloadLocation = downloadLocation,
        )
        insertFile(file)

        return file
    }

    @Transaction
    open suspend fun removeFavorite(
        photoId: String,
    ): PhotoFileEntity? {
        val oldFile = findFile(photoId)
        oldFile?.localImageRelativePath?.let { insertDeletion(PendingFileDeletionEntity(it)) }
        deleteFavoriteRow(photoId)
        return oldFile
    }

    @Transaction
    open suspend fun toggleFavorite(
        favorite: FavoritePhotoEntity,
        imageUrl: String,
        newRequestId: String,
        downloadLocation: String? = null,
    ): FavoriteToggleResult {
        return if(findById(favorite.photoId) == null) {
            FavoriteToggleResult(
                isFavorite = true,
                requestToEnqueue = addFavorite(favorite, imageUrl, newRequestId, downloadLocation),
            )
        } else {
            FavoriteToggleResult(
                isFavorite = false,
                requestIdToCancel = removeFavorite(favorite.photoId)?.requestId,
            )
        }
    }

    @Transaction
    open suspend fun publishDownloadedFile(
        photoId: String,
        requestId: String,
        newPath: String,
    ): Boolean {
        require(newPath.isNotBlank())

        val current = findFile(photoId)

        if(
            current?.requestId == requestId &&
            current.state == PhotoFileState.READY &&
            current.localImageRelativePath == newPath
        ) {
            return true
        }

        if(
            current?.requestId == requestId &&
            current.state == PhotoFileState.PENDING &&
            !isQueuedForDeletion(newPath)
        ) {
            check(!isReferenced(newPath))
            if(markReady(photoId, requestId, newPath) == 1) {
                return true
            }
        }

        queueUnreferencedFile(newPath)
        return false
    }

    @Transaction
    open suspend fun retryFailed(
        photoId: String,
        expectedRequestId: String,
        newRequestId: String,
    ): PhotoFileEntity? {
        require(newRequestId.isNotBlank() && newRequestId != expectedRequestId)

        val current = findFile(photoId) ?: return null

        if(current.requestId != expectedRequestId || current.state != PhotoFileState.FAILED) {
            return null
        }
        check(!current.imageUrl.isNullOrBlank())
        if(resetFailed(photoId, expectedRequestId, newRequestId) != 1) {
            return null
        }

        return findFile(photoId)
    }

    @Transaction
    open suspend fun queueUnreferencedFile(
        relativePath: String,
    ) {
        require(relativePath.isNotBlank())
        if(!isReferenced(relativePath)) {
            insertDeletion(PendingFileDeletionEntity(relativePath))
        }
    }
}

data class FavoriteToggleResult(
    val isFavorite: Boolean,
    val requestToEnqueue: PhotoFileEntity? = null,
    val requestIdToCancel: String? = null,
)