package com.example.composefirst.second.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenUI() {
    val viewModel = viewModel<HomeViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Hello")}) }
    ) { padding ->

        FlowPageContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            viewState = uiState.pageState,
            onRetry = viewModel::loadData,
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.articles.size) {
                    HomeItem(info = uiState.articles[it])
                }
            }
        }
    }
}

@Composable
fun HomeItem(modifier: Modifier = Modifier, info: HomeArticleInfo) {
    Card(
        modifier = modifier.padding(horizontal = 16.dp)
    ) {
        Column(modifier = modifier.padding(15.dp)) {
            Text("id: ${info.id}")
            Text("userId: ${info.userId}")
            Text(info.body, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}
