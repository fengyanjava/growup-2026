package com.example.composefirst.second.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePickScreen(
    articleId: Int,
    initialValue: String,
    onBack: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by rememberSaveable(articleId, initialValue) {
        mutableStateOf(initialValue.ifEmpty { "模拟数据 $articleId" })
    }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("模拟页面") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("返回") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("当前文章 id：$articleId")
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("返回数据") },
                singleLine = true,
            )
            Button(
                onClick = { onConfirm(value.trim()) },
                enabled = value.isNotBlank(),
            ) { Text("确认并返回") }
            TextButton(onClick = onBack) { Text("取消") }
        }
    }
}
