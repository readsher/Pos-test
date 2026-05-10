package com.postest.app.data.repo

import com.postest.app.data.db.AuditDao
import com.postest.app.data.entity.AuditLog
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuditRepo @Inject constructor(private val dao: AuditDao) {
    fun observeRecent(): Flow<List<AuditLog>> = dao.observeRecent()
    suspend fun page(limit: Int, offset: Int) = dao.page(limit, offset)

    suspend fun log(
        userId: Long,
        action: String,
        targetType: String? = null,
        targetId: Long? = null,
        payloadJson: String? = null,
        approvedByUserId: Long? = null,
    ) {
        dao.insert(AuditLog(
            userId = userId, action = action, targetType = targetType, targetId = targetId,
            payloadJson = payloadJson, approvedByUserId = approvedByUserId,
        ))
    }
}
