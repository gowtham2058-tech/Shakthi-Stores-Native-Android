package com.aarees.shakthistores

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Green = Color(0xFF087C50)
private val PaleGreen = Color(0xFFE8F7EF)
private val Page = Color(0xFFF6F8FA)
private val Border = Color(0xFFE1E7EB)

data class Product(val name: String, val price: Double)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ShakthiDashboard()
            }
        }
    }
}

@Composable
fun ShakthiDashboard() {
    var search by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf("All") }

    val products = listOf(
        Product("GARAM- ACHI-5RS", 195.00),
        Product("LALITHA- MAIDA 1KG-PKC", 1140.00),
        Product("MUSTARD -BIG", 110.00),
        Product("JEERA PKT", 40.00),
        Product("PEPPER PKT", 90.00),
        Product("JONNALU KG", 45.00),
        Product("SAGO -SMALL*KG", 85.00),
        Product("GARLIC PASTE- 5RS*DECCAN", 54.00),
        Product("POOJA 100ML PKT", 249.00),
        Product("COVER- 6X9", 250.00),
        Product("WHEAT", 42.00),
        Product("SABJA*1KG", 245.00),
        Product("SALJEERA-100", 50.00),
        Product("CANDLE -2RS-70", 123.00),
        Product("IPPA NUNE-100ML-10", 240.00),
        Product("CANDLE -10RS -34", 220.00)
    )

    val filtered = products.filter {
        it.name.contains(search, ignoreCase = true)
    }.let {
        when (sort) {
            "A - Z" -> it.sortedBy { p -> p.name }
            "Z - A" -> it.sortedByDescending { p -> p.name }
            "Low - High" -> it.sortedBy { p -> p.price }
            "High - Low" -> it.sortedByDescending { p -> p.price }
            else -> it
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Page
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SHAKTHI STORES", color = Green,
                    fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.weight(1f))
                Text("● ONLINE", color = Green, fontSize = 11.sp)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("OUR PRICE LIST", color = Color.White,
                    modifier = Modifier
                        .background(Green, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Product Catalogue", color = Green,
                    modifier = Modifier
                        .background(PaleGreen, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    fontSize = 12.sp)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(14.dp))
                            .padding(16.dp)
                    ) {
                        Text("Shakthi Stores  >  Our Price List",
                            color = Green, fontSize = 11.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("OUR PRICE LIST",
                            fontSize = 23.sp, fontWeight = FontWeight.Bold,
                            color = Color(0xFF263238))
                        Text("Search and view the latest selling prices of all products.",
                            fontSize = 12.sp, color = Color.Gray)
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Text("⌕  Search & Filter Products",
                            fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Start typing to discover products instantly.",
                            color = Color.Gray, fontSize = 11.sp)

                        Spacer(Modifier.height(14.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("All", "A - Z", "Z - A",
                                "Low - High", "High - Low").forEach { option ->
                                FilterChip(
                                    selected = sort == option,
                                    onClick = { sort = option },
                                    label = { Text(option, fontSize = 10.sp) }
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("Search for Rice, Dal, Oil...") }
                        )

                        Spacer(Modifier.height(12.dp))
                        Text("Showing ${filtered.size} of ${products.size} Products",
                            color = Green, fontSize = 12.sp,
                            fontWeight = FontWeight.Bold)
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF0F3F5))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("PRODUCT NAME", fontSize = 11.sp,
                            fontWeight = FontWeight.Bold)
                        Text("SALERATE (₹)", fontSize = 11.sp,
                            fontWeight = FontWeight.Bold)
                    }
                }

                items(filtered) { product ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 12.dp, vertical = 17.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(product.name, fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f))
                        Text("₹${"%,.2f".format(product.price)}",
                            color = Green, fontWeight = FontWeight.Bold,
                            fontSize = 13.sp)
                    }
                    HorizontalDivider(color = Border)
                }

                item {
                    Text(
                        "Showing ${filtered.size} products",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}
