package com.example.composefirst.second.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.composefirst.ui.theme.ComposeFirstTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pickingAndCancellingOnlyUpdateTheCorrespondingItem() {
        var loadCount = 0
        val factory = createFactory { loadCount++ }
        composeRule.setContent {
            TestHomeNavigation(factory)
        }

        pickArticle(1)
        composeRule.onNodeWithText("当前文章 id：1").assertIsDisplayed()
        composeRule.onNodeWithText("返回数据").performTextReplacement("第一项结果")
        composeRule.onNodeWithText("确认并返回").performClick()
        assertItemResult(1, "第一项结果")

        pickArticle(2)
        composeRule.onNodeWithText("返回数据").performTextReplacement("不应保存")
        composeRule.onNodeWithText("取消").performClick()
        composeRule.onNodeWithText("返回数据：不应保存").assertDoesNotExist()
        assertItemResult(1, "第一项结果")

        pickArticle(2)
        composeRule.onNodeWithText("返回数据").performTextReplacement("第二项结果")
        composeRule.onNodeWithText("确认并返回").performClick()
        assertItemResult(2, "第二项结果")
        assertItemResult(1, "第一项结果")

        pickArticle(1)
        composeRule.onNodeWithText("第一项结果").assertIsDisplayed()
        composeRule.onNodeWithText("返回数据").performTextReplacement("修改后的结果")
        composeRule.onNodeWithText("确认并返回").performClick()
        assertItemResult(1, "修改后的结果")
        assertItemResult(2, "第二项结果")
        composeRule.runOnIdle { assertEquals(1, loadCount) }
    }

    @Test
    fun restoringStateRetainsPickerRouteAndDraft() {
        val restorationTester = StateRestorationTester(composeRule)
        val factory = createFactory()
        restorationTester.setContent { TestHomeNavigation(factory) }

        pickArticle(2)
        composeRule.onNodeWithText("返回数据").performTextReplacement("尚未提交的结果")
        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("当前文章 id：2").assertIsDisplayed()
        composeRule.onNodeWithText("尚未提交的结果").assertIsDisplayed()
        composeRule.onNodeWithText("确认并返回").performClick()
        assertItemResult(2, "尚未提交的结果")
        composeRule.onNodeWithText("id: 1").performScrollTo().assertIsDisplayed()
    }

    @Composable
    private fun TestHomeNavigation(factory: ViewModelProvider.Factory) {
        ComposeFirstTheme {
            HomeNavigation(
                homeContent = {
                    HomeScreenUI(
                        viewModel = viewModel(factory = factory),
                    )
                },
            )
        }
    }

    private fun createFactory(onLoad: () -> Unit = {}): ViewModelProvider.Factory {
        val repository = HomeRepository(object : HomeApi {
            override suspend fun getArticles(offset: Int, limit: Int): List<HomeArticleInfo> {
                onLoad()
                return listOf(
                    HomeArticleInfo(1, 1, "第一篇文章"),
                    HomeArticleInfo(2, 1, "第二篇文章"),
                )
            }
        })
        return viewModelFactory { initializer { HomeViewModel(repository) } }
    }

    private fun pickArticle(id: Int) {
        composeRule.onNode(
            hasText("Pick") and hasAnySibling(hasText("id: $id"))
        ).performScrollTo().performClick()
    }

    private fun assertItemResult(id: Int, value: String) {
        composeRule.onNode(
            hasText("返回数据：$value") and hasAnySibling(hasText("id: $id"))
        ).performScrollTo().assertIsDisplayed()
    }
}
