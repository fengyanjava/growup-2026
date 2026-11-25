package com.example.composefirst.second.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


/** 展示上层提供的页面状态；容器只发送重试事件，不发起请求或修改状态。 */
@Composable
fun FlowPageContainer(
    modifier: Modifier = Modifier,
    viewState: FlowPageContainerViewState,
    onRetry: (() -> Unit)? = null,
    loadingContent: @Composable () -> Unit = { CircularProgressIndicator() },
    emptyContent: @Composable () -> Unit = { Text("暂无数据") },
    errorContent: @Composable ((() -> Unit)?) -> Unit = { DefaultPageError(it) },
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = if (viewState == FlowPageContainerViewState.Content) {
            Alignment.TopStart
        } else {
            Alignment.Center
        },
    ) {
        when (viewState) {
            FlowPageContainerViewState.Loading -> loadingContent()
            FlowPageContainerViewState.Empty -> emptyContent()
            FlowPageContainerViewState.Error -> errorContent(onRetry)
            FlowPageContainerViewState.Content -> content()
        }
    }
}

@Composable
private fun DefaultPageError(onRetry: (() -> Unit)?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("加载失败")
        if (onRetry != null) {
            Button(onClick = onRetry) { Text("重试") }
        }
    }
}

enum class FlowPageContainerViewState {
    Content, Loading, Empty, Error,
}
