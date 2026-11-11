package com.example.android_basic.feature.mvvm.data.repository

import com.example.android_basic.feature.mvvm.data.model.Post

/** ViewModel 只依赖数据入口，测试时可替换为假仓库。 */
interface PostRepository {
    suspend fun getPosts(): List<Post>
}
