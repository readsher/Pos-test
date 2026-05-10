package com.postest.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.postest.app.auth.SessionManager
import com.postest.app.data.entity.User
import com.postest.app.data.repo.UserRepo
import com.postest.app.ui.common.PinPad
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManagerOverrideViewModel @Inject constructor(
    private val userRepo: UserRepo,
) : ViewModel() {
    val managers: StateFlow<List<User>> = userRepo.observeActive()
        .map { all -> all.filter { SessionManager.canApproveOverride(it.role) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun verify(userId: Long, pin: String): Boolean = userRepo.verify(userId, pin)
}

@Composable
fun ManagerOverrideDialog(
    onApproved: (approverUserId: Long) -> Unit,
    onDismiss: () -> Unit,
    vm: ManagerOverrideViewModel = hiltViewModel(),
) {
    val managers by vm.managers.collectAsState()
    var selected by remember { mutableStateOf<User?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Manager approval required") },
        text = {
            if (selected == null) {
                Column {
                    Text("Pick a manager to approve")
                    Spacer(Modifier.height(8.dp))
                    managers.forEach { u ->
                        TextButton(onClick = { selected = u; error = null }, modifier = Modifier.fillMaxWidth()) {
                            Text("${u.displayName}  (${u.role.name})")
                        }
                    }
                }
            } else {
                PinPad(
                    title = "PIN for ${selected!!.displayName}",
                    error = error,
                    onComplete = { pin ->
                        scope.launch {
                            if (vm.verify(selected!!.id, pin)) onApproved(selected!!.id)
                            else error = "Wrong PIN"
                        }
                    },
                    onCancel = { selected = null; error = null },
                )
            }
        },
    )
}
