package com.iptvtv.player.data.repository

import com.iptvtv.player.data.local.db.dao.SourceDao
import com.iptvtv.player.data.local.db.entities.toDomain
import com.iptvtv.player.data.local.db.entities.toEntity
import com.iptvtv.player.domain.model.Source
import com.iptvtv.player.domain.repository.SourceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SourceRepositoryImpl(private val dao: SourceDao) : SourceRepository {
    override fun observeSources(): Flow<List<Source>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getSource(id: Long): Source? =
        dao.getById(id)?.toDomain()

    override suspend fun addSource(source: Source): Long =
        dao.insert(source.toEntity())

    override suspend fun updateSource(source: Source) {
        // toEntity() knows nothing about lastSyncedAt, so it would reset it and make every edit -
        // even a rename - cost a full re-import on the next visit.
        val lastSyncedAt = dao.lastSyncedAt(source.id) ?: 0
        dao.update(source.toEntity().copy(lastSyncedAt = lastSyncedAt))
    }

    override suspend fun deleteSource(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun lastSyncedAt(id: Long): Long = dao.lastSyncedAt(id) ?: 0

    override suspend fun markSynced(id: Long, at: Long) {
        dao.markSynced(id, at)
    }
}
