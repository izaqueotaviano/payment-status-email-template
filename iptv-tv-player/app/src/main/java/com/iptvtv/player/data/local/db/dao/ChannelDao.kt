package com.iptvtv.player.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.iptvtv.player.data.local.db.entities.ChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels WHERE sourceId = :sourceId ORDER BY sortOrder")
    fun observeForSource(sourceId: Long): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE id = :id")
    suspend fun getById(id: Long): ChannelEntity?

    /**
     * Just the ids, in order: all the player needs for previous/next. Observing full rows there
     * would mean a second complete copy of a playlist that can run to tens of thousands of
     * channels, and would delay playback until the whole query and mapping finished.
     */
    @Query("SELECT id FROM channels WHERE sourceId = :sourceId AND isHidden = 0 ORDER BY sortOrder")
    fun observeVisibleIds(sourceId: Long): Flow<List<Long>>

    @Insert
    suspend fun insertAll(entities: List<ChannelEntity>)

    @Update
    suspend fun updateAll(entities: List<ChannelEntity>)

    /** The rows of one import batch, so a merge never has to read the whole source at once. */
    @Query("SELECT * FROM channels WHERE sourceId = :sourceId AND streamKey IN (:streamKeys)")
    suspend fun getByStreamKeys(sourceId: Long, streamKeys: List<String>): List<ChannelEntity>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM channels WHERE sourceId = :sourceId")
    suspend fun maxSortOrder(sourceId: Long): Int

    @Query("SELECT COUNT(*) FROM channels WHERE sourceId = :sourceId")
    suspend fun countForSource(sourceId: Long): Int

    /** Drops whatever the finished import did not stamp: the channels the provider removed. */
    @Query("DELETE FROM channels WHERE sourceId = :sourceId AND syncStamp != :stamp")
    suspend fun deleteStale(sourceId: Long, stamp: Long)

    @Query("DELETE FROM channels WHERE sourceId = :sourceId")
    suspend fun deleteForSource(sourceId: Long)

    @Query("UPDATE channels SET isHidden = :hidden WHERE id = :id")
    suspend fun setHidden(id: Long, hidden: Boolean)

    @Query("UPDATE channels SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    /** Trims a big playlist down to the favorites in one write instead of one per channel. */
    @Query("UPDATE channels SET isHidden = 1 WHERE sourceId = :sourceId AND isFavorite = 0")
    suspend fun hideNonFavorites(sourceId: Long)

    @Query("UPDATE channels SET isHidden = 0 WHERE sourceId = :sourceId")
    suspend fun showAll(sourceId: Long)

    @Query("SELECT COUNT(*) FROM channels WHERE sourceId = :sourceId AND isHidden = 1")
    fun observeHiddenCount(sourceId: Long): Flow<Int>

    @Query("UPDATE channels SET displayName = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("UPDATE channels SET displayGroup = :group WHERE id = :id")
    suspend fun regroup(id: Long, group: String)

    @Query("UPDATE channels SET sortOrder = :order WHERE id = :id")
    suspend fun setSortOrder(id: Long, order: Int)
}
