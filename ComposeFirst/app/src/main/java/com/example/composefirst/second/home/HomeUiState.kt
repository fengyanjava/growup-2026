package com.example.composefirst.second.home

data class HomeUiState(
    val pageState: FlowPageContainerViewState = FlowPageContainerViewState.Loading,
    val articles: List<HomeArticleInfo> = emptyList(),
    val pickedValues: Map<Int, String> = emptyMap(),
)
