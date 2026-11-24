package com.example.composefirst.first.tab_switching

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.composefirst.first.tab_switching.article.ArticleListRoute
import kotlinx.serialization.Serializable

@Serializable
internal object MainTabsDestination

@Serializable
internal object ArticleListDestination

private const val SELECTED_ARTICLE_ID = "selected_article_id"

@Composable
fun TabSwitchingApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    // Tab 之间仍由 selectedTab 切换，只有进入子页面才向返回栈添加条目。
    NavHost(
        navController = navController,
        startDestination = MainTabsDestination,
        modifier = modifier,
    ) {
        composable<MainTabsDestination> { backStackEntry ->
            // 观察启动页面自己的结果。作为选择状态保留，后续选择会覆盖此值。
            val selectedArticleIdFlow = remember(backStackEntry) {
                backStackEntry.savedStateHandle.getStateFlow<Int?>(SELECTED_ARTICLE_ID, null)
            }
            val selectedArticleId by selectedArticleIdFlow.collectAsStateWithLifecycle()
            TabSwitchingScreen(
                selectedArticleId = selectedArticleId,
                onOpenArticles = {
                    navController.navigate(ArticleListDestination) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable<ArticleListDestination> {
            ArticleListRoute(
                onBack = { navController.popBackStack() },
                onArticleSelected = { articleId ->
                    // 先把结果写给上一个导航条目，再弹出当前文章页。
                    navController.previousBackStackEntry?.savedStateHandle
                        ?.set(SELECTED_ARTICLE_ID, articleId)
                    navController.popBackStack()
                },
            )
        }
    }
}
