package com.postest.app.ui.checkout

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.postest.app.ui.common.Responsive
import com.postest.app.util.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(orderId: Long, nav: NavController, vm: CheckoutViewModel = hiltViewModel()) {
    LaunchedEffect(orderId) { vm.load(orderId) }
    val s by vm.state.collectAsState()
    var tendered by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("CASH") }
    val compact = Responsive.isCompact

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Checkout") },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, null) } },
        )
    }) { pad ->
        if (compact) {
            Column(Modifier.padding(pad).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
                OrderSummaryPane(s)
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                PaymentPane(
                    s, method,
                    onMethodChange = { method = it; if (it == "PROMPTPAY") vm.showPromptPayQr() },
                    tendered = tendered, onTenderedChange = { tendered = it },
                    onPayCash = { c -> vm.payCash(c, andPrint = true) },
                    onPayPromptPay = { vm.payPromptPay(andPrint = true) },
                    onReprint = { vm.reprintReceipt() },
                    onDone = { nav.popBackStack() },
                )
            }
        } else {
            Row(Modifier.padding(pad).fillMaxSize().padding(16.dp)) {
                Column(Modifier.weight(1f).padding(end = 16.dp).verticalScroll(rememberScrollState())) {
                    OrderSummaryPane(s)
                }
                VerticalDivider()
                Column(Modifier.weight(1f).padding(start = 16.dp).verticalScroll(rememberScrollState())) {
                    PaymentPane(
                        s, method,
                        onMethodChange = { method = it; if (it == "PROMPTPAY") vm.showPromptPayQr() },
                        tendered = tendered, onTenderedChange = { tendered = it },
                        onPayCash = { c -> vm.payCash(c, andPrint = true) },
                        onPayPromptPay = { vm.payPromptPay(andPrint = true) },
                        onReprint = { vm.reprintReceipt() },
                        onDone = { nav.popBackStack() },
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderSummaryPane(s: CheckoutState) {
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

@Composable
private fun PaymentPane(
    s: CheckoutState,
    method: String,
    onMethodChange: (String) -> Unit,
    tendered: String,
    onTenderedChange: (String) -> Unit,
    onPayCash: (Long) -> Unit,
    onPayPromptPay: () -> Unit,
    onReprint: () -> Unit,
    onDone: () -> Unit,
) {
    Row {
        FilterChip(selected = method == "CASH", onClick = { onMethodChange("CASH") }, label = { Text("Cash") })
        Spacer(Modifier.width(8.dp))
        FilterChip(selected = method == "PROMPTPAY", onClick = { onMethodChange("PROMPTPAY") }, label = { Text("PromptPay") })
    }
    Spacer(Modifier.height(12.dp))
    if (method == "CASH") {
        OutlinedTextField(
            value = tendered, onValueChange = onTenderedChange,
            label = { Text("Tendered") }, singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        val tCents = Money.parse(tendered)
        val totalCents = s.order?.totalCents ?: 0
        if (tCents != null && tCents >= totalCents) Text("Change: ${Money.fmt(tCents - totalCents)}")
        Spacer(Modifier.height(12.dp))
        Button(
            enabled = !s.paid && tCents != null && tCents >= totalCents,
            onClick = { onPayCash(tCents!!) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Pay & print") }
    } else {
        Text("Scan with any banking app to pay")
        Spacer(Modifier.height(8.dp))
        s.qrBitmap?.let {
            Image(it.asImageBitmap(), null, modifier = Modifier.sizeIn(maxWidth = 280.dp, maxHeight = 280.dp))
        }
        Spacer(Modifier.height(8.dp))
        Button(enabled = !s.paid, onClick = onPayPromptPay, modifier = Modifier.fillMaxWidth()) {
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
                    Button(onClick = onReprint) { Text("Reprint") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = onDone) { Text("Done") }
                }
                if (s.printError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text("Print error: ${s.printError}", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
