package com.example.android_basic.feature.mvvmflow.data.repository

import com.example.android_basic.core.network.NetworkResult
import com.example.android_basic.feature.mvvmflow.data.model.Post

/** ViewModel 只依赖统一的数据结果，测试时可替换为假仓库；取消仍通过异常传播。 */
interface PostRepository {
    suspend fun getPosts(): NetworkResult<List<Post>>
}
