package tech.salev.optimum.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Room Migration testleri.
 *
 * Her test bir önceki schema sürümünden bir sonrakine geçişi doğrular.
 * MigrationTestHelper, şema export dosyalarını (app/schemas/) kullanarak
 * migration öncesi DB'yi oluşturur; migration sonrası Room'un schema
 * beklentisiyle karşılaştırır.
 *
 * Nasıl çalıştırılır:
 *   ./gradlew connectedAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class OptimumDatabaseMigrationTest {

    companion object {
        private const val TEST_DB = "migration-test"
    }

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        OptimumDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migration_1_2_createsDailyEvaluationsTable() {
        helper.createDatabase(TEST_DB, 1).close()

        val db = helper.runMigrationsAndValidate(
            TEST_DB, 2, true,
            OptimumDatabase.MIGRATION_1_2
        )

        val cursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='daily_evaluations'"
        )
        assertTrue("daily_evaluations tablosu olusturulmali", cursor.count > 0)
        cursor.close()
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migration_3_4_addsShortCodeAndTimestamp() {
        helper.createDatabase(TEST_DB, 3).close()

        val db = helper.runMigrationsAndValidate(
            TEST_DB, 4, true,
            OptimumDatabase.MIGRATION_3_4
        )

        val cursor = db.query("PRAGMA table_info(activity_items)")
        val columns = mutableListOf<String>()
        while (cursor.moveToNext()) {
            columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
        }
        cursor.close()
        assertTrue("shortCode kolonu eklenmis olmali", columns.contains("shortCode"))
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migration_6_7_addsDisplayOrder() {
        helper.createDatabase(TEST_DB, 6).close()

        val db = helper.runMigrationsAndValidate(
            TEST_DB, 7, true,
            OptimumDatabase.MIGRATION_6_7
        )

        val catCursor = db.query("PRAGMA table_info(categories)")
        val catColumns = mutableListOf<String>()
        while (catCursor.moveToNext()) {
            catColumns.add(catCursor.getString(catCursor.getColumnIndexOrThrow("name")))
        }
        catCursor.close()
        assertTrue("categories.displayOrder kolonu eklenmis olmali", catColumns.contains("displayOrder"))

        val actCursor = db.query("PRAGMA table_info(activity_items)")
        val actColumns = mutableListOf<String>()
        while (actCursor.moveToNext()) {
            actColumns.add(actCursor.getString(actCursor.getColumnIndexOrThrow("name")))
        }
        actCursor.close()
        assertTrue("activity_items.displayOrder kolonu eklenmis olmali", actColumns.contains("displayOrder"))
        db.close()
    }

    /**
     * KRITIK TEST — Onceki cokusu onleyecek olan kontrol.
     *
     * Room, @Index("date") annotasyonundan otomatik olarak "index_time_slot_logs_date"
     * ismini uretir. Migration'da olusturulan index ismi bu isimle tam eslesmelidir;
     * aksinde IllegalStateException firlatir ve uygulama acilmaz.
     */
    @Test
    @Throws(IOException::class)
    fun migration_7_8_addsCorrectIndexName() {
        helper.createDatabase(TEST_DB, 7).close()

        // validateDroppedTables = true: Room beklenen schema ile tam dogrulama yapar
        val db = helper.runMigrationsAndValidate(
            TEST_DB, 8, true,
            OptimumDatabase.MIGRATION_7_8
        )

        val cursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='time_slot_logs'"
        )
        val indexNames = mutableListOf<String>()
        while (cursor.moveToNext()) {
            indexNames.add(cursor.getString(0))
        }
        cursor.close()

        assertTrue(
            "Index 'index_time_slot_logs_date' adiyla olusturulmali. Bulunan: $indexNames",
            indexNames.contains("index_time_slot_logs_date")
        )
        db.close()
    }

    /**
     * Tam zincir testi: surüm 1'den guncel surüme (8) kadar tum migration'lar.
     * Cok eski surumden guncelleme yapan kullanici senaryosunu simule eder.
     */
    @Test
    @Throws(IOException::class)
    fun fullMigrationChain_1_to_8_isValid() {
        helper.createDatabase(TEST_DB, 1).close()

        helper.runMigrationsAndValidate(
            TEST_DB, 8, true,
            *OptimumDatabase.ALL_MIGRATIONS
        ).close()
    }

    /**
     * Surüm 3'ten (schemas klasoründeki en eski export) guncel surume gecis.
     */
    @Test
    @Throws(IOException::class)
    fun migrationChain_3_to_8_isValid() {
        helper.createDatabase(TEST_DB, 3).close()

        helper.runMigrationsAndValidate(
            TEST_DB, 8, true,
            OptimumDatabase.MIGRATION_3_4,
            OptimumDatabase.MIGRATION_4_5,
            OptimumDatabase.MIGRATION_5_6,
            OptimumDatabase.MIGRATION_6_7,
            OptimumDatabase.MIGRATION_7_8
        ).close()
    }
}
