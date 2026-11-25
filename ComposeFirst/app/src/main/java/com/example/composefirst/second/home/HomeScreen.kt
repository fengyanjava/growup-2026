package com.example.composefirst.second.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
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
import androidx.navigation3.runtime.result.ResultEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenUI(
    viewModel: HomeViewModel = viewModel(),
) {
    val backStack = LocalHomeNavigator.current.backStack
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // 接收模拟页回传的事件，由首页自己的 ViewModel 更新对应项。
    ResultEffect<HomePickResult> { result ->
        viewModel.onArticlePicked(result.articleId, result.value)
    }
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
                items(uiState.articles, key = { it.id }) { article ->
                    HomeItem(
                        info = article,
                        pickedValue = uiState.pickedValues[article.id],
                        onPick = {
                            if (backStack.lastOrNull() == HomeDestination) {
                                backStack.add(
                                    HomePickDestination(
                                        articleId = article.id,
                                        initialValue = uiState.pickedValues[article.id].orEmpty(),
                                    )
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun HomeItem(
    modifier: Modifier = Modifier,
    info: HomeArticleInfo,
    pickedValue: String? = null,
    onPick: () -> Unit,
) {
    Card(
        modifier = modifier.padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Text("id: ${info.id}")
            Text("userId: ${info.userId}")
            Text(info.body, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (pickedValue != null) {
                Text("返回数据：$pickedValue")
            }
            Button(onClick = onPick) {
                Text("Pick")
            }
        }
    }
}
