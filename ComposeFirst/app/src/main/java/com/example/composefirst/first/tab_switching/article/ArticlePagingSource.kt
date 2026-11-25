package com.example.composefirst.first.tab_switching.article

import androidx.paging.PagingSource
import androidx.paging.PagingState
import java.io.IOException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

class ArticlePagingSource(private val api: ArticleApi) : PagingSource<Int, Article>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Article> {
        val offset = params.key ?: 0
        // 只处理预期的请求错误；协程取消继续传播给 Retrofit。
        return try {
            val articles = api.getArticles(offset = offset, limit = params.loadSize)
            LoadResult.Page(
                data = articles,
                prevKey = if (offset == 0) null else (offset - params.loadSize).coerceAtLeast(0),
                nextKey = if (articles.size < params.loadSize) null else offset + articles.size,
            )
        } catch (error: IOException) {
            LoadResult.Error(error)
        } catch (error: HttpException) {
            LoadResult.Error(error)
        } catch (error: SerializationException) {
            LoadResult.Error(error)
        }
    }

    // 此示例下拉刷新从第一条开始；cachedIn 在同一 ViewModel 作用域内缓存已加载的数据。
    override fun getRefreshKey(state: PagingState<Int, Article>): Int? = null
}
