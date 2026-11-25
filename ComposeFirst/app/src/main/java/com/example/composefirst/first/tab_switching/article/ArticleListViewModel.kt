package com.example.composefirst.first.tab_switching.article

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn

class ArticleListViewModel(
    repository: ArticleRepository = ArticleRepository(),
) : ViewModel() {
    // UI 收集时开始加载；同一导航条目内共享分页数据，避免重组时重新请求。
    val articles = repository.getPagedArticles().cachedIn(viewModelScope)
}
