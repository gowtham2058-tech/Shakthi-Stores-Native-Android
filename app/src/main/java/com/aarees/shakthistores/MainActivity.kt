
package com.aarees.shakthistores

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.Locale

private val Green = Color(0xFF087C50)
private val PaleGreen = Color(0xFFE8F7EF)
private val Page = Color(0xFFF6F8FA)
private val Border = Color(0xFFE1E7EB)

private const val CATALOGUE_URL =
    "https://aareeshyd.s3.ap-south-1.amazonaws.com/SaktheItems.csv"

data class Product(
    val name: String,
    val price: Double
)

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

private fun parseCsvLine(line: String): List<String> {
    val result = mutableListOf<String>()
    val field = StringBuilder()
    var quoted = false
    var i = 0

    while (i < line.length) {
        val c = line[i]

        when {
            c == '"' && quoted &&
                i + 1 < line.length &&
                line[i + 1] == '"' -> {
                field.append('"')
                i++
            }
            c == '"' -> quoted = !quoted
            c == ',' && !quoted -> {
                result.add(field.toString().trim())
                field.clear()
            }
            else -> field.append(c)
        }
        i++
    }

    result.add(field.toString().trim())
    return result
}

private suspend fun fetchProducts(): List<Product> =
    withContext(Dispatchers.IO) {
        val connection = URL(CATALOGUE_URL).openConnection()
        connection.connectTimeout = 15000
        connection.readTimeout = 20000

        val rows = connection.getInputStream()
            .bufferedReader(Charsets.UTF_8)
            .use { it.readLines() }

        require(rows.isNotEmpty()) {
            "The S3 catalogue is empty"
        }

        val headers = parseCsvLine(rows.first())
            .map {
                it.trim()
                    .removePrefix("\uFEFF")
                    .lowercase(Locale.ROOT)
            }

        val nameIndex = headers.indexOf("product name")
        val priceIndex = headers.indexOf("sale rate")

        require(nameIndex >= 0 && priceIndex >= 0) {
            "The CSV must contain Product Name and Sale Rate columns"
        }

        rows.drop(1).mapNotNull { line ->
            val columns = parseCsvLine(line)

            if (columns.size <= maxOf(nameIndex, priceIndex)) {
                null
            } else {
                val name = columns[nameIndex].trim()
                val price = columns[priceIndex]
                    .trim()
                    .replace(",", "")
                    .toDoubleOrNull()

                if (name.isNotBlank() && price != null) {
                    Product(name, price)
                } else {
                    null
                }
            }
        }
    }

@Composable
fun ShakthiDashboard() {
    var search by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf("All") }

    var products by remember {
        mutableStateOf<List<Product>>(emptyList())
    }

    var loading by remember { mutableStateOf(true) }
    var syncError by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshKey) {
        loading = true
        syncError = null

        try {
            products = fetchProducts()
        } catch (e: Exception) {
            syncError = e.message ?: "Unable to connect to S3"
        } finally {
            loading = false
        }
    }

    val filtered = products
        .filter { it.name.contains(search, ignoreCase = true) }
        .let {
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
                Image(
          painter = painterResource(id = R.drawable.shakthi_logo),
          contentDescription = "Shakthi Stores Logo",
          modifier = Modifier
           .width(150.dp)
           .height(55.dp),
         contentScale = ContentScale.Fit
)

                Spacer(Modifier.weight(1f))

                Text(
                    if (syncError == null && !loading) "● ONLINE"
                    else "● SYNC",
                    color = Green,
                    fontSize = 11.sp
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "OUR PRICE LIST",
                    color = Color.White,
                    modifier = Modifier
                        .background(Green, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Product Catalogue",
                    color = Green,
                    modifier = Modifier
                        .background(PaleGreen, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    fontSize = 12.sp
                )
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
                            .background(
                                Color.White,
                                RoundedCornerShape(14.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            "Shakthi Stores  >  Our Price List",
                            color = Green,
                            fontSize = 11.sp
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "OUR PRICE LIST",
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF263238)
                        )

                        Text(
                            "Search and view the latest selling prices of all products.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color.White,
                                RoundedCornerShape(14.dp)
                            )
                            .padding(14.dp)
                    ) {
                        Text(
                            "⌕  Search & Filter Products",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Text(
                            "Find products and view their current prices.",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )

                        Spacer(Modifier.height(12.dp))

                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                "All",
                                "A - Z",
                                "Z - A",
                                "Low - High",
                                "High - Low"
                            ).forEach { option ->
                                FilterChip(
                                    selected = sort == option,
                                    onClick = { sort = option },
                                    label = {
                                        Text(option, fontSize = 9.sp)
                                    }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = {
                                Text("Search products...")
                            }
                        )

                        Spacer(Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Showing ${filtered.size} products",
                                color = Green,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.weight(1f))

                            TextButton(
                                onClick = {
                                    refreshKey++
                                }
                            ) {
                                Text("↻ Refresh", color = Green)
                            }
                        }
                    }
                }

                item {
                    when {
                        loading -> Text(
                            "Loading live product catalogue...",
                            color = Green,
                            modifier = Modifier.padding(12.dp)
                        )

                        syncError != null -> Text(
                            "Catalogue sync failed: $syncError",
                            color = Color.Red,
                            modifier = Modifier.padding(12.dp)
                        )

                        else -> Text(
                            "Live S3 catalogue connected",
                            color = Green,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEFF3F4))
                            .padding(12.dp),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {
                        Text(
                            "PRODUCT NAME",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "SALERATE (₹)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                items(filtered) { product ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            product.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            "₹" + String.format(
                                Locale.US,
                                "%,.2f",
                                product.price
                            ),
                            color = Green,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    HorizontalDivider(color = Border)
                }

                item {
                    if (!loading && syncError == null) {
                        Text(
                            "End of product catalogue",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}
