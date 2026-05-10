package com.postest.app.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.postest.app.auth.SessionManager
import com.postest.app.data.entity.PosMode
import com.postest.app.data.entity.SettingRow
import com.postest.app.data.entity.UserRole
import com.postest.app.data.repo.SettingsRepo
import com.postest.app.print.BtEscPosPrinter
import com.postest.app.print.PrinterService
import com.postest.app.print.PrinterTarget
import com.postest.app.ui.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BtChoice(val name: String, val mac: String)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepo: SettingsRepo,
    private val printer: PrinterService,
    private val bt: BtEscPosPrinter,
    val session: SessionManager,
) : ViewModel() {

    val settings: StateFlow<SettingRow> = settingsRepo.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingRow())

    fun update(transform: (SettingRow) -> SettingRow) =
        viewModelScope.launch { settingsRepo.update(transform) }

    fun pairedBt(): List<BtChoice> = bt.pairedDevices().map { d ->
        BtChoice(name = runCatching { d.name }.getOrNull() ?: d.address, mac = d.address)
    }

    fun testReceipt() = viewModelScope.launch {
        val s = settingsRepo.get()
        val target = when (s.receiptPrinterTarget) {
            "STAR" -> PrinterTarget.Lan(s.starPrinterIp)
            "BT" -> PrinterTarget.Bt(s.btPrinterMac)
            else -> PrinterTarget.None
        }
        printer.testPrint(target)
    }

    fun testKitchen() = viewModelScope.launch {
        val s = settingsRepo.get()
        val target = when (s.kitchenPrinterTarget) {
            "STAR" -> PrinterTarget.Lan(s.starPrinterIp)
            "BT" -> PrinterTarget.Bt(s.btPrinterMac)
            else -> PrinterTarget.None
        }
        printer.testPrint(target)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(nav: NavController, vm: SettingsViewModel = hiltViewModel()) {
    val s by vm.settings.collectAsState()
    val current by vm.session.current.collectAsState()
    val isOwner = current?.role == UserRole.OWNER
    val isManagerOrOwner = current?.role == UserRole.OWNER || current?.role == UserRole.MANAGER

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, null) } },
        )
    }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState())) {

            SectionTitle("General")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Mode:")
                Spacer(Modifier.width(8.dp))
                FilterChip(selected = s.mode == PosMode.RETAIL, onClick = { vm.update { it.copy(mode = PosMode.RETAIL) } }, label = { Text("Retail") })
                Spacer(Modifier.width(8.dp))
                FilterChip(selected = s.mode == PosMode.RESTAURANT, onClick = { vm.update { it.copy(mode = PosMode.RESTAURANT) } }, label = { Text("Restaurant") })
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Language:"); Spacer(Modifier.width(8.dp))
                FilterChip(selected = s.language == "en", onClick = {
                    vm.update { it.copy(language = "en") }
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                }, label = { Text("English") })
                Spacer(Modifier.width(8.dp))
                FilterChip(selected = s.language == "th", onClick = {
                    vm.update { it.copy(language = "th") }
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("th"))
                }, label = { Text("ไทย") })
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = s.shopName, onValueChange = { v -> vm.update { it.copy(shopName = v) } },
                label = { Text("Shop name") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = s.shopAddress, onValueChange = { v -> vm.update { it.copy(shopAddress = v) } },
                label = { Text("Shop address") }, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row {
                OutlinedTextField(
                    value = s.taxPct.toString(), onValueChange = { v -> vm.update { it.copy(taxPct = v.toDoubleOrNull() ?: 0.0) } },
                    label = { Text("Tax %") }, singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = s.idleLockMinutes.toString(),
                    onValueChange = { v -> vm.update { it.copy(idleLockMinutes = v.toIntOrNull() ?: 5) } },
                    label = { Text("Auto-lock (min)") }, singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = s.promptPayId, onValueChange = { v -> vm.update { it.copy(promptPayId = v) } },
                label = { Text("PromptPay ID (mobile or NID)") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            SectionTitle("Printers")
            OutlinedTextField(
                value = s.starPrinterIp, onValueChange = { v -> vm.update { it.copy(starPrinterIp = v) } },
                label = { Text("Star LAN printer IP") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text("Bluetooth printer (paired devices):")
            val paired = remember { vm.pairedBt() }
            paired.forEach { dev ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = s.btPrinterMac == dev.mac,
                        onClick = { vm.update { it.copy(btPrinterMac = dev.mac) } })
                    Text("${dev.name}  (${dev.mac})")
                }
            }
            if (paired.isEmpty()) Text("No paired Bluetooth devices found", style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(8.dp))
            Text("Receipt printer:")
            Row {
                FilterChip(selected = s.receiptPrinterTarget == "STAR", onClick = { vm.update { it.copy(receiptPrinterTarget = "STAR") } }, label = { Text("Star LAN") })
                Spacer(Modifier.width(8.dp))
                FilterChip(selected = s.receiptPrinterTarget == "BT", onClick = { vm.update { it.copy(receiptPrinterTarget = "BT") } }, label = { Text("Bluetooth") })
                Spacer(Modifier.width(8.dp))
                FilterChip(selected = s.receiptPrinterTarget == "NONE", onClick = { vm.update { it.copy(receiptPrinterTarget = "NONE") } }, label = { Text("None") })
            }
            Text("Kitchen printer:")
            Row {
                FilterChip(selected = s.kitchenPrinterTarget == "STAR", onClick = { vm.update { it.copy(kitchenPrinterTarget = "STAR") } }, label = { Text("Star LAN") })
                Spacer(Modifier.width(8.dp))
                FilterChip(selected = s.kitchenPrinterTarget == "BT", onClick = { vm.update { it.copy(kitchenPrinterTarget = "BT") } }, label = { Text("Bluetooth") })
                Spacer(Modifier.width(8.dp))
                FilterChip(selected = s.kitchenPrinterTarget == "NONE", onClick = { vm.update { it.copy(kitchenPrinterTarget = "NONE") } }, label = { Text("None") })
            }
            Spacer(Modifier.height(8.dp))
            Row {
                Button(onClick = { vm.testReceipt() }) { Text("Test receipt printer") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { vm.testKitchen() }) { Text("Test kitchen printer") }
            }

            if (isManagerOrOwner) {
                Spacer(Modifier.height(16.dp))
                SectionTitle("Admin")
                if (isOwner) {
                    OutlinedButton(onClick = { nav.navigate(Routes.Users) }) { Text("Users") }
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(onClick = { nav.navigate(Routes.Audit) }) { Text("Audit log") }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
}
