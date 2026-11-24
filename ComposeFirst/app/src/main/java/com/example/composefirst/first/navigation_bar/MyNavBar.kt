package com.example.composefirst.first.navigation_bar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController

@Composable
fun MyNavBar(navController: NavHostController, key: String) {
    val items = listOf(
        NavItem("Home", Icons.Default.Home, NavBarRoutes.Home),
        NavItem("Notification", Icons.Default.Notifications, NavBarRoutes.Notification),
        NavItem("Search", Icons.Default.Search, NavBarRoutes.Search),
        NavItem("Profile", Icons.Default.Person, NavBarRoutes.Profile),
    )
    NavigationBar() {
        items.forEach { item ->
            NavigationBarItem(
                selected = key == item.title,
                onClick = {
                    navController.navigate(item.route)
                },
                icon = {
                    Icon(
                        item.icon, contentDescription = null
                    )
                },
                label = { Text(item.title) },

                )
        }
    }
}

data class NavItem(
    val title: String,
    val icon: ImageVector,
    val route: NavBarRoutes,
)