# Room schema and migrations

Census observations live in a Room database named `observations_database`.
Changing an entity class without a version bump and a migration will either
fail the build or crash on open. Do not put
`fallbackToDestructiveMigration()` back on — that drops every table and
deletes the season.

CSV import/export does **not** go through this path. Room still stores the
rows; Admin export still writes them through
`ObservationRepository.writeDataToStream`. If you add a column that should
appear in the CSV, update that writer (and any import parser) as a separate
step.

## Current baseline (Play 1.0)

| Item | Value |
|------|--------|
| Database class | `weddellseal.markrecap.frameworks.room.AppDatabase` |
| File name | `observations_database` |
| Version | **19** |
| Schema JSON | `app/schemas/weddellseal.markrecap.frameworks.room.AppDatabase/19.json` |
| Tables | `observationLogs`, `wedCheck`, `sealColonies`, `observers`, `fileUploads` |

`exportSchema` is true. KSP writes JSON into this directory:

```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```

`AppDatabase` calls `configureAppMigrations()`:

- `addMigrations(*AppDatabaseMigrations.ALL)` — currently empty; 19 is the
  baseline, so there is no 18→19 migration to keep.
- `fallbackToDestructiveMigrationFrom(true, 1..18)` — pre-19 sideloads still
  wipe and recreate. **Version 19 and later will crash on open** if a
  migration is missing, instead of deleting the season.

## When you must bump the version

Bump `AppDatabase` version for any of these:

- Add, remove, or rename a column, index, or table
- Change a column type or nullability
- Change a `ForeignKey` or `@TypeConverter` that affects stored values

Query-only DAO changes do not need a version bump.

## How to change the schema

Worked example: add a column, taking 19 → 20. Substitute the real from/to
versions.

1. Edit the entity (`ObservationRecord`, `WedCheckRecord`, `SealColony`,
   `Observers`, or `FileUploadEntity`).
2. Set `@Database(version = 20)` on `AppDatabase`.
3. Add a migration (auto or handwritten — see below).
4. Compile so Room exports the new JSON:

   ```bash
   cd weddell-seal-android
   ./gradlew :app:kspDebugKotlin
   ```

5. Confirm `.../AppDatabase/20.json` exists. Commit it with the code change.
6. Append the new `Migration` to `AppDatabaseMigrations.ALL`.
7. Extend `AppDatabaseSchemaMigrationInstrumentedTest` so a v19 row survives
   the migration to 20. Do **not** add `.fallbackToDestructiveMigration()`.

If you change entities and forget to bump `version`, KSP fails because the
identity hash no longer matches `19.json`. That is intentional.

## Auto-migration vs handwritten `Migration`

**Auto-migration** is enough for additive changes Room can infer (new table,
new column with a default). It requires the exported JSON for both versions:

```kotlin
@Database(
    entities = [/* unchanged list */],
    version = 20,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 19, to = 20)]
)
```

**Handwritten `Migration`** is required for renames, drops, type changes, or
data backfills:

```kotlin
val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE observationLogs ADD COLUMN pupCount TEXT NOT NULL DEFAULT ''"
        )
    }
}

// Register it in AppDatabaseMigrations.ALL. configureAppMigrations() already
// calls addMigrations(*ALL) on the production builder.
```

Keep every historical `Migration(from, to)` registered. A tablet may skip
releases (19 → 22). Room will chain 19→20→21→22 if each step is present.

## Testing a migration

`androidx.room:room-testing` is already a dependency. JVM tests cover pre-19
wipe and v19 reopen (`AppDatabaseMigrationTest`). Instrumented tests load JSON
from this folder via `androidTest` assets and validate `19.json`
(`AppDatabaseSchemaMigrationInstrumentedTest`).

```kotlin
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun migrate19To20_preservesObservation() {
        helper.createDatabase("observations_database", 19).apply {
            execSQL(
                """
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
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate("observations_database", 20, true, MIGRATION_19_20)
    }
}
```

For `AutoMigration`, pass no extra `Migration` objects to
`runMigrationsAndValidate` — Room uses the exported schemas.

Run with a device or emulator:

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=weddellseal.markrecap.frameworks.room.AppDatabaseSchemaMigrationInstrumentedTest
```

## Rules

- Commit every new `N.json`. Auto-migrations and `MigrationTestHelper` need it.
- Do not edit an already-shipped JSON file. Export a new version instead.
- Do not restore `fallbackToDestructiveMigration()`. Pre-19 wipes stay
  limited to `fallbackToDestructiveMigrationFrom(1..18)`.
- Do not ship an entity change in a Play update without a version bump,
  a migration, and a passing migration test.
