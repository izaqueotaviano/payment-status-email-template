package com.iptvtv.player.navigation

/** Central place for every navigation route string used by [com.iptvtv.player.navigation.AppNavHost]. */
object Routes {
    const val SOURCES = "sources"

    const val ADD_EDIT_SOURCE_PATTERN = "add_edit_source?sourceId={sourceId}&sourceType={sourceType}"
    fun addSource(sourceType: String): String = "add_edit_source?sourceId=-1&sourceType=$sourceType"
    fun editSource(sourceId: Long): String = "add_edit_source?sourceId=$sourceId&sourceType="

    const val CHANNEL_LIST_PATTERN = "channel_list/{sourceId}"
    fun channelList(sourceId: Long): String = "channel_list/$sourceId"

    const val PLAYER_PATTERN = "player/{sourceId}/{channelId}"
    fun player(sourceId: Long, channelId: Long): String = "player/$sourceId/$channelId"

    const val SETTINGS = "settings"
}
