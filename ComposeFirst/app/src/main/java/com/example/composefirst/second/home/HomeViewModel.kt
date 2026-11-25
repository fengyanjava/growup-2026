package com.example.composefirst.second.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val api = HomeRepository()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        loadData()
    }

    fun loadData() {
        if (loadJob?.isActive == true) return

        loadJob = viewModelScope.launch {
            _uiState.value = HomeUiState()
            try {
                val articles = api.loadList()
                _uiState.value = HomeUiState(
                    pageState = if (articles.isEmpty()) {
                        FlowPageContainerViewState.Empty
                    } else {
                        FlowPageContainerViewState.Content
                    },
                    articles = articles,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.value = HomeUiState(pageState = FlowPageContainerViewState.Error)
            }
        }
    }
}
