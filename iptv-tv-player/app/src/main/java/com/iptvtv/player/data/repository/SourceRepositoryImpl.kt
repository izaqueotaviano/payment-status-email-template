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
        dao.update(source.toEntity())
    }

    override suspend fun deleteSource(id: Long) {
        dao.deleteById(id)
    }
}
