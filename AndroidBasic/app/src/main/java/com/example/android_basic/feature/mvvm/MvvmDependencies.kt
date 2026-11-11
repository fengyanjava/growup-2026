package com.example.android_basic.feature.mvvm

import com.example.android_basic.core.network.NetworkClient
import com.example.android_basic.feature.mvvm.data.remote.PostApi
import com.example.android_basic.feature.mvvm.data.repository.NetworkPostRepository
import com.example.android_basic.feature.mvvm.data.repository.PostRepository

/** 简单手动依赖注入：集中组装本功能，避免 Activity / ViewModel 创建网络对象。 */
object MvvmDependencies {
    val postRepository: PostRepository by lazy {
        val api = NetworkClient.createService(
            baseUrl = "https://jsonplaceholder.typicode.com/",
            serviceClass = PostApi::class.java,
        )
        NetworkPostRepository(api)
    }
}
