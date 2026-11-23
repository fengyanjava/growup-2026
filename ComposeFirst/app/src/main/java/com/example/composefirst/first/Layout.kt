package com.example.composefirst.first

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp

@Preview(showBackground = true)
@Composable
fun RowExample() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Text("Apple", fontSize = 30.sp)
        Text("Grape", fontSize = 30.sp)
        Text("Banana", fontSize = 30.sp)
    }
}

@Preview
@Composable
private fun ColumnExample() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Apple", fontSize = 30.sp)
        Text("Grape Grape Grape Grape Grape Grape Grape Grape Grape Grape ", fontSize = 30.sp)
        Text("Banana", fontSize = 30.sp)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ColumnExample02() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Login Here", fontSize = 30.sp)
        OutlinedTextField(
            value = "",
            onValueChange = {},
            placeholder = {
                Text("Please enter your name")
            }
        )
        OutlinedTextField(
            value = "",
            onValueChange = {},
            placeholder = {
                Text("Please enter your password")
            }
        )
    }
}

@Preview
@Composable
private fun BoxExample() {
    Box(
        modifier = Modifier.fillMaxSize(),
//        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Note 1",
            fontSize = 30.sp,
            modifier = Modifier.align(Alignment.BottomEnd)
        )

        Text(
            "Note 2", fontSize = 30.sp,
            modifier = Modifier.align(Alignment.TopEnd)
        )

        Text(
            "Note 3", fontSize = 30.sp,
            modifier = Modifier.align(Alignment.CenterStart)
        )
    }
}