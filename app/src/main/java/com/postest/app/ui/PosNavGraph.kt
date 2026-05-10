package com.postest.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.postest.app.auth.SessionManager
import com.postest.app.ui.audit.AuditLogScreen
import com.postest.app.ui.auth.LoginScreen
import com.postest.app.ui.checkout.CheckoutScreen
import com.postest.app.ui.menu.MenuAdminScreen
import com.postest.app.ui.pos.PosScreen
import com.postest.app.ui.report.ReportScreen
import com.postest.app.ui.settings.SettingsScreen
import com.postest.app.ui.shift.ShiftScreen
import com.postest.app.ui.tables.TablesScreen
import com.postest.app.ui.users.UsersAdminScreen
import javax.inject.Inject

object Routes {
    const val Login = "login"
    const val Pos = "pos"
    const val Tables = "tables"
    const val Checkout = "checkout/{orderId}"
    fun checkout(orderId: Long) = "checkout/$orderId"
    const val Menu = "menu"
    const val Settings = "settings"
    const val Report = "report"
    const val Shift = "shift"
    const val Users = "users"
    const val Audit = "audit"
}

@Composable
fun PosNavGraph(vm: NavGate = hiltViewModel()) {
    val nav = rememberNavController()
    val current by vm.session.current.collectAsState()
    val start = if (current == null) Routes.Login else Routes.Pos

    NavHost(navController = nav, startDestination = start) {
        composable(Routes.Login)    { LoginScreen(onLoggedIn = {
            nav.navigate(Routes.Pos) { popUpTo(Routes.Login) { inclusive = true } }
        }) }
        composable(Routes.Pos)      { PosScreen(nav) }
        composable(Routes.Tables)   { TablesScreen(nav) }
        composable(Routes.Checkout) { backStack ->
            val id = backStack.arguments?.getString("orderId")?.toLongOrNull() ?: 0L
            CheckoutScreen(orderId = id, nav = nav)
        }
        composable(Routes.Menu)     { MenuAdminScreen(nav) }
        composable(Routes.Settings) { SettingsScreen(nav) }
        composable(Routes.Report)   { ReportScreen(nav) }
        composable(Routes.Shift)    { ShiftScreen(nav) }
        composable(Routes.Users)    { UsersAdminScreen(nav) }
        composable(Routes.Audit)    { AuditLogScreen(nav) }
    }
}

@dagger.hilt.android.lifecycle.HiltViewModel
class NavGate @Inject constructor(val session: SessionManager) : androidx.lifecycle.ViewModel()
