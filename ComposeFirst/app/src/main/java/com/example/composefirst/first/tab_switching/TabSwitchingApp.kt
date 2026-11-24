package com.example.composefirst.first.tab_switching

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.composefirst.first.tab_switching.article.ArticleListRoute
import kotlinx.serialization.Serializable

@Serializable
internal object MainTabsDestination

@Serializable
internal object ArticleListDestination

@Composable
fun TabSwitchingApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    // Tab 之间仍由 selectedTab 切换，只有进入子页面才向返回栈添加条目。
    NavHost(
        navController = navController,
        startDestination = MainTabsDestination,
        modifier = modifier,
    ) {
        composable<MainTabsDestination> {
            TabSwitchingScreen(
                onOpenArticles = {
                    navController.navigate(ArticleListDestination) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable<ArticleListDestination> {
            ArticleListRoute(onBack = { navController.popBackStack() })
        }
    }
}
