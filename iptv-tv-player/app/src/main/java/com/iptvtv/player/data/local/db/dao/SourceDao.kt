package com.iptvtv.player.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.iptvtv.player.data.local.db.entities.SourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao {
    @Query("SELECT * FROM sources ORDER BY id")
    fun observeAll(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM sources WHERE id = :id")
    suspend fun getById(id: Long): SourceEntity?

    @Insert
    suspend fun insert(entity: SourceEntity): Long

    @Update
    suspend fun update(entity: SourceEntity)

    @Query("SELECT lastSyncedAt FROM sources WHERE id = :id")
    suspend fun lastSyncedAt(id: Long): Long?

    @Query("UPDATE sources SET lastSyncedAt = :at WHERE id = :id")
    suspend fun markSynced(id: Long, at: Long)

    @Query("DELETE FROM sources WHERE id = :id")
    suspend fun deleteById(id: Long)
}
