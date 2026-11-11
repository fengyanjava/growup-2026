package com.example.android_basic.feature.mvvm.ui

import com.example.android_basic.feature.mvvm.data.model.Post

/** 一次只展示一种状态，避免 loading、空态和错误提示同时出现。 */
sealed interface PostUiState {
    data object Loading : PostUiState
    data class Success(val posts: List<Post>) : PostUiState
    data object Empty : PostUiState
    data class Error(val reason: PostError) : PostUiState
}

enum class PostError {
    NETWORK,
    SERVER,
    UNKNOWN,
}
