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

    private const val LOGO_ATTRIBUTE = "tvg-logo=\""
    private const val GROUP_ATTRIBUTE = "group-title=\""

    /**
     * Parses [lines] into a lazy sequence: an entry is produced as its lines are read, and
     * nothing accumulates here.
     *
     * A provider playlist can carry hundreds of thousands of entries, so whoever consumes this
     * decides what to keep - collecting them all into a list first is what exhausts the heap.
     */
    fun entries(lines: Sequence<String>): Sequence<ParsedM3uEntry> = sequence {
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

                // Two named attributes are all this reads, so it reads exactly those. Matching
                // every attribute with a regex and collecting them into a map cost some twenty
                // throwaway objects per entry, which on a playlist of a hundred thousand entries
                // is most of what the parser spent its time on.
                val attributesEnd = if (lastCommaIndex >= 0) lastCommaIndex else line.length
                pendingLogo = attributeValue(line, attributesEnd, LOGO_ATTRIBUTE)
                pendingGroup = attributeValue(line, attributesEnd, GROUP_ATTRIBUTE)
                continue
            }

            if (line.startsWith("#")) {
                // Other metadata line (#EXTM3U, #EXTGRP, #EXTVLCOPT, ...) - not needed.
                continue
            }

            // Non-comment line: this is a stream URL.
            val name = pendingName
            if (name != null) {
                yield(
                    ParsedM3uEntry(
                        name = name,
                        logoUrl = pendingLogo,
                        groupTitle = pendingGroup,
                        streamUrl = line,
                    ),
                )
                pendingName = null
                pendingLogo = null
                pendingGroup = null
            }
            // A stream URL with no preceding #EXTINF is not a valid entry; ignore it.
        }
    }

    /**
     * The value of [attribute] in the part of [line] before [attributesEnd], or null when it is
     * absent or empty. Case-insensitive because playlists are not consistent about it.
     */
    private fun attributeValue(line: String, attributesEnd: Int, attribute: String): String? {
        val nameStart = line.indexOf(attribute, ignoreCase = true)
        if (nameStart < 0 || nameStart >= attributesEnd) return null
        val valueStart = nameStart + attribute.length
        val valueEnd = line.indexOf('"', valueStart)
        if (valueEnd < 0 || valueEnd > attributesEnd) return null
        return line.substring(valueStart, valueEnd).takeIf { it.isNotBlank() }
    }
}
