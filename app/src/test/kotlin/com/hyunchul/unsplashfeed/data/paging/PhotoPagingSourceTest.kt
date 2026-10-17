package com.hyunchul.unsplashfeed.data.paging

import androidx.paging.PagingSource
import com.hyunchul.unsplashfeed.data.remote.resolveHasNextPage
import com.hyunchul.unsplashfeed.domain.error.PhotoLoadException
import com.hyunchul.unsplashfeed.domain.model.FeedPhotoItem
import com.hyunchul.unsplashfeed.domain.model.PhotoPage
import com.hyunchul.unsplashfeed.fixture.FeedFixtures.photo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoPagingSourceTest {

    private fun params(page: Int): PagingSource.LoadParams<Int> =
        if(page == 1) PagingSource.LoadParams.Refresh(null, 20, false)
        else PagingSource.LoadParams.Append(page, 20, false)

    private suspend fun page(source: PhotoPagingSource, key: Int): PagingSource.LoadResult.Page<Int, FeedPhotoItem> =
        source.load(params(key)) as PagingSource.LoadResult.Page<Int, FeedPhotoItem>

    @Test
    fun `같은 페이지의 중복 사진을 유지하고 항목마다 고유한 키를 부여한다`() = runBlocking {
        val original = photo("A")
        val source = PhotoPagingSource { _, _ -> PhotoPage(listOf(original, original), false)}
        val items = page(source, 1).data
        assertEquals(listOf("A", "A"), items.map { it.photo.id })
        assertEquals(2, items.map { it.entryKey }.toSet().size)
        assertSame(original, items.first().photo)
        assertEquals("feed:1:0:A", items[0].entryKey)
        assertEquals("feed:1:1:A", items[1].entryKey)
    }

    @Test
    fun `서로 다른 페이지의 중복 사진을 유지하고 항목 키를 구분한다`() = runBlocking {
        val source = PhotoPagingSource { p, _ -> PhotoPage(listOf(photo("A")), p == 1)}
        val first = page(source, 1).data.single()
        val second = page(source, 2).data.single()
        assertEquals(first.photo.id, second.photo.id)
        assertNotEquals(first.entryKey, second.entryKey)
    }

    @Test
    fun `성공 페이지를 캐시하지 않고 다시 조회해도 동일한 응답은 동일한 키를 만든다`() = runBlocking {
        var calls = 0
        val source = PhotoPagingSource { _, _ ->
            calls++
            PhotoPage(listOf(photo("A")), false)
        }
        assertEquals(page(source, 1).data, page(source, 1).data)
        assertEquals(2, calls)
    }

    @Test
    fun `네트워크 실패 후 같은 페이지를 다시 요청하면 데이터를 반환한다`() = runBlocking {
        val calls = mutableListOf<Int>()
        val failure = IOException("test")
        val source = PhotoPagingSource { p, _ ->
            calls += p
            if(calls.size == 1) throw failure
            PhotoPage(listOf(photo("A")), false)
        }
        val error = source.load(params(1)) as PagingSource.LoadResult.Error
        assertSame(failure, error.throwable)
        assertEquals("A", page(source, 1).data.single().photo.id)
        assertEquals(listOf(1, 1), calls)
    }

    @Test
    fun `서버가 다음 페이지를 표시해도 데이터가 비어 있으면 로딩을 종료한다`() = runBlocking {
        val source = PhotoPagingSource { _, _ -> PhotoPage(emptyList(), true) }
        assertNull(page(source, 1).nextKey)
    }

    @Test
    fun `요청 개수보다 적게 받아도 서버가 다음 페이지를 표시하면 다음 키를 반환한다`() = runBlocking {
        val source = PhotoPagingSource { _, size ->
            assertEquals(20, size)
            PhotoPage(listOf(photo("A")), true)
        }
        assertEquals(2, page(source, 1).nextKey)
    }

    @Test
    fun `무효화된 소스는 데이터 요청 없이 Invalid 결과를 반환한다`() = runBlocking {
        var calls = 0
        val source = PhotoPagingSource { _, _ ->
            calls++
            PhotoPage(emptyList(), false)
        }
        source.invalidate()
        assertTrue(source.load(params(1)) is PagingSource.LoadResult.Invalid)
        assertEquals(0, calls)
    }

    @Test
    fun `link에 next가 존재하면 hasNextPage는 true를 반환한다`() {
        val link = """
            <https://api.unsplash.com/photos?page=1>; rel="first",
            <https://api.unsplash.com/photos?page=3>; rel="next"
        """.trimIndent()

        val result = resolveHasNextPage(
            linkHeader = link,
            receivedCount = 20,
            requestedPerPage = 20,
        )

        assertTrue(result)
    }

    @Test
    fun `link에 next가 없으면 hasNextPage는 false를 반환한다`() {
        val link = """
            <https://api.unsplash.com/photos?page=1>; rel="first",
            <https://api.unsplash.com/photos?page=3>; rel="last"
        """.trimIndent()

        val result = resolveHasNextPage(
            linkHeader = link,
            receivedCount = 20,
            requestedPerPage = 20,
        )

        assertFalse(result)
    }

    @Test
    fun `헤더에 link가 없으면 hasNextPage는 사진 개수를 사용하여 반환한다`() {
        assertTrue(resolveHasNextPage(null, 20, 20))
        assertFalse(resolveHasNextPage(null, 10, 20))
    }

    @Test
    fun `load 중 예외가 발생하면 Error를 반환한다`() = runBlocking {
        val originalException = PhotoLoadException.Network(
            IOException("Network connection failed")
        )

        val pagingSource = PhotoPagingSource { _, _ ->
            throw originalException
        }

        val result = pagingSource.load(params(1))

        assertTrue(result is PagingSource.LoadResult.Error)

        val error = result as PagingSource.LoadResult.Error<Int, FeedPhotoItem>

        assertSame(originalException, error.throwable)
    }

    @Test
    fun `load 중 CancellationException이 발생하면 예외를 전파한다`() {
        val originalException = CancellationException(
            "Paging request cancelled"
        )

        val pagingSource = PhotoPagingSource { _, _ ->
            throw originalException
        }

        val exception = assertThrows(CancellationException::class.java) {
            runBlocking {
                pagingSource.load(params(1))
            }
        }

        assertSame(originalException, exception)
    }
}
