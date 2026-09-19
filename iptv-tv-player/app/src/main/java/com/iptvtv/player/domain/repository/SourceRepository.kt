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
     * When an import of [id] was last *started*, as epoch millis, or 0 when none ever was. The
     * channel list consults this instead of re-importing the playlist on every visit.
     *
     * It records the attempt rather than the success on purpose: an import that is cancelled
     * (the user leaves the screen) or fails (the provider serves an error page) would otherwise
     * leave no trace, and every later visit would download the whole playlist again.
     */
    suspend fun lastImportAttempt(id: Long): Long

    suspend fun markImportAttempt(id: Long, at: Long)

    /**
     * Makes every source stale, so the next visit re-imports. For a change that alters what an
     * import returns - the live-only switch - rather than for anything about the sources.
     */
    suspend fun clearImportAttempts()
}
