package com.example.composefirst.first.scaffold

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun AppTopBar() {
    TopAppBar(
        title = { Text("Hi Compose", fontWeight = FontWeight.SemiBold) },
        actions = {
            IconButton(onClick = {}) {
                Icon(imageVector = Icons.Default.AddCircle, contentDescription = null)
            }
            IconButton(onClick = {}) {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Magenta,
            titleContentColor = Color.White,
            actionIconContentColor = Color.White
        ),
    )
}