package com.example.composefirst.first.tab_switching

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.composefirst.ui.theme.ComposeFirstTheme

@Composable
fun TabSwitchingScreen(modifier: Modifier = Modifier) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.Home) }
    val stateHolder = rememberSaveableStateHolder()

    // 公共底栏只创建一份，点击时只更新 selectedTab，不向返回栈添加页面。
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            TabBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
            )
        },
    ) { contentPadding ->
        Box(Modifier.fillMaxSize().padding(contentPadding)) {
            // 按 Tab 分别保存 rememberSaveable 状态，页面无需一直留在 Composition 中。
            stateHolder.SaveableStateProvider(key = selectedTab.name) {
                when (selectedTab) {
                    MainTab.Home -> HomeContent()
                    MainTab.Notification -> NotificationContent()
                    MainTab.Search -> SearchContent()
                    MainTab.Profile -> ProfileContent()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TabSwitchingScreenPreview() {
    ComposeFirstTheme {
        TabSwitchingScreen()
    }
}
