package com.example.composefirst.second.home

import com.example.composefirst.network.ApiClient
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

class HomeRepository(
    private val api: HomeApi = ApiClient.retrofit.create(HomeApi::class.java),
) {

    suspend fun loadList(): List<HomeArticleInfo> {
        return api.getArticles(0, 50)
    }
}

interface HomeApi {
    @Headers("Accept: application/json")
    @GET("posts")
    suspend fun getArticles(
        @Query("_start") offset: Int,
        @Query("_limit") limit: Int,
    ): List<HomeArticleInfo>
}