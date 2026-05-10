package com.postest.app.ui.checkout

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.postest.app.util.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(orderId: Long, nav: NavController, vm: CheckoutViewModel = hiltViewModel()) {
    LaunchedEffect(orderId) { vm.load(orderId) }
    val s by vm.state.collectAsState()
    var tendered by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("CASH") }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Checkout") },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, null) } },
        )
    }) { pad ->
        Row(Modifier.padding(pad).fillMaxSize().padding(16.dp)) {
            Column(Modifier.weight(1f).padding(end = 16.dp)) {
                Text("Order #${s.order?.id ?: "—"}", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                s.items.forEach { it ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${it.qty}× ${it.nameEn}")
                        Text(Money.fmt(it.unitPriceCents * it.qty))
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal"); Text(Money.fmt(s.order?.subtotalCents ?: 0))
                }
                if ((s.order?.taxCents ?: 0) > 0) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tax"); Text(Money.fmt(s.order?.taxCents ?: 0))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total", fontWeight = FontWeight.Bold)
                    Text(Money.fmt(s.order?.totalCents ?: 0), fontWeight = FontWeight.Bold)
                }
            }
            VerticalDivider()
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Row {
                    FilterChip(selected = method == "CASH", onClick = { method = "CASH" }, label = { Text("Cash") })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = method == "PROMPTPAY", onClick = {
                        method = "PROMPTPAY"; vm.showPromptPayQr()
                    }, label = { Text("PromptPay") })
                }
                Spacer(Modifier.height(12.dp))
                if (method == "CASH") {
                    OutlinedTextField(
                        value = tendered, onValueChange = { tendered = it },
                        label = { Text("Tendered") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    val tCents = Money.parse(tendered)
                    val totalCents = s.order?.totalCents ?: 0
                    if (tCents != null && tCents >= totalCents) {
                        Text("Change: ${Money.fmt(tCents - totalCents)}")
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        enabled = !s.paid && tCents != null && tCents >= totalCents,
                        onClick = { vm.payCash(tCents!!, andPrint = true) },
                    ) { Text("Pay & print") }
                } else {
                    Text("Scan with any banking app to pay")
                    Spacer(Modifier.height(8.dp))
                    s.qrBitmap?.let { Image(it.asImageBitmap(), null, modifier = Modifier.size(240.dp)) }
                    Spacer(Modifier.height(8.dp))
                    Button(enabled = !s.paid, onClick = { vm.payPromptPay(andPrint = true) }) {
                        Text("Mark paid & print")
                    }
                }
                if (s.paid) {
                    Spacer(Modifier.height(16.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Payment complete", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Row {
                                Button(onClick = { vm.reprintReceipt() }) { Text("Reprint") }
                                Spacer(Modifier.width(8.dp))
                                OutlinedButton(onClick = { nav.popBackStack() }) { Text("Done") }
                            }
                            if (s.printError != null) {
                                Spacer(Modifier.height(8.dp))
                                Text("Print error: ${s.printError}", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
