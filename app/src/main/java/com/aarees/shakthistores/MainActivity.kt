package com.aarees.shakthistores

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ShakthiGreen = Color(0xFF16834A)
private val ShakthiBackground = Color(0xFFF5F7F5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ShakthiStoresApp()
            }
        }
    }
}

@Composable
fun ShakthiStoresApp() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ShakthiBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "SHAKTHI STORES",
                color = ShakthiGreen,
                fontSize = 25.sp
            )

            Text(
                text = "OUR PRICE LIST",
                color = Color.DarkGray,
                fontSize = 19.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Native Android application",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}
