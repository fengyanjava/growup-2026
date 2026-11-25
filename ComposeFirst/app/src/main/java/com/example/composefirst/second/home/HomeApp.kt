package com.example.composefirst.second.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.result.rememberResultEventBusNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable

@Serializable
internal data object HomeDestination : NavKey

@Serializable
internal data class HomePickDestination(val articleId: Int, val initialValue: String) : NavKey

internal data class HomePickResult(val articleId: Int, val value: String)

@Composable
fun HomeApp(modifier: Modifier = Modifier) {
    HomeNavigation(modifier = modifier)
}

@Composable
internal fun HomeNavigation(
    modifier: Modifier = Modifier,
    homeContent: @Composable () -> Unit = { HomeScreenUI() },
) {
    val backStack = rememberNavBackStack(HomeDestination)
    val navigator = remember(backStack) { HomeNavigator(backStack) }
    CompositionLocalProvider(LocalHomeNavigator provides navigator) {
        NavDisplay(
            backStack = backStack,
            modifier = modifier,
            onBack = {
                if (backStack.size > 1) backStack.removeLastOrNull()
            },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                // 为每个页面提供独立的 ViewModelStoreOwner，出栈后释放。
                rememberViewModelStoreNavEntryDecorator(),
                rememberResultEventBusNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<HomeDestination> {
                    homeContent()
                }
                entry<HomePickDestination> { destination ->
                    HomePickScreen(
                        articleId = destination.articleId,
                        initialValue = destination.initialValue,
                    )
                }
            },
        )
    }
}
