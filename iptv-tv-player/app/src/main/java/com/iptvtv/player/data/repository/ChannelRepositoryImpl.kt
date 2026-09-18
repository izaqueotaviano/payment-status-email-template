package com.iptvtv.player.data.repository

import com.iptvtv.player.data.local.db.dao.ChannelDao
import com.iptvtv.player.data.local.db.entities.ChannelEntity
import com.iptvtv.player.data.local.db.entities.toDomain
import com.iptvtv.player.data.local.db.entities.toEntity
import com.iptvtv.player.domain.model.Channel
import com.iptvtv.player.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChannelRepositoryImpl(private val dao: ChannelDao) : ChannelRepository {
    override fun observeChannels(sourceId: Long): Flow<List<Channel>> =
        dao.observeForSource(sourceId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getChannel(id: Long): Channel? =
        dao.getById(id)?.toDomain()

    override suspend fun replaceChannelsForSource(sourceId: Long, freshChannels: List<Channel>) {
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

        if (toDeleteIds.isNotEmpty()) dao.deleteByIds(toDeleteIds)
        if (toUpdate.isNotEmpty()) dao.updateAll(toUpdate)
        if (toInsert.isNotEmpty()) dao.insertAll(toInsert)
    }

    override suspend fun setHidden(channelId: Long, hidden: Boolean) {
        dao.setHidden(channelId, hidden)
    }

    override suspend fun setFavorite(channelId: Long, favorite: Boolean) {
        dao.setFavorite(channelId, favorite)
    }

    override suspend fun rename(channelId: Long, newName: String) {
        dao.rename(channelId, newName)
    }

    override suspend fun regroup(channelId: Long, newGroup: String) {
        dao.regroup(channelId, newGroup)
    }

    override suspend fun reorder(orderedChannelIds: List<Long>) {
        orderedChannelIds.forEachIndexed { index, id ->
            dao.setSortOrder(id, index)
        }
    }

    override suspend fun deleteChannelsForSource(sourceId: Long) {
        dao.deleteForSource(sourceId)
    }
}
