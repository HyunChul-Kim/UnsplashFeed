package com.hyunchul.unsplashfeed.data.repository

import com.hyunchul.unsplashfeed.data.remote.UnsplashApi
import com.hyunchul.unsplashfeed.data.remote.createUnsplashClient
import com.hyunchul.unsplashfeed.domain.error.PhotoLoadException
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoRepositoryTest {

    @Test
    fun `getPhotos는 Photo로 변환하여 반환한다`() = runBlocking {
        val engine = MockEngine { request ->
            respond(
                content = """
                    [
                        {
                            "id": "photo-1",
                            "width": 5245,
                            "height": 3497,
                            "description": null,
                            "alt_description": "man in black crew neck t-shirt drinking coffee",
                            "user": {
                                "username": "tester",
                                "name": "Test User",
                                "profile_image": {
                                    "small": "profile-small-url",
                                    "medium": "profile-medium-url",
                                    "large": "profile-large-url"
                                }
                            },
                            "urls": {
                                "raw": "raw-url",
                                "full": "full-url",
                                "regular": "regular-url",
                                "small": "small-url",
                                "thumb": "thumb-url"
                            },
                            "links": {
                                "download_location": "download-url"
                            }
                        }
                    ]
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(
                    HttpHeaders.ContentType,
                    "application/json",
                ),
            )
        }

        val client = createUnsplashClient(
            accessKey = TEST_ACCESS_KEY,
            engine = engine,
        )

        client.use {
            val repository = PhotoRepository(UnsplashApi(client))

            val photos = repository.getPhotos(
                page = 2,
                perPage = 5,
            )

            assertEquals(1, photos.size)

            val photo = photos.first()

            assertEquals("photo-1", photo.id)
        }
    }

    @Test
    fun `getPhotos는 page가 0인 경우 예외를 반환한다`() {
        val client = createUnsplashClient(
            accessKey = TEST_ACCESS_KEY,
            engine = MockEngine {
                error("HTTP request should not be sent")
            },
        )

        client.use {
            val repository = PhotoRepository(UnsplashApi(client))

            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    repository.getPhotos(page = 0)
                }
            }
        }
    }

    @Test
    fun `getPhotos는 perPage가 invalid인 경우 예외를 반환한다`() {
        val client = createUnsplashClient(
            accessKey = TEST_ACCESS_KEY,
            engine = MockEngine {
                error("HTTP request should not be sent")
            },
        )

        client.use {
            val repository = PhotoRepository(UnsplashApi(client))

            listOf(0, 31).forEach { perPage ->
                assertThrows(IllegalArgumentException::class.java) {
                    runBlocking {
                        repository.getPhotos(perPage = perPage)
                    }
                }
            }
        }
    }

    @Test
    fun `getPhotos는 MalformedJson인 경우 Unexpected 예외를 반환한다`() {
        val exception = assertRepositoryError<PhotoLoadException.Unexpected>(
            status = HttpStatusCode.OK,
            content = "invalid-json",
        )

        assertNotNull(exception.cause)
    }

    @Test
    fun `getPhotos는 401응답에 Unauthorized 예외를 반환한다`() {
        val exception = assertRepositoryError<PhotoLoadException.Unauthorized>(HttpStatusCode.Unauthorized)

        assertNotNull(exception.cause)
    }

    @Test
    fun `getPhotos는 429응답에 RateLimited 예외를 반환한다`() {
        val exception = assertRepositoryError<PhotoLoadException.RateLimited>(HttpStatusCode.TooManyRequests)

        assertNotNull(exception.cause)
    }

    @Test
    fun `getPhotos는 500응답에 HttpError 예외를 반환한다`() {
        val exception = assertRepositoryError<PhotoLoadException.HttpError>(HttpStatusCode.InternalServerError)

        assertEquals(500, exception.statusCode)
        assertNotNull(exception.cause)
    }

    @Test
    fun `getPhotos는 IOException을 Network 예외로 변환하여 반환한다`() {
        val engine = MockEngine {
            throw IOException("Simulated network failure")
        }

        val client = createUnsplashClient(
            accessKey = TEST_ACCESS_KEY,
            engine = engine,
        )

        client.use {
            val exception = assertThrows(PhotoLoadException.Network::class.java) {
                runBlocking {
                    PhotoRepository(UnsplashApi(client)).getPhotos()
                }
            }

            assertTrue(exception.cause is IOException)
        }
    }

    @Test
    fun `getPhotos는 CancellationException을 전파한다`() {
        val engine = MockEngine {
            throw CancellationException(
                "Simulated cancellation"
            )
        }

        val client = createUnsplashClient(
            accessKey = TEST_ACCESS_KEY,
            engine = engine,
        )

        client.use {
            assertThrows(CancellationException::class.java) {
                runBlocking {
                    PhotoRepository(UnsplashApi(client)).getPhotos()
                }
            }
        }
    }

    private inline fun <reified T: Throwable> assertRepositoryError(
        status: HttpStatusCode,
        content: String = """{"errors":["test error"]}""",
    ): T {
        val engine = MockEngine {
            respond(
                content = content,
                status = status,
                headers = headersOf(
                    HttpHeaders.ContentType,
                    "application/json",
                ),
            )
        }

        val client = createUnsplashClient(
            accessKey = TEST_ACCESS_KEY,
            engine = engine,
        )

        return client.use {
            assertThrows(T::class.java) {
                runBlocking {
                    PhotoRepository(UnsplashApi(client)).getPhotos()
                }
            }
        }
    }

    private companion object {
        const val TEST_ACCESS_KEY = "test-key"
    }
}