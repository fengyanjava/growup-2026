package com.example.composefirst.second.home

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

internal class HomeNavigator(
    val backStack: NavBackStack<NavKey>,
)

internal val LocalHomeNavigator = staticCompositionLocalOf<HomeNavigator> {
    error("HomeNavigator must be provided by HomeNavigation")
}
