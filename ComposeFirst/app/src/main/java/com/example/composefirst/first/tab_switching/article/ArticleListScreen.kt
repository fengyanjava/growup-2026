package com.example.composefirst.first.tab_switching.article

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ArticleListRoute(
    onBack: () -> Unit,
    viewModel: ArticleListViewModel = viewModel(),
) {
    // ViewModel 归属于文章页的导航条目，重组或旋转屏幕不会重复请求。
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ArticleListScreen(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::loadArticles,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleListScreen(
    uiState: ArticleListUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("文章列表") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { contentPadding ->
        Box(Modifier.fillMaxSize().padding(contentPadding)) {
            when (uiState) {
                ArticleListUiState.Loading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CircularProgressIndicator()
                        Text("正在加载文章…")
                    }
                }
                ArticleListUiState.Error -> {
                    ArticleListMessage(
                        message = "文章加载失败，请检查网络后重试。",
                        actionLabel = "重试",
                        onAction = onRetry,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                is ArticleListUiState.Success -> {
                    if (uiState.articles.isEmpty()) {
                        ArticleListMessage(
                            message = "暂无文章",
                            actionLabel = "重新加载",
                            onAction = onRetry,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(uiState.articles, key = { it.id }) { article ->
                                Card(Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            article.title,
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                        Text(
                                            "作者 #${article.userId} · 文章 #${article.id}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(article.body, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticleListMessage(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(message)
        Button(onClick = onAction) { Text(actionLabel) }
    }
}
