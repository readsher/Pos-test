package com.postest.app.ui.report

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.postest.app.data.repo.ReportRepo
import com.postest.app.data.repo.SalesReport
import com.postest.app.util.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val repo: ReportRepo,
) : ViewModel() {
    private val _report = MutableStateFlow<SalesReport?>(null)
    val report: StateFlow<SalesReport?> = _report.asStateFlow()

    fun loadToday() = viewModelScope.launch {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val from = cal.timeInMillis
        val to = from + 24L * 3600_000L
        _report.value = repo.build(from, to)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(nav: NavController, vm: ReportViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) { vm.loadToday() }
    val r by vm.report.collectAsState()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Reports — Today") },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, null) } },
        )
    }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState())) {
            if (r == null) { Text("Loading…"); return@Column }
            val rep = r!!
            Text("Total sales: ${Money.fmt(rep.totalCents)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Text("Transactions: ${rep.txCount}")

            Spacer(Modifier.height(12.dp))
            SectionTitle("By payment method")
            for ((m, v) in rep.byMethod) Text("$m: ${Money.fmt(v)}")

            Spacer(Modifier.height(12.dp))
            SectionTitle("By hour")
            for ((h, v) in rep.byHour.toSortedMap()) Text("%02d:00  %s".format(h, Money.fmt(v)))

            Spacer(Modifier.height(12.dp))
            SectionTitle("By category")
            for ((c, v) in rep.byCategory) Text("Category #$c: ${Money.fmt(v)}")

            Spacer(Modifier.height(12.dp))
            SectionTitle("By user")
            for ((u, v) in rep.byUser) Text("User #$u: ${Money.fmt(v)}")
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(4.dp))
}
