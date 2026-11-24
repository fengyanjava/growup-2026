package com.example.composefirst.first.tab_switching.article

import com.example.composefirst.first.tab_switching.network.ApiClient

class ArticleRepository(
    private val api: ArticleApi = ApiClient.retrofit.create(ArticleApi::class.java),
) {
    // Retrofit 的 suspend 接口负责异步请求、JSON 转换和协程取消，无需手动切换 IO 线程。
    suspend fun getArticles(): List<Article> = api.getArticles()
}
