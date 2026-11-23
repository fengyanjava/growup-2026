package com.example.composefirst.first

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.composefirst.R

@Preview
@Composable
private fun ImageExample() {
    Image(
        painter = painterResource(R.drawable.test_pic),
        contentDescription = null,
        modifier = Modifier.padding(20.dp).clip(RoundedCornerShape(14.dp)),
    )
}