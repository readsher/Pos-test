package com.postest.app.ui.shift

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.postest.app.auth.SessionManager
import com.postest.app.data.entity.Shift
import com.postest.app.data.repo.AuditRepo
import com.postest.app.data.repo.SettingsRepo
import com.postest.app.data.repo.ShiftRepo
import com.postest.app.data.repo.ShiftSummary
import com.postest.app.print.PrinterService
import com.postest.app.print.ReceiptRenderer
import com.postest.app.print.receiptPrinterTarget
import com.postest.app.util.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ShiftViewModel @Inject constructor(
    private val shiftRepo: ShiftRepo,
    private val session: SessionManager,
    private val audit: AuditRepo,
    private val settingsRepo: SettingsRepo,
    private val printer: PrinterService,
    private val renderer: ReceiptRenderer,
) : ViewModel() {

    val current: StateFlow<Shift?> = shiftRepo.observeCurrent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val all: StateFlow<List<Shift>> = shiftRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _summary = kotlinx.coroutines.flow.MutableStateFlow<ShiftSummary?>(null)
    val summary: StateFlow<ShiftSummary?> = _summary

    fun refreshSummary() = viewModelScope.launch {
        val cur = shiftRepo.current() ?: run { _summary.value = null; return@launch }
        _summary.value = shiftRepo.summarize(cur)
    }

    fun openShift(openingCashCents: Long) = viewModelScope.launch {
        val user = session.current.value ?: return@launch
        shiftRepo.open(openingCashCents, user.id)
        audit.log(user.id, "OPEN_SHIFT", "shift", null, """{"openingCashCents":$openingCashCents}""")
        refreshSummary()
    }

    fun closeShift(countedCashCents: Long, andPrint: Boolean) = viewModelScope.launch {
        val user = session.current.value ?: return@launch
        val closed = shiftRepo.close(countedCashCents, user.id)
        audit.log(user.id, "CLOSE_SHIFT", "shift", closed.id, """{"countedCashCents":$countedCashCents}""")
        val summary = shiftRepo.summarize(closed)
        if (andPrint) {
            val s = settingsRepo.get()
            val bmp = renderer.renderShiftReport(summary, s, s.language, "Z-REPORT")
            printer.print(bmp, s.receiptPrinterTarget())
        }
        _summary.value = null
    }

    fun printX() = viewModelScope.launch {
        val cur = shiftRepo.current() ?: return@launch
        val s = settingsRepo.get()
        val sum = shiftRepo.summarize(cur)
        val bmp = renderer.renderShiftReport(sum, s, s.language, "X-REPORT")
        printer.print(bmp, s.receiptPrinterTarget())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftScreen(nav: NavController, vm: ShiftViewModel = hiltViewModel()) {
    val cur by vm.current.collectAsState()
    val all by vm.all.collectAsState()
    val summary by vm.summary.collectAsState()

    var openingText by remember { mutableStateOf("0") }
    var countedText by remember { mutableStateOf("0") }

    LaunchedEffect(cur?.id) { vm.refreshSummary() }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Shift") },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, null) } },
        )
    }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).fillMaxSize()) {
            if (cur == null) {
                Text("No shift open", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = openingText, onValueChange = { openingText = it },
                    label = { Text("Opening cash (THB)") }, singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    val cents = Money.parse(openingText) ?: 0L
                    vm.openShift(cents)
                }) { Text("Open shift") }
            } else {
                Text("Shift open since ${SimpleDateFormat("HH:mm", Locale.US).format(Date(cur!!.openedAt))}",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                summary?.let { sum ->
                    Card { Column(Modifier.padding(12.dp)) {
                        Text("Sales: ${Money.fmt(sum.totalSalesCents)}  ·  Tx: ${sum.txCount}")
                        Text("Opening cash: ${Money.fmt(sum.shift.openingCashCents)}")
                        Text("Expected cash: ${Money.fmt(sum.expectedCashCents)}")
                    } }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = countedText, onValueChange = { countedText = it },
                    label = { Text("Counted cash (THB)") }, singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    Button(onClick = { vm.printX() }) { Text("Print X-report") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val cents = Money.parse(countedText) ?: 0L
                        vm.closeShift(cents, andPrint = true)
                    }) { Text("Close shift & print Z-report") }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("History", style = MaterialTheme.typography.titleMedium)
            LazyColumn {
                items(all) { sh ->
                    ListItem(
                        headlineContent = { Text("Shift #${sh.id}") },
                        supportingContent = {
                            val opened = SimpleDateFormat("MM/dd HH:mm", Locale.US).format(Date(sh.openedAt))
                            val closed = sh.closedAt?.let { SimpleDateFormat("MM/dd HH:mm", Locale.US).format(Date(it)) } ?: "OPEN"
                            Text("$opened → $closed")
                        },
                    )
                }
            }
        }
    }
}
