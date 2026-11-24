package com.example.composefirst.first.tab_switching

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun HomeContent() {
    var count by rememberSaveable { mutableIntStateOf(0) }

    TabPage(title = "Home Screen") {
        Text("计数：$count")
        Button(onClick = { count++ }) {
            Text("加一")
        }
        Text("增加计数后切换 Tab，再回来查看。")
    }
}

@Composable
fun NotificationContent() {
    var enabled by rememberSaveable { mutableStateOf(true) }

    TabPage(title = "Notification Screen") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("接收通知")
            Switch(
                checked = enabled,
                onCheckedChange = { enabled = it },
                modifier = Modifier.semantics { contentDescription = "接收通知" },
            )
        }
        Text("切换 Tab 后，开关保持原来的选择。")
    }
}

@Composable
fun SearchContent() {
    var query by rememberSaveable { mutableStateOf("") }

    TabPage(title = "Search Screen") {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("搜索关键词") },
            singleLine = true,
        )
        Text("输入关键词后切换 Tab，再回来查看。")
    }
}

@Composable
fun ProfileContent() {
    var nickname by rememberSaveable { mutableStateOf("") }

    TabPage(title = "Profile Screen") {
        OutlinedTextField(
            value = nickname,
            onValueChange = { nickname = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("昵称") },
            singleLine = true,
        )
        Text("昵称和搜索关键词分别保存在各自的 Tab 中。")
    }
}

@Composable
private fun TabPage(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        content()
    }
}
