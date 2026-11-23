package com.example.composefirst.first

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

//@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ButtonExample() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val context = LocalContext.current
        Button(
            onClick = {
                Toast.makeText(context, "Click me ~~~", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                contentColor = Color.Red,
                containerColor = Color.Green
            ),
            border = BorderStroke(2.dp, Color.Blue)
        ) {
            Row {
//                Icon(imageVector = Icon.Default.Person, contentDescription = null),
                Text("Click me", color = Color.Yellow)
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginScreen() {
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var pwd by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Login Here", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = email, onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(), label = { Text("Email") },
        )
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = pwd, onValueChange = { pwd = it },
            modifier = Modifier.fillMaxWidth(), label = { Text("Password") },
        )
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                Toast.makeText(context, "Click me ~~~", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 15.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                contentColor = Color.Red,
                containerColor = Color.Blue,
                disabledContentColor = Color.Gray,
                disabledContainerColor = Color.Yellow,
            ),
//            border = BorderStroke(2.dp, Color.Blue),
            enabled = email.isNotEmpty() && pwd.isNotEmpty(),
        ) {
            Row {
//                Icon(imageVector = Icon.Default.Person, contentDescription = null),
                Text("Click me")
            }
        }
    }
}