package com.hyunchul.unsplashfeed.data.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.workDataOf
import com.hyunchul.unsplashfeed.data.local.entity.PhotoFileEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteWorkScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val manager: WorkManager
        get() = WorkManager.getInstance(context)

    suspend fun enqueueDownload(
        request: PhotoFileEntity,
    ) {
        val inputData = workDataOf(
            KEY_PHOTO_ID to request.photoId,
            KEY_REQUEST_ID to request.requestId,
        )
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val reportWork = OneTimeWorkRequestBuilder<FavoriteDownloadReportWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(DOWNLOAD_TAG)
            .build()

        val downloadWork = OneTimeWorkRequestBuilder<FavoriteDownloadWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(DOWNLOAD_TAG)
            .build()

        manager.beginUniqueWork(
            uniqueWorkName = downloadName(request.requestId),
            existingWorkPolicy = ExistingWorkPolicy.KEEP,
            request = reportWork
        ).then(downloadWork).enqueue().await()
    }

    suspend fun enqueueCleanup() {
        val work = OneTimeWorkRequestBuilder<FavoriteFileCleanupWorker>()
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        manager.enqueueUniqueWork(
            uniqueWorkName = CLEANUP_NAME,
            existingWorkPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE,
            request = work,
        ).await()
    }

    suspend fun cancelDownload(
        requestId: String,
    ) {
        manager.cancelUniqueWork(downloadName(requestId)).await()
    }

    companion object {
        const val KEY_PHOTO_ID = "photo_id"
        const val KEY_REQUEST_ID = "request_id"
        const val DOWNLOAD_TAG = "favorite-download"
        private const val CLEANUP_NAME = "favorite-file-cleanup"
        private fun downloadName(requestId: String) = "favorite-download:$requestId"
    }
}