package com.example.composefirst.first.tab_switching.article

import retrofit2.http.GET
import retrofit2.http.Headers

interface ArticleApi {
    @Headers("Accept: application/json")
    @GET("posts")
    suspend fun getArticles(): List<Article>
}
