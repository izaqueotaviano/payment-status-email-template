package com.iptvtv.player.data.local.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.iptvtv.player.domain.model.Channel

@Entity(
    tableName = "channels",
    indices = [Index("sourceId"), Index("streamKey")],
)
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true)
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
    val isFavorite: Boolean,
    val isHidden: Boolean,
    /**
     * Stamp of the import that last saw this channel. Rows still carrying an older stamp when an
     * import finishes are the ones the provider dropped, so they can be deleted with a single
     * statement instead of collecting every id in memory.
     */
    val syncStamp: Long = 0,
)

fun ChannelEntity.toDomain(): Channel = Channel(
    id = id,
    sourceId = sourceId,
    originalName = originalName,
    displayName = displayName,
    logoUrl = logoUrl,
    streamUrl = streamUrl,
    streamKey = streamKey,
    originalGroup = originalGroup,
    displayGroup = displayGroup,
    sortOrder = sortOrder,
    isFavorite = isFavorite,
    isHidden = isHidden,
)

fun Channel.toEntity(): ChannelEntity = ChannelEntity(
    id = id,
    sourceId = sourceId,
    originalName = originalName,
    displayName = displayName,
    logoUrl = logoUrl,
    streamUrl = streamUrl,
    streamKey = streamKey,
    originalGroup = originalGroup,
    displayGroup = displayGroup,
    sortOrder = sortOrder,
    isFavorite = isFavorite,
    isHidden = isHidden,
)
