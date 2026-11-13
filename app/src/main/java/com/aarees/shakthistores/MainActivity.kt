package com.aarees.shakthistores

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.Locale

private val Brand = Color(0xFF087C50)
private val BrandDark = Color(0xFF075C3D)
private val Mint = Color(0xFFE8F6EF)
private val Canvas = Color(0xFFF5F7F6)
private val Ink = Color(0xFF17231D)
private val Muted = Color(0xFF718078)
private val Line = Color(0xFFE3EAE6)
private const val CSV_URL = "https://aareeshyd.s3.ap-south-1.amazonaws.com/SaktheItems.csv"

private data class StoreProduct(val name: String, val price: Double, val unit: String = "")
private enum class Screen { PRICE, CATALOGUE }
private enum class PriceFilter(val title: String) {
    ALL("All prices"), UNDER50("Below ₹50"), FROM50TO100("₹50–₹100"), FROM100TO500("₹100–₹500"),
    FROM500TO1000("₹500–₹1,000"), OVER1000("Above ₹1,000"), ATLEAST("At least"), ATMOST("At most"),
    ABOVE("Above"), BELOW("Below"), RANGE("Custom range")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = lightColorScheme(primary = Brand, background = Canvas)) { ShakthiNativeApp() } }
    }
}

private fun csvFields(line: String): List<String> {
    val out = mutableListOf<String>(); val cell = StringBuilder(); var quoted = false; var i = 0
    while (i < line.length) {
        val c = line[i]
        when {
            c == '"' && quoted && i + 1 < line.length && line[i + 1] == '"' -> { cell.append('"'); i++ }
            c == '"' -> quoted = !quoted
            c == ',' && !quoted -> { out.add(cell.toString().trim()); cell.clear() }
            else -> cell.append(c)
        }; i++
    }; out.add(cell.toString().trim()); return out
}

private suspend fun loadCatalogue(): List<StoreProduct> = withContext(Dispatchers.IO) {
    val c = URL(CSV_URL).openConnection(); c.connectTimeout = 15000; c.readTimeout = 20000
    val lines = c.getInputStream().bufferedReader(Charsets.UTF_8).use { it.readLines() }
    require(lines.isNotEmpty()) { "The S3 catalogue is empty" }
    val headers = csvFields(lines.first()).map { it.trim().removePrefix("\uFEFF").lowercase(Locale.ROOT) }
    val ni = headers.indexOf("product name"); val pi = headers.indexOf("sale rate")
    require(ni >= 0 && pi >= 0) { "CSV needs Product Name and Sale Rate columns" }
    lines.drop(1).mapNotNull { row ->
        val f = csvFields(row); if (f.size <= maxOf(ni, pi)) null else {
            val n = f[ni].trim(); val p = f[pi].replace(",", "").trim().toDoubleOrNull()
            if (n.isNotBlank() && p != null) StoreProduct(n, p) else null
        }
    }
}

@Composable private fun ShakthiNativeApp() {
    val context = LocalContext.current
    var screen by remember { mutableStateOf(Screen.PRICE) }
    var products by remember { mutableStateOf(emptyList<StoreProduct>()) }
    var loading by remember { mutableStateOf(true) }; var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }; var sort by remember { mutableStateOf("All") }
    var priceFilter by remember { mutableStateOf(PriceFilter.ALL) }; var amount by remember { mutableStateOf("") }
    var min by remember { mutableStateOf("") }; var max by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(1) }; var pageSize by remember { mutableIntStateOf(50) }
    var showFilters by remember { mutableStateOf(true) }; var showActions by remember { mutableStateOf(false) }
    LaunchedEffect(refresh) { loading = true; error = null; try { products = loadCatalogue() } catch (e: Exception) { error = e.message ?: "Unable to sync catalogue" } finally { loading = false } }
    val filtered = remember(products, search, sort, priceFilter, amount, min, max) {
        var list = products.filter { it.name.contains(search, true) }
        val v = amount.toDoubleOrNull(); val lo = min.toDoubleOrNull(); val hi = max.toDoubleOrNull()
        list = when (priceFilter) {
            PriceFilter.UNDER50 -> list.filter { it.price < 50 }
            PriceFilter.FROM50TO100 -> list.filter { it.price >= 50 && it.price <= 100 }
            PriceFilter.FROM100TO500 -> list.filter { it.price >= 100 && it.price <= 500 }
            PriceFilter.FROM500TO1000 -> list.filter { it.price >= 500 && it.price <= 1000 }
            PriceFilter.OVER1000 -> list.filter { it.price > 1000 }
            PriceFilter.ATLEAST -> if (v != null) list.filter { it.price >= v } else list
            PriceFilter.ATMOST -> if (v != null) list.filter { it.price <= v } else list
            PriceFilter.ABOVE -> if (v != null) list.filter { it.price > v } else list
            PriceFilter.BELOW -> if (v != null) list.filter { it.price < v } else list
            PriceFilter.RANGE -> list.filter { (lo == null || it.price >= lo) && (hi == null || it.price <= hi) }
            else -> list
        }
        when (sort) { "A–Z" -> list.sortedBy { it.name.lowercase() }; "Z–A" -> list.sortedByDescending { it.name.lowercase() }; "Low–High" -> list.sortedBy { it.price }; "High–Low" -> list.sortedByDescending { it.price }; else -> list }
    }
    LaunchedEffect(search, sort, priceFilter, amount, min, max, screen) { page = 1 }
    val pages = maxOf(1, (filtered.size + pageSize - 1) / pageSize)
    val safePage = page.coerceIn(1, pages)
    val shown = if (pageSize == -1) filtered else filtered.drop((safePage - 1) * pageSize).take(pageSize)
    Surface(Modifier.fillMaxSize(), color = Canvas) {
        Column {
            Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.shakthi_logo), "Shakthi Stores", Modifier.width(142.dp).height(48.dp), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
                Spacer(Modifier.weight(1f)); Surface(color = Mint, shape = RoundedCornerShape(30.dp)) { Text(if (loading) "SYNCING" else if (error == null) "● LIVE" else "● OFFLINE", Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = Brand, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            }
            Row(Modifier.fillMaxWidth().background(Color.White).horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NavPill("OUR PRICE LIST", screen == Screen.PRICE) { screen = Screen.PRICE }
                NavPill("Product Catalogue", screen == Screen.CATALOGUE) { screen = Screen.CATALOGUE }
            }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(18.dp)) {
                    Text("SHAKTHI STORES  /  ${if (screen == Screen.PRICE) "OUR PRICE LIST" else "PRODUCT CATALOGUE"}", color = Brand, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp)); Text(if (screen == Screen.PRICE) "OUR PRICE LIST" else "PRODUCT CATALOGUE", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Find products and view their latest selling prices.", color = Muted, fontSize = 12.sp)
                } }
                item { Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Search & Filter Products", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold); Text("Search the live store catalogue", color = Muted, fontSize = 11.sp) }; IconButton(onClick = { showFilters = !showFilters }) { Icon(if (showFilters) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = Brand) } }
                    OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Search products by name…") }, leadingIcon = { Icon(Icons.Default.Search, null) }, trailingIcon = { if (search.isNotEmpty()) IconButton(onClick = { search = "" }) { Icon(Icons.Default.Close, null) } }, shape = RoundedCornerShape(12.dp))
                    AnimatedVisibility(showFilters, enter = fadeIn(), exit = fadeOut()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("SORT PRODUCTS", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("All", "A–Z", "Z–A", "Low–High", "High–Low").forEach { label -> FilterChip(sort == label, { sort = label }, label = { Text(label, fontSize = 11.sp) }) } }
                            Text("PRICE FILTER", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            var menu by remember { mutableStateOf(false) }
                            Box { OutlinedButton(onClick = { menu = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) { Text(priceFilter.title, Modifier.weight(1f)); Icon(Icons.Default.ArrowDropDown, null) }; DropdownMenu(menu, { menu = false }) { PriceFilter.values().forEach { opt -> DropdownMenuItem(text = { Text(opt.title) }, onClick = { priceFilter = opt; menu = false }) } } }
                            if (priceFilter in listOf(PriceFilter.ATLEAST, PriceFilter.ATMOST, PriceFilter.ABOVE, PriceFilter.BELOW)) OutlinedTextField(amount, { amount = it }, Modifier.fillMaxWidth(), label = { Text("Amount (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                            if (priceFilter == PriceFilter.RANGE) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(min, { min = it }, Modifier.weight(1f), label = { Text("Minimum ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true); OutlinedTextField(max, { max = it }, Modifier.weight(1f), label = { Text("Maximum ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true) }
                            Row(verticalAlignment = Alignment.CenterVertically) { Text("${filtered.size} products found", color = Brand, fontSize = 12.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); TextButton(onClick = { search = ""; sort = "All"; priceFilter = PriceFilter.ALL; amount = ""; min = ""; max = "" }) { Text("Reset filters") } }
                        }
                    }
                } }
                item { if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Brand) else if (error != null) Row(verticalAlignment = Alignment.CenterVertically) { Text("Sync failed: $error", Modifier.weight(1f), color = Color.Red, fontSize = 11.sp); TextButton(onClick = { refresh++ }) { Text("Retry") } } else Row(verticalAlignment = Alignment.CenterVertically) { Text("Catalogue updated from S3", color = Brand, fontSize = 11.sp); Spacer(Modifier.weight(1f)); TextButton(onClick = { refresh++ }) { Icon(Icons.Default.Refresh, null); Text("Refresh") } } }
                item { Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)).background(Color(0xFFEAF1ED)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Text("PRODUCT NAME", Modifier.weight(1f), color = Ink, fontSize = 10.sp, fontWeight = FontWeight.Bold); Text("SALE RATE", color = Ink, fontSize = 10.sp, fontWeight = FontWeight.Bold) } }
                items(shown) { product -> ProductRow(product) }
                item { if (!loading && error == null) Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).padding(14.dp)) {
                    Text("Showing ${if (filtered.isEmpty()) 0 else (safePage - 1) * if (pageSize == -1) filtered.size else pageSize + 1}–${if (pageSize == -1) filtered.size else minOf(safePage * pageSize, filtered.size)} of ${filtered.size} products", color = Muted, fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Text("Rows", color = Muted, fontSize = 11.sp); Spacer(Modifier.width(6.dp)); listOf(25, 50, 100, -1).forEach { n -> TextButton(onClick = { pageSize = n; page = 1 }, contentPadding = PaddingValues(horizontal = 7.dp)) { Text(if (n == -1) "All" else "$n", color = if (pageSize == n) Brand else Muted, fontWeight = if (pageSize == n) FontWeight.Bold else FontWeight.Normal) } }; Spacer(Modifier.weight(1f)); IconButton(onClick = { page = (safePage - 1).coerceAtLeast(1) }, enabled = safePage > 1) { Icon(Icons.Default.ChevronLeft, null) }; Text("$safePage / $pages", fontSize = 11.sp, color = Ink); IconButton(onClick = { page = (safePage + 1).coerceAtMost(pages) }, enabled = safePage < pages) { Icon(Icons.Default.ChevronRight, null) } }
                } }
            }
            Box(Modifier.fillMaxWidth().background(Color.White).padding(8.dp), contentAlignment = Alignment.CenterEnd) {
                if (showActions) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { TextButton(onClick = { val csv = buildString { append("Product Name,Sale Rate\n"); filtered.forEach { append("\"").append(it.name.replace("\"", "\"\"")).append("\",").append(it.price).append("\n") } }; val send = Intent(Intent.ACTION_SEND).setType("text/csv").putExtra(Intent.EXTRA_TEXT, csv); context.startActivity(Intent.createChooser(send, "Share price list")) }) { Icon(Icons.Default.FileDownload, null); Text("Export CSV") }; TextButton(onClick = { refresh++ }) { Icon(Icons.Default.Refresh, null); Text("Refresh") } }
                FloatingActionButton(onClick = { showActions = !showActions }, containerColor = Brand, contentColor = Color.White, shape = CircleShape) { Icon(if (showActions) Icons.Default.Close else Icons.Default.Bolt, "Actions") }
            }
        }
    }
}

@Composable private fun NavPill(label: String, selected: Boolean, onClick: () -> Unit) { Surface(onClick = onClick, color = if (selected) Brand else Mint, shape = RoundedCornerShape(24.dp)) { Text(label, Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = if (selected) Color.White else Brand, fontSize = 11.sp, fontWeight = FontWeight.Bold) } }

@Composable private fun ProductRow(p: StoreProduct) {
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth().padding(bottom = 1.dp).background(Color.White).padding(horizontal = 12.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(p.name, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis); if (p.unit.isNotBlank()) Text(p.unit, color = Muted, fontSize = 10.sp) }
        Text("₹" + String.format(Locale.US, "%,.2f", p.price), color = Brand, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        IconButton(onClick = { val clip = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager; clip.setPrimaryClip(android.content.ClipData.newPlainText("Product", "${p.name} - ₹${p.price}")) }, modifier = Modifier.size(34.dp)) { Icon(Icons.Default.ContentCopy, "Copy product and price", tint = Muted, modifier = Modifier.size(16.dp)) }
    }
    HorizontalDivider(color = Line, thickness = 0.7.dp)
}
