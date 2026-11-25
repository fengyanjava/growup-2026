package com.example.composefirst.second.home

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

@Serializable
internal object HomeDestination

@Serializable
internal data class HomePickDestination(val articleId: Int, val initialValue: String)

internal const val HOME_PICK_RESULT = "home_pick_result"
internal const val HOME_PICK_ARTICLE_ID = "article_id"
internal const val HOME_PICK_VALUE = "value"

@Composable
fun HomeApp(modifier: Modifier = Modifier) {
    HomeNavigation(modifier = modifier)
}

@Composable
internal fun HomeNavigation(
    modifier: Modifier = Modifier,
    homeContent: @Composable (NavHostController, NavBackStackEntry) -> Unit = { controller, entry ->
        HomeScreenUI(navController = controller, backStackEntry = entry)
    },
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = HomeDestination,
        modifier = modifier,
    ) {
        composable<HomeDestination> { entry ->
            homeContent(navController, entry)
        }
        composable<HomePickDestination> { entry ->
            val destination = entry.toRoute<HomePickDestination>()
            HomePickScreen(
                articleId = destination.articleId,
                initialValue = destination.initialValue,
                onBack = { navController.popBackStack() },
                onConfirm = { value ->
                    // 模拟页只向上一个页面回传结果，不访问首页 ViewModel。
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        HOME_PICK_RESULT,
                        Bundle().apply {
                            putInt(HOME_PICK_ARTICLE_ID, destination.articleId)
                            putString(HOME_PICK_VALUE, value)
                        },
                    )
                    navController.popBackStack()
                },
            )
        }
    }
}
