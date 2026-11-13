package com.example.android_basic.feature.mvvmflow.data.model

/** 与 /posts 返回的字段对应；简单示例共用网络模型和展示模型。 */
data class Post(
    val userId: Int,
    val id: Int,
    val title: String,
    val body: String,
)
