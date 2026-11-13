package com.example.android_basic.feature.mvvmflow.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.android_basic.core.network.NetworkResult
import com.example.android_basic.feature.mvvmflow.data.repository.PostRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PostViewModel(private val repository: PostRepository) : ViewModel() {
    // 与 LiveData 版不同：MutableLiveData 换成 MutableStateFlow；StateFlow 必须提供初始值。
    private val _uiState = MutableStateFlow<PostUiState>(PostUiState.Loading)
    // 与 LiveData 版不同：对外类型由 LiveData 换成 StateFlow，使用 asStateFlow() 暴露只读视图。
    // 这里没有复制状态；Activity 通过 uiState 收集的仍然是 _uiState 的状态。
    val uiState: StateFlow<PostUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadPosts()
    }

    fun loadPosts() {
        // 加载期间忽略重复点击，避免多个请求竞争覆盖状态。
        if (loadJob?.isActive == true) return

        loadJob = viewModelScope.launch {
            // 与 LiveData 版不同：仍用 .value 赋值，但 StateFlow 按 equals 去重；相等时不重复通知。
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
