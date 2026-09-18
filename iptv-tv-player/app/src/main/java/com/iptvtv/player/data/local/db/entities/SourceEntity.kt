package com.iptvtv.player.data.local.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.iptvtv.player.domain.model.Source

@Entity(tableName = "sources")
data class SourceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String,
    val url: String?,
    val host: String?,
    val port: Int?,
    val username: String?,
    val password: String?,
    val useHttps: Boolean,
    val fileUri: String?,
)

private const val TYPE_M3U = "m3u"
private const val TYPE_XTREAM = "xtream"
private const val TYPE_LOCAL = "local"

fun SourceEntity.toDomain(): Source = when (type) {
    TYPE_M3U -> Source.M3uUrlSource(
        id = id,
        name = name,
        url = requireNotNull(url) { "M3uUrlSource entity $id is missing url" },
    )
    TYPE_XTREAM -> Source.XtreamSource(
        id = id,
        name = name,
        host = requireNotNull(host) { "XtreamSource entity $id is missing host" },
        port = port,
        username = requireNotNull(username) { "XtreamSource entity $id is missing username" },
        password = requireNotNull(password) { "XtreamSource entity $id is missing password" },
        useHttps = useHttps,
    )
    TYPE_LOCAL -> Source.LocalFileSource(
        id = id,
        name = name,
        fileUri = requireNotNull(fileUri) { "LocalFileSource entity $id is missing fileUri" },
    )
    else -> throw IllegalArgumentException("Unknown source type: $type")
}

fun Source.toEntity(): SourceEntity = when (this) {
    is Source.M3uUrlSource -> SourceEntity(
        id = id,
        name = name,
        type = TYPE_M3U,
        url = url,
        host = null,
        port = null,
        username = null,
        password = null,
        useHttps = false,
        fileUri = null,
    )
    is Source.XtreamSource -> SourceEntity(
        id = id,
        name = name,
        type = TYPE_XTREAM,
        url = null,
        host = host,
        port = port,
        username = username,
        password = password,
        useHttps = useHttps,
        fileUri = null,
    )
    is Source.LocalFileSource -> SourceEntity(
        id = id,
        name = name,
        type = TYPE_LOCAL,
        url = null,
        host = null,
        port = null,
        username = null,
        password = null,
        useHttps = false,
        fileUri = fileUri,
    )
}
