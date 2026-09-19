package com.iptvtv.player.data.repository

import androidx.room.withTransaction
import com.iptvtv.player.data.local.db.AppDatabase
import com.iptvtv.player.data.local.db.dao.ChannelDao
import com.iptvtv.player.data.local.db.entities.ChannelEntity
import com.iptvtv.player.data.local.db.entities.toDomain
import com.iptvtv.player.data.local.db.entities.toEntity
import com.iptvtv.player.domain.model.Channel
import com.iptvtv.player.domain.repository.ChannelRepository
import com.iptvtv.player.domain.repository.SyncSession
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

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

    override suspend fun channelsFor(sourceId: Long): List<Channel> =
        withContext(Dispatchers.Default) { dao.getForSourceOnce(sourceId).map { it.toDomain() } }

    override fun observeVisibleChannelIds(sourceId: Long): Flow<List<Long>> =
        dao.observeVisibleIds(sourceId).conflate()

    override fun observeChannelCounts(): Flow<Map<Long, Int>> =
        dao.observeCountsBySource()
            .conflate()
            .map { rows -> rows.associate { it.sourceId to it.channelCount } }
            .flowOn(Dispatchers.Default)

    /**
     * The sources with an import in flight. A second import of the same source would read the same
     * "before" snapshot, and whichever finished last would delete the other's rows as stale.
     */
    private val inFlightMutex = Mutex()
    private val inFlight = HashSet<Long>()

    /** A live import. Its ordering counter belongs to it alone, not to a map shared by source. */
    private class Session(
        override val sourceId: Long,
        override val previousCount: Int,
        val stamp: Long,
        var nextSortOrder: Int,
    ) : SyncSession {
        /** Guarded by the repository's mutex. */
        var released = false
    }

    override suspend fun countForSource(sourceId: Long): Int = dao.countForSource(sourceId)

    override suspend fun beginSync(sourceId: Long): SyncSession? {
        inFlightMutex.withLock {
            if (!inFlight.add(sourceId)) return null
        }
        // Past the point the source is marked busy, nothing may throw without unmarking it, or
        // the source would refuse every later import for the life of the process.
        return try {
            Session(
                sourceId = sourceId,
                previousCount = dao.countForSource(sourceId),
                // Any value no earlier import used; the clock is enough and needs no bookkeeping.
                stamp = System.currentTimeMillis(),
                nextSortOrder = dao.maxSortOrder(sourceId) + 1,
            )
        } catch (error: Throwable) {
            inFlightMutex.withLock { inFlight.remove(sourceId) }
            throw error
        }
    }

    override suspend fun writeSyncBatch(session: SyncSession, batch: List<Channel>) {
        val live = session as Session
        if (batch.isEmpty()) return

        // A provider can list the same stream twice. Left in, the duplicates would be inserted as
        // separate rows and the next import would silently delete one of them.
        val fresh = batch.distinctBy { it.streamKey }

        val existingByKey = fresh.map { it.streamKey }
            // Chunked because SQLite binds one variable per value and caps them at 999 below API 31.
            .chunked(QUERY_CHUNK)
            .flatMap { chunk -> dao.getByStreamKeys(live.sourceId, chunk) }
            .associateBy { it.streamKey }

        val toInsert = ArrayList<ChannelEntity>(fresh.size)
        val toUpdate = ArrayList<ChannelEntity>()
        val toStamp = ArrayList<String>(fresh.size)

        for (channel in fresh) {
            val existing = existingByKey[channel.streamKey]
            if (existing == null) {
                toInsert += channel.toEntity().copy(
                    id = 0,
                    sourceId = live.sourceId,
                    sortOrder = live.nextSortOrder++,
                    syncStamp = live.stamp,
                )
                continue
            }
            val unchanged = existing.originalName == channel.originalName &&
                existing.logoUrl == channel.logoUrl &&
                existing.streamUrl == channel.streamUrl &&
                existing.originalGroup == channel.originalGroup
            if (unchanged) {
                // Only the stamp has to move. A full-row UPDATE per channel here is what made
                // re-importing an unchanged playlist cost as much as importing it the first time;
                // the stamp goes on in one statement per chunk instead.
                toStamp += channel.streamKey
            } else {
                toUpdate += existing.copy(
                    originalName = channel.originalName,
                    logoUrl = channel.logoUrl,
                    streamUrl = channel.streamUrl,
                    originalGroup = channel.originalGroup,
                    syncStamp = live.stamp,
                )
            }
        }

        database.withTransaction {
            if (toUpdate.isNotEmpty()) dao.updateAll(toUpdate)
            toStamp.chunked(QUERY_CHUNK).forEach { chunk ->
                dao.stampSeen(live.sourceId, live.stamp, chunk)
            }
            if (toInsert.isNotEmpty()) dao.insertAll(toInsert)
        }
    }

    override suspend fun finishSync(session: SyncSession, importedCount: Int): Int {
        val live = session as Session
        try {
            if (importedCount == 0) {
                // Providers answer with an HTML error or maintenance page under HTTP 200 often
                // enough that trusting an empty parse would mean wiping the list - and every
                // favorite, rename and hidden flag with it - on a routine refresh.
                throw IOException("A fonte não retornou nenhum canal.")
            }
            database.withTransaction {
                dao.deleteStale(live.sourceId, live.stamp)
            }
            return dao.countForSource(live.sourceId)
        } finally {
            release(live)
        }
    }

    override suspend fun cancelSync(session: SyncSession) {
        release(session as Session)
    }

    /**
     * Unmarks the source, once. Keyed to the session and not just to the source id: a session that
     * released on its way out and then released again would otherwise unmark whichever import had
     * started in between, letting two run at once - the very thing the marking prevents.
     */
    private suspend fun release(session: Session) {
        inFlightMutex.withLock {
            if (session.released) return@withLock
            session.released = true
            inFlight.remove(session.sourceId)
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
