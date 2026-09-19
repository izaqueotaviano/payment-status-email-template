package com.iptvtv.player.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.iptvtv.player.data.local.db.dao.ChannelDao
import com.iptvtv.player.data.local.db.dao.SourceDao
import com.iptvtv.player.data.local.db.entities.ChannelEntity
import com.iptvtv.player.data.local.db.entities.SourceEntity

@Database(
    entities = [SourceEntity::class, ChannelEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao
    abstract fun channelDao(): ChannelDao

    companion object {
        /** Adds the import stamp. Existing rows keep 0, which the next import overwrites. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE channels ADD COLUMN syncStamp INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * Replaces the two single-column channel indices with the composites the list and the
         * import actually read by, and records when a source was last imported so opening the
         * channel list no longer has to re-import the playlist to find out.
         *
         * Existing sources keep lastSyncedAt = 0 deliberately: they are due one more import, the
         * one that drops the films and series a previous version stored as channels.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sources ADD COLUMN lastSyncedAt INTEGER NOT NULL DEFAULT 0")

                // (sourceId, streamKey) becomes unique, so any duplicate a previous import left
                // behind has to go before the index can be created. The lowest id survives - it is
                // the one the app has been showing - but the user's edits can be on either copy, so
                // favorite and hidden are folded onto the survivor first.
                db.execSQL(
                    """
                    UPDATE channels SET isFavorite = 1 WHERE id IN (
                        SELECT MIN(id) FROM channels
                        GROUP BY sourceId, streamKey HAVING MAX(isFavorite) = 1
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    UPDATE channels SET isHidden = 1 WHERE id IN (
                        SELECT MIN(id) FROM channels
                        GROUP BY sourceId, streamKey HAVING MIN(isHidden) = 1
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    DELETE FROM channels WHERE id NOT IN (
                        SELECT MIN(id) FROM channels GROUP BY sourceId, streamKey
                    )
                    """.trimIndent(),
                )

                db.execSQL("DROP INDEX IF EXISTS index_channels_sourceId")
                db.execSQL("DROP INDEX IF EXISTS index_channels_streamKey")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_channels_sourceId_sortOrder " +
                        "ON channels (sourceId, sortOrder)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_channels_sourceId_streamKey " +
                        "ON channels (sourceId, streamKey)",
                )
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "iptv.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    // An import is thousands of writes. Left to AUTOMATIC this ends up on the
                    // truncate journal on a low-RAM TV box, which fsyncs the whole database file
                    // on every transaction.
                    .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                    .build()
                    .also { instance = it }
            }
    }
}
