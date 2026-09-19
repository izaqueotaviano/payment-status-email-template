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
        val stored = dao.getById(source.id)
        val entity = source.toEntity()
        // A rename keeps the stamp - re-importing a whole playlist because the user fixed a typo in
        // the name would be absurd. Anything that changes what an import would FETCH clears it, so
        // the corrected URL or password takes effect on the next visit instead of in twelve hours.
        val fetchesTheSame = stored != null &&
            stored.type == entity.type &&
            stored.url == entity.url &&
            stored.host == entity.host &&
            stored.port == entity.port &&
            stored.username == entity.username &&
            stored.password == entity.password &&
            stored.useHttps == entity.useHttps &&
            stored.fileUri == entity.fileUri
        dao.update(entity.copy(lastSyncedAt = if (fetchesTheSame) stored.lastSyncedAt else 0))
    }

    override suspend fun deleteSource(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun lastImportAttempt(id: Long): Long = dao.lastSyncedAt(id) ?: 0

    override suspend fun markImportAttempt(id: Long, at: Long) {
        dao.markSynced(id, at)
    }

    override suspend fun clearImportAttempts() {
        dao.clearSyncStamps()
    }
}
