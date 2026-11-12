package com.example.android_basic.feature.mvvm.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.android_basic.core.network.NetworkResult
import com.example.android_basic.feature.mvvm.data.repository.PostRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PostViewModel(private val repository: PostRepository) : ViewModel() {
    private val _uiState = MutableLiveData<PostUiState>(PostUiState.Loading)
    // 只向 View 暴露不可修改的 LiveData，不使用 Flow。
    val uiState: LiveData<PostUiState> = _uiState

    private var loadJob: Job? = null

    init {
        loadPosts()
    }

    fun loadPosts() {
        // 加载期间忽略重复点击，避免多个请求竞争覆盖状态。
        if (loadJob?.isActive == true) return

        loadJob = viewModelScope.launch {
            _uiState.value = PostUiState.Loading
            // Repository 统一处理请求异常，这里只将数据结果转换为页面状态。
            _uiState.value = when (val result = repository.getPosts()) {
                is NetworkResult.Success -> {
                    val posts = result.data
                    if (posts.isEmpty()) {
                        PostUiState.Empty
                    } else {
                        PostUiState.Success(posts)
                    }
                }
                is NetworkResult.Failure -> PostUiState.Error(result.error)
            }
        }
    }

    class Factory(private val repository: PostRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(PostViewModel::class.java)) {
                "Unknown ViewModel: ${modelClass.name}"
            }
            @Suppress("UNCHECKED_CAST")
            return PostViewModel(repository) as T
        }
    }
}
