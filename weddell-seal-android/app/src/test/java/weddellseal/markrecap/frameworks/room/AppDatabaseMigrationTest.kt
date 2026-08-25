package weddellseal.markrecap.frameworks.room

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import weddellseal.markrecap.TestFixtures

/**
 * Play 1.0 baseline: pre-19 sideloads still open by wiping; a v19 observation
 * survives reopen. Schema JSON matching is covered by
 * [AppDatabaseSchemaMigrationInstrumentedTest].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppDatabaseMigrationTest {

    @Test
    fun preBaselineSideload_opensByRecreating() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(PRE_BASELINE_DB)

        context.openOrCreateDatabase(PRE_BASELINE_DB, Context.MODE_PRIVATE, null).use { sqlite ->
            sqlite.execSQL("PRAGMA user_version = 18")
            sqlite.execSQL("CREATE TABLE leftover (id INTEGER PRIMARY KEY)")
            sqlite.execSQL("INSERT INTO leftover (id) VALUES (1)")
        }

        val db = Room.databaseBuilder(context, AppDatabase::class.java, PRE_BASELINE_DB)
            .configureAppMigrations()
            .build()
        try {
            assertEquals(
                AppDatabaseMigrations.BASELINE_VERSION,
                db.openHelper.readableDatabase.version,
            )
            db.openHelper.readableDatabase
                .query("SELECT name FROM sqlite_master WHERE type='table' AND name='leftover'")
                .use { cursor ->
                    assertFalse(cursor.moveToFirst())
                }
            runBlocking {
                assertEquals(0, db.observationDao().getCount())
            }
        } finally {
            db.close()
            context.deleteDatabase(PRE_BASELINE_DB)
        }
    }

    @Test
    fun baselineReopen_preservesObservation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(BASELINE_DB)

        runBlocking {
            val first = Room.databaseBuilder(context, AppDatabase::class.java, BASELINE_DB)
                .configureAppMigrations()
                .build()
            try {
                first.observationDao().upsert(TestFixtures.minimalObservationRecord())
                assertEquals(1, first.observationDao().getCount())
            } finally {
                first.close()
            }

            val second = Room.databaseBuilder(context, AppDatabase::class.java, BASELINE_DB)
                .configureAppMigrations()
                .build()
            try {
                assertEquals(1, second.observationDao().getCount())
            } finally {
                second.close()
            }
        }

        context.deleteDatabase(BASELINE_DB)
    }

    private companion object {
        const val PRE_BASELINE_DB = "pre-baseline-v18"
        const val BASELINE_DB = "baseline-v19"
    }
}
