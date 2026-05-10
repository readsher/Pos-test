package com.postest.app.auth

import com.postest.app.data.entity.User
import com.postest.app.data.entity.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class Permission {
    TAKE_ORDER,
    OPEN_SHIFT,
    CLOSE_SHIFT,
    VOID_ORDER,
    REFUND,
    PRICE_OVERRIDE,
    EDIT_MENU,
    EDIT_SETTINGS,
    EDIT_USERS,
    VIEW_REPORTS,
    VIEW_AUDIT,
}

@Singleton
class SessionManager @Inject constructor() {
    private val _current = MutableStateFlow<User?>(null)
    val current: StateFlow<User?> = _current.asStateFlow()

    private var lastActivityAt: Long = System.currentTimeMillis()

    fun signIn(user: User) {
        _current.value = user
        touch()
    }

    fun signOut() { _current.value = null }

    fun touch() { lastActivityAt = System.currentTimeMillis() }

    fun isIdleExpired(idleMinutes: Int): Boolean =
        System.currentTimeMillis() - lastActivityAt > idleMinutes * 60_000L

    fun has(perm: Permission): Boolean {
        val role = _current.value?.role ?: return false
        return permissionsFor(role).contains(perm)
    }

    companion object {
        fun permissionsFor(role: UserRole): Set<Permission> = when (role) {
            UserRole.OWNER -> Permission.entries.toSet()
            UserRole.MANAGER -> setOf(
                Permission.TAKE_ORDER,
                Permission.OPEN_SHIFT,
                Permission.CLOSE_SHIFT,
                Permission.VOID_ORDER,
                Permission.REFUND,
                Permission.PRICE_OVERRIDE,
                Permission.EDIT_MENU,
                Permission.EDIT_SETTINGS,
                Permission.VIEW_REPORTS,
            )
            UserRole.CASHIER -> setOf(
                Permission.TAKE_ORDER,
                Permission.OPEN_SHIFT,
            )
        }

        fun canApproveOverride(role: UserRole): Boolean =
            role == UserRole.OWNER || role == UserRole.MANAGER
    }
}
