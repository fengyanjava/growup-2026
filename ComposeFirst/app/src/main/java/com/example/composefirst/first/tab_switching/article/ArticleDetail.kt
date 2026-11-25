package com.example.composefirst.first.tab_switching.article

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

@Composable
fun ArticleDetail(id: String, modifier: Modifier = Modifier) {
//    val list by rememberSaveable { mutableStateOf(mutableListOf<String>()) }
    var list by rememberSaveable { mutableStateOf(listOf<String>()) }
    var name by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(id) {
        delay(100)
        name = "newName $id"
        list = list + "Hello"
    }

    Column() {
        Text(name)
        Text(list.joinToString(","))
//        HorizontalPager() { }
//        AnimatedVisibility() { }
    }
}