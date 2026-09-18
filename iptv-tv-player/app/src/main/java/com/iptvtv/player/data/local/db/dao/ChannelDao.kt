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

    @Query("SELECT * FROM channels WHERE sourceId = :sourceId ORDER BY sortOrder")
    suspend fun getAllForSourceOnce(sourceId: Long): List<ChannelEntity>

    @Insert
    suspend fun insertAll(entities: List<ChannelEntity>)

    @Update
    suspend fun updateAll(entities: List<ChannelEntity>)

    @Query("DELETE FROM channels WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM channels WHERE sourceId = :sourceId")
    suspend fun deleteForSource(sourceId: Long)

    @Query("UPDATE channels SET isHidden = :hidden WHERE id = :id")
    suspend fun setHidden(id: Long, hidden: Boolean)

    @Query("UPDATE channels SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE channels SET displayName = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("UPDATE channels SET displayGroup = :group WHERE id = :id")
    suspend fun regroup(id: Long, group: String)

    @Query("UPDATE channels SET sortOrder = :order WHERE id = :id")
    suspend fun setSortOrder(id: Long, order: Int)
}
