package com.example.composefirst.first.scaffold

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MyScreen() {
    Scaffold(
        topBar = { AppTopBar() },
        bottomBar = {AppBottomBar()}
    ) { padding ->
//        Column(
//            modifier = Modifier.padding(padding)
//                .padding(horizontal = 16.dp, vertical = 12.dp),
//            verticalArrangement = Arrangement.spacedBy(10.dp)
//        ) {
//
//            MyScreenContent()
//            MyScreenContent()
//            MyScreenContent()
//            MyScreenContent()
//
//        }

        LazyColumn (
            modifier = Modifier.padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(modifier = Modifier.height(0.dp)) }
            items(20) {
                MyScreenContent()
            }

        }
    }
}