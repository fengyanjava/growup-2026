package com.example.composefirst.second.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.example.composefirst.ui.theme.ComposeFirstTheme

class ActivityResultDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialValue = intent.getStringExtra(EXTRA_INITIAL_VALUE).orEmpty()
        setContent {
            ComposeFirstTheme {
                ActivityResultDemoScreen(
                    initialValue = initialValue,
                    onCancel = { finish() },
                    onConfirm = { value ->
                        // 先设置返回结果，再结束当前 Activity。
                        setResult(RESULT_OK, Intent().putExtra(EXTRA_RESULT_VALUE, value))
                        finish()
                    },
                )
            }
        }
    }

    companion object {
        const val EXTRA_INITIAL_VALUE = "initial_value"
        const val EXTRA_RESULT_VALUE = "result_value"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityResultDemoScreen(
    initialValue: String,
    onCancel: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by rememberSaveable { mutableStateOf(initialValue) }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Activity 返回参数示例") },
                navigationIcon = {
                    TextButton(onClick = onCancel) { Text("返回") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("首页传入：$initialValue")
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("返回给首页的参数") },
                singleLine = true,
            )
            Button(
                onClick = { onConfirm(value.trim()) },
                enabled = value.isNotBlank(),
            ) { Text("确认并返回结果") }
            TextButton(onClick = onCancel) { Text("取消") }
        }
    }
}
