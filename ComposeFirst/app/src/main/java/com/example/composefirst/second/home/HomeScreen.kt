package com.example.composefirst.second.home

import android.os.Bundle
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenUI(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    viewModel: HomeViewModel = viewModel(viewModelStoreOwner = backStackEntry),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val resultFlow = remember(backStackEntry) {
        backStackEntry.savedStateHandle.getStateFlow<Bundle?>(HOME_PICK_RESULT, null)
    }
    LaunchedEffect(backStackEntry, viewModel) {
        backStackEntry.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            resultFlow.filterNotNull().collect { result ->
                val value = result.getString(HOME_PICK_VALUE)
                if (result.containsKey(HOME_PICK_ARTICLE_ID) && value != null) {
                    viewModel.onArticlePicked(result.getInt(HOME_PICK_ARTICLE_ID), value)
                }
                // 消费后置空，保留 StateFlow 订阅，以便接收下一次结果。
                backStackEntry.savedStateHandle.set<Bundle?>(HOME_PICK_RESULT, null)
            }
        }
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
                            navController.navigate(
                                HomePickDestination(
                                    articleId = article.id,
                                    initialValue = uiState.pickedValues[article.id].orEmpty(),
                                )
                            ) {
                                launchSingleTop = true
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
