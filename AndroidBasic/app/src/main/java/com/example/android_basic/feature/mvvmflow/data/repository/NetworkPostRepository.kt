package com.example.android_basic.feature.mvvmflow.data.repository

import com.example.android_basic.core.network.NetworkResult
import com.example.android_basic.core.network.safeRequest
import com.example.android_basic.feature.mvvmflow.data.model.Post
import com.example.android_basic.feature.mvvmflow.data.remote.PostApi

class NetworkPostRepository(private val api: PostApi) : PostRepository {
    // Retrofit 的挂起调用本身不阻塞主线程，无需再包 Dispatchers.IO。
    override suspend fun getPosts(): NetworkResult<List<Post>> = safeRequest {
        api.getPosts()
    }
}
