package weddellseal.markrecap.frameworks.room

import androidx.room.RoomDatabase
import androidx.room.migration.Migration

/**
 * Play 1.0 baseline is schema version [BASELINE_VERSION].
 *
 * Sideloads older than that still wipe and recreate. Version 19 and later must
 * have a [Migration] (or AutoMigration) or Room will crash on open instead of
 * deleting the season.
 *
 * Next schema change: bump `@Database(version)`, add `MIGRATION_19_20` (or
 * AutoMigration), append it to [ALL], export `20.json`, and add a
 * `MigrationTestHelper` test that a v19 row survives.
 */
object AppDatabaseMigrations {
    const val BASELINE_VERSION = 19

    val DESTRUCTIVE_FROM: IntArray = IntArray(BASELINE_VERSION - 1) { it + 1 }

    val ALL: Array<Migration> = emptyArray()
}

fun RoomDatabase.Builder<AppDatabase>.configureAppMigrations():
    RoomDatabase.Builder<AppDatabase> =
    addMigrations(*AppDatabaseMigrations.ALL)
        .fallbackToDestructiveMigrationFrom(
            true,
            *AppDatabaseMigrations.DESTRUCTIVE_FROM,
        )
