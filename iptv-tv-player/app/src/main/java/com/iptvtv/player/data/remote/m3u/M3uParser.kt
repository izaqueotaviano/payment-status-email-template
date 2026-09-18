package com.iptvtv.player.data.remote.m3u

/**
 * A single entry parsed out of an M3U/M3U8 playlist: the `#EXTINF:` attributes plus the
 * stream URL that follows on the next non-blank, non-`#`-prefixed line.
 */
data class ParsedM3uEntry(
    val name: String,
    val logoUrl: String?,
    val groupTitle: String?,
    val streamUrl: String,
)

/**
 * Tolerant parser for standard M3U/M3U8 playlists as produced by IPTV providers.
 *
 * Only `#EXTINF:` lines are inspected for metadata (`tvg-logo` and `group-title` attributes,
 * plus the display name after the last comma on the line); every other `#`-prefixed line
 * (`#EXTM3U`, `#EXTGRP`, `#EXTVLCOPT`, etc.) is skipped. The first non-blank, non-`#` line
 * following an `#EXTINF` line is taken as that entry's stream URL.
 */
object M3uParser {

    private val ATTRIBUTE_REGEX = Regex("""([A-Za-z0-9_-]+)="([^"]*)"""")

    fun parse(content: String): List<ParsedM3uEntry> {
        val entries = mutableListOf<ParsedM3uEntry>()
        val lines = content.split("\r\n", "\n", "\r")

        var pendingName: String? = null
        var pendingLogo: String? = null
        var pendingGroup: String? = null

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                val lastCommaIndex = line.lastIndexOf(',')
                pendingName = if (lastCommaIndex >= 0) {
                    line.substring(lastCommaIndex + 1).trim()
                } else {
                    ""
                }

                val attributesPart = if (lastCommaIndex >= 0) {
                    line.substring(0, lastCommaIndex)
                } else {
                    line
                }
                val attributes = ATTRIBUTE_REGEX.findAll(attributesPart)
                    .associate { it.groupValues[1].lowercase() to it.groupValues[2] }
                pendingLogo = attributes["tvg-logo"]?.takeIf { it.isNotBlank() }
                pendingGroup = attributes["group-title"]?.takeIf { it.isNotBlank() }
                continue
            }

            if (line.startsWith("#")) {
                // Other metadata line (#EXTM3U, #EXTGRP, #EXTVLCOPT, ...) - not needed.
                continue
            }

            // Non-comment line: this is a stream URL.
            val name = pendingName
            if (name != null) {
                entries.add(
                    ParsedM3uEntry(
                        name = name,
                        logoUrl = pendingLogo,
                        groupTitle = pendingGroup,
                        streamUrl = line,
                    )
                )
                pendingName = null
                pendingLogo = null
                pendingGroup = null
            }
            // A stream URL with no preceding #EXTINF is not a valid entry; ignore it.
        }

        return entries
    }
}
