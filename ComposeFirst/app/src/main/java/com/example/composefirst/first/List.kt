package com.example.composefirst.first

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LazyColumnExample() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(120) { index ->
            Text(
                "Item $index",
                fontSize = 20.sp,
                color = Color.White,
                modifier = Modifier
                    .background(Color.Blue, shape = RoundedCornerShape(8.dp))
                    .padding(10.dp),
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LazyRowExample() {
    LazyRow(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(120) { index ->
            Text(
                "Item $index",
                fontSize = 20.sp,
                color = Color.White,
                modifier = Modifier
                    .background(Color.Blue, shape = RoundedCornerShape(8.dp))
                    .padding(10.dp),
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun NestedListExample() {
    LazyColumn(

    ) {
        items(50) { index ->
            Text(
                "Item index=$index",
                fontSize = 24.sp,
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(20) { index ->
                    Box(
                        modifier = Modifier
                            .height(50.dp)
                            .background(Color.Yellow), contentAlignment = Alignment.Center
                    ) {
                        Text("box $index")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TypesOfItem() {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        LazyColumn(

        ) {
            item {
                Text("Header")
            }
            item {
                Text("Header2")
            }
            item {
                Text("Header3")
            }
        }

        LazyColumn(
        ) {
            items(count = 20) { index ->
                Text("item $index")
            }
        }

        val names = listOf("111", "222", "333", "444")
        LazyColumn() {
            itemsIndexed(names) { index, name ->
                Text("$index = $name")
            }
        }
    }
}



