package com.example.composefirst.first.tab_switching.article

import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface ArticleApi {
    @Headers("Accept: application/json")
    @GET("posts")
    suspend fun getArticles(
        @Query("_start") offset: Int,
        @Query("_limit") limit: Int,
    ): List<Article>
}
