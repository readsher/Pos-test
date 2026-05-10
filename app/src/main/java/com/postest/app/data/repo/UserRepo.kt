package com.postest.app.data.repo

import com.postest.app.auth.PinHasher
import com.postest.app.data.db.UserDao
import com.postest.app.data.entity.User
import com.postest.app.data.entity.UserRole
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepo @Inject constructor(
    private val dao: UserDao,
    private val hasher: PinHasher,
) {
    fun observeActive(): Flow<List<User>> = dao.observeActive()
    fun observeAll(): Flow<List<User>> = dao.observeAll()
    suspend fun byId(id: Long): User? = dao.byId(id)

    suspend fun create(displayName: String, pin: String, role: UserRole): Long {
        val h = hasher.hash(pin)
        return dao.insert(User(displayName = displayName, pinHash = h.hash, pinSalt = h.salt, role = role))
    }

    suspend fun setPin(userId: Long, newPin: String) {
        val u = dao.byId(userId) ?: return
        val h = hasher.hash(newPin)
        dao.update(u.copy(pinHash = h.hash, pinSalt = h.salt))
    }

    suspend fun setActive(userId: Long, active: Boolean) {
        val u = dao.byId(userId) ?: return
        dao.update(u.copy(active = active))
    }

    suspend fun setRole(userId: Long, role: UserRole) {
        val u = dao.byId(userId) ?: return
        dao.update(u.copy(role = role))
    }

    suspend fun rename(userId: Long, newName: String) {
        val u = dao.byId(userId) ?: return
        dao.update(u.copy(displayName = newName))
    }

    suspend fun verify(userId: Long, pin: String): Boolean {
        val u = dao.byId(userId) ?: return false
        return hasher.verify(pin, u.pinHash, u.pinSalt)
    }
}
