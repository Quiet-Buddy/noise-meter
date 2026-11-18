package com.quietbuddy.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Builds a database exactly as an older version shipped it (from its schema in app/schemas), then opens it with
 * today's Room setup. Room runs the migrations and then checks every table against the current entities, so a
 * missing or wrong migration fails here instead of on testers' phones. Add a test with every new version.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DatabaseMigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "migration-test.db"

    /** Creates [dbName] with the tables, identity hash and user_version of schema [version]. */
    private fun createOldDatabase(version: Int, fill: (SQLiteDatabase) -> Unit = {}) {
        val schema = JSONObject(File("schemas/com.quietbuddy.data.QuietBuddyDatabase/$version.json").readText())
            .getJSONObject("database")
        val file = context.getDatabasePath(dbName).apply { parentFile?.mkdirs(); delete() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            db.version = version
            fill(db)
        }
    }

    /** Opens [dbName] the way DataModule does. */
    private fun openWithRoom(): QuietBuddyDatabase =
        Room.databaseBuilder(context, QuietBuddyDatabase::class.java, dbName)
            .addMigrations(MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

    @Test
    fun v1To2KeepsMinuteHistoryAndDropsTheChallenge() {
        createOldDatabase(1) { db ->
            db.execSQL(
                "INSERT INTO level_minute (minuteStartMs, laeqDb, minDb, maxDb, dominantMood, listeningSeconds) " +
                    "VALUES (60000, 48.5, 40.0, 61.0, 'FINE', 60)"
            )
        }

        val room = openWithRoom()
        val db = room.openHelper.writableDatabase // runs the migration and validates the result
        assertEquals(2, db.version)
        db.query("SELECT minuteStartMs, laeqDb, dominantMood, listeningSeconds FROM level_minute").use { c ->
            assertEquals(1, c.count)
            c.moveToFirst()
            assertEquals(60_000L, c.getLong(0))
            assertEquals(48.5f, c.getFloat(1))
            assertEquals("FINE", c.getString(2))
            assertEquals(60, c.getInt(3))
        }
        db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'challenge'").use { c ->
            assertFalse(c.moveToFirst())
        }
        room.close()
    }

    @Test
    fun currentSchemaFileMatchesTheEntities() {
        // A database built from the newest checked-in schema must open without any migration. If this fails,
        // the entities changed without a version bump (or the schema file wasn't regenerated and committed).
        createOldDatabase(QUIET_BUDDY_DB_VERSION)
        val room = openWithRoom()
        assertEquals(QUIET_BUDDY_DB_VERSION, room.openHelper.writableDatabase.version)
        room.close()
    }

    private companion object {
        const val QUIET_BUDDY_DB_VERSION = 2
    }
}
