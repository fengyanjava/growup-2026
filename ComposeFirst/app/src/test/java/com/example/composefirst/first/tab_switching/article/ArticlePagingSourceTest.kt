package com.example.composefirst.first.tab_switching.article

import androidx.paging.PagingSource
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ArticlePagingSourceTest {
    @Test
    fun refreshUsesRequestedLoadSize() = runBlocking {
        val requests = mutableListOf<Pair<Int, Int>>()
        val source = ArticlePagingSource(api { offset, limit ->
            requests += offset to limit
            articles(offset, limit)
        })

        val page = source.load(refresh(size = 60)) as PagingSource.LoadResult.Page

        assertEquals(listOf(0 to 60), requests)
        assertEquals((1..60).toList(), page.data.map { it.id })
        assertNull(page.prevKey)
        assertEquals(60, page.nextKey)
    }

    @Test
    fun appendContinuesAfterInitialLargeLoadWithoutGapsOrDuplicates() = runBlocking {
        val requests = mutableListOf<Pair<Int, Int>>()
        val source = ArticlePagingSource(api { offset, limit ->
            requests += offset to limit
            articles(offset, limit)
        })
        val first = source.load(refresh(size = 60)) as PagingSource.LoadResult.Page
        val next = source.load(append(key = first.nextKey!!)) as PagingSource.LoadResult.Page

        assertEquals(listOf(0 to 60, 60 to 20), requests)
        assertEquals((1..80).toList(), (first.data + next.data).map { it.id })
        assertEquals(80, next.nextKey)
    }

    @Test
    fun partialLastPageStopsLoading() = runBlocking {
        val source = ArticlePagingSource(api { offset, limit ->
            articles(offset, minOf(limit, 100 - offset))
        })
        val page = source.load(append(key = 80, size = 30)) as PagingSource.LoadResult.Page

        assertEquals((81..100).toList(), page.data.map { it.id })
        assertNull(page.nextKey)
    }

    @Test
    fun emptyResponseStopsLoading() = runBlocking {
        val source = ArticlePagingSource(api { _, _ -> emptyList() })
        val page = source.load(refresh()) as PagingSource.LoadResult.Page

        assertEquals(emptyList<Article>(), page.data)
        assertNull(page.nextKey)
    }

    @Test
    fun requestFailuresBecomeLoadErrors() = runBlocking {
        val errors = listOf(
            IOException("offline"),
            HttpException(Response.error<List<Article>>(503, "unavailable".toResponseBody())),
            SerializationException("invalid json"),
        )
        for (error in errors) {
            val source = ArticlePagingSource(api { _, _ -> throw error })
            val result = source.load(refresh()) as PagingSource.LoadResult.Error
            assertSame(error, result.throwable)
        }
    }

    @Test
    fun retryingFailedAppendUsesTheSameOffset() = runBlocking {
        val offsets = mutableListOf<Int>()
        val source = ArticlePagingSource(api { offset, limit ->
            offsets += offset
            if (offsets.size == 1) throw IOException("temporary failure")
            articles(offset, limit)
        })
        val failed = source.load(append(key = 20)) as PagingSource.LoadResult.Error
        val retried = source.load(append(key = 20)) as PagingSource.LoadResult.Page

        assertEquals("temporary failure", failed.throwable.message)
        assertEquals(listOf(20, 20), offsets)
        assertEquals((21..40).toList(), retried.data.map { it.id })
    }

    @Test(expected = CancellationException::class)
    fun cancellationIsNotConvertedToLoadError(): Unit = runBlocking {
        val source = ArticlePagingSource(api { _, _ -> throw CancellationException("cancelled") })
        source.load(refresh())
        Unit
    }

    private fun refresh(size: Int = 20) = PagingSource.LoadParams.Refresh<Int>(
        key = null,
        loadSize = size,
        placeholdersEnabled = false,
    )

    private fun append(key: Int, size: Int = 20) = PagingSource.LoadParams.Append(
        key = key,
        loadSize = size,
        placeholdersEnabled = false,
    )

    private fun api(load: suspend (Int, Int) -> List<Article>) = object : ArticleApi {
        override suspend fun getArticles(offset: Int, limit: Int) = load(offset, limit)
    }

    private fun articles(offset: Int, count: Int) = List(count) { index ->
        val id = offset + index + 1
        Article(id = id, userId = 1, title = "Article $id", body = "Body $id")
    }
}
