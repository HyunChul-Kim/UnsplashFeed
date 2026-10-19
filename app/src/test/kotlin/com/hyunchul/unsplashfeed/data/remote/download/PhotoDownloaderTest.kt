package com.hyunchul.unsplashfeed.data.remote.download

import com.hyunchul.unsplashfeed.data.local.file.PhotoFileStore
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.Base64
import java.util.zip.GZIPOutputStream

class PhotoDownloaderTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var filesDir: File
    private lateinit var store: PhotoFileStore

    @Before
    fun setUp() {
        filesDir = temporaryFolder.newFolder("files")
        store = PhotoFileStore(filesDir)
    }

    @Test
    fun `이미지 URL의 쿼리를 유지하고 인증 없이 받은 사진 데이터를 파일에 저장한다`() = runBlocking {
        withDownloader(contentLength = PNG_BYTES.size.toString()) { downloader ->
            val path = saveWith(downloader)

            assertArrayEquals(PNG_BYTES, store.fileFor(path).readBytes())
            assertEquals(1, storedFiles().size)
        }
    }

    @Test
    fun `Content-Encoding이 없으면 수신 데이터를 저장한다`() = runBlocking {
        withDownloader(
            contentEncoding = null,
        ) { downloader ->
            val path = saveWith(downloader)

            assertArrayEquals(
                PNG_BYTES,
                store.fileFor(path).readBytes(),
            )
        }
    }

    @Test
    fun `Content-Encoding이 identity이면 수신 데이터를 저장한다`() = runBlocking {
        withDownloader(
            contentEncoding = "identity",
        ) { downloader ->
            val path = saveWith(downloader)

            assertArrayEquals(
                PNG_BYTES,
                store.fileFor(path).readBytes(),
            )
        }
    }

    @Test
    fun `엔진이 해제하지 않은 gzip 응답은 저장하지 않는다`() {
        val compressed = ByteArrayOutputStream().use { bytes ->
            GZIPOutputStream(bytes).use { it.write(PNG_BYTES) }
            bytes.toByteArray()
        }

        val exception = assertThrows(IOException::class.java) {
            runBlocking {
                withDownloader(
                    body = ByteReadChannel(compressed),
                    contentLength = compressed.size.toString(),
                    contentEncoding = "gzip",
                ) { downloader ->
                    saveWith(downloader)
                }
            }
        }

        assertEquals(
            "Encoded HTTP photo bodies are not supported.",
            exception.message,
        )
        assertTrue(storedFiles().isEmpty())
    }

    @Test
    fun `Content-Length와 실제 수신 데이터 길이가 같으면 저장한다`() {
        listOf(null, "identity").forEach { encoding ->
            runBlocking {
                withDownloader(
                    contentEncoding = encoding,
                    contentLength = PNG_BYTES.size.toString(),
                ) { downloader ->
                    val path = saveWith(downloader)

                    assertArrayEquals(
                        PNG_BYTES,
                        store.fileFor(path).readBytes(),
                    )
                }
            }
        }
    }

    @Test
    fun `Content-Length와 실제 수신 데이터 길이가 다르면 실패하고 파일을 제거한다`() {
        listOf(null, "identity").forEach { encoding ->
            assertThrows(IOException::class.java) {
                runBlocking {
                    withDownloader(
                        contentEncoding = encoding,
                        contentLength = (PNG_BYTES.size + 10).toString()
                    ) { downloader ->
                        saveWith(downloader)
                    }
                }
            }

            assertThrows(IOException::class.java) {
                runBlocking {
                    withDownloader(
                        contentEncoding = encoding,
                        contentLength = (PNG_BYTES.size - 10).toString()
                    ) { downloader ->
                        saveWith(downloader)
                    }
                }
            }

            assertTrue(storedFiles().isEmpty())
        }
    }

    @Test
    fun `Content-Length가 없어도 수신한 데이터를 저장한다`() {
        listOf(null, "identity").forEach { encoding ->
            runBlocking {
                withDownloader(
                    contentEncoding = encoding,
                    contentLength = null,
                ) { downloader ->
                    val path = saveWith(downloader)

                    assertArrayEquals(
                        PNG_BYTES,
                        store.fileFor(path).readBytes(),
                    )
                }
            }
        }
    }

    @Test
    fun `빈 이미지 응답은 저장하지 않는다`() {
        assertThrows(IOException::class.java) {
            runBlocking {
                withDownloader(
                    body = ByteReadChannel(byteArrayOf()),
                    contentLength = "0",
                ) { downloader ->
                    saveWith(downloader)
                }
            }
        }

        assertTrue(storedFiles().isEmpty())
    }

    private suspend fun saveWith(downloader: PhotoDownloader): String {
        return store.save { output ->
            downloader.download(IMAGE_URL, output)
        }
    }

    private suspend fun <T> withDownloader(
        body: ByteReadChannel = ByteReadChannel(PNG_BYTES),
        status: HttpStatusCode = HttpStatusCode.OK,
        contentType: String? = "image/png",
        contentLength: String? = null,
        contentEncoding: String? = null,
        block: suspend (PhotoDownloader) -> T,
    ): T {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals(IMAGE_URL, request.url.toString())
            assertEquals("test-referral", request.url.parameters["ixid"])
            assertTrue(request.headers.getAll(HttpHeaders.AcceptEncoding).isNullOrEmpty())
            assertTrue(request.headers.getAll(HttpHeaders.Authorization).isNullOrEmpty())

            respond(
                content = body,
                status = status,
                headers = Headers.build {
                    contentType?.let { append(HttpHeaders.ContentType, it) }
                    contentLength?.let { append(HttpHeaders.ContentLength, it) }
                    contentEncoding?.let { append(HttpHeaders.ContentEncoding, it) }
                    if(status == HttpStatusCode.Found) {
                        append(HttpHeaders.Location, "https://example.com/other-image")
                    }
                },
            )
        }

        val client = createPhotoDownloadClient(engine)

        return try {
            block(PhotoDownloader(client))
        } finally {
            client.close()
            engine.close()
        }
    }

    private fun storedFiles(): List<File> {
        return File(filesDir, "favorites").listFiles()?.toList().orEmpty()
    }

    private class TrackingOutputStream: ByteArrayOutputStream() {
        var wasClosed: Boolean = false
            private set

        override fun close() {
            wasClosed = true
            super.close()
        }
    }

    private companion object {
        const val IMAGE_URL = "https://images.unsplash.com/photo-test?ixid=test-referral&w=1080"
        val PNG_BYTES: ByteArray = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADElEQVR4nGNoaGgAAAMEAYFL09IQAAAAAElFTkSuQmCC"
        )
    }
}