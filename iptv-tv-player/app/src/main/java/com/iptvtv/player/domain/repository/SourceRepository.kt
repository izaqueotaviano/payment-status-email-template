package com.iptvtv.player.domain.repository

import com.iptvtv.player.domain.model.Source
import kotlinx.coroutines.flow.Flow

interface SourceRepository {
    fun observeSources(): Flow<List<Source>>
    suspend fun getSource(id: Long): Source?
    suspend fun addSource(source: Source): Long
    suspend fun updateSource(source: Source)
    suspend fun deleteSource(id: Long)
}
