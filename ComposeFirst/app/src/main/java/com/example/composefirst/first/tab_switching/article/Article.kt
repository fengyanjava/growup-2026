package com.example.composefirst.first.tab_switching.article

import kotlinx.serialization.Serializable

@Serializable
data class Article(
    val id: Int,
    val userId: Int,
    val title: String,
    val body: String,
)
