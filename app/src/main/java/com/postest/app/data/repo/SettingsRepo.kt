package com.postest.app.data.repo

import com.postest.app.data.db.SettingDao
import com.postest.app.data.entity.SettingRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepo @Inject constructor(private val dao: SettingDao) {
    fun observe(): Flow<SettingRow> = dao.observe().filterNotNull()
    suspend fun get(): SettingRow = dao.get() ?: SettingRow().also { dao.upsert(it) }
    suspend fun update(transform: (SettingRow) -> SettingRow) {
        val current = get()
        dao.upsert(transform(current))
    }
    suspend fun getLanguage(): String = get().language
}
