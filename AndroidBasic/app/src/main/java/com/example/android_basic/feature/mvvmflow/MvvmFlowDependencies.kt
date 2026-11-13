package com.example.android_basic.feature.mvvmflow

import com.example.android_basic.core.network.NetworkClient
import com.example.android_basic.feature.mvvmflow.data.remote.PostApi
import com.example.android_basic.feature.mvvmflow.data.repository.NetworkPostRepository
import com.example.android_basic.feature.mvvmflow.data.repository.PostRepository

/** 简单手动依赖注入：集中组装本功能，避免 Activity / ViewModel 创建网络对象。 */
object MvvmFlowDependencies {
    val postRepository: PostRepository by lazy {
        val api = NetworkClient.createService(
            baseUrl = "https://jsonplaceholder.typicode.com/",
            serviceClass = PostApi::class.java,
        )
        NetworkPostRepository(api)
    }
}
