package weddellseal.markrecap.frameworks.room

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Confirms exported `19.json` still matches the entity classes. Run on a device
 * or emulator; JVM coverage of open/reopen is [AppDatabaseMigrationTest].
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseSchemaMigrationInstrumentedTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun exportedV19Schema_matchesEntities() {
        helper.createDatabase(TEST_DB, AppDatabaseMigrations.BASELINE_VERSION).use { db ->
            db.execSQL(INSERT_V19_OBSERVATION)
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            AppDatabaseMigrations.BASELINE_VERSION,
            true,
            *AppDatabaseMigrations.ALL,
        ).use { migrated ->
            migrated.query("SELECT speno, tag_id_one FROM observationLogs").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("1", cursor.getString(0))
                assertEquals("A1001", cursor.getString(1))
            }
        }
    }

    private companion object {
        const val TEST_DB = "migration-schema-v19"

        const val INSERT_V19_OBSERVATION = """
            INSERT INTO observationLogs (
                device_id, season, speno, date, time, census_id,
                latitude, longitude, age_class, sex, num_relatives,
                old_tag_id_one, old_tag_id_two, tag_id_one, tag_one_indicator,
                tag_id_two, tag_two_indicator, rel_tag_id_one, rel_tag_id_two,
                seal_condition, observer_initials, flagged_entry, tag_event,
                weight, tissue_sampled, comments, colony, retag_reason,
                insertedAt
            ) VALUES (
                'dev', '2026', '1', '2026-01-01', '12:00:00', '',
                '0', '0', 'A', 'F', '0',
                '', '', 'A1001', '',
                '', '', '', '',
                '', 'ABC', '', 'TAG',
                '', '', '', '', '',
                0
            )
            """
    }
}
