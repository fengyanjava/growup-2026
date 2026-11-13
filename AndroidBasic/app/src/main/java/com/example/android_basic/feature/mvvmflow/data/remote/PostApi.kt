package com.example.android_basic.feature.mvvmflow.data.remote

import com.example.android_basic.feature.mvvmflow.data.model.Post
import retrofit2.http.GET

interface PostApi {
    // Retrofit 的 suspend 支持异步网络调用，并将协程取消传递给 HTTP 请求。
    @GET("posts")
    suspend fun getPosts(): List<Post>
}
