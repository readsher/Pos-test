package com.postest.app.ui.tables

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.postest.app.auth.SessionManager
import com.postest.app.data.entity.OrderTable
import com.postest.app.data.entity.PosMode
import com.postest.app.data.entity.TableStatus
import com.postest.app.data.repo.CatalogRepo
import com.postest.app.data.repo.OrderRepo
import com.postest.app.data.repo.SettingsRepo
import com.postest.app.data.repo.ShiftRepo
import com.postest.app.ui.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TablesViewModel @Inject constructor(
    catalogRepo: CatalogRepo,
    private val orderRepo: OrderRepo,
    private val shiftRepo: ShiftRepo,
    private val settingsRepo: SettingsRepo,
    private val session: SessionManager,
) : ViewModel() {
    val tables: StateFlow<List<OrderTable>> = catalogRepo.observeTables()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun openOrCreateOrderForTable(t: OrderTable): Long {
        t.currentOrderId?.let { return it }
        val user = session.current.value ?: error("Not signed in")
        val shift = shiftRepo.current()
        val mode = settingsRepo.get().mode
        return orderRepo.startOrder(mode, user.id, shift?.id, t.id)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TablesScreen(nav: NavController, vm: TablesViewModel = hiltViewModel()) {
    val tables by vm.tables.collectAsState()
    val scope = rememberCoroutineScope()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Tables") },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, null) } },
        )
    }) { pad ->
        LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 140.dp), modifier = Modifier.padding(pad).padding(12.dp)) {
            items(tables) { t ->
                ElevatedCard(
                    modifier = Modifier.padding(8.dp).heightIn(min = 100.dp).fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            val orderId = vm.openOrCreateOrderForTable(t)
                            nav.previousBackStackEntry?.savedStateHandle?.set("resume_order_id", orderId)
                            nav.popBackStack()
                        }
                    },
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (t.status == TableStatus.FREE) MaterialTheme.colorScheme.surface
                        else MaterialTheme.colorScheme.tertiaryContainer
                    ),
                ) {
                    Column(Modifier.fillMaxSize().padding(12.dp)) {
                        Text(t.label, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.weight(1f))
                        Text(if (t.status == TableStatus.FREE) "Free" else "Occupied", color = Color.Gray)
                    }
                }
            }
        }
    }
}
