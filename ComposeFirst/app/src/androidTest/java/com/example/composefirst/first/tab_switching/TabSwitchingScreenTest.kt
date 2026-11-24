package com.example.composefirst.first.tab_switching

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.composefirst.ui.theme.ComposeFirstTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TabSwitchingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun switchingTabsRetainsEachTabsState() {
        composeRule.setContent {
            ComposeFirstTheme { TabSwitchingScreen() }
        }

        composeRule.onNodeWithText("加一").performClick()
        composeRule.onNodeWithText("Notification").performClick()
        composeRule.onNodeWithContentDescription("接收通知").performClick()
        composeRule.onNodeWithText("Search").performClick()
        composeRule.onNodeWithText("搜索关键词").performTextInput("Compose")
        composeRule.onNodeWithText("Profile").performClick()
        composeRule.onNodeWithText("昵称").performTextInput("Alice")

        composeRule.onNodeWithText("Home").performClick()
        composeRule.onNodeWithText("计数：1").assertIsDisplayed()
        composeRule.onNodeWithText("Notification").performClick()
        composeRule.onNodeWithContentDescription("接收通知").assertIsOff()
        composeRule.onNodeWithText("Search").performClick()
        composeRule.onNodeWithText("搜索关键词").assertTextContains("Compose")
        composeRule.onNodeWithText("Profile").performClick()
        composeRule.onNodeWithText("昵称").assertTextContains("Alice")
        composeRule.onNodeWithText("Profile").performClick()
        composeRule.onNodeWithText("昵称").assertTextContains("Alice")
    }

    @Test
    fun restoringStateRestoresSelectedTabAndInactiveTabState() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            ComposeFirstTheme { TabSwitchingScreen() }
        }

        composeRule.onNodeWithText("加一").performClick()
        composeRule.onNodeWithText("Search").performClick()
        composeRule.onNodeWithText("搜索关键词").performTextInput("Kotlin")
        composeRule.onNodeWithText("Profile").performClick()
        composeRule.onNodeWithText("昵称").performTextInput("Bob")

        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("Profile").assertIsSelected()
        composeRule.onNodeWithText("昵称").assertTextContains("Bob")
        composeRule.onNodeWithText("Home").performClick()
        composeRule.onNodeWithText("计数：1").assertIsDisplayed()
        composeRule.onNodeWithText("Search").performClick()
        composeRule.onNodeWithText("搜索关键词").assertTextContains("Kotlin")
    }
}
