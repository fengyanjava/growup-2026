package com.example.composefirst.first

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Preview(showBackground = true, showSystemUi = false)
@Composable
fun TextExample() {
    Text(
        text = "你好，Hello Text 2026，Hello Text 2026",
        modifier = Modifier.padding(top = 50.dp),
        color = Color.Red,
        fontSize = 30.sp,
        fontWeight = FontWeight.Thin,
        textAlign = TextAlign.Center,
    )
}

@Preview(showBackground = true)
@Composable
fun TextFieldExample() {
    val name = remember { mutableStateOf("Jason") }
    TextField(
        value = name.value,
        onValueChange = { name.value = it },
        label = {
            Text(text = "输入名称")
        },
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Yellow,
            unfocusedContainerColor = Color.Cyan,
            focusedTextColor = Color.Blue,
            unfocusedTextColor = Color.Red,
        )
    )
}

@Preview(showBackground = true)
@Composable
fun OutlinedTextFieldExample() {
    val name = remember { mutableStateOf("Jason") }
    OutlinedTextField(
        value = name.value,
        onValueChange = { name.value = it },
        label = {
            Text(text = "输入名称")
        },
        shape = CircleShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.Yellow,
            unfocusedContainerColor = Color.Cyan,
            focusedTextColor = Color.Blue,
            unfocusedTextColor = Color.Red,
        )
    )
}