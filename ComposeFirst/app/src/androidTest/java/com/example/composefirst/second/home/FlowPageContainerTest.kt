package com.example.composefirst.second.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.composefirst.ui.theme.ComposeFirstTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FlowPageContainerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun changingExternalStateReplacesVisiblePage() {
        val state = mutableStateOf(FlowPageContainerViewState.Loading)
        composeRule.setContent {
            ComposeFirstTheme {
                FlowPageContainer(
                    modifier = Modifier.fillMaxSize(),
                    viewState = state.value,
                    loadingContent = { Text("正在加载") },
                ) { Text("文章列表") }
            }
        }

        composeRule.onNodeWithText("正在加载").assertIsDisplayed()
        composeRule.onNodeWithText("文章列表").assertDoesNotExist()
        composeRule.runOnIdle { state.value = FlowPageContainerViewState.Content }
        composeRule.onNodeWithText("文章列表").assertIsDisplayed()
        composeRule.onNodeWithText("正在加载").assertDoesNotExist()
        composeRule.runOnIdle { state.value = FlowPageContainerViewState.Empty }
        composeRule.onNodeWithText("暂无数据").assertIsDisplayed()
        composeRule.onNodeWithText("文章列表").assertDoesNotExist()
        composeRule.runOnIdle { state.value = FlowPageContainerViewState.Error }
        composeRule.onNodeWithText("加载失败").assertIsDisplayed()
        composeRule.onNodeWithText("暂无数据").assertDoesNotExist()
        composeRule.onNodeWithText("重试").assertDoesNotExist()
    }

    @Test
    fun retryNotifiesCallerWithoutChangingPageState() {
        val state = FlowPageContainerViewState.Error
        var retryCount = 0
        composeRule.setContent {
            ComposeFirstTheme {
                FlowPageContainer(
                    modifier = Modifier.fillMaxSize(),
                    viewState = state,
                    onRetry = { retryCount++ },
                ) { Text("文章列表") }
            }
        }

        composeRule.onNodeWithText("重试").performClick()
        composeRule.runOnIdle {
            assertEquals(1, retryCount)
            assertEquals(FlowPageContainerViewState.Error, state)
        }
        composeRule.onNodeWithText("加载失败").assertIsDisplayed()
    }

    @Test
    fun customErrorReceivesRetryCallback() {
        var retryCount = 0
        composeRule.setContent {
            ComposeFirstTheme {
                FlowPageContainer(
                    modifier = Modifier.fillMaxSize(),
                    viewState = FlowPageContainerViewState.Error,
                    onRetry = { retryCount++ },
                    errorContent = { retry ->
                        Button(onClick = { retry?.invoke() }) { Text("重新连接") }
                    },
                ) { Text("文章列表") }
            }
        }

        composeRule.onNodeWithText("加载失败").assertDoesNotExist()
        composeRule.onNodeWithText("重新连接").performClick()
        composeRule.runOnIdle { assertEquals(1, retryCount) }
    }
}
