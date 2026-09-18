package com.iptvtv.player.domain.model

/**
 * A saved channel source. The user can save several and switch the active one at any time.
 */
sealed class Source {
    abstract val id: Long
    abstract val name: String

    data class M3uUrlSource(
        override val id: Long = 0,
        override val name: String,
        val url: String,
    ) : Source()

    data class XtreamSource(
        override val id: Long = 0,
        override val name: String,
        val host: String,
        val port: Int? = null,
        val username: String,
        val password: String,
        val useHttps: Boolean = false,
    ) : Source() {
        /** Base URL used both for the Xtream Codes JSON API and for building live stream URLs. */
        fun baseUrl(): String {
            val scheme = if (useHttps) "https" else "http"
            val portSuffix = port?.let { ":$it" } ?: ""
            return "$scheme://$host$portSuffix"
        }
    }

    data class LocalFileSource(
        override val id: Long = 0,
        override val name: String,
        /** content:// URI for which we hold a persisted read permission (SAF). */
        val fileUri: String,
    ) : Source()
}
