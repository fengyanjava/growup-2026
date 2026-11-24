package com.example.composefirst.first

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun StatementExample() {

//    var score by remember { mutableIntStateOf(0) }
    var score by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {

        Text("Score: $score", fontSize = 20.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {

            Button(onClick = { score++ }) {
                Text("Increase")
            }
            Button(onClick = { score-- }, enabled = score > 0) {
                Text("Decrease")
            }
        }
    }
}

@Composable
fun StateHoistingParent() {
    var score by remember { mutableIntStateOf(0) }
    CounterChild(score = score, onDecrease = {score++}, onIncrease = {score--})
}

@Composable
fun CounterChild(score: Int, onIncrease: () -> Unit, onDecrease: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {

        Text("Score: $score", fontSize = 20.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {

            Button(onClick = onIncrease) {
                Text("Increase")
            }
            Button(onClick = onDecrease, enabled = score > 0) {
                Text("Decrease")
            }
        }
    }
}

