package com.iptvtv.player.domain.repository

import com.iptvtv.player.domain.model.Source
import kotlinx.coroutines.flow.Flow

interface SourceRepository {
    fun observeSources(): Flow<List<Source>>
    suspend fun getSource(id: Long): Source?
    suspend fun addSource(source: Source): Long
    suspend fun updateSource(source: Source)
    suspend fun deleteSource(id: Long)

    /**
     * When [id] was last imported successfully, as epoch millis, or 0 when it never was. The
     * channel list consults this instead of re-importing the playlist on every visit.
     */
    suspend fun lastSyncedAt(id: Long): Long

    suspend fun markSynced(id: Long, at: Long)
}
