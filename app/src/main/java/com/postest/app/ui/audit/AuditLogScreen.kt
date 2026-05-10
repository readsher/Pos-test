package com.postest.app.ui.audit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.postest.app.data.entity.AuditLog
import com.postest.app.data.repo.AuditRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AuditLogViewModel @Inject constructor(repo: AuditRepo) : ViewModel() {
    val logs: StateFlow<List<AuditLog>> = repo.observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogScreen(nav: NavController, vm: AuditLogViewModel = hiltViewModel()) {
    val logs by vm.logs.collectAsState()
    val df = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Audit log") },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, null) } },
        )
    }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(12.dp)) {
            items(logs) { log ->
                ListItem(
                    headlineContent = { Text("${log.action}  ·  user #${log.userId}") },
                    supportingContent = {
                        val target = listOfNotNull(log.targetType, log.targetId?.let { "#$it" }).joinToString(" ")
                        val approver = log.approvedByUserId?.let { " (approved by #$it)" } ?: ""
                        Text("${df.format(Date(log.at))}  ·  $target$approver")
                    },
                )
                HorizontalDivider()
            }
        }
    }
}
