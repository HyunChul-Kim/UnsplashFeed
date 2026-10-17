package com.hyunchul.unsplashfeed.data.paging

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.testing.asSnapshot
import com.hyunchul.unsplashfeed.fixture.FeedFixtures
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PagingSnapshotTest {
    private fun config() = PagingConfig(
        pageSize = 20,
        initialLoadSize = 20,
        prefetchDistance = 5,
        enablePlaceholders = false,
        maxSize = PagingConfig.MAX_SIZE_UNBOUNDED,
    )

    @Test
    fun `Pager는 원본 순서를 유지하며 고유한 키를 가진 항목을 제공한다`() = runTest {
        val fixture = FeedFixtures.itemsWithDuplicates()
        val calls = mutableListOf<Int>()
        val pager = Pager(config(), initialKey = 1) {
            PhotoPagingSource { p, _ ->
                calls += p
                fixture[p - 1]
            }
        }
        val items = pager.flow.asSnapshot { appendScrollWhile { true } }
        assertEquals(400, items.size)
        assertEquals(400, items.map { it.entryKey }.toSet().size)
        assertEquals(379, items.map { it.photo.id }.toSet().size)
        assertEquals(fixture.flatMap { it.photos }, items.map { it.photo })
        assertEquals((1..20).toList(), calls)
    }
}