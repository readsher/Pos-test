package com.postest.app.ui.pos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.postest.app.data.entity.OrderItem
import com.postest.app.data.entity.PosMode
import com.postest.app.ui.Routes
import com.postest.app.ui.common.Responsive
import com.postest.app.util.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(nav: NavController, vm: PosViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()

    val savedHandle = nav.currentBackStackEntry?.savedStateHandle
    LaunchedEffect(Unit) {
        savedHandle?.getStateFlow<Long?>("resume_order_id", null)?.collect { id ->
            if (id != null) { vm.resumeOrder(id); savedHandle["resume_order_id"] = null }
        }
    }

    var showCartSheet by remember { mutableStateOf(false) }
    val compact = Responsive.isCompact

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PosTest — ${if (s.mode == PosMode.RESTAURANT) "Restaurant" else "Retail"}") },
                actions = {
                    AssistChip(
                        onClick = { nav.navigate(Routes.Shift) },
                        label = { Text(if (s.openShift != null) "Shift open" else "Open shift") },
                    )
                    IconButton(onClick = { nav.navigate(Routes.Menu) })     { Icon(Icons.Filled.MenuBook,    null) }
                    IconButton(onClick = { nav.navigate(Routes.Report) })   { Icon(Icons.Filled.PointOfSale, null) }
                    IconButton(onClick = { nav.navigate(Routes.Settings) }) { Icon(Icons.Filled.Settings,    null) }
                },
            )
        },
        floatingActionButton = {
            if (compact && s.cart.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showCartSheet = true },
                    icon = { Icon(Icons.Filled.ShoppingCart, null) },
                    text = { Text("${s.cart.sumOf { it.qty }}  ·  ${Money.fmt(s.subtotalCents)}") },
                )
            }
        },
    ) { pad ->
        if (compact) {
            Column(Modifier.padding(pad).fillMaxSize()) {
                CatalogPane(s, vm, nav, modifier = Modifier.fillMaxSize().padding(12.dp))
            }
            if (showCartSheet) {
                ModalBottomSheet(onDismissRequest = { showCartSheet = false }) {
                    CartPane(s, vm, nav, onCheckoutNav = { showCartSheet = false }, modifier = Modifier.fillMaxWidth().padding(16.dp))
                }
            }
        } else {
            Row(Modifier.padding(pad).fillMaxSize()) {
                CatalogPane(s, vm, nav, modifier = Modifier.weight(2f).padding(12.dp))
                VerticalDivider()
                CartPane(s, vm, nav, modifier = Modifier.weight(1f).padding(12.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogPane(s: PosUiState, vm: PosViewModel, nav: NavController, modifier: Modifier = Modifier) {
    Column(modifier) {
        if (s.mode == PosMode.RESTAURANT) {
            Button(onClick = { nav.navigate(Routes.Tables) }) { Text("Tables") }
            Spacer(Modifier.height(8.dp))
        }
        if (s.openShift == null) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(12.dp)) {
                    Text("No open shift — open one to start selling", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { nav.navigate(Routes.Shift) }) { Text("Open shift") }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        ScrollableTabRow(
            selectedTabIndex = (s.categories.indexOfFirst { it.id == s.selectedCategoryId }).coerceAtLeast(0),
        ) {
            s.categories.forEach { c ->
                Tab(
                    selected = c.id == s.selectedCategoryId,
                    onClick = { vm.selectCategory(c.id) },
                    text = { Text(c.nameEn) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        // GridCells.Adaptive lets the grid reflow on any width.
        LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 130.dp), modifier = Modifier.fillMaxSize()) {
            items(s.products) { p ->
                ElevatedCard(
                    modifier = Modifier.padding(6.dp).heightIn(min = 100.dp),
                    onClick = { if (s.openShift != null) vm.addProduct(p) },
                ) {
                    Column(
                        Modifier.fillMaxSize().padding(8.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(p.nameEn, fontWeight = FontWeight.SemiBold, maxLines = 2)
                        Text(Money.fmt(p.priceCents))
                    }
                }
            }
        }
    }
}

@Composable
private fun CartPane(
    s: PosUiState,
    vm: PosViewModel,
    nav: NavController,
    modifier: Modifier = Modifier,
    onCheckoutNav: () -> Unit = {},
) {
    Column(modifier.fillMaxHeight()) {
        Text("Cart", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (s.cart.isEmpty()) {
            Text("Cart is empty", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(Modifier.weight(1f, fill = false).heightIn(max = 600.dp)) {
                items(s.cart) { item -> CartRow(item, vm) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Subtotal", fontWeight = FontWeight.SemiBold)
            Text(Money.fmt(s.subtotalCents), fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
        Row {
            OutlinedButton(onClick = { vm.clearOrder() }, enabled = s.cart.isNotEmpty()) { Text("Clear") }
            Spacer(Modifier.width(8.dp))
            Button(
                enabled = s.cart.isNotEmpty() && s.orderId != null,
                onClick = {
                    s.orderId?.let { nav.navigate(Routes.checkout(it)) }
                    onCheckoutNav()
                },
            ) { Text("Checkout") }
        }
        if (s.mode == PosMode.RESTAURANT && s.orderId != null &&
            s.cart.any { !it.sentToKitchen && it.kitchenPrint }) {
            Spacer(Modifier.height(8.dp))
            Button(onClick = { vm.sendToKitchen() }) { Text("Send to kitchen") }
        }
    }
}

@Composable
private fun CartRow(item: OrderItem, vm: PosViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(item.nameEn, fontWeight = FontWeight.SemiBold)
            Text("${item.qty} × ${Money.fmt(item.unitPriceCents)}", style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = { vm.changeQty(item, -1) }) { Text("−") }
        Text(item.qty.toString())
        IconButton(onClick = { vm.changeQty(item, +1) }) { Text("+") }
    }
    HorizontalDivider()
}
