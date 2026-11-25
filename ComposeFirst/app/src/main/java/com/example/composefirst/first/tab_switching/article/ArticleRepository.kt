package com.example.composefirst.first.tab_switching.article

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.composefirst.network.ApiClient
import kotlinx.coroutines.flow.Flow

class ArticleRepository(
    private val api: ArticleApi = ApiClient.retrofit.create(ArticleApi::class.java),
) {
    fun getPagedArticles(): Flow<PagingData<Article>> = Pager(
        config = PagingConfig(
            pageSize = 20,
            initialLoadSize = 20,
            prefetchDistance = 5,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = { ArticlePagingSource(api) },
    ).flow
}
