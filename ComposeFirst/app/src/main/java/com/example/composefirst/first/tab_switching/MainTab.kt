package com.example.composefirst.first.tab_switching

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

enum class MainTab(val title: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Home),
    Notification("Notification", Icons.Default.Notifications),
    Search("Search", Icons.Default.Search),
    Profile("Profile", Icons.Default.Person),
}
