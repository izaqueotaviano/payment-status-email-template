package com.iptvtv.player.domain.model

/**
 * A single playable channel that belongs to a [Source].
 *
 * [streamKey] is the stable identity used to merge a fresh import into the existing list
 * (see [com.iptvtv.player.domain.repository.ChannelRepository.writeSyncBatch]):
 * it is the Xtream stream id when available, otherwise the raw stream URL. Everything the
 * user customized (favorite, hidden, rename, regroup, order) survives a re-sync as long as
 * the channel's [streamKey] is still present in the new import.
 */
data class Channel(
    val id: Long = 0,
    val sourceId: Long,
    val originalName: String,
    val displayName: String,
    val logoUrl: String?,
    val streamUrl: String,
    val streamKey: String,
    val originalGroup: String,
    val displayGroup: String,
    val sortOrder: Int,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
)
