package com.postest.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepo: UserRepo,
    private val session: SessionManager,
) : ViewModel() {

    val users: StateFlow<List<User>> = userRepo.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var failures = 0
    var lockUntil: Long = 0L; private set

    suspend fun authenticate(userId: Long, pin: String): Boolean {
        if (System.currentTimeMillis() < lockUntil) return false
        val ok = userRepo.verify(userId, pin)
        if (ok) {
            failures = 0
            userRepo.byId(userId)?.let { session.signIn(it) }
            return true
        }
        failures++
        if (failures >= 5) {
            lockUntil = System.currentTimeMillis() + 30_000
            failures = 0
        }
        return false
    }
}

@androidx.compose.runtime.Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    vm: LoginViewModel = hiltViewModel(),
) {
    val users by vm.users.collectAsState()
    var selected by remember { mutableStateOf<User?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(topBar = { TopAppBar(title = { Text("PosTest") }) }) { pad ->
        Box(Modifier.padding(pad).padding(24.dp).fillMaxSize()) {
            if (selected == null) {
                Column {
                    Text("Select user", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(16.dp))
                    LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 160.dp)) {
                        items(users) { u ->
                            ElevatedCard(
                                modifier = Modifier.padding(8.dp).heightIn(min = 100.dp).fillMaxWidth(),
                                onClick = { selected = u; error = null },
                            ) {
                                Column(
                                    Modifier.fillMaxSize().padding(12.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(u.displayName, style = MaterialTheme.typography.titleMedium)
                                    Text(u.role.name, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(selected!!.displayName, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(12.dp))
                    PinPad(
                        title = "Enter PIN",
                        error = error,
                        onComplete = { pin ->
                            scope.launch {
                                val ok = vm.authenticate(selected!!.id, pin)
                                if (ok) onLoggedIn() else error = "Wrong PIN"
                            }
                        },
                        onCancel = { selected = null; error = null },
                    )
                }
            }
        }
    }
}
