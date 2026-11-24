package com.example.composefirst.first.scaffold

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun AppBottomBar() {
    BottomAppBar(
        containerColor = Color.White,
        contentColor = Color.Blue,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            IconButton(onClick = {}) {
                Icon(imageVector = Icons.Default.Person, contentDescription = null , modifier = Modifier.size(20.dp) )
            }
            IconButton(onClick = {}) {
                Icon(imageVector = Icons.Default.Person, contentDescription = null , modifier = Modifier.size(20.dp) )
            }
            IconButton(onClick = {}) {
                Icon(imageVector = Icons.Default.Person, contentDescription = null , modifier = Modifier.size(20.dp) )
            }
            IconButton(onClick = {}) {
                Icon(imageVector = Icons.Default.Person, contentDescription = null , modifier = Modifier.size(20.dp) )
            }
        }
    }
}