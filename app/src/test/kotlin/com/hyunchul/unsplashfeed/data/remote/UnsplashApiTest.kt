package com.hyunchul.unsplashfeed.data.remote

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class UnsplashApiTest {

    @Test
    fun getPhotos_returnsPhotos() = runBlocking {
        val mockEngine = MockEngine { request ->
            assertEquals("api.unsplash.com", request.url.host)
            assertEquals("/photos", request.url.encodedPath)
            assertEquals("1", request.url.parameters["page"])
            assertEquals("3", request.url.parameters["per_page"])
            assertEquals(
                "Client-ID $TEST_ACCESS_KEY",
                request.headers[HttpHeaders.Authorization],
            )

            respond(
                content = """
                    [
                        {
                            "id": "photo-1",
                            "width": 5245,
                            "height": 3497,
                            "description": "A man drinking a coffee.",
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
            engine = mockEngine,
        )

        client.use { client ->
            val photos = UnsplashApi(client).getPhotos(
                page = 1,
                perPage = 3,
            )

            assertEquals(1, photos.size)
            assertEquals("photo-1", photos.first().id)
            assertEquals("Test User", photos.first().user.name)
        }
    }

    @Test
    fun getPhotos_throwsClientRequestException_on401() {
        val mockEngine = MockEngine {
            respond(
                content = """
                    {
                        "errors": ["Unauthorized"]
                    }
                """.trimIndent(),
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(
                    HttpHeaders.ContentType,
                    "application/json",
                ),
            )
        }

        val client = createUnsplashClient(
            accessKey = TEST_ACCESS_KEY,
            engine = mockEngine,
        )

        client.use {
            val exception = assertThrows(ClientRequestException::class.java) {
                runBlocking {
                    UnsplashApi(client).getPhotos()
                }
            }

            assertEquals(
                HttpStatusCode.Unauthorized,
                exception.response.status,
            )
        }
    }

    private companion object {
        const val TEST_ACCESS_KEY = "test-key"
    }
}