package com.example.composefirst.second.home

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    var activityResult by rememberSaveable { mutableStateOf<String?>(null) }
    // 始终在页面组合时注册，点击按钮时再启动 Activity。
    val activityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringExtra(ActivityResultDemoActivity.EXTRA_RESULT_VALUE)?.let {
                activityResult = it
            }
        }
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // 接收模拟页回传的事件，由首页自己的 ViewModel 更新对应项。
    ResultEffect<HomePickResult> { result ->
        viewModel.onArticlePicked(result.articleId, result.value)
    }
    val listState = rememberLazyListState()
    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Hello")}) }
    ) { padding ->

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Activity 跳转与结果回传")
                    Text("Activity 返回：${activityResult ?: "暂无"}")
                    Button(onClick = {
                        val intent = Intent(context, ActivityResultDemoActivity::class.java).apply {
                            putExtra(
                                ActivityResultDemoActivity.EXTRA_INITIAL_VALUE,
                                activityResult ?: "来自首页的参数",
                            )
                        }
                        activityLauncher.launch(intent)
                    }) {
                        Text("打开 Activity 获取结果")
                    }
                }
            }
            FlowPageContainer(
                modifier = Modifier.weight(1f).fillMaxWidth(),
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
