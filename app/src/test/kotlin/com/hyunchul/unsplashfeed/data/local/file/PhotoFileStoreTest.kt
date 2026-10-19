package com.hyunchul.unsplashfeed.data.local.file

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

class PhotoFileStoreTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var filesDir: File
    private lateinit var store: PhotoFileStore

    private val content = byteArrayOf(10, 20, 30, 40)

    @Before
    fun setUp() {
        filesDir = temporaryFolder.newFolder("files")
        store = PhotoFileStore(filesDir)
    }

    @Test
    fun `사진을 저장하고 상대 경로를 반환한다`() = runBlocking {
        val relativePath = store.save { output ->
            output.write(content)
        }

        assertTrue(relativePath.startsWith("favorites/"))
        assertFalse(File(relativePath).isAbsolute)

        val file = store.fileFor(relativePath)
        assertTrue(file.isFile)
        assertArrayEquals(content, file.readBytes())
        assertEquals(listOf(file), storedFiles())
    }

    @Test
    fun `쓰기 도중 실패하면 이번에 생성한 파일을 제거하고 원래 오류를 전파한다`() {
        val failure = IOException("Test write failure")

        val exception = assertThrows(IOException::class.java) {
            runBlocking {
                store.save { output ->
                    output.write(content)
                    throw failure
                }
            }
        }

        assertSame(failure.message, exception.message)
        assertTrue(storedFiles().isEmpty())
    }

    @Test
    fun `저장 된 바이트가 없으면 빈 파일을 남기지 않는다`() {
        assertThrows(IOException::class.java) {
            runBlocking {
                store.save {  }
            }
        }

        assertTrue(storedFiles().isEmpty())
    }

    @Test
    fun `저장 코루틴을 취소하면 미완료 파일을 제거한다`() = runBlocking {
        val writeStarted = CompletableDeferred<Unit>()
        val job = launch {
            store.save { output ->
                output.write(content)
                writeStarted.complete(Unit)
                awaitCancellation()
            }
        }

        withTimeout(5_000.milliseconds) { writeStarted.await() }
        job.cancelAndJoin()

        assertTrue(job.isCancelled)
        assertTrue(storedFiles().isEmpty())
    }

    @Test
    fun `저장할 때마다 새 경로를 사용하여 기존 파일을 덮어쓰지 않는다`() = runBlocking {
        val firstPath = store.save { it.write(content) }
        val secondContent = byteArrayOf(50, 60)
        val secondPath = store.save { it.write(secondContent) }

        assertNotEquals(firstPath, secondPath)
        assertArrayEquals(content, store.fileFor(firstPath).readBytes())
        assertArrayEquals(secondContent, store.fileFor(secondPath).readBytes())
        assertEquals(2, storedFiles().size)
    }

    @Test
    fun `저장소 객체가 달라도 상대 경로로 기존 파일을 찾는다`() = runBlocking {
        val relativePath = store.save { it.write(content) }
        val newStore = PhotoFileStore(filesDir)

        assertArrayEquals(content, newStore.fileFor(relativePath).readBytes())
    }

    @Test
    fun `지정한 파일만 삭제하고 같은 경로를 다시 삭제해도 성공한다`() = runBlocking {
        val firstPath = store.save { it.write(content) }
        val secondContent = byteArrayOf(50, 60)
        val secondPath = store.save { it.write(secondContent) }

        store.delete(firstPath)
        store.delete(firstPath)

        assertFalse(store.fileFor(firstPath).exists())
        assertArrayEquals(secondContent, store.fileFor(secondPath).readBytes())
        assertEquals(1, storedFiles().size)
    }

    @Test
    fun `저장 directory가 아니거나 중첩된 경로는 거부한다`() {
        val protectedFile = File(filesDir, "keep.txt").apply { writeText("keep") }
        val invalidPaths = listOf(
            "",
            "../keep.txt",
            "favorites/../keep.txt",
            "favorites/..",
            "favorites/.",
            "/favorites/photo_123.img",
            "favorites/nested/photo_123.img",
            "favorites\\photo_123.img",
        )

        invalidPaths.forEach { relativePath ->
            assertThrows(IllegalArgumentException::class.java) {
                store.fileFor(relativePath)
            }
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    store.delete(relativePath)
                }
            }
        }

        assertEquals("keep", protectedFile.readText())
    }

    private fun storedFiles(): List<File> {
        return File(filesDir, "favorites").listFiles()?.toList().orEmpty()
    }
}