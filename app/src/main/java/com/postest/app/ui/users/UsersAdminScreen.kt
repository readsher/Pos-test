package com.postest.app.ui.users

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
import com.postest.app.auth.SessionManager
import com.postest.app.data.entity.User
import com.postest.app.data.entity.UserRole
import com.postest.app.data.repo.AuditRepo
import com.postest.app.data.repo.UserRepo
import com.postest.app.ui.common.PinPad
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UsersAdminViewModel @Inject constructor(
    private val repo: UserRepo,
    private val audit: AuditRepo,
    private val session: SessionManager,
) : ViewModel() {
    val users: StateFlow<List<User>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(name: String, pin: String, role: UserRole) = viewModelScope.launch {
        val id = repo.create(name, pin, role)
        session.current.value?.let { audit.log(it.id, "EDIT_USER", "user", id, """{"action":"create","name":"$name","role":"$role"}""") }
    }
    fun setActive(u: User, active: Boolean) = viewModelScope.launch {
        repo.setActive(u.id, active)
        session.current.value?.let { audit.log(it.id, "EDIT_USER", "user", u.id, """{"active":$active}""") }
    }
    fun setRole(u: User, role: UserRole) = viewModelScope.launch {
        repo.setRole(u.id, role)
        session.current.value?.let { audit.log(it.id, "EDIT_USER", "user", u.id, """{"role":"$role"}""") }
    }
    fun rename(u: User, name: String) = viewModelScope.launch {
        repo.rename(u.id, name)
        session.current.value?.let { audit.log(it.id, "EDIT_USER", "user", u.id, """{"name":"$name"}""") }
    }
    fun resetPin(u: User, newPin: String) = viewModelScope.launch {
        repo.setPin(u.id, newPin)
        session.current.value?.let { audit.log(it.id, "EDIT_USER", "user", u.id, """{"action":"reset_pin"}""") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersAdminScreen(nav: NavController, vm: UsersAdminViewModel = hiltViewModel()) {
    val users by vm.users.collectAsState()
    var addOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<User?>(null) }
    var resetting by remember { mutableStateOf<User?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Users") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, null) } },
                actions = { TextButton(onClick = { addOpen = true }) { Text("New") } },
            )
        },
    ) { pad ->
        LazyColumn(Modifier.padding(pad).padding(12.dp)) {
            items(users) { u ->
                ListItem(
                    headlineContent = { Text(u.displayName) },
                    supportingContent = { Text("${u.role}  ·  ${if (u.active) "active" else "inactive"}") },
                    trailingContent = {
                        Row {
                            TextButton(onClick = { editing = u }) { Text("Edit") }
                            TextButton(onClick = { resetting = u }) { Text("Reset PIN") }
                            TextButton(onClick = { vm.setActive(u, !u.active) }) {
                                Text(if (u.active) "Deactivate" else "Activate")
                            }
                        }
                    },
                )
                HorizontalDivider()
            }
        }
    }

    if (addOpen) {
        UserEditDialog(
            initial = null,
            onSave = { name, pin, role -> vm.add(name, pin, role); addOpen = false },
            onDismiss = { addOpen = false },
        )
    }
    if (editing != null) {
        UserEditDialog(
            initial = editing,
            onSave = { name, _, role -> vm.rename(editing!!, name); vm.setRole(editing!!, role); editing = null },
            onDismiss = { editing = null },
            requirePin = false,
        )
    }
    if (resetting != null) {
        AlertDialog(
            onDismissRequest = { resetting = null },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { resetting = null }) { Text("Cancel") } },
            title = { Text("Reset PIN for ${resetting!!.displayName}") },
            text = {
                PinPad(
                    title = "New PIN",
                    onComplete = { newPin ->
                        vm.resetPin(resetting!!, newPin)
                        resetting = null
                    },
                )
            },
        )
    }
}

@Composable
private fun UserEditDialog(
    initial: User?,
    onSave: (name: String, pin: String, role: UserRole) -> Unit,
    onDismiss: () -> Unit,
    requirePin: Boolean = true,
) {
    var name by remember { mutableStateOf(initial?.displayName ?: "") }
    var role by remember { mutableStateOf(initial?.role ?: UserRole.CASHIER) }
    var stage by remember { mutableStateOf(if (requirePin) 0 else 1) } // 0=enter PIN, 1=enter rest
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            if (stage == 1) TextButton(
                onClick = { onSave(name, pin, role) },
                enabled = name.isNotBlank() && (!requirePin || pin.length >= 4),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text(if (initial == null) "New user" else "Edit user") },
        text = {
            Column {
                if (stage == 0) {
                    PinPad(
                        title = "Set PIN",
                        onComplete = { p -> pin = p; stage = 1 },
                    )
                } else {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                    Spacer(Modifier.height(8.dp))
                    Text("Role:")
                    UserRole.entries.forEach { r ->
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            RadioButton(selected = role == r, onClick = { role = r })
                            Text(r.name)
                        }
                    }
                }
            }
        },
    )
}
