package com.example.composefirst.first.tab_switching.article

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey

@Composable
fun ArticleListRoute(
    onBack: () -> Unit,
    onArticleSelected: (Int) -> Unit,
    viewModel: ArticleListViewModel = viewModel(),
) {
    val articles = viewModel.articles.collectAsLazyPagingItems()
    ArticleListScreen(
        articles = articles,
        onBack = onBack,
        onArticleSelected = onArticleSelected,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleListScreen(
    articles: LazyPagingItems<Article>,
    onBack: () -> Unit,
    onArticleSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val refreshState = articles.loadState.refresh
    val hasArticles = articles.itemCount > 0

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
        PullToRefreshBox(
            isRefreshing = hasArticles && refreshState is LoadState.Loading,
            onRefresh = { articles.refresh() },
            modifier = Modifier.fillMaxSize().padding(contentPadding),
        ) {
            // 空列表和错误状态也放在滚动容器中，仍可通过下拉重新加载。
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when {
                    !hasArticles && refreshState is LoadState.Loading -> {
                        item(key = "initial_loading") {
                            ArticleListLoading(Modifier.fillParentMaxSize())
                        }
                    }
                    !hasArticles && refreshState is LoadState.Error -> {
                        item(key = "initial_error") {
                            ArticleListMessage(
                                message = "文章加载失败，请检查网络后重试。",
                                actionLabel = "重试",
                                onAction = { articles.retry() },
                                modifier = Modifier.fillParentMaxSize(),
                            )
                        }
                    }
                    !hasArticles -> {
                        item(key = "empty") {
                            ArticleListMessage(
                                message = "暂无文章",
                                actionLabel = "重新加载",
                                onAction = { articles.refresh() },
                                modifier = Modifier.fillParentMaxSize(),
                            )
                        }
                    }
                    else -> {
                        item(key = "hint") {
                            Text(
                                "已加载 ${articles.itemCount} 篇，点击文章选择后返回首页",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        if (refreshState is LoadState.Error) {
                            item(key = "refresh_error") {
                                ArticleListMessage(
                                    message = "刷新失败，已保留当前文章。",
                                    actionLabel = "重试刷新",
                                    onAction = { articles.retry() },
                                )
                            }
                        }
                        items(
                            count = articles.itemCount,
                            key = articles.itemKey { it.id },
                            contentType = { "article" },
                        ) { index ->
                            // 通过下标读取会通知 Paging 接近哪一页，自动触发预加载。
                            articles[index]?.let { article ->
                                ArticleCard(article, onArticleSelected)
                            }
                        }
                        when (val appendState = articles.loadState.append) {
                            is LoadState.Loading -> {
                                item(key = "append_loading") { ArticleListLoading() }
                            }
                            is LoadState.Error -> {
                                item(key = "append_error") {
                                    ArticleListMessage(
                                        message = "更多文章加载失败。",
                                        actionLabel = "重试加载",
                                        onAction = { articles.retry() },
                                    )
                                }
                            }
                            is LoadState.NotLoading -> {
                                if (appendState.endOfPaginationReached) {
                                    item(key = "end") {
                                        Text(
                                            "没有更多文章了",
                                            modifier = Modifier.padding(16.dp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
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
private fun ArticleCard(article: Article, onArticleSelected: (Int) -> Unit) {
    Card(
        onClick = { onArticleSelected(article.id) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(article.title, style = MaterialTheme.typography.titleMedium)
            Text(
                "作者 #${article.userId} · 文章 #${article.id}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(article.body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ArticleListLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        CircularProgressIndicator()
        Text("正在加载文章…")
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
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(message)
        Button(onClick = onAction) { Text(actionLabel) }
    }
}
