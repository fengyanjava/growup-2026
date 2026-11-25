package com.example.composefirst.second.home

import kotlinx.serialization.Serializable

@Serializable
class HomeArticleInfo(
    val id: Int,
    val userId: Int,
    val body: String,
)