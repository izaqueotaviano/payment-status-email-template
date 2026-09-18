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
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * SQLite binds one variable per value in an `IN (...)` clause and caps them at 999 below API 31,
 * so lookups by a batch of keys have to be split.
 */
private const val QUERY_CHUNK = 900

class ChannelRepositoryImpl(
    private val database: AppDatabase,
    private val dao: ChannelDao,
) : ChannelRepository {

    override fun observeChannels(sourceId: Long): Flow<List<Channel>> =
        dao.observeForSource(sourceId)
            // An import writes many batches, and Room re-runs this query after each one. Without
            // conflating, the screen would re-map the whole table once per batch - the work grows
            // with the square of the playlist and is what made importing crawl.
            .conflate()
            .map { entities -> entities.map { it.toDomain() } }
            // Playlists reach tens of thousands of rows; mapping them is not main-thread work.
            .flowOn(Dispatchers.Default)

    override suspend fun getChannel(id: Long): Channel? =
        dao.getById(id)?.toDomain()

    override fun observeVisibleChannelIds(sourceId: Long): Flow<List<Long>> =
        dao.observeVisibleIds(sourceId).conflate()

    override fun observeChannelCounts(): Flow<Map<Long, Int>> =
        dao.observeCountsBySource()
            .conflate()
            .map { rows -> rows.associate { it.sourceId to it.channelCount } }
            .flowOn(Dispatchers.Default)

    /** Guards the per-source sortOrder counters, which batches advance one after another. */
    private val syncMutex = Mutex()
    private val nextSortOrder = HashMap<Long, Int>()

    override suspend fun beginSync(sourceId: Long): Long {
        val startOrder = dao.maxSortOrder(sourceId) + 1
        syncMutex.withLock { nextSortOrder[sourceId] = startOrder }
        // Any value no earlier import used; the clock is enough and needs no extra bookkeeping.
        return System.currentTimeMillis()
    }

    override suspend fun writeSyncBatch(sourceId: Long, stamp: Long, batch: List<Channel>) {
        if (batch.isEmpty()) return

        val keys = batch.map { it.streamKey }
        // Chunked because SQLite binds one variable per value and caps them at 999 below API 31.
        val existingByKey = keys.chunked(QUERY_CHUNK)
            .flatMap { chunk -> dao.getByStreamKeys(sourceId, chunk) }
            .associateBy { it.streamKey }

        val toInsert = ArrayList<ChannelEntity>(batch.size)
        val toUpdate = ArrayList<ChannelEntity>()

        syncMutex.withLock {
            var order = nextSortOrder[sourceId] ?: (dao.maxSortOrder(sourceId) + 1)
            for (fresh in batch) {
                val existing = existingByKey[fresh.streamKey]
                if (existing != null) {
                    toUpdate += existing.copy(
                        originalName = fresh.originalName,
                        logoUrl = fresh.logoUrl,
                        streamUrl = fresh.streamUrl,
                        originalGroup = fresh.originalGroup,
                        syncStamp = stamp,
                    )
                } else {
                    toInsert += fresh.toEntity().copy(
                        id = 0,
                        sourceId = sourceId,
                        sortOrder = order++,
                        syncStamp = stamp,
                    )
                }
            }
            nextSortOrder[sourceId] = order
        }

        database.withTransaction {
            if (toUpdate.isNotEmpty()) dao.updateAll(toUpdate)
            if (toInsert.isNotEmpty()) dao.insertAll(toInsert)
        }
    }

    override suspend fun finishSync(sourceId: Long, stamp: Long, importedCount: Int): Int {
        if (importedCount == 0) {
            // Providers answer with an HTML error or maintenance page under HTTP 200 often
            // enough that trusting an empty parse would mean wiping the list - and every
            // favorite, rename and hidden flag with it - on a routine refresh.
            throw IOException("A fonte não retornou nenhum canal.")
        }
        database.withTransaction {
            dao.deleteStale(sourceId, stamp)
        }
        return dao.countForSource(sourceId)
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
