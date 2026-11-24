package com.example.composefirst.first.tab_switching.article

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ArticleListUiState {
    data object Loading : ArticleListUiState
    data class Success(val articles: List<Article>) : ArticleListUiState
    data object Error : ArticleListUiState
}

class ArticleListViewModel(
    private val repository: ArticleRepository = ArticleRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow<ArticleListUiState>(ArticleListUiState.Loading)
    val uiState = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        loadArticles()
    }

    fun loadArticles() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _uiState.value = ArticleListUiState.Loading
            _uiState.value = try {
                ArticleListUiState.Success(repository.getArticles())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                ArticleListUiState.Error
            }
        }
    }
}
