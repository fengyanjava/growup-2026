package com.example.composefirst.first

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Preview(showBackground = true, showSystemUi = false)
@Composable
fun ModifierExample() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.Yellow)
            .padding(vertical = 10.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color.Green, shape = RoundedCornerShape(size = 6.dp))
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            Text(
                text = "Text1", fontSize = 20.sp
            )
            Text(
                text = "Text1", fontSize = 20.sp
            )
            Text(
                text = "Text1", fontSize = 20.sp
            )
        }

        Box(
            modifier = Modifier
                .background(color = Color.Magenta, shape = RoundedCornerShape(size = 12.dp))
                .height(150.dp)
                .width(150.dp)
                .clickable(onClick = {})
                .border(width = 3.dp, color = Color.Red, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Center", fontSize = 20.sp, color = Color.White,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color.LightGray, shape = CircleShape)
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            Text(
                text = "One", fontSize = 20.sp
            )
            Text(
                text = "Two", fontSize = 20.sp
            )
            Text(
                text = "Three", fontSize = 20.sp
            )
        }
    }
}