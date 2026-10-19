package com.hyunchul.unsplashfeed.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hyunchul.unsplashfeed.data.local.UnsplashDatabase
import com.hyunchul.unsplashfeed.data.local.entity.FavoritePhotoEntity
import com.hyunchul.unsplashfeed.data.local.entity.PhotoFileState
import com.hyunchul.unsplashfeed.data.local.file.PhotoFileStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
class FavoritePhotoDaoTest {

    private lateinit var database: UnsplashDatabase
    private lateinit var dao: FavoritePhotoDao
    private lateinit var fileRoot: File
    private lateinit var fileStore: PhotoFileStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        database = Room.inMemoryDatabaseBuilder(
            context,
            UnsplashDatabase::class.java,
        ).build()

        dao = database.favoritePhotoDao()

        fileRoot = File(context.cacheDir, "favorite-test")
        check(fileRoot.mkdirs())

        fileStore = PhotoFileStore(fileRoot)
    }

    @After
    fun tearDown() {
        if(::database.isInitialized) {
            database.close()
        }
        if(::fileRoot.isInitialized) {
            fileRoot.deleteRecursively()
        }
    }

    @Test
    fun 좋아요를_등록하면_파일없이_PENDING과_좋아요_ID를_함께_조회한다() = verify {
        val favorite = createFavoriteEntity()
        val request = add("request-1", favorite)

        assertEquals(favorite, dao.findById("photo-1"))
        assertEquals(PhotoFileState.PENDING, request.state)
        assertNull(request.localImageRelativePath)
        assertEquals(listOf("photo-1"), dao.observePhotoIds().first())
        assertEquals(listOf(request), dao.pendingDownloads())
        assertTrue(dao.observeReadyPhotos().first().isEmpty())
        assertEquals(request, dao.observeWithFiles().first().single().file)
    }

    @Test
    fun 같은_ID를_다시_등록하면_기존_데이터를_유지한다() = verify {
        val original = createFavoriteEntity()
        val first = add("request-1", original)
        val second = add("request-2", original.copy(createdAt = 2000L))

        assertEquals(first, second)
        assertEquals(original, dao.findById("photo-1"))
        assertEquals(1, dao.observeAll().first().size)
    }

    @Test
    fun 현재_다운로드가_완료되면_READY_목록에_추가한다() = verify {
        add("request-1")

        val path = newFile()
        assertTrue(dao.publishDownloadedFile("photo-1", "request-1", path))

        val ready = dao.observeReadyPhotos().first().single()
        assertEquals(PhotoFileState.READY, ready.file.state)
        assertEquals(path, ready.file.localImageRelativePath)
        assertTrue(fileStore.fileFor(path).exists())
        assertTrue(dao.pendingDownloads().isEmpty())
    }

    @Test
    fun 완료된_좋아요를_취소하면_선택은_즉시_사라지고_파일은_삭제대기로_남는다() = verify {
        add("request-1")

        val path = newFile()
        dao.publishDownloadedFile("photo-1", "request-1", path)

        assertEquals("request-1", dao.removeFavorite("photo-1")?.requestId)
        assertNull(dao.findById("photo-1"))
        assertNull(dao.findFile("photo-1"))
        assertTrue(dao.observePhotoIds().first().isEmpty())
        assertEquals(listOf(path), deletionPaths())
        assertTrue(fileStore.fileFor(path).exists())

        fileStore.delete(path)
        dao.acknowledgeFileDeleted(path)

        assertFalse(fileStore.fileFor(path).exists())
        assertTrue(deletionPaths().isEmpty())
    }

    @Test
    fun 취소후_재등록하면_이전_성공과_실패는_새_요청을_변경하지_않는다() = verify {
        add("request-old")
        dao.removeFavorite("photo-1")
        add("request-new")
        val oldPath = newFile()

        assertFalse(dao.publishDownloadedFile("photo-1", "request-old", oldPath))
        assertEquals(0, dao.markFailed("photo-1", "request-old", "NETWORK"))

        val pending = dao.findFile("photo-1")!!

        assertEquals("request-new", pending.requestId)
        assertEquals(PhotoFileState.PENDING, pending.state)

        val newPath = newFile()

        assertTrue(dao.publishDownloadedFile("photo-1", "request-new", newPath))
        assertEquals(listOf(oldPath), deletionPaths())

        fileStore.delete(oldPath)
        dao.acknowledgeFileDeleted(oldPath)

        assertTrue(fileStore.fileFor(newPath).exists())
        assertEquals(newPath, dao.findFile("photo-1")?.localImageRelativePath)
    }

    @Test
    fun 다운로드_최종실패여도_좋아요는_유지하고_READY에는_포함하지_않는다() = verify {
        add("request-1")
        assertEquals(1, dao.markFailed("photo-1", "request-1", "WRITE_FAILED"))
        assertEquals(PhotoFileState.FAILED, dao.findFile("photo-1")?.state)
        assertEquals(listOf("photo-1"), dao.observePhotoIds().first())
        assertTrue(dao.observeReadyPhotos().first().isEmpty())
        assertTrue(dao.pendingDownloads().isEmpty())
    }

    @Test
    fun 같은_요청이_다른_파일을_완료해도_첫_파일만_채택한다() = verify {
        add("request-1")
        val firstPath = newFile()
        val secondPath = newFile()

        assertTrue(dao.publishDownloadedFile("photo-1", "request-1", firstPath))
        assertFalse(dao.publishDownloadedFile("photo-1", "request-1", secondPath))
        assertEquals(firstPath, dao.findFile("photo-1")?.localImageRelativePath)
        assertEquals(listOf(secondPath), deletionPaths())
    }

    @Test
    fun 좋아요_목록은_다운로드완료_순서가_아닌_좋아요_생성_시각_내림차순이고_같은_경우_ID_오름차순이다() = verify {
        add("request-1", createFavoriteEntity("photo-1", 1_000L))
        add("request-2", createFavoriteEntity("photo-2", 2_000L))
        add("request-3", createFavoriteEntity("photo-3", 3_000L))
        add("request-4", createFavoriteEntity("photo-4", 3_000L))

        dao.publishDownloadedFile("photo-3", "request-3", newFile())
        dao.publishDownloadedFile("photo-2", "request-2", newFile())
        dao.publishDownloadedFile("photo-4", "request-4", newFile())
        dao.publishDownloadedFile("photo-1", "request-1", newFile())

        assertEquals(
            listOf("photo-3", "photo-4", "photo-2", "photo-1"),
            dao.observeReadyPhotos().first().map { it.favorite.photoId },
        )
    }

    private fun verify(
        block: suspend () -> Unit,
    ) = runBlocking {
        withTimeout(20_000L.milliseconds) {
            block()
        }
    }

    private suspend fun add(
        requestId: String,
        favorite: FavoritePhotoEntity = createFavoriteEntity(),
    ) = dao.addFavorite(
        favorite = favorite,
        imageUrl = TEST_IMAGE_URL,
        newRequestId = requestId,
        downloadLocation = "https://api.unsplash.com/photos/${favorite.photoId}/download",
    )

    private suspend fun newFile(): String = fileStore.save {
        it.write(byteArrayOf(1, 2, 3, 4, 5))
    }

    private suspend fun deletionPaths(): List<String> = dao.pendingFileDeletions().map { it.relativePath }

    private fun createFavoriteEntity(
        photoId: String = "photo-1",
        createdAt: Long = 1_000L,
    ): FavoritePhotoEntity = FavoritePhotoEntity(
        photoId = photoId,
        photographerName = "Test User",
        photographerProfileImageUrl = "https://example.com/user",
        description = "test photo description",
        altDescription = "test photo alt description",
        createdAt = createdAt,
    )

    private companion object {
        const val TEST_TIMEOUT_MILLIS = 10_000L
        const val TEST_IMAGE_URL = "https://example.com/photo.jpg"
    }
}