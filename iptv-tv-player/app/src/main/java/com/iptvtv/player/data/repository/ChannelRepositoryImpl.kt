package com.iptvtv.player.data.repository

import androidx.room.withTransaction
import com.iptvtv.player.data.local.db.AppDatabase
import com.iptvtv.player.data.local.db.dao.ChannelDao
import com.iptvtv.player.data.local.db.entities.ChannelEntity
import com.iptvtv.player.data.local.db.entities.toDomain
import com.iptvtv.player.data.local.db.entities.toEntity
import com.iptvtv.player.domain.model.Channel
import com.iptvtv.player.domain.repository.ChannelRepository
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * SQLite binds one variable per id in an `IN (...)` clause and caps them at 999 below API 31,
 * so bulk deletes have to be split.
 */
private const val DELETE_CHUNK = 900

class ChannelRepositoryImpl(
    private val database: AppDatabase,
    private val dao: ChannelDao,
) : ChannelRepository {

    override fun observeChannels(sourceId: Long): Flow<List<Channel>> =
        dao.observeForSource(sourceId)
            .map { entities -> entities.map { it.toDomain() } }
            // Playlists reach tens of thousands of rows; mapping them is not main-thread work.
            .flowOn(Dispatchers.Default)

    override suspend fun getChannel(id: Long): Channel? =
        dao.getById(id)?.toDomain()

    override suspend fun replaceChannelsForSource(sourceId: Long, freshChannels: List<Channel>) {
        if (freshChannels.isEmpty()) {
            // Providers answer with an HTML error or maintenance page under HTTP 200 often
            // enough that trusting an empty parse would mean wiping the list - and every
            // favorite, rename and hidden flag with it - on a routine refresh.
            throw IOException("A fonte não retornou nenhum canal.")
        }

        val existing = dao.getAllForSourceOnce(sourceId)
        val existingByKey = existing.associateBy { it.streamKey }
        val freshKeys = freshChannels.map { it.streamKey }.toSet()
        var nextOrder = (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1

        val toInsert = mutableListOf<ChannelEntity>()
        val toUpdate = mutableListOf<ChannelEntity>()

        for (fresh in freshChannels) {
            val existingEntity = existingByKey[fresh.streamKey]
            if (existingEntity != null) {
                toUpdate += existingEntity.copy(
                    originalName = fresh.originalName,
                    logoUrl = fresh.logoUrl,
                    streamUrl = fresh.streamUrl,
                    originalGroup = fresh.originalGroup,
                )
            } else {
                toInsert += fresh.toEntity().copy(
                    id = 0,
                    sourceId = sourceId,
                    sortOrder = nextOrder++,
                )
            }
        }

        val toDeleteIds = existing.filter { it.streamKey !in freshKeys }.map { it.id }

        // One transaction: a failure part-way through must not leave the source with half a
        // list, and a re-import must never be observable as an empty list.
        database.withTransaction {
            toDeleteIds.chunked(DELETE_CHUNK).forEach { chunk -> dao.deleteByIds(chunk) }
            if (toUpdate.isNotEmpty()) dao.updateAll(toUpdate)
            if (toInsert.isNotEmpty()) dao.insertAll(toInsert)
        }
    }

    override suspend fun setHidden(channelId: Long, hidden: Boolean) {
        dao.setHidden(channelId, hidden)
    }

    override suspend fun setFavorite(channelId: Long, favorite: Boolean) {
        dao.setFavorite(channelId, favorite)
    }

    override suspend fun hideNonFavorites(sourceId: Long) {
        dao.hideNonFavorites(sourceId)
    }

    override suspend fun showAllChannels(sourceId: Long) {
        dao.showAll(sourceId)
    }

    override fun observeHiddenCount(sourceId: Long): Flow<Int> =
        dao.observeHiddenCount(sourceId)

    override suspend fun rename(channelId: Long, newName: String) {
        dao.rename(channelId, newName)
    }

    override suspend fun regroup(channelId: Long, newGroup: String) {
        dao.regroup(channelId, newGroup)
    }

    override suspend fun swapOrder(firstChannelId: Long, secondChannelId: Long) {
        database.withTransaction {
            val first = dao.getById(firstChannelId) ?: return@withTransaction
            val second = dao.getById(secondChannelId) ?: return@withTransaction
            dao.setSortOrder(firstChannelId, second.sortOrder)
            dao.setSortOrder(secondChannelId, first.sortOrder)
        }
    }

    override suspend fun deleteChannelsForSource(sourceId: Long) {
        dao.deleteForSource(sourceId)
    }
}
