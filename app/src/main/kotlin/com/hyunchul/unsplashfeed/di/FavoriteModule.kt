package com.hyunchul.unsplashfeed.di

import android.content.Context
import androidx.room.Room
import com.hyunchul.unsplashfeed.data.download.FavoriteDownloadTask
import com.hyunchul.unsplashfeed.data.download.PendingPhotoFileCleaner
import com.hyunchul.unsplashfeed.data.local.UnsplashDatabase
import com.hyunchul.unsplashfeed.data.local.dao.FavoritePhotoDao
import com.hyunchul.unsplashfeed.data.local.file.PhotoFileStore
import com.hyunchul.unsplashfeed.data.remote.download.PhotoDownloader
import com.hyunchul.unsplashfeed.data.remote.download.createPhotoDownloadClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER
)
@Retention(AnnotationRetention.BINARY)
annotation class PhotoDownloadClient

@Qualifier
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER
)
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object FavoriteModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): UnsplashDatabase = Room.databaseBuilder(
        context,
        UnsplashDatabase::class.java,
        "unsplash.db",
    ).build()

    @Provides
    fun provideFavoriteDao(
        database: UnsplashDatabase,
    ): FavoritePhotoDao = database.favoritePhotoDao()

    @Provides
    @Singleton
    fun provideFileStore(
        @ApplicationContext context: Context,
    ): PhotoFileStore = PhotoFileStore(context.filesDir)

    @Provides
    @Singleton
    @PhotoDownloadClient
    fun providePhotoDownloadClient(): HttpClient = createPhotoDownloadClient()

    @Provides
    @Singleton
    fun provideDownloader(
        @PhotoDownloadClient client: HttpClient,
    ): PhotoDownloader = PhotoDownloader(client)

    @Provides
    fun provideDownloadTask(
        dao: FavoritePhotoDao,
        fileStore: PhotoFileStore,
        downloader: PhotoDownloader,
    ): FavoriteDownloadTask = FavoriteDownloadTask(dao, fileStore, downloader::download)

    @Provides
    fun provideCleaner(
        dao: FavoritePhotoDao,
        fileStore: PhotoFileStore,
    ): PendingPhotoFileCleaner = PendingPhotoFileCleaner(dao, fileStore::delete)

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
}